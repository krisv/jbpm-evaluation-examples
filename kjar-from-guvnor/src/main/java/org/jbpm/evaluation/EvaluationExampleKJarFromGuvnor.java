package org.jbpm.evaluation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.jbpm.test.JBPMHelper;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeEnvironmentBuilder;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.manager.RuntimeManagerFactory;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.TaskService;
import org.kie.api.task.UserGroupCallback;
import org.kie.api.task.model.TaskSummary;

/**
 * Example that fetches the Evaluation kjar from Guvnor Maven2 repository and initializes a local
 * ksession to execute the Evaluation process there.
 * 
 * This example assumes:
 *  - you have the jbpm console running at http://localhost:8080/jbpm-console
 *    (automatically when using jbpm-installer)
 *  - you have users krisv/krisv, john/john and mary/mary
 *    (automatically when using jbpm-installer)
 *  - you have deployed the Evaluation project (part of the jbpm-playground)
 *  - you have added the Guvnor m2 repository as a known Maven2 repository, by either:
 *    + running this class with the following system parameter:
 *        -Dkie.maven.settings.custom=settings.xml
 *      (this will pick up the settings.xml as defined in this project rather than the default one)
 *    + updating your <HOME_FOLDER>/.m2/settings.xml to add the server and profile there 
 */
public class EvaluationExampleKJarFromGuvnor {

	public static void main(String[] args) throws Exception {
		JBPMHelper.setupDataSource();
		
		KieServices ks = KieServices.Factory.get(); 
		KieContainer kContainer = ks.newKieContainer(ks.newReleaseId("org.jbpm", "Evaluation", "1.0"));
		KieBase kbase = kContainer.newKieBase(ks.newKieBaseConfiguration()); 
		
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("org.jbpm.persistence.jpa");
		RuntimeEnvironmentBuilder builder = RuntimeEnvironmentBuilder.Factory.get()
			.newDefaultBuilder().entityManagerFactory(emf).knowledgeBase(kbase)
			.userGroupCallback(new UserGroupCallback() {
				public List<String> getGroupsForUser(String userId, List<String> groupIds, List<String> allExistingGroupIds) {
					List<String> result = new ArrayList<String>();
					switch (userId) {
						case "john": result.add("PM"); break;
						case "mary": result.add("HR"); break;
						default: break;
					}
					return result;
				}
				public boolean existsUser(String userId) {
					return true;
				}
				public boolean existsGroup(String groupId) {
					return true;
				}
			});
		RuntimeManager manager = RuntimeManagerFactory.Factory.get()
			.newSingletonRuntimeManager(builder.get(), "org.jbpm:Evaluation:1.0");
		RuntimeEngine engine = manager.getRuntimeEngine(null);
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
		tasks = taskService.getTasksAssignedAsPotentialOwner("john", "en-UK");
		task = findTask(tasks, processInstance.getId());
		System.out.println("'john' completing task " + task.getName() + ": " + task.getDescription());
		taskService.claim(task.getId(), "john");
		taskService.start(task.getId(), "john");
		results = new HashMap<String, Object>();
		results.put("performance", "acceptable");
		taskService.complete(task.getId(), "john", results);
		
		// mary from PM
		tasks = taskService.getTasksAssignedAsPotentialOwner("mary", "en-UK");
		task = findTask(tasks, processInstance.getId());
		System.out.println("'mary' completing task " + task.getName() + ": " + task.getDescription());
		taskService.claim(task.getId(), "mary");
		taskService.start(task.getId(), "mary");
		results = new HashMap<String, Object>();
		results.put("performance", "outstanding");
		taskService.complete(task.getId(), "mary", results);
		
		System.out.println("Process instance completed");

		manager.disposeRuntimeEngine(engine);
		manager.close();
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
