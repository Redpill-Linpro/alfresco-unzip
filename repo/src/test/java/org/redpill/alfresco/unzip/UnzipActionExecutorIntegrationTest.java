package org.redpill.alfresco.unzip;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class UnzipActionExecutorIntegrationTest extends AbstractUnzipIntegrationTest {

  private static final String ADMIN_USER_NAME = "admin";

  @Autowired
  @Qualifier("ActionService")
  protected ActionService _actionService;

  @Test(expected = InvalidNodeRefException.class)
  public void testFailed1() {

    AuthenticationUtil.runAs(new RunAsWork<Void>() {

      @Override
      public Void doWork() throws Exception {
        SiteInfo site = createSite("site-dashboard");

        try {
          File file = createTestZipFile();
          
          String filename = "documents.zip";

          FileInfo zipFile = uploadDocument(site, filename, new FileInputStream(file));

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

  @Test(expected = IllegalArgumentException.class)
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
          File file = createTestZipFile();
          
          String filename = "documents.zip";

          FileInfo zipFile = uploadDocument(site, filename, new FileInputStream(file));

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
          
          @SuppressWarnings("unchecked")
          List<NodeRef> result = (List<NodeRef>) action.getParameterValue(UnzipActionExecutor.PARAM_RESULT);

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
          Assert.assertEquals("Number of unzipped files differ from the expected result", EXPECTED_NUMBER_OF_FILES, result.size());
        } finally {
          deleteSite(site);
        }

        return null;
      }

    }, ADMIN_USER_NAME);

  }

}
