package org.redpill.alfresco.unzip;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.junit.Assert;
import org.junit.Test;
import org.redpill.alfresco.unzip.UnzipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class UnzipServiceImplIntegrationTest extends AbstractRepoIntegrationTest {

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
          String filename = "documents.zip";

          FileInfo zipFile = uploadDocument(site, filename);

          NodeRef documentLibrary = _siteService.getContainer(site.getShortName(), SiteService.DOCUMENT_LIBRARY);

          _unzipService.importZip(zipFile.getNodeRef(), documentLibrary);

          NodeRef zipFileNodeRef = _nodeService.getChildByName(documentLibrary, ContentModel.ASSOC_CONTAINS, filename);
          NodeRef file1 = _nodeService.getChildByName(documentLibrary, ContentModel.ASSOC_CONTAINS, "file1.txt");
          NodeRef file2 = _nodeService.getChildByName(documentLibrary, ContentModel.ASSOC_CONTAINS, "file2.txt");
          NodeRef folder1 = _nodeService.getChildByName(documentLibrary, ContentModel.ASSOC_CONTAINS, "folder1");

          Assert.assertTrue(zipFile.getNodeRef().equals(zipFileNodeRef));
          Assert.assertNotNull(zipFileNodeRef);
          Assert.assertNotNull(file1);
          Assert.assertNotNull(file2);
          Assert.assertNotNull(folder1);
        } finally {
          deleteSite(site);
        }

        return null;
      }

    }, ADMIN_USER_NAME);

  }

}
