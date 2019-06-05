/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.qa.story;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;

import net.thucydides.core.annotations.Steps;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.cloud.acc.core.steps.audit.AuditSteps;
import org.activiti.cloud.acc.core.steps.query.ProcessQuerySteps;
import org.activiti.cloud.acc.core.steps.runtime.ProcessRuntimeBundleSteps;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

public class ProcessInstanceSignalEvents {

    @Steps
    private ProcessRuntimeBundleSteps runtimeBundleSteps;
    
    @Steps
    private ProcessQuerySteps processQuerySteps;
    
    @Steps
    private AuditSteps auditSteps;
    
    private CloudProcessInstance processInstanceCatchSignal;
    private CloudProcessInstance processInstanceThrowSignal;
    
    @When("services are started")
    public void checkServicesStatus() {
        runtimeBundleSteps.checkServicesHealth();
        processQuerySteps.checkServicesHealth();
        auditSteps.checkServicesHealth();
    }
    
    @When("the user starts a process with intermediate catch signal")
    public void startSignalCatchProcessInstance() {
        processInstanceCatchSignal = runtimeBundleSteps.startProcess("SignalCatchEventProcess");
        assertThat(processInstanceCatchSignal).isNotNull();
    }
    
    @When("the user starts a process with intermediate throw signal")
    public void startSignalThrowProcessInstance() {
        processInstanceThrowSignal = runtimeBundleSteps.startProcess("SignalThrowEventProcess");
        assertThat(processInstanceThrowSignal).isNotNull();
    }

    @Then("the process throwing a signal is completed")
    public void sheckSignalThrowProcessInstance() throws Exception {
        processQuerySteps.checkProcessInstanceStatus(processInstanceThrowSignal.getId(),
                                                     ProcessInstance.ProcessInstanceStatus.COMPLETED);
    }
    
    @Then("the process catching a signal is completed")
    public void sheckSignalCatchProcessInstance() throws Exception {
        processQuerySteps.checkProcessInstanceStatus(processInstanceCatchSignal.getId(),
                                                     ProcessInstance.ProcessInstanceStatus.COMPLETED);       
    }
    
    @Then("the SIGNAL_RECEIVED event was catched up")
    public void sheckSignalReceivedEvent() throws Exception {
        Collection<CloudRuntimeEvent> events = auditSteps.getEventsByProcessInstanceIdAndEventType(processInstanceCatchSignal.getId(),
                                                                                                   "SIGNAL_RECEIVED"); 
        
        assertThat(events).isNotEqualTo(0);        
    }
    
  

}
