package org.redpill.alfresco.unzip;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class UnzipServiceImplIntegrationTest extends AbstractUnzipIntegrationTest {

  private static final String ADMIN_USER_NAME = "admin";

  @Autowired
  @Qualifier("rl.unzipService")
  protected UnzipService _unzipService;

  @Test
  public void testImportZip() {

    AuthenticationUtil.runAs(new RunAsWork<Void>() {

      @Override
      public Void doWork() throws Exception {
        SiteInfo site = createSite("site-dashboard");

        try {
          File file = createTestZipFile();
          
          String filename = "documents.zip";

          FileInfo zipFile = uploadDocument(site, filename, new FileInputStream(file));

          NodeRef documentLibrary = _siteService.getContainer(site.getShortName(), SiteService.DOCUMENT_LIBRARY);

          List<NodeRef> result = _unzipService.importZip(zipFile.getNodeRef(), documentLibrary);
          
          NodeRef zipFileNodeRef = _nodeService.getChildByName(documentLibrary, ContentModel.ASSOC_CONTAINS, filename);
          NodeRef file1 = _nodeService.getChildByName(documentLibrary, ContentModel.ASSOC_CONTAINS, "file1.txt");
          NodeRef file2 = _nodeService.getChildByName(documentLibrary, ContentModel.ASSOC_CONTAINS, "file2.txt");
          NodeRef folder1 = _nodeService.getChildByName(documentLibrary, ContentModel.ASSOC_CONTAINS, "folder1");

          for (NodeRef document : result) {
            Assert.assertNotEquals(zipFileNodeRef.toString(), document.toString());
            Assert.assertNotEquals(folder1.toString(), document.toString());
          }

          Assert.assertTrue(zipFile.getNodeRef().equals(zipFileNodeRef));
          Assert.assertNotNull(zipFileNodeRef);
          Assert.assertNotNull(file1);
          Assert.assertNotNull(file2);
          Assert.assertNotNull(folder1);
          Assert.assertEquals("Number of unzipped files differ from the expected result", 6, result.size());
        } finally {
          deleteSite(site);
        }

        return null;
      }

    }, ADMIN_USER_NAME);

  }

}
