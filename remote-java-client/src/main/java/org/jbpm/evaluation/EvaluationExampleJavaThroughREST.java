package org.jbpm.evaluation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kie.server.api.model.instance.ProcessInstance;
import org.kie.server.api.model.instance.TaskSummary;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;
import org.kie.server.client.ProcessServicesClient;
import org.kie.server.client.QueryServicesClient;
import org.kie.server.client.UserTaskServicesClient;

/**
 * Example that uses the remote Java Client (through REST) to connect to the execution server
 * of the jBPM Console and execute the Evaluation process there.
 * 
 * This example assumes:
 *  - you have the jbpm console running at http://localhost:8080/jbpm-console
 *    (automatically when using jbpm-installer)
 *  - you have users krisv/krisv, john/john and mary/mary
 *    (automatically when using jbpm-installer)
 *  - you have deployed the Evaluation project (part of the jbpm-playground)
 *
 */
public class EvaluationExampleJavaThroughREST {

	public static void main(String[] args) throws Exception {
		KieServicesConfiguration config =  KieServicesFactory.newRestConfiguration(
			"http://localhost:8080/kie-server/services/rest/server", "krisv", "krisv");
		KieServicesClient client = KieServicesFactory.newKieServicesClient(config);
		ProcessServicesClient processServices = client.getServicesClient(ProcessServicesClient.class);
		UserTaskServicesClient taskServices = client.getServicesClient(UserTaskServicesClient.class);
		
		// start a new process instance
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("employee", "krisv");
		params.put("reason", "Yearly performance evaluation");
		Long processInstanceId = processServices.startProcess("evaluation_1.0.0-SNAPSHOT", "evaluation", params);
		System.out.println("Start Evaluation process " + processInstanceId);
		
		// complete Self Evaluation
		List<TaskSummary> tasks = taskServices.findTasksAssignedAsPotentialOwner("krisv", 0, 10);
		TaskSummary task = findTask(tasks, processInstanceId);
		System.out.println("'krisv' completing task " + task.getName() + ": " + task.getDescription());
		taskServices.startTask("evaluation_1.0.0-SNAPSHOT", task.getId(), "krisv");
		Map<String, Object> results = new HashMap<String, Object>();
		results.put("performance", "10");
		taskServices.completeTask("evaluation_1.0.0-SNAPSHOT", task.getId(), "krisv", results);
		
		
	    // john from HR
		config =  KieServicesFactory.newRestConfiguration(
			"http://localhost:8080/kie-server/services/rest/server", "john", "john");
		client = KieServicesFactory.newKieServicesClient(config);
		taskServices = client.getServicesClient(UserTaskServicesClient.class);
		
		tasks = taskServices.findTasksAssignedAsPotentialOwner("john", 0, 10);
		task = findTask(tasks, processInstanceId);
		System.out.println("'john' completing task " + task.getName() + ": " + task.getDescription());
		taskServices.claimTask("evaluation_1.0.0-SNAPSHOT", task.getId(), "john");
		taskServices.startTask("evaluation_1.0.0-SNAPSHOT", task.getId(), "john");
		results = new HashMap<String, Object>();
		results.put("performance", "9");
		taskServices.completeTask("evaluation_1.0.0-SNAPSHOT", task.getId(), "john", results);
		
		// mary from PM
		config =  KieServicesFactory.newRestConfiguration(
			"http://localhost:8080/kie-server/services/rest/server", "mary", "mary");
		client = KieServicesFactory.newKieServicesClient(config);
		taskServices = client.getServicesClient(UserTaskServicesClient.class);
		
		tasks = taskServices.findTasksAssignedAsPotentialOwner("mary", 0, 10);
		task = findTask(tasks, processInstanceId);
		System.out.println("'mary' completing task " + task.getName() + ": " + task.getDescription());
		taskServices.claimTask("evaluation_1.0.0-SNAPSHOT", task.getId(), "mary");
		taskServices.startTask("evaluation_1.0.0-SNAPSHOT", task.getId(), "mary");
		results = new HashMap<String, Object>();
		results.put("performance", "10");
		taskServices.completeTask("evaluation_1.0.0-SNAPSHOT", task.getId(), "mary", results);
		
		QueryServicesClient queryServices = client.getServicesClient(QueryServicesClient.class);
		ProcessInstance processInstance = queryServices.findProcessInstanceById(processInstanceId);
		if (processInstance.getState() != 2) {
			throw new RuntimeException("Process instance not completed");
		}
		System.out.println("Process instance completed");
	}
	
	private static TaskSummary findTask(List<TaskSummary> tasks, long processInstanceId) {
		for (TaskSummary task: tasks) {
			if (task.getProcessInstanceId() == processInstanceId) {
				return task;
			}
		}
		throw new RuntimeException("Could not find task for process instance "
			+ processInstanceId + " [" + tasks.size() + " task(s) in total]");
	}
	
}
