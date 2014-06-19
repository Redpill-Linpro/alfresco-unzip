package org.redpill.alfresco.unzip;

import java.util.List;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.util.ParameterCheck;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;

public class UnzipActionExecutor extends ActionExecuterAbstractBase implements InitializingBean {

  private static final Logger LOG = Logger.getLogger(UnzipActionExecutor.class);
  
  public static final String NAME = "unzip";

  public static final String PARAM_DESTINATION_FOLDER = "destination-folder";

  private static final String UNZIP_EXCEPTION_SOURCE_MESSAGE = "unzip.exception.source";

  private static final String UNZIP_EXCEPTION_TARGET_MESSAGE = "unzip.exception.target";

  private NodeService _nodeService;

  private UnzipService _unzipService;
  
  @Override
  protected void executeImpl(Action action, NodeRef actionedUponNodeRef) {
    if (LOG.isTraceEnabled()) {
      LOG.trace("Entering executeImpl...");
    }

    NodeRef targetNodeRef = (NodeRef) action.getParameterValue(PARAM_DESTINATION_FOLDER);

    if (LOG.isTraceEnabled()) {
      LOG.trace("Source: " + actionedUponNodeRef + ", Target: " + targetNodeRef);
    }

    if (actionedUponNodeRef == null || !_nodeService.exists(actionedUponNodeRef)) {
      throw new AlfrescoRuntimeException(UNZIP_EXCEPTION_SOURCE_MESSAGE);
    }

    if (targetNodeRef == null || !_nodeService.exists(targetNodeRef)) {
      throw new AlfrescoRuntimeException(UNZIP_EXCEPTION_TARGET_MESSAGE);
    }

    _unzipService.importZip(actionedUponNodeRef, targetNodeRef);
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

  @Override
  public void afterPropertiesSet() throws Exception {
    ParameterCheck.mandatory("nodeService", _nodeService);
    ParameterCheck.mandatory("unzipService", _unzipService);
  }

}
