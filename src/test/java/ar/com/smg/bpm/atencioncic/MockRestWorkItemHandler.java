package ar.com.smg.bpm.atencioncic;

import java.util.HashMap;
import java.util.Map;

import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;

public class MockRestWorkItemHandler implements WorkItemHandler {

	private Integer cdor;

	public MockRestWorkItemHandler() {
		this.cdor = 0;
	}
	
	public MockRestWorkItemHandler(Integer cdor) {
		this.cdor = cdor;
	}
	
	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
		
		System.out.println("### REST -> " + cdor);
		// notify manager that work item has been completed
		Map<String,Object> res = null;
		
		if(cdor-- <= 0) {
			res = new HashMap<String,Object>();
			res.put("Result", Boolean.TRUE);				
			manager.completeWorkItem(workItem.getId(), res);
		}
		
	}

	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
		// Do nothing, notifications cannot be aborted
	}

}