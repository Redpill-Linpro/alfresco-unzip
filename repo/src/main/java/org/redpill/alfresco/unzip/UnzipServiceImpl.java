package org.redpill.alfresco.unzip;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.executer.ImporterActionExecuter;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.model.FileExistsException;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ParameterCheck;
import org.alfresco.util.TempFileProvider;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;

public class UnzipServiceImpl implements UnzipService, InitializingBean {

  private final static Logger LOG = Logger.getLogger(UnzipServiceImpl.class);

  private static final String TEMP_FILE_PREFIX = "alf";

  private static final String TEMP_FILE_SUFFIX_ZIP = ".zip";

  private static final String DEFAULT_ENCODING = "UTF-8";

  private String _encoding = DEFAULT_ENCODING;

  private static final int BUFFER_SIZE = 16384;

  private NodeService _nodeService;

  private ContentService _contentService;

  private FileFolderService _fileFolderService;

  @Override
  public void importZip(NodeRef zipFileNodeRef, NodeRef destinationFolderNodeRef) {
    if (!_nodeService.exists(zipFileNodeRef)) {
      return;
    }

    // The node being passed in should be an Alfresco content package
    ContentReader reader = _contentService.getReader(zipFileNodeRef, ContentModel.PROP_CONTENT);

    if (reader == null) {
      return;
    }

    if (!MimetypeMap.MIMETYPE_ZIP.equals(reader.getMimetype())) {
      return;
    }

    // perform an import of a standard ZIP file
    ZipFile zipFile = null;

    File tempFile = null;

    try {
      tempFile = TempFileProvider.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX_ZIP);

      reader.getContent(tempFile);

      zipFile = new ZipFile(tempFile, _encoding, true);

      // build a temp dir name based on the ID of the noderef we are
      // importing
      // also use the long life temp folder as large ZIP files can take a
      // while
      File alfTempDir = TempFileProvider.getLongLifeTempDir("import");

      File tempDir = new File(alfTempDir.getPath() + File.separatorChar + zipFileNodeRef.getId());

      try {
        // TODO: improve this code to directly pipe the zip stream output
        // into the repo objects -
        // to remove the need to expand to the filesystem first?
        ImporterActionExecuter.extractFile(zipFile, tempDir.getPath());

        importDirectory(tempDir.getPath(), destinationFolderNodeRef);
      } finally {
        ImporterActionExecuter.deleteDir(tempDir);
      }
    } catch (IOException ioErr) {
      throw new AlfrescoRuntimeException("Failed to import ZIP file.", ioErr);
    } finally {
      // now the import is done, delete the temporary file
      if (tempFile != null) {
        tempFile.delete();
      }

      ZipFile.closeQuietly(zipFile);
    }
  }

  /**
   * Recursively import a directory structure into the specified root node
   * 
   * @param dir
   *          The directory of files and folders to import
   * @param root
   *          The root node to import into
   */
  protected void importDirectory(String dir, NodeRef root) {
    importDirectory(dir, root, "");
  }

  protected void importDirectory(String dir, NodeRef root, String displayPath) {
    File topdir = new File(dir);

    for (File file : topdir.listFiles()) {
      try {
        String fileName = parseValidFileName(file.getName());

        if (LOG.isDebugEnabled()) {
          LOG.debug("Extracting " + displayPath + "/" + fileName);
        }

        if (file.isFile()) {
          // create content node based on the file name
          FileInfo fileInfo = _fileFolderService.create(root, fileName, ContentModel.TYPE_CONTENT);

          NodeRef fileRef = fileInfo.getNodeRef();

          // add titled aspect for the read/edit properties screens
          Map<QName, Serializable> titledProps = new HashMap<QName, Serializable>(1, 1.0f);

          titledProps.put(ContentModel.PROP_TITLE, fileName);

          _nodeService.addAspect(fileRef, ContentModel.ASPECT_TITLED, titledProps);

          // push the content of the file into the node
          InputStream contentStream = new BufferedInputStream(new FileInputStream(file), BUFFER_SIZE);

          ContentWriter writer = _contentService.getWriter(fileRef, ContentModel.PROP_CONTENT, true);

          writer.guessMimetype(fileName);

          writer.putContent(contentStream);
        } else {
          // create a folder based on the folder name
          FileInfo folderInfo = this._fileFolderService.create(root, fileName, ContentModel.TYPE_FOLDER);
          NodeRef folderRef = folderInfo.getNodeRef();

          importDirectory(file.getPath(), folderRef, displayPath + "/" + fileName);
        }
      } catch (FileNotFoundException e) {
        // TODO: add failed file info to status message?
        throw new AlfrescoRuntimeException("Failed to process ZIP file.", e);
      } catch (FileExistsException e) {
        // TODO: add failed file info to status message?
        throw new AlfrescoRuntimeException("Failed to process ZIP file.", e);
      }
    }
  }

  /**
   * Parses a valid filename
   * 
   * @param fileName
   * @return
   */
  public String parseValidFileName(String fileName) {
    fileName = fileName.trim();
    return fileName;
  }

  public void setNodeService(NodeService nodeService) {
    _nodeService = nodeService;
  }

  public void setContentService(ContentService contentService) {
    _contentService = contentService;
  }

  public void setFileFolderService(FileFolderService fileFolderService) {
    _fileFolderService = fileFolderService;
  }

  public void setEncoding(String encoding) {
    _encoding = encoding;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    ParameterCheck.mandatory("nodeService", _nodeService);
    ParameterCheck.mandatory("contentService", _contentService);
    ParameterCheck.mandatory("fileFolderService", _fileFolderService);
    ParameterCheck.mandatoryString("encoding", _encoding);
  }

}
