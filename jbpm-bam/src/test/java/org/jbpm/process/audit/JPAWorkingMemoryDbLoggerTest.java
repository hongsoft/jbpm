/**
 * Copyright 2010 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jbpm.process.audit;

import static org.jbpm.persistence.util.PersistenceUtil.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.io.impl.ClassPathResource;
import org.drools.persistence.jpa.JPAKnowledgeService;
import org.drools.runtime.Environment;
import org.drools.runtime.EnvironmentName;
import org.drools.runtime.KnowledgeSessionConfiguration;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.WorkItem;
import org.drools.runtime.process.WorkItemHandler;
import org.drools.runtime.process.WorkItemManager;
import org.jbpm.JbpmTestCase;
import org.jbpm.process.instance.impl.demo.SystemOutWorkItemHandler;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.resource.jdbc.PoolingDataSource;

public class JPAWorkingMemoryDbLoggerTest extends JbpmTestCase {

    PoolingDataSource ds1;
    EntityManagerFactory emf;

    @Override
    protected void setUp() throws Exception {
        ds1 = setupPoolingDataSource();
        ds1.init();
        
        emf = Persistence.createEntityManagerFactory( PERSISTENCE_UNIT_NAME );
    }

    @Override
    protected void tearDown() throws Exception {
        ds1.close();
    }

	public void testLogger1() throws Exception {
	    
        // load the process
        KnowledgeBase kbase = createKnowledgeBase();
        // create a new session
        Environment env = KnowledgeBaseFactory.newEnvironment();
        env.set( EnvironmentName.ENTITY_MANAGER_FACTORY,
                 emf );
        env.set( EnvironmentName.TRANSACTION_MANAGER,
                 TransactionManagerServices.getTransactionManager() );
        Properties properties = new Properties();
		properties.put("drools.processInstanceManagerFactory", "org.jbpm.persistence.processinstance.JPAProcessInstanceManagerFactory");
		properties.put("drools.processSignalManagerFactory", "org.jbpm.persistence.processinstance.JPASignalManagerFactory");
		KnowledgeSessionConfiguration config = KnowledgeBaseFactory.newKnowledgeSessionConfiguration(properties);
		StatefulKnowledgeSession session = JPAKnowledgeService.newStatefulKnowledgeSession(kbase, config, env);
        new JPAWorkingMemoryDbLogger(session);
        JPAProcessInstanceDbLog log = new JPAProcessInstanceDbLog(env);
        session.getWorkItemManager().registerWorkItemHandler("Human Task", new SystemOutWorkItemHandler());

        // record the initial count to compare to later
        List<ProcessInstanceLog> processInstances =
        	log.findProcessInstances("com.sample.ruleflow");
	    int initialProcessInstanceSize = processInstances.size();
	    
        // start process instance
        long processInstanceId = session.startProcess("com.sample.ruleflow").getId();
        
        System.out.println("Checking process instances for process 'com.sample.ruleflow'");
        processInstances = log.findProcessInstances("com.sample.ruleflow");
        assertEquals(initialProcessInstanceSize + 1, processInstances.size());
        ProcessInstanceLog processInstance = processInstances.get(0);
        System.out.print(processInstance);
        System.out.println(" -> " + processInstance.getStart() + " - " + processInstance.getEnd());
        assertNotNull(processInstance.getStart());
        assertNotNull(processInstance.getEnd());
        assertEquals(processInstanceId, processInstance.getProcessInstanceId());
        assertEquals("com.sample.ruleflow", processInstance.getProcessId());
        List<NodeInstanceLog> nodeInstances = log.findNodeInstances(processInstanceId);
        assertEquals(6, nodeInstances.size());
        for (NodeInstanceLog nodeInstance: nodeInstances) {
        	System.out.println(nodeInstance);
            assertEquals(processInstanceId, processInstance.getProcessInstanceId());
            assertEquals("com.sample.ruleflow", processInstance.getProcessId());
            assertNotNull(nodeInstance.getDate());
        }
        log.clear();
        processInstances = log.findProcessInstances("com.sample.ruleflow");
        assertEquals(0, processInstances.size());
        log.dispose();
	}
	
	public void testLogger2() {
        // load the process
        KnowledgeBase kbase = createKnowledgeBase();
        // create a new session
        Environment env = KnowledgeBaseFactory.newEnvironment();
        env.set( EnvironmentName.ENTITY_MANAGER_FACTORY,
                 emf );
        env.set( EnvironmentName.TRANSACTION_MANAGER,
                 TransactionManagerServices.getTransactionManager() );
        Properties properties = new Properties();
		properties.put("drools.processInstanceManagerFactory", "org.jbpm.persistence.processinstance.JPAProcessInstanceManagerFactory");
		properties.put("drools.processSignalManagerFactory", "org.jbpm.persistence.processinstance.JPASignalManagerFactory");
		KnowledgeSessionConfiguration config = KnowledgeBaseFactory.newKnowledgeSessionConfiguration(properties);
		StatefulKnowledgeSession session = JPAKnowledgeService.newStatefulKnowledgeSession(kbase, config, env);
        new JPAWorkingMemoryDbLogger(session);
        JPAProcessInstanceDbLog log = new JPAProcessInstanceDbLog(env);
        session.getWorkItemManager().registerWorkItemHandler("Human Task", new SystemOutWorkItemHandler());

        // record the initial count to compare to later
        List<ProcessInstanceLog> processInstances =
        	log.findProcessInstances("com.sample.ruleflow");
	    int initialProcessInstanceSize = processInstances.size();
	    
        // start process instance
        session.startProcess("com.sample.ruleflow");
        session.startProcess("com.sample.ruleflow");
        
        System.out.println("Checking process instances for process 'com.sample.ruleflow'");
        processInstances = log.findProcessInstances("com.sample.ruleflow");
        assertEquals(initialProcessInstanceSize + 2, processInstances.size());
        for (ProcessInstanceLog processInstance: processInstances) {
            System.out.print(processInstance);
            System.out.println(" -> " + processInstance.getStart() + " - " + processInstance.getEnd());
            List<NodeInstanceLog> nodeInstances = log.findNodeInstances(processInstance.getProcessInstanceId());
            for (NodeInstanceLog nodeInstance: nodeInstances) {
            	System.out.print(nodeInstance);
                System.out.println(" -> " + nodeInstance.getDate());
            }
            assertEquals(6, nodeInstances.size());
        }
        log.clear();
        log.dispose();
	}
	
	public void testLogger3() {
        // load the process
        KnowledgeBase kbase = createKnowledgeBase();
        // create a new session
        Environment env = KnowledgeBaseFactory.newEnvironment();
        env.set( EnvironmentName.ENTITY_MANAGER_FACTORY,
                 emf );
        env.set( EnvironmentName.TRANSACTION_MANAGER,
                 TransactionManagerServices.getTransactionManager() );
        Properties properties = new Properties();
		properties.put("drools.processInstanceManagerFactory", "org.jbpm.persistence.processinstance.JPAProcessInstanceManagerFactory");
		properties.put("drools.processSignalManagerFactory", "org.jbpm.persistence.processinstance.JPASignalManagerFactory");
		KnowledgeSessionConfiguration config = KnowledgeBaseFactory.newKnowledgeSessionConfiguration(properties);
		StatefulKnowledgeSession session = JPAKnowledgeService.newStatefulKnowledgeSession(kbase, config, env);
        new JPAWorkingMemoryDbLogger(session);
        JPAProcessInstanceDbLog log = new JPAProcessInstanceDbLog(env);
        session.getWorkItemManager().registerWorkItemHandler("Human Task", new SystemOutWorkItemHandler());

        // record the initial count to compare to later
        List<ProcessInstanceLog> processInstances =
        	log.findProcessInstances("com.sample.ruleflow");
	    int initialProcessInstanceSize = processInstances.size();
	    
        // start process instance
        long processInstanceId = session.startProcess("com.sample.ruleflow2").getId();
        
        System.out.println("Checking process instances for process 'com.sample.ruleflow2'");
        processInstances = log.findProcessInstances("com.sample.ruleflow2");
        assertEquals(initialProcessInstanceSize + 1, processInstances.size());
        ProcessInstanceLog processInstance = processInstances.get(0);
        System.out.print(processInstance);
        System.out.println(" -> " + processInstance.getStart() + " - " + processInstance.getEnd());
        assertNotNull(processInstance.getStart());
        assertNotNull(processInstance.getEnd());
        assertEquals(processInstanceId, processInstance.getProcessInstanceId());
        assertEquals("com.sample.ruleflow2", processInstance.getProcessId());
        List<NodeInstanceLog> nodeInstances = log.findNodeInstances(processInstanceId);
        for (NodeInstanceLog nodeInstance: nodeInstances) {
        	System.out.print(nodeInstance);
            System.out.println(" -> " + nodeInstance.getDate());
            assertEquals(processInstanceId, processInstance.getProcessInstanceId());
            assertEquals("com.sample.ruleflow2", processInstance.getProcessId());
            assertNotNull(nodeInstance.getDate());
        }
        assertEquals(14, nodeInstances.size());
        log.clear();
        log.dispose();
	}
	
	public void testLogger4() throws Exception {
        // load the process
        KnowledgeBase kbase = createKnowledgeBase();
        // create a new session
        Environment env = KnowledgeBaseFactory.newEnvironment();
        env.set( EnvironmentName.ENTITY_MANAGER_FACTORY,
                 emf );
        env.set( EnvironmentName.TRANSACTION_MANAGER,
                 TransactionManagerServices.getTransactionManager() );
        Properties properties = new Properties();
		properties.put("drools.processInstanceManagerFactory", "org.jbpm.persistence.processinstance.JPAProcessInstanceManagerFactory");
		properties.put("drools.processSignalManagerFactory", "org.jbpm.persistence.processinstance.JPASignalManagerFactory");
		KnowledgeSessionConfiguration config = KnowledgeBaseFactory.newKnowledgeSessionConfiguration(properties);
		StatefulKnowledgeSession session = JPAKnowledgeService.newStatefulKnowledgeSession(kbase, config, env);
        new JPAWorkingMemoryDbLogger(session);
        JPAProcessInstanceDbLog log = new JPAProcessInstanceDbLog(env);
        session.getWorkItemManager().registerWorkItemHandler("Human Task", new WorkItemHandler() {
			public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
				Map<String, Object> results = new HashMap<String, Object>();
				results.put("Result", "ResultValue");
				manager.completeWorkItem(workItem.getId(), results);
			}
			public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
			}
		});
        
        // record the initial count to compare to later
        List<ProcessInstanceLog> processInstances =
        	log.findProcessInstances("com.sample.ruleflow");
	    int initialProcessInstanceSize = processInstances.size();
	    
        // start process instance
		Map<String, Object> params = new HashMap<String, Object>();
		List<String> list = new ArrayList<String>();
		list.add("One");
		list.add("Two");
		list.add("Three");
		params.put("list", list);
		long processInstanceId = session.startProcess("com.sample.ruleflow3", params).getId();
        
        System.out.println("Checking process instances for process 'com.sample.ruleflow3'");
        processInstances =
        	log.findProcessInstances("com.sample.ruleflow3");
        assertEquals(initialProcessInstanceSize + 1, processInstances.size());
        ProcessInstanceLog processInstance = processInstances.get(0);
        System.out.print(processInstance);
        System.out.println(" -> " + processInstance.getStart() + " - " + processInstance.getEnd());
        assertNotNull(processInstance.getStart());
        assertNotNull(processInstance.getEnd());
        assertEquals(processInstanceId, processInstance.getProcessInstanceId());
        assertEquals("com.sample.ruleflow3", processInstance.getProcessId());
        List<VariableInstanceLog> variableInstances = log.findVariableInstances(processInstanceId);
        assertEquals(6, variableInstances.size());
        for (VariableInstanceLog variableInstance: variableInstances) {
        	System.out.println(variableInstance);
            assertEquals(processInstanceId, processInstance.getProcessInstanceId());
            assertEquals("com.sample.ruleflow3", processInstance.getProcessId());
            assertNotNull(variableInstance.getDate());
        }
        log.clear();
        processInstances = log.findProcessInstances("com.sample.ruleflow3");
        assertEquals(0, processInstances.size());
        log.dispose();
	}
	
	public void testLogger4LargeVariable() throws Exception {
        // load the process
        KnowledgeBase kbase = createKnowledgeBase();
        // create a new session
        Environment env = KnowledgeBaseFactory.newEnvironment();
        env.set( EnvironmentName.ENTITY_MANAGER_FACTORY,
                 emf );
        env.set( EnvironmentName.TRANSACTION_MANAGER,
                 TransactionManagerServices.getTransactionManager() );
        Properties properties = new Properties();
		properties.put("drools.processInstanceManagerFactory", "org.jbpm.persistence.processinstance.JPAProcessInstanceManagerFactory");
		properties.put("drools.processSignalManagerFactory", "org.jbpm.persistence.processinstance.JPASignalManagerFactory");
		KnowledgeSessionConfiguration config = KnowledgeBaseFactory.newKnowledgeSessionConfiguration(properties);
		StatefulKnowledgeSession session = JPAKnowledgeService.newStatefulKnowledgeSession(kbase, config, env);
        new JPAWorkingMemoryDbLogger(session);
        JPAProcessInstanceDbLog log = new JPAProcessInstanceDbLog(env);
        session.getWorkItemManager().registerWorkItemHandler("Human Task", new WorkItemHandler() {
			public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
				Map<String, Object> results = new HashMap<String, Object>();
				results.put("Result", "ResultValue");
				manager.completeWorkItem(workItem.getId(), results);
			}
			public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
			}
		});
        
        // record the initial count to compare to later
        List<ProcessInstanceLog> processInstances =
        	log.findProcessInstances("com.sample.ruleflow");
	    int initialProcessInstanceSize = processInstances.size();
        
        // start process instance
		Map<String, Object> params = new HashMap<String, Object>();
		List<String> list = new ArrayList<String>();
		list.add("One");
		list.add("Two");
		String three = "";
		for (int i = 0; i < 1024; i++) {
			three += "*";
		}
		list.add(three);
		params.put("list", list);
		long processInstanceId = session.startProcess("com.sample.ruleflow3", params).getId();
        
        System.out.println("Checking process instances for process 'com.sample.ruleflow3'");
        processInstances = log.findProcessInstances("com.sample.ruleflow3");
        assertEquals(initialProcessInstanceSize + 1, processInstances.size());
        ProcessInstanceLog processInstance = processInstances.get(0);
        System.out.print(processInstance);
        System.out.println(" -> " + processInstance.getStart() + " - " + processInstance.getEnd());
        assertNotNull(processInstance.getStart());
        assertNotNull(processInstance.getEnd());
        assertEquals(processInstanceId, processInstance.getProcessInstanceId());
        assertEquals("com.sample.ruleflow3", processInstance.getProcessId());
        List<VariableInstanceLog> variableInstances = log.findVariableInstances(processInstanceId);
        assertEquals(6, variableInstances.size());
        for (VariableInstanceLog variableInstance: variableInstances) {
        	System.out.println(variableInstance);
            assertEquals(processInstanceId, processInstance.getProcessInstanceId());
            assertEquals("com.sample.ruleflow3", processInstance.getProcessId());
            assertNotNull(variableInstance.getDate());
        }
        log.clear();
        processInstances = log.findProcessInstances("com.sample.ruleflow3");
        assertEquals(0, processInstances.size());
        log.dispose();
	}
	
    private KnowledgeBase createKnowledgeBase() {
    	KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
    	kbuilder.add(new ClassPathResource("ruleflow.rf"), ResourceType.DRF);
    	kbuilder.add(new ClassPathResource("ruleflow2.rf"), ResourceType.DRF);
    	kbuilder.add(new ClassPathResource("ruleflow3.rf"), ResourceType.DRF);
    	KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase();
    	kbase.addKnowledgePackages(kbuilder.getKnowledgePackages());
        return kbase;
    }
}
