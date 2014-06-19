package org.redpill.alfresco.unzip;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class UnzipActionExecutorIntegrationTest extends AbstractRepoIntegrationTest {

  private static final String ADMIN_USER_NAME = "admin";

  @Autowired
  @Qualifier("ActionService")
  protected ActionService _actionService;

  @Test(expected = AlfrescoRuntimeException.class)
  public void testFailed1() {

    AuthenticationUtil.runAs(new RunAsWork<Void>() {

      @Override
      public Void doWork() throws Exception {
        SiteInfo site = createSite("site-dashboard");

        try {
          String filename = "documents.zip";

          FileInfo zipFile = uploadDocument(site, filename);

          Action action = _actionService.createAction("unzip");

          action.setParameterValue("destination-folder", new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "foobar"));

          _actionService.executeAction(action, zipFile.getNodeRef());
        } finally {
          deleteSite(site);
        }

        return null;
      }

    }, ADMIN_USER_NAME);

  }

  @Test(expected = AlfrescoRuntimeException.class)
  public void testFailed2() {

    AuthenticationUtil.runAs(new RunAsWork<Void>() {

      @Override
      public Void doWork() throws Exception {
        SiteInfo site = createSite("site-dashboard");

        try {
          NodeRef documentLibrary = _siteService.getContainer(site.getShortName(), SiteService.DOCUMENT_LIBRARY);

          Action action = _actionService.createAction("unzip");

          action.setParameterValue("destination-folder", documentLibrary);

          _actionService.executeAction(action, null);
        } finally {
          deleteSite(site);
        }

        return null;
      }

    }, ADMIN_USER_NAME);

  }

  @Test
  public void testSuccess() {

    AuthenticationUtil.runAs(new RunAsWork<Void>() {

      @Override
      public Void doWork() throws Exception {
        SiteInfo site = createSite("site-dashboard");

        try {
          String filename = "documents.zip";

          FileInfo zipFile = uploadDocument(site, filename);

          NodeRef documentLibrary = _siteService.getContainer(site.getShortName(), SiteService.DOCUMENT_LIBRARY);

          Action action = _actionService.createAction("unzip");
          action.setExecuteAsynchronously(true);
          action.setTrackStatus(true);
          action.setParameterValue("destination-folder", documentLibrary);

          _actionService.executeAction(action, zipFile.getNodeRef());

          while (action.getExecutionEndDate() == null) {
            Thread.sleep(1000);
            System.out.println("Sleeping . . . (" + action.getExecutionStatus() + ")");
          }

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
