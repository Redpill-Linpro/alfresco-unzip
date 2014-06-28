package org.redpill.alfresco.unzip;

import java.util.List;

import org.activiti.engine.delegate.DelegateExecution;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.repo.workflow.WorkflowModel;
import org.alfresco.repo.workflow.WorkflowQNameConverter;
import org.alfresco.repo.workflow.activiti.ActivitiScriptNode;
import org.alfresco.repo.workflow.activiti.BaseJavaDelegate;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.transaction.TransactionService;

public class UnzipDocumentServiceTask extends BaseJavaDelegate {

  @Override
  public void execute(final DelegateExecution execution) throws Exception {
    Action unzipAction = getActionService().createAction(UnzipActionExecutor.NAME);
    unzipAction.setExecuteAsynchronously(true);
    unzipAction.setTrackStatus(true);

    ActivitiScriptNode bpmPackage = (ActivitiScriptNode) execution.getVariable(getWorkflowQNameConverter().mapQNameToName(WorkflowModel.ASSOC_PACKAGE));

    NodeRef workflowPackage = bpmPackage.getNodeRef();

    List<ChildAssociationRef> childAssocs = getNodeService().getChildAssocs(workflowPackage);

    for (ChildAssociationRef childAssoc : childAssocs) {
      NodeRef zipFileNodeRef = childAssoc.getChildRef();
      NodeRef folderNodeRef = getNodeService().getPrimaryParent(zipFileNodeRef).getParentRef();

      unzipAction.setParameterValue("destination-folder", folderNodeRef);

      unzipDocument(unzipAction, zipFileNodeRef);
    }
  }

  private void unzipDocument(final Action unzipAction, final NodeRef zipFileNodeRef) {
    RetryingTransactionHelper rth = getTransactionService().getRetryingTransactionHelper();

    RetryingTransactionCallback<Void> callback = new RetryingTransactionCallback<Void>() {

      @Override
      public Void execute() throws Throwable {
        getActionService().executeAction(unzipAction, zipFileNodeRef);

        return null;
      }

    };

    rth.doInTransaction(callback, false, true);
  }

  private WorkflowQNameConverter getWorkflowQNameConverter() {
    return new WorkflowQNameConverter(getServiceRegistry().getNamespaceService());
  }

  private NodeService getNodeService() {
    return getServiceRegistry().getNodeService();
  }

  private ActionService getActionService() {
    return getServiceRegistry().getActionService();
  }

  private TransactionService getTransactionService() {
    return getServiceRegistry().getTransactionService();
  }

}
