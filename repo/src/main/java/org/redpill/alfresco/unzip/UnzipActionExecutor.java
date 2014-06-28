package org.redpill.alfresco.unzip;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.action.executer.MailActionExecuter;
import org.alfresco.repo.dictionary.RepositoryLocation;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.admin.RepoAdminService;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.model.FileExistsException;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.ModelUtil;
import org.alfresco.util.ParameterCheck;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.extensions.surf.util.I18NUtil;

public class UnzipActionExecutor extends ActionExecuterAbstractBase implements InitializingBean {

  private static final Logger LOG = Logger.getLogger(UnzipActionExecutor.class);

  public static final String NAME = "unzip";

  public static final String PARAM_DESTINATION_FOLDER = "destination-folder";

  public static final String PARAM_RESULT = "result";

  private static final String UNZIP_EXCEPTION_SOURCE_MESSAGE = "unzip.exception.source";

  private static final String UNZIP_EXCEPTION_TARGET_MESSAGE = "unzip.exception.target";

  private static final String MSG_EMAIL_SUBJECT = "unzip.email.subject";

  private NodeService _nodeService;

  private UnzipService _unzipService;

  private SiteService _siteService;

  private ActionService _actionService;

  private PersonService _personService;

  private PermissionService _permissionService;

  private SearchService _searchService;

  private NamespacePrefixResolver _namespaceService;

  private FileFolderService _fileFolderService;

  private RepoAdminService _repoAdminService;

  private TransactionService _transactionService;

  @Override
  protected void executeImpl(Action action, NodeRef actionedUponNodeRef) {
    if (LOG.isTraceEnabled()) {
      LOG.trace("Entering executeImpl...");
    }

    NodeRef targetNodeRef = (NodeRef) action.getParameterValue(PARAM_DESTINATION_FOLDER);

    if (LOG.isTraceEnabled()) {
      LOG.trace("Source: " + actionedUponNodeRef + ", Target: " + targetNodeRef);
    }

    try {
      if (actionedUponNodeRef == null || !_nodeService.exists(actionedUponNodeRef)) {
        throw new AlfrescoRuntimeException(UNZIP_EXCEPTION_SOURCE_MESSAGE);
      }

      if (targetNodeRef == null || !_nodeService.exists(targetNodeRef)) {
        throw new AlfrescoRuntimeException(UNZIP_EXCEPTION_TARGET_MESSAGE);
      }

      List<NodeRef> unzippedDocuments = _unzipService.importZip(actionedUponNodeRef, targetNodeRef);

      for (NodeRef unzippedDocument : unzippedDocuments) {
        _nodeService.addAspect(unzippedDocument, UnzipModel.ASPECT_UNZIPPED_FROM, null);

        _nodeService.createAssociation(unzippedDocument, actionedUponNodeRef, UnzipModel.ASSOC_ZIP_DOCUMENT);
      }

      action.setParameterValue(PARAM_RESULT, (Serializable) unzippedDocuments);

      sendEmail(actionedUponNodeRef, targetNodeRef, unzippedDocuments, getSuccessEmailTemplateRef(), null);
    } catch (Exception ex) {
      String errorMessage = ex.getMessage();

      if (ex instanceof AlfrescoRuntimeException) {
        if (((AlfrescoRuntimeException) ex).getCause() instanceof FileExistsException) {
          errorMessage = I18NUtil.getMessage("unzip.exception.target.alreadyExists");
        }
      }

      sendEmail(actionedUponNodeRef, targetNodeRef, null, getFailureEmailTemplateRef(), errorMessage);

      throw new AlfrescoRuntimeException(ex.getMessage(), ex);
    }
  }

  private void sendEmail(final NodeRef zipFileNodeRef, final NodeRef folderNodeRef, final List<NodeRef> unzippedDocuments, final NodeRef emailTemplate, final String errorMessage) {
    Map<String, Object> model = new HashMap<String, Object>();

    if (unzippedDocuments != null) {
      List<Map<String, Object>> documents = new ArrayList<Map<String, Object>>();

      for (NodeRef unzippedDocument : unzippedDocuments) {
        String name = (String) _nodeService.getProperty(unzippedDocument, ContentModel.PROP_NAME);

        Map<String, Object> document = new HashMap<String, Object>();

        document.put("name", getFolderPath(unzippedDocument) + "/" + name);
        document.put("nodeRef", unzippedDocument.toString());

        documents.add(document);
      }

      model.put("documents", documents);
    }

    String zipFileName = (String) _nodeService.getProperty(zipFileNodeRef, ContentModel.PROP_NAME);
    String folderName = (String) _nodeService.getProperty(folderNodeRef, ContentModel.PROP_NAME);
    if ("documentLibrary".equalsIgnoreCase(folderName)) {
      folderName = "Startbiblioteket";
    }
    String folderPath = getFolderPath(zipFileNodeRef);

    model.put("zipFileName", zipFileName);
    model.put("zipFileNodeRef", zipFileNodeRef.toString());
    model.put("folderName", folderName);
    model.put("folderNodeRef", folderNodeRef.toString());
    model.put("folderPath", StringUtils.replace(folderPath, "/", "%2F"));
    model.put("errorMessage", errorMessage);

    SiteInfo site = _siteService.getSite(zipFileNodeRef);
    if (site != null) {
      model.put("siteShortName", site.getShortName());
      model.put("siteTitle", site.getTitle());
    }

    Action mailAction = _actionService.createAction(MailActionExecuter.NAME);
    mailAction.setExecuteAsynchronously(false);

    NodeRef user = _personService.getPerson(AuthenticationUtil.getRunAsUser());

    String to = (String) _nodeService.getProperty(user, ContentModel.PROP_EMAIL);
    String subject = buildSubjectText();

    mailAction.setParameterValue(MailActionExecuter.PARAM_TO, to);
    mailAction.setParameterValue(MailActionExecuter.PARAM_SUBJECT, subject);
    mailAction.setParameterValue(MailActionExecuter.PARAM_TEMPLATE, emailTemplate);
    mailAction.setParameterValue(MailActionExecuter.PARAM_TEMPLATE_MODEL, (Serializable) model);

    _actionService.executeAction(mailAction, null);
  }

  private String getFolderPath(final NodeRef zipFileNodeRef) {
    String folderPath = _nodeService.getPath(zipFileNodeRef).toDisplayPath(_nodeService, _permissionService);

    if (!folderPath.endsWith("documentLibrary")) {
      folderPath = folderPath.substring(folderPath.indexOf("documentLibrary") + 16);
    }

    return "/" + folderPath;
  }

  private NodeRef getSuccessEmailTemplateRef() {
    RepositoryLocation unzipSuccessEmailTemplateLocation = (RepositoryLocation) ApplicationContextHolder.getApplicationContext().getBean("rl.unzip-repo.unzipSuccessEmailTemplateLocation");

    StoreRef store = unzipSuccessEmailTemplateLocation.getStoreRef();
    String xpath = unzipSuccessEmailTemplateLocation.getPath();

    if (!unzipSuccessEmailTemplateLocation.getQueryLanguage().equals(SearchService.LANGUAGE_XPATH)) {
      LOG.warn("Cannot find the unzip success email template - repository location query language is not 'xpath': " + unzipSuccessEmailTemplateLocation.getQueryLanguage());

      return null;
    }

    List<NodeRef> nodeRefs = _searchService.selectNodes(_nodeService.getRootNode(store), xpath, null, _namespaceService, false);

    if (nodeRefs.size() != 1) {
      LOG.warn("Cannot find the unzip success email template: " + xpath);

      return null;
    }

    return _fileFolderService.getLocalizedSibling(nodeRefs.get(0));
  }

  private NodeRef getFailureEmailTemplateRef() {
    RepositoryLocation unzipFailureEmailTemplateLocation = (RepositoryLocation) ApplicationContextHolder.getApplicationContext().getBean("rl.unzip-repo.unzipFailureEmailTemplateLocation");

    StoreRef store = unzipFailureEmailTemplateLocation.getStoreRef();
    String xpath = unzipFailureEmailTemplateLocation.getPath();

    if (!unzipFailureEmailTemplateLocation.getQueryLanguage().equals(SearchService.LANGUAGE_XPATH)) {
      LOG.warn("Cannot find the unzip failure email template - repository location query language is not 'xpath': " + unzipFailureEmailTemplateLocation.getQueryLanguage());

      return null;
    }

    List<NodeRef> nodeRefs = _searchService.selectNodes(_nodeService.getRootNode(store), xpath, null, _namespaceService, false);

    if (nodeRefs.size() != 1) {
      LOG.warn("Cannot find the unzip failure email template: " + xpath);

      return null;
    }

    return _fileFolderService.getLocalizedSibling(nodeRefs.get(0));
  }

  protected String buildSubjectText() {
    return I18NUtil.getMessage(MSG_EMAIL_SUBJECT, ModelUtil.getProductName(_repoAdminService));
  }

  @Override
  protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
    paramList.add(new ParameterDefinitionImpl(PARAM_DESTINATION_FOLDER, DataTypeDefinition.NODE_REF, true, getParamDisplayLabel(PARAM_DESTINATION_FOLDER)));
  }

  public void setNodeService(NodeService nodeService) {
    _nodeService = nodeService;
  }

  public void setUnzipService(UnzipService unzipService) {
    _unzipService = unzipService;
  }

  public void setSiteService(SiteService siteService) {
    _siteService = siteService;
  }

  public void setActionService(ActionService actionService) {
    _actionService = actionService;
  }

  public void setPersonService(PersonService personService) {
    _personService = personService;
  }

  public void setPermissionService(PermissionService permissionService) {
    _permissionService = permissionService;
  }

  public void setSearchService(SearchService searchService) {
    _searchService = searchService;
  }

  public void setNamespaceService(NamespacePrefixResolver namespaceService) {
    _namespaceService = namespaceService;
  }

  public void setFileFolderService(FileFolderService fileFolderService) {
    _fileFolderService = fileFolderService;
  }

  public void setRepoAdminService(RepoAdminService repoAdminService) {
    _repoAdminService = repoAdminService;
  }

  public void setTransactionService(TransactionService transactionService) {
    _transactionService = transactionService;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    ParameterCheck.mandatory("actionService", _actionService);
    ParameterCheck.mandatory("nodeService", _nodeService);
    ParameterCheck.mandatory("unzipService", _unzipService);
    ParameterCheck.mandatory("siteService", _siteService);
    ParameterCheck.mandatory("personService", _personService);
    ParameterCheck.mandatory("permissionService", _permissionService);
    ParameterCheck.mandatory("searchService", _searchService);
    ParameterCheck.mandatory("namespaceService", _namespaceService);
    ParameterCheck.mandatory("fileFolderService", _fileFolderService);
    ParameterCheck.mandatory("repoAdminService", _repoAdminService);
    ParameterCheck.mandatory("transactionService", _transactionService);
  }

}
