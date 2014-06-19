package org.redpill.alfresco.unzip;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.InputStream;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.site.SiteModel;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.MutableAuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.cmr.site.SiteVisibility;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.PropertyMap;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.tradeshift.test.remote.Remote;
import com.tradeshift.test.remote.RemoteTestRunner;

@RunWith(RemoteTestRunner.class)
@Remote(runnerClass = SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:alfresco/application-context.xml")
public abstract class AbstractRepoIntegrationTest {

  private final static String NAMESPACE_BEGIN = "" + QName.NAMESPACE_BEGIN;

  @Autowired
  @Qualifier("authenticationComponent")
  protected AuthenticationComponent _authenticationComponent;

  @Autowired
  @Qualifier("TransactionService")
  protected TransactionService _transactionService;

  protected RetryingTransactionHelper _transactionHelper;

  @Autowired
  @Qualifier("policyBehaviourFilter")
  protected BehaviourFilter _behaviourFilter;

  @Autowired
  @Qualifier("repositoryHelper")
  protected Repository _repository;

  @Autowired
  @Qualifier("SiteService")
  protected SiteService _siteService;

  @Autowired
  @Qualifier("AuthenticationService")
  protected MutableAuthenticationService _authenticationService;

  @Autowired
  @Qualifier("PersonService")
  protected PersonService _personService;

  @Autowired
  @Qualifier("NodeService")
  protected NodeService _nodeService;

  @Autowired
  @Qualifier("FileFolderService")
  protected FileFolderService _fileFolderService;

  @Autowired
  @Qualifier("NamespaceService")
  protected NamespaceService _namespaceService;

  @Autowired
  @Qualifier("ContentService")
  protected ContentService _contentService;

  @Before
  public void setup() {
    _transactionHelper = _transactionService.getRetryingTransactionHelper();
  }

  /**
   * Creates a user and a related person in the repository.
   * 
   * @param userId
   * @return
   */
  protected NodeRef createUser(String userId) {
    if (!_authenticationService.authenticationExists(userId)) {
      _authenticationService.createAuthentication(userId, "password".toCharArray());
      PropertyMap properties = new PropertyMap(3);
      properties.put(ContentModel.PROP_USERNAME, userId);
      properties.put(ContentModel.PROP_FIRSTNAME, userId);
      properties.put(ContentModel.PROP_LASTNAME, "Test");

      return _personService.createPerson(properties);
    } else {
      fail("User exists: " + userId);
      return null;
    }
  }

  /**
   * Creates a site and makes sure that a document library and a data list container exist.
   * 
   * @param preset
   * @param visibility
   * @param siteType
   * @return
   */
  protected SiteInfo createSite(final String preset, final String siteName, final SiteVisibility visibility, final QName siteType) {
    return _transactionHelper.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<SiteInfo>() {

      @Override
      public SiteInfo execute() throws Throwable {
        // Create site
        // behaviourFilter.disableBehaviour(SiteModel.TYPE_SITE);
        String name = null;

        if (siteName == null) {
          name = "it-" + System.currentTimeMillis();
        } else {
          name = siteName;
        }

        SiteInfo site = _siteService.createSite(preset, name, name, name, visibility, siteType);

        assertNotNull(site);

        _nodeService.addAspect(site.getNodeRef(), ContentModel.ASPECT_TEMPORARY, null);

        // Create document library container
        NodeRef documentLibrary = _siteService.getContainer(name, SiteService.DOCUMENT_LIBRARY);

        if (documentLibrary == null) {
          documentLibrary = _siteService.createContainer(name, SiteService.DOCUMENT_LIBRARY, ContentModel.TYPE_FOLDER, null);
        }

        assertNotNull(documentLibrary);

        return site;
      }

    }, false, false);
  }

  protected SiteInfo createSite(final String preset) {
    return _transactionHelper.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<SiteInfo>() {

      @Override
      public SiteInfo execute() throws Throwable {
        return createSite(preset, null, SiteVisibility.PRIVATE, SiteModel.TYPE_SITE);
      }

    }, false, true);
  }

  protected void deleteSite(final SiteInfo siteInfo) {
    _transactionHelper.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<Void>() {

      @Override
      public Void execute() throws Throwable {
        _authenticationComponent.setCurrentUser(AuthenticationUtil.getAdminUserName());

        _siteService.deleteSite(siteInfo.getShortName());

        System.out.println("deleted site with shortName: " + siteInfo.getShortName());

        return null;
      }

    }, false, true);
  }

  protected FileInfo uploadDocument(SiteInfo site, String filename) {
    return uploadDocument(site, filename, null, null, null);
  }

  protected FileInfo uploadDocument(SiteInfo site, String filename, String name) {
    return uploadDocument(site, filename, name, null, null);
  }

  protected FileInfo uploadDocument(SiteInfo site, String filename, String name, NodeRef parentNodeRef) {
    return uploadDocument(site, filename, name, parentNodeRef, null);
  }

  protected FileInfo uploadDocument(SiteInfo site, String filename, String name, NodeRef parentNodeRef, String type) {
    String finalName = StringUtils.isNotEmpty(name) ? name : FilenameUtils.getName(filename);

    NodeRef documentLibrary = _siteService.getContainer(site.getShortName(), SiteService.DOCUMENT_LIBRARY);

    NodeRef finalParentNodeRef = parentNodeRef != null ? parentNodeRef : documentLibrary;

    FileInfo fileInfo = _fileFolderService.create(finalParentNodeRef, finalName, type == null ? ContentModel.TYPE_CONTENT : createQName(type));

    ContentWriter writer = _contentService.getWriter(fileInfo.getNodeRef(), ContentModel.PROP_CONTENT, true);

    writer.guessEncoding();

    writer.guessMimetype(filename);

    InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);

    try {
      writer.putContent(inputStream);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    } finally {
      IOUtils.closeQuietly(inputStream);
    }

    return fileInfo;
  }

  protected QName createQName(String s) {
    QName qname;
    if (s.indexOf(NAMESPACE_BEGIN) != -1) {
      qname = QName.createQName(s);
    } else {
      qname = QName.createQName(s, _namespaceService);
    }
    return qname;
  }

}
