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
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import net.serenitybdd.core.Serenity;
import net.thucydides.core.annotations.Steps;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.builders.MessagePayloadBuilder;
import org.activiti.api.process.model.events.BPMNMessageEvent;
import org.activiti.api.process.model.payloads.ReceiveMessagePayload;
import org.activiti.api.process.model.payloads.StartMessagePayload;
import org.activiti.cloud.acc.core.steps.audit.AuditSteps;
import org.activiti.cloud.acc.core.steps.query.ProcessQuerySteps;
import org.activiti.cloud.acc.core.steps.runtime.ProcessRuntimeBundleSteps;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

public class ProcessInstanceMessages {

    @Steps
    private ProcessRuntimeBundleSteps processRuntimeBundleSteps;
    
    @Steps
    private ProcessQuerySteps processQuerySteps;
    
    @Steps
    private AuditSteps auditSteps;
    
    private ProcessInstance processInstance;
    
    @When("services are started")
    public void checkServicesStatus() {
        processRuntimeBundleSteps.checkServicesHealth();
        processQuerySteps.checkServicesHealth();
        auditSteps.checkServicesHealth();
    }
    
    @When("the user sends a start message named $messageName with businessKey value $businessKey")
    public void startMessage(String messageName, String businessKey) throws IOException, InterruptedException {      
        StartMessagePayload payload = MessagePayloadBuilder.start(messageName)
                                                           .withBusinessKey(businessKey)
                                                           .build();

        processInstance = processRuntimeBundleSteps.message(payload);
        
        Serenity.setSessionVariable("processInstanceId").to(processInstance.getId());
    }

    @Then("the sends a message named $messageName with correlationKey value $correlationKey")
    public void receiveMessage(String messageName, String correlationKey) throws IOException, InterruptedException {      
        ReceiveMessagePayload payload = MessagePayloadBuilder.receive(messageName)
                                                             .withCorrelationKey(correlationKey)
                                                             .build();

        processRuntimeBundleSteps.message(payload);
    }
    
    @Then("MESSAGE_RECEIVED events are emitted for the message '$messageName' and timeout $timeoutSeconds seconds")
    public void verifyTimerScheduleEventsEmitted(String messageName,
                                                 long timeoutSeconds) throws Exception {
        if (timeoutSeconds  < 0) {
            timeoutSeconds = 0;
        }
        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");
        
        await().atMost(timeoutSeconds, TimeUnit.SECONDS)
               .untilAsserted(() -> {
                   Collection<CloudRuntimeEvent> events = auditSteps.getEventsByProcessAndEntityId(processInstanceId,
                                                                                                   messageName);
                   assertThat(events)
                                     .isNotEmpty()
                                     .extracting("eventType",
                                                 "entityId",
                                                 "processInstanceId")
                                     .contains(tuple(BPMNMessageEvent.MessageEvents.MESSAGE_RECEIVED,
                                                     messageName,
                                                     processInstanceId));
               });
    }
    
    @Then("MESSAGE_WAITING events are emitted for the message '$messageName' and timeout $timeoutSeconds seconds")
    public void verifyMessageWaitingEventsEmitted(String messageName,
                                                 long timeoutSeconds) throws Exception {
     
        if (timeoutSeconds  < 0) {
            timeoutSeconds = 0;
        }
        
        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");
        await().atMost(timeoutSeconds, TimeUnit.SECONDS)
        .untilAsserted(() -> {
            Collection<CloudRuntimeEvent> events = auditSteps.getEventsByProcessAndEntityId(processInstanceId,
                                                                                            messageName);
            assertThat(events)
                              .isNotEmpty()
                              .extracting("eventType",
                                          "entityId",
                                          "processInstanceId")
                              .contains(tuple(BPMNMessageEvent.MessageEvents.MESSAGE_WAITING,
                                              messageName,
                                              processInstanceId));
        });
    }

    @Then("MESSAGE_SENT events are emitted for the message '$messageName' and timeout $timeoutSeconds seconds")
    public void verifyMessageSentEventsEmitted(String messageName,
                                               long timeoutSeconds) throws Exception {
     
        if (timeoutSeconds  < 0) {
            timeoutSeconds = 0;
        }
        
        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");
        await().atMost(timeoutSeconds, TimeUnit.SECONDS)
        .untilAsserted(() -> {
            Collection<CloudRuntimeEvent> events = auditSteps.getEventsByProcessAndEntityId(processInstanceId,
                                                                                            messageName);
            assertThat(events)
                              .isNotEmpty()
                              .extracting("eventType",
                                          "entityId",
                                          "processInstanceId")
                              .contains(tuple(BPMNMessageEvent.MessageEvents.MESSAGE_SENT,
                                              messageName,
                                              processInstanceId));
        });
    }
    
    @Then("the process with message events is completed")
    public void verifyProcessCompleted() throws Exception {
        String processId = Serenity.sessionVariableCalled("processInstanceId");
        
        processQuerySteps.checkProcessInstanceStatus(processId,
                                                     ProcessInstance.ProcessInstanceStatus.COMPLETED);
    }
    
}
