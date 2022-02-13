/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.logic.condition;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.logic.LogicalStatementException;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Context;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowService;
import org.dspace.workflow.factory.WorkflowServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A condition that accepts a list of collection handles and returns true
 * if the item belongs to any of them.
 *
 * @author Kim Shepherd
 * @version $Revision$
 */
public class InCollectionCondition extends AbstractCondition {
    private static Logger log = Logger.getLogger(InCollectionCondition.class);

    @Autowired(required = true)
    protected CollectionService collectionService;

    @Override
    public Boolean getResult(Context context, Item item) throws LogicalStatementException {

        List<String> collectionHandles = (List<String>)getParameters().get("collections");
        List<Collection> itemCollections = new ArrayList<>();
        itemCollections.addAll(item.getCollections());
        
        if (itemCollections.size() < 1) {
            log.debug("No collection found for item " + item.getHandle()
                    + ". Looking for corresponding workspace or workflow item.");
            try {
                // check for Workflow or Workspace item
                WorkspaceItem wsi = ContentServiceFactory.getInstance().getWorkspaceItemService()
                        .findByItem(context, item);
                if (wsi != null) {
                    Collection collection = wsi.getCollection();
                    itemCollections.add(collection);
                    log.debug("found workspace item and owning collection: " + collection.getHandle());
                }
                WorkflowItem wfi = WorkflowServiceFactory.getInstance().getWorkflowItemService()
                        .findByItem(context, item);
                if (wfi != null) {
                    Collection collection = wfi.getCollection();
                    itemCollections.add(collection);
                    log.debug("found workflow item and owning collection: " + collection.getHandle());
                }
            } catch (Exception ex) {
                log.debug("Caught exception while looking for workspace/workflow item:", ex);
            }
        }

        for(Collection collection : itemCollections) {
            if(collectionHandles.contains(collection.getHandle())) {
                log.debug("item " + item.getHandle() + " is in collection "
                    + collection.getHandle() + ", returning true");
                return true;
            } else {
                log.debug("item " + item.getHandle() + " is in collection "
                    + collection.getHandle() + ", continue loop");
            }
        }

        log.debug("item " + item.getHandle() + " not found in the passed collection handle list");

        return false;
    }
}
