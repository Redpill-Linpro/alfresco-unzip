package org.redpill.alfresco.unzip;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.site.SiteModel;
import org.alfresco.repo.workflow.WorkflowModel;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.workflow.WorkflowDefinition;
import org.alfresco.service.cmr.workflow.WorkflowPath;
import org.alfresco.service.namespace.QName;
import org.junit.Test;
import org.springframework.extensions.surf.util.I18NUtil;

public class UnzipDocumentWorkflowIntegrationTest extends AbstractUnzipIntegrationTest {

  private static final String PROCESS_DEFINITION_ID = "activiti$unzipDocument";

  private static SiteInfo site;
  private static String siteManagerUser;
  private static String siteConsumerUser;

  @Override
  public void beforeClassSetup() {
    super.beforeClassSetup();

    Locale locale = new Locale("sv");
    I18NUtil.setLocale(locale);

    // Setup authentication
    _authenticationComponent.setCurrentUser(AuthenticationUtil.getAdminUserName());

    // Create a site
    site = createSite();
    assertNotNull(site);

    // Create a user
    siteManagerUser = "sitemanager" + System.currentTimeMillis();
    createUser(siteManagerUser);
    siteConsumerUser = "siteconsumer" + System.currentTimeMillis();
    createUser(siteConsumerUser);

    // Make user the site manager of the site
    _siteService.setMembership(site.getShortName(), siteManagerUser, SiteModel.SITE_MANAGER);

    _siteService.setMembership(site.getShortName(), siteConsumerUser, SiteModel.SITE_CONSUMER);

    // Run the tests as this user
    _authenticationComponent.setCurrentUser(siteManagerUser);
  }

  @Override
  public void afterClassSetup() {
    _authenticationComponent.setCurrentUser(AuthenticationUtil.getAdminUserName());

    deleteSite(site);

    _personService.deletePerson(siteManagerUser);

    if (_authenticationService.authenticationExists(siteManagerUser)) {
      _authenticationService.deleteAuthentication(siteManagerUser);
    }

    _authenticationComponent.clearCurrentSecurityContext();
  }

  protected String start(final Map<QName, Serializable> properties) {
    // Start workflow as internal
    _authenticationComponent.setCurrentUser(siteManagerUser);

    WorkflowDefinition wfDefinition = _workflowService.getDefinitionByName(PROCESS_DEFINITION_ID);

    WorkflowPath wfPath = _workflowService.startWorkflow(wfDefinition.getId(), properties);

    assertNotNull(wfPath);

    return wfPath.getId();
  }

  @Test
  public void testStartSuccess() throws InterruptedException, FileNotFoundException {
    File file = createTestZipFile();

    String filename = "documents.zip";

    FileInfo zipFile = uploadDocument(site, filename, new FileInputStream(file));

    NodeRef workflowPackage = _workflowService.createPackage(null);

    Map<QName, Serializable> workflowProps = new HashMap<QName, Serializable>();

    workflowProps.put(WorkflowModel.ASSOC_PACKAGE, workflowPackage);

    workflowProps.put(WorkflowModel.ASSOC_ASSIGNEE, AuthenticationUtil.getAdminUserName());

    ChildAssociationRef childAssoc = _nodeService.getPrimaryParent(zipFile.getNodeRef());

    _nodeService.addChild(workflowPackage, zipFile.getNodeRef(), WorkflowModel.ASSOC_PACKAGE_CONTAINS, childAssoc.getQName());

    // start the workflow
    start(workflowProps);

    Thread.sleep(5000);

    List<AssociationRef> list = _nodeService.getSourceAssocs(zipFile.getNodeRef(), UnzipModel.ASSOC_ZIP_DOCUMENT);

    assertEquals(6, list.size());
  }

  @Test
  public void testStartFailure() throws InterruptedException {
    String filename = "broken_documents.zip";

    FileInfo zipFile = uploadDocument(site, filename);

    NodeRef workflowPackage = _workflowService.createPackage(null);

    Map<QName, Serializable> workflowProps = new HashMap<QName, Serializable>();

    workflowProps.put(WorkflowModel.ASSOC_PACKAGE, workflowPackage);

    workflowProps.put(WorkflowModel.ASSOC_ASSIGNEE, AuthenticationUtil.getAdminUserName());

    ChildAssociationRef childAssoc = _nodeService.getPrimaryParent(zipFile.getNodeRef());

    _nodeService.addChild(workflowPackage, zipFile.getNodeRef(), WorkflowModel.ASSOC_PACKAGE_CONTAINS, childAssoc.getQName());

    start(workflowProps);

    Thread.sleep(5000);

    List<AssociationRef> list = _nodeService.getSourceAssocs(zipFile.getNodeRef(), UnzipModel.ASSOC_ZIP_DOCUMENT);

    assertEquals(0, list.size());
  }

}
