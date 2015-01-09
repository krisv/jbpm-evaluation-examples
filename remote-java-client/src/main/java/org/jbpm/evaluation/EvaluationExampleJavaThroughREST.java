package org.jbpm.evaluation;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.TaskSummary;
import org.kie.remote.client.api.RemoteRuntimeEngineFactory;

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
		RuntimeEngine engine = RemoteRuntimeEngineFactory.newRestBuilder()
			.addUrl(new URL("http://localhost:8080/jbpm-console"))
			.addUserName("krisv").addPassword("krisv")
			.addDeploymentId("org.jbpm:Evaluation:1.0")
				.build();
		KieSession ksession = engine.getKieSession();
		TaskService taskService = engine.getTaskService();
		
		// start a new process instance
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("employee", "krisv");
		params.put("reason", "Yearly performance evaluation");
		ProcessInstance processInstance = 
			ksession.startProcess("evaluation", params);
		System.out.println("Start Evaluation process " + processInstance.getId());
		
		// complete Self Evaluation
		List<TaskSummary> tasks = taskService.getTasksAssignedAsPotentialOwner("krisv", "en-UK");
		TaskSummary task = findTask(tasks, processInstance.getId());
		System.out.println("'krisv' completing task " + task.getName() + ": " + task.getDescription());
		taskService.start(task.getId(), "krisv");
		Map<String, Object> results = new HashMap<String, Object>();
		results.put("performance", "exceeding");
		taskService.complete(task.getId(), "krisv", results);
		
		// john from HR
		engine = RemoteRuntimeEngineFactory.newRestBuilder()
			.addUrl(new URL("http://localhost:8080/jbpm-console"))
			.addUserName("john").addPassword("john")
			.addDeploymentId("org.jbpm:Evaluation:1.0")
				.build();
		ksession = engine.getKieSession();
		taskService = engine.getTaskService();
		
		tasks = taskService.getTasksAssignedAsPotentialOwner("john", "en-UK");
		task = findTask(tasks, processInstance.getId());
		System.out.println("'john' completing task " + task.getName() + ": " + task.getDescription());
		taskService.claim(task.getId(), "john");
		taskService.start(task.getId(), "john");
		results = new HashMap<String, Object>();
		results.put("performance", "acceptable");
		taskService.complete(task.getId(), "john", results);
		
		// mary from PM
		engine = RemoteRuntimeEngineFactory.newRestBuilder()
			.addUrl(new URL("http://localhost:8080/jbpm-console"))
			.addUserName("mary").addPassword("mary")
			.addDeploymentId("org.jbpm:Evaluation:1.0")
				.build();
		ksession = engine.getKieSession();
		taskService = engine.getTaskService();

		tasks = taskService.getTasksAssignedAsPotentialOwner("mary", "en-UK");
		task = findTask(tasks, processInstance.getId());
		System.out.println("'mary' completing task " + task.getName() + ": " + task.getDescription());
		taskService.claim(task.getId(), "mary");
		taskService.start(task.getId(), "mary");
		results = new HashMap<String, Object>();
		results.put("performance", "outstanding");
		taskService.complete(task.getId(), "mary", results);
		
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
