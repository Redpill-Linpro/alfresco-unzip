/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * Copyright (C) 2013-2014 Redpill Linpro AB
 *
 * This file is part of Unzip Here module for Alfresco
 *
 * Unzip Here module for Alfresco is free software:
 * you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Unzip Here module for Alfresco is distributed in the
 * hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Unzip Here module for Alfresco.
 * If not, see <http://www.gnu.org/licenses/>.
 */
package org.redpill_linpro.alfresco.webscripts.unzip;

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
import org.alfresco.util.TempFileProvider;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.util.Assert;

/**
 * WebScript for handling "unzip" requests for files
 * 
 * @author Marcus Svensson <marcus.svensson@redpill-linpro.com>
 * 
 */
public class UnzipWebScript extends DeclarativeWebScript implements InitializingBean {
  private static final Logger LOG = Logger.getLogger(UnzipWebScript.class);
  private NodeService nodeService;
  private FileFolderService fileFolderService;
  private ContentService contentService;

  private static final int BUFFER_SIZE = 16384;
  private static final String TEMP_FILE_PREFIX = "alf";
  private static final String TEMP_FILE_SUFFIX_ZIP = ".zip";

  @Override
  protected Map<String, Object> executeImpl(final WebScriptRequest req, final Status status, final Cache cache) {
    Map<String, Object> result = new HashMap<String, Object>();
    if (LOG.isTraceEnabled()) {
      LOG.trace("Entering executeImpl...");
    }

    String source = req.getParameter("source");
    String target = req.getParameter("target");

    if (LOG.isTraceEnabled()) {
      LOG.trace("Source: " + source + ", Target: " + target);
    }

    if (source == null) {
      status.setCode(400);
      status.setMessage("unzip.exception.source.null");
      return result;
    } else if (target == null) {
      status.setCode(400);
      status.setMessage("unzip.exception.target.null");
      return result;
    } else if (!NodeRef.isNodeRef(source)) {
      status.setCode(400);
      status.setMessage("unzip.exception.source.nodeRef");
      return result;
    } else if (!NodeRef.isNodeRef(target)) {
      status.setCode(400);
      status.setMessage("unzip.exception.target.nodeRef");
      return result;
    }

    NodeRef sourceNodeRef = new NodeRef(source);
    NodeRef targetNodeRef = new NodeRef(target);

    if (!nodeService.exists(sourceNodeRef)) {
      status.setCode(404);
      status.setMessage("unzip.exception.source.notExists");
      return result;
    } else if (!nodeService.exists(sourceNodeRef)) {
      status.setCode(404);
      status.setMessage("unzip.exception.target.notExists");
      return result;
    } else {
      try {
        importZip(sourceNodeRef, targetNodeRef);
      } catch (AlfrescoRuntimeException re) {
        Throwable cause = re.getCause();
        if (cause instanceof FileExistsException) {
          status.setCode(500);
          status.setMessage("unzip.exception.target.alreadyExists");
          LOG.error(cause.getMessage(), cause);
          return result;
        } else {
          throw re;
        }
      }
    }

    if (LOG.isTraceEnabled()) {
      LOG.trace("Leaving executeImpl...");
    }
    return result;
  }

  public void importZip(final NodeRef zipFileNodeRef, final NodeRef importDestNodeRef) {
    if (this.nodeService.exists(zipFileNodeRef) == true) {
      // The node being passed in should be an Alfresco content package
      ContentReader reader = this.contentService.getReader(zipFileNodeRef, ContentModel.PROP_CONTENT);
      if (reader != null) {
        if (MimetypeMap.MIMETYPE_ZIP.equals(reader.getMimetype())) {
          // perform an import of a standard ZIP file
          ZipFile zipFile = null;
          File tempFile = null;
          try {
            tempFile = TempFileProvider.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX_ZIP);
            reader.getContent(tempFile);
            // NOTE: This encoding allows us to workaround bug:
            // http://bugs.sun.com/bugdatabase/view_bug.do;:WuuT?bug_id=4820807
            // We also try to use the extra encoding information if present
            // ALF-2016
            zipFile = new ZipFile(tempFile, "UTF-8", true);

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
              importDirectory(tempDir.getPath(), importDestNodeRef);
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
            if (zipFile != null) {
              try {
                zipFile.close();
              } catch (IOException e) {
                throw new AlfrescoRuntimeException("Failed to close zip package.", e);
              }
            }
          }
        }
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
          FileInfo fileInfo = this.fileFolderService.create(root, fileName, ContentModel.TYPE_CONTENT);
          NodeRef fileRef = fileInfo.getNodeRef();

          // add titled aspect for the read/edit properties screens
          Map<QName, Serializable> titledProps = new HashMap<QName, Serializable>(1, 1.0f);
          titledProps.put(ContentModel.PROP_TITLE, fileName);
          this.nodeService.addAspect(fileRef, ContentModel.ASPECT_TITLED, titledProps);

          // push the content of the file into the node
          InputStream contentStream = new BufferedInputStream(new FileInputStream(file), BUFFER_SIZE);
          ContentWriter writer = this.contentService.getWriter(fileRef, ContentModel.PROP_CONTENT, true);
          writer.guessMimetype(fileName);
          writer.putContent(contentStream);
        } else {
          // create a folder based on the folder name
          FileInfo folderInfo = this.fileFolderService.create(root, fileName, ContentModel.TYPE_FOLDER);
          NodeRef folderRef = folderInfo.getNodeRef();

          // add the uifacets aspect for the read/edit properties screens
          // Is this needed?
          // this.nodeService.addAspect(folderRef,
          // ApplicationModel.ASPECT_UIFACETS, null);

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

  public void setNodeService(NodeService nodeService) {
    this.nodeService = nodeService;
  }

  public void setFileFolderService(FileFolderService fileFolderService) {
    this.fileFolderService = fileFolderService;
  }

  public void setContentService(ContentService contentService) {
    this.contentService = contentService;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(nodeService, "NodeService must not be null");
    Assert.notNull(fileFolderService, "FileFolderService must not be null");
    Assert.notNull(contentService, "ContentService must not be null");
  }

}
