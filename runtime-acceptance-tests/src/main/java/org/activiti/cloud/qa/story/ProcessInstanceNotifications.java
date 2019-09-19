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

import static org.activiti.cloud.qa.helpers.ProcessDefinitionRegistry.processDefinitionKeyMatcher;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.serenitybdd.core.Serenity;
import net.thucydides.core.annotations.Steps;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.builders.MessagePayloadBuilder;
import org.activiti.api.process.model.payloads.ReceiveMessagePayload;
import org.activiti.api.process.model.payloads.StartMessagePayload;
import org.activiti.cloud.acc.core.steps.notifications.NotificationsSteps;
import org.activiti.cloud.acc.core.steps.query.ProcessQuerySteps;
import org.activiti.cloud.acc.core.steps.runtime.ProcessRuntimeBundleSteps;
import org.activiti.cloud.acc.shared.model.AuthToken;
import org.activiti.cloud.acc.shared.rest.TokenHolder;
import org.assertj.core.util.Arrays;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.reactivestreams.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ReplayProcessor;
import reactor.test.StepVerifier;
import reactor.test.StepVerifier.Step;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class ProcessInstanceNotifications {

    @Steps
    private ProcessRuntimeBundleSteps processRuntimeBundleSteps;
    
    @Steps
    private ProcessQuerySteps processQuerySteps;
    
    @Steps
    private NotificationsSteps notificationsSteps;
    
    @Autowired
    private ObjectMapper objectMapper = new ObjectMapper();
    
    private AtomicReference<ProcessInstance> processInstanceRef;
    private ReplayProcessor<String> data;
    private AtomicReference<Subscription> subscriptionRef;
    
    private Step<String> stepVerifier;

    @When("services are started")
    public void checkServicesStatus() {
        processRuntimeBundleSteps.checkServicesHealth();
        processQuerySteps.checkServicesHealth();
    }
    
    @Given("generated random value for session variable called $variableName")
    public void generateUniqueBusinessId(String variableName) {
        Serenity.setSessionVariable(variableName).to(UUID.randomUUID().toString());
    }

    @Given("session timeout of $timeoutSeconds seconds")
    public void setSessionTimeoutSeconds(long timeoutSeconds) {
        Serenity.setSessionVariable("sessionTimeoutSeconds").to(timeoutSeconds);
    }

    @Given("subscription timeout of $timeoutSeconds seconds")
    public void setSubscriptionTimeoutSeconds(long timeoutSeconds) {
        Serenity.setSessionVariable("subscriptionTimeoutSeconds").to(timeoutSeconds);
    }
    
    @When("the user subscribes to the list of $eventTypes notifications")
    public void subscribeToNotifications(String eventTypes) throws IOException, InterruptedException {

        String serviceName = notificationsSteps.getRuntimeBundleServiceName();
        String[] eventTypesArray = Arrays.array(eventTypes.split(","));
        long subscriptionTimeoutSeconds = subscriptionTimeoutSeconds();
        long sessionTimeoutSeconds = sessionTimeoutSeconds();
        
        subscriptionRef = new AtomicReference<>();

        CountDownLatch countDownLatch = new CountDownLatch(1);
        AuthToken authToken = TokenHolder.getAuthToken();
        
        String query = "subscription($serviceName: String!, $eventTypes: [EngineEventType!]) {" +
                        "  engineEvents(serviceName: [$serviceName], eventType: $eventTypes) {" +
                        "    serviceName " +
                        "    processDefinitionKey " +
                        "    eventType " +
                        "  }" +
                        "}";

        Map<String, Object> variables = Map.of("serviceName", serviceName,
                                               "eventTypes", eventTypesArray);

        Consumer<Subscription> action = countDownLatchAction(countDownLatch, 
                                                             subscriptionRef, 
                                                             Duration.ofSeconds(subscriptionTimeoutSeconds),
                                                             () -> {});
        
        data = notificationsSteps.subscribe(authToken.getAccess_token(), 
                                            query, 
                                            variables, 
                                            action);
        
        
        assertThat(countDownLatch.await(sessionTimeoutSeconds, TimeUnit.SECONDS)).as("should subscribe to notifications")
                                                                                 .isTrue();

        stepVerifier = StepVerifier.create(data)
                                   .expectSubscription();
    }
    
    @When("the user starts a process $processName with PROCESS_STARTED and PROCESS_COMPLETED events subscriptions")
    public void startProcess(String processName) throws IOException, InterruptedException {

        processInstanceRef =  new AtomicReference<>();
        subscriptionRef = new AtomicReference<>();
                
        CountDownLatch countDownLatch = new CountDownLatch(1);
        AuthToken authToken = TokenHolder.getAuthToken();
        
        String query = "subscription($serviceName: String!, $processDefinitionKey: String!, $eventTypes: [EngineEventType!]) {" +
                      "  engineEvents(serviceName: [$serviceName], processDefinitionKey: [$processDefinitionKey], eventType: $eventTypes ) {" +
                      "    processDefinitionKey " +
                      "    eventType " +
                      "  }" +
                      "}";
            
        Map<String, Object> variables = Map.of("serviceName", notificationsSteps.getRuntimeBundleServiceName(),
                                               "processDefinitionKey",processDefinitionKeyMatcher(processName),
                                               "eventTypes", Arrays.array("PROCESS_STARTED", "PROCESS_COMPLETED"));

        Consumer<Subscription> action = startProcessAction(processName, countDownLatch, subscriptionRef);
        
        data = notificationsSteps.subscribe(authToken.getAccess_token(), query, variables, action);

        assertThat(countDownLatch.await(10, TimeUnit.SECONDS)).as("should start a process with events subscription")
                                                              .isTrue();
    }

    @When("the user starts a process $processName with SIGNAL_RECEIVED subscription")
    public void startProcessWithSignalSubscription(String processName) throws IOException, InterruptedException {

        processInstanceRef =  new AtomicReference<>();
        subscriptionRef = new AtomicReference<>();

        CountDownLatch countDownLatch = new CountDownLatch(1);
        AuthToken authToken = TokenHolder.getAuthToken();
         
        String query = "subscription($serviceName: String!) {" +
                        "  engineEvents(serviceName: [$serviceName], eventType: [SIGNAL_RECEIVED]) {" +
                        "    serviceName " +
                        "    eventType " +
                        "  }" +
                        "}";

        Map<String, Object> variables = Map.of("serviceName", notificationsSteps.getRuntimeBundleServiceName());

        Consumer<Subscription> action = startProcessAction(processName, countDownLatch, subscriptionRef);
        
        data = notificationsSteps.subscribe(authToken.getAccess_token(), query, variables, action);
        
        assertThat(countDownLatch.await(10, TimeUnit.SECONDS)).as("should start a process with subscription")
                                                              .isTrue();
    }   
    
    @When("the user starts a process $processName with TIMER subscriptions")
    public void startProcessWithTimerSubscription(String processName) throws IOException, InterruptedException {

        processInstanceRef =  new AtomicReference<>();
        subscriptionRef = new AtomicReference<>();

        CountDownLatch countDownLatch = new CountDownLatch(1);
        AuthToken authToken = TokenHolder.getAuthToken();
         
        String query = "subscription($serviceName: String!, $eventTypes: [EngineEventType!]) {" +
                        "  engineEvents(serviceName: [$serviceName], eventType: $eventTypes) {" +
                        "    serviceName " +
                        "    processDefinitionKey " +
                        "    eventType " +
                        "  }" +
                        "}";

        Map<String, Object> variables = Map.of("serviceName", notificationsSteps.getRuntimeBundleServiceName(),
                                               "eventTypes", Arrays.array("TIMER_SCHEDULED", "TIMER_FIRED", "TIMER_EXECUTED"));
        
        Consumer<Subscription> action = startProcessAction(processName, countDownLatch, subscriptionRef);
        
        data = notificationsSteps.subscribe(authToken.getAccess_token(), query, variables, action);
        
        assertThat(countDownLatch.await(10, TimeUnit.SECONDS)).as("should start a process with subscription")
                                                              .isTrue();
    }       

    @When("the user sends message $messageName with businessKey value of $variableName session variable")
    public void sendStartMessage(String messageName,
                                 String variableName) throws IOException, InterruptedException {

        processInstanceRef =  new AtomicReference<>();
        String variableValue = Serenity.sessionVariableCalled(variableName);
        
        StartMessagePayload payload = MessagePayloadBuilder.start(messageName)
                                                           .withBusinessKey(variableValue)
                                                           .build();
         
        processInstanceRef.set(processRuntimeBundleSteps.message(payload));
        
        assertThat(processInstanceRef.get()).as("should start a process by message")
                                            .isNotNull();
    }
    
    @Then("the user sends a message named $messageName with correlationKey value of $variableName session variable")
    public void sendMessage(String messageName, 
                            String variableName) throws Exception {

        String variableValue = Serenity.sessionVariableCalled(variableName);

        ReceiveMessagePayload payload = MessagePayloadBuilder.receive(messageName)
                                                             .withCorrelationKey(variableValue)
                                                             .build();
        
        processRuntimeBundleSteps.message(payload);
    }    
    
    @Then("the status of the process is completed")
    public void verifyProcessCompleted() throws Exception {
        assertThat(processInstanceRef.get()).isNotNull();

        try {
            processQuerySteps.checkProcessInstanceStatus(processInstanceRef.get()
                                                                           .getId(),
                                                         ProcessInstance.ProcessInstanceStatus.COMPLETED);
        } finally {
            // signal to stop receiving notifications 
            subscriptionRef.get()
                           .cancel();
        }
    }
    
    @Then("the user completes subscription with all notifications received")
    public void completeSubscription() {
        long sessionTimeout = sessionTimeoutSeconds(); 
        
        stepVerifier.expectComplete()
                    .verify(Duration.ofSeconds(sessionTimeout));
    }
    
    @SuppressWarnings("serial")
    @Then("PROCESS_STARTED and PROCESS_COMPLETED notifications are received")
    public void verifyNotifications() throws Exception {
        Map<String, Object> startProcessMessagePayload = new ObjectMap() {{
            put("payload", new ObjectMap() {{
                put("data", new ObjectMap() {{
                    put("engineEvents", Arrays.array(new ObjectMap() {{
                        put("processDefinitionKey", "ConnectorProcess");
                        put("eventType", "PROCESS_STARTED");
                    }}));
                }});
            }});
            put("id","1");
            put("type", "data");
        }};
        
        Map<String, Object> completeProcessMessagePayload = new ObjectMap() {{
            put("payload", new ObjectMap() {{
                put("data", new ObjectMap() {{
                    put("engineEvents", Arrays.array(new ObjectMap() {{
                        put("processDefinitionKey", "ConnectorProcess");
                        put("eventType", "PROCESS_COMPLETED");
                    }}));
                }});
            }});
            put("id","1");
            put("type", "data");
        }};
        
        
        String startProcessMessage = objectMapper.writeValueAsString(startProcessMessagePayload);
        String completeProcessMessage = objectMapper.writeValueAsString(completeProcessMessagePayload);
        
        notificationsSteps.verifyData(data, startProcessMessage, completeProcessMessage);
    }
    
    @SuppressWarnings("serial")
    @Then("SIGNAL_RECEIVED notification with $elementId signal event is received")
    public void verifySignalReceivedEventNotifications(String elementId) throws Exception {
        
        Map<String, Object> payload = new ObjectMap() {{
            put("payload", new ObjectMap() {{
                put("data", new ObjectMap() {{
                    put("engineEvents", Arrays.array(new ObjectMap() {{
                        put("serviceName", notificationsSteps.getRuntimeBundleServiceName());
                        put("eventType", "SIGNAL_RECEIVED");
                    }}));
                }});
            }});
            put("id","1");
            put("type", "data");
        }};

        String expected =  objectMapper.writeValueAsString(payload);
        
        notificationsSteps.verifyData(data, expected);
    }
    
    @SuppressWarnings("serial")
    @Then("TIMER notifications are received")
    public void verifyTimerNotifications() throws Exception {
        ProcessInstance processInstnace = processInstanceRef.get();
        
        Map<String, Object> timerScheduledMessagePayload = new ObjectMap() {{
            put("payload", new ObjectMap() {{
                put("data", new ObjectMap() {{
                    put("engineEvents", Arrays.array(new ObjectMap() {{
                        put("serviceName", notificationsSteps.getRuntimeBundleServiceName());
                        put("processDefinitionKey", processInstnace.getProcessDefinitionKey());
                        put("eventType", "TIMER_SCHEDULED");
                    }}));
                }});
            }});
            put("id","1");
            put("type", "data");
        }};
        
        Map<String, Object> timerFiredMessagePayload = new ObjectMap() {{
            put("payload", new ObjectMap() {{
                put("data", new ObjectMap() {{
                    put("engineEvents", 
                        Arrays.array(new ObjectMap() {{
                            put("serviceName", notificationsSteps.getRuntimeBundleServiceName());
                            put("processDefinitionKey", processInstnace.getProcessDefinitionKey());
                            put("eventType", "TIMER_FIRED");
                        }},
                        new ObjectMap() {{
                            put("serviceName", notificationsSteps.getRuntimeBundleServiceName());
                            put("processDefinitionKey", processInstnace.getProcessDefinitionKey());
                            put("eventType", "TIMER_EXECUTED");
                        }}));
                }});
            }});
            put("id","1");
            put("type", "data");
        }};

        String startProcessMessage = objectMapper.writeValueAsString(timerScheduledMessagePayload);
        String completeProcessMessage = objectMapper.writeValueAsString(timerFiredMessagePayload);
        
        notificationsSteps.verifyData(data, startProcessMessage, completeProcessMessage);
    }    
    
    @Then("the list of $eventTypes notifications are received")
    public void verifyMessageReceivedWaitingNotifications(String eventTypes) throws Exception {
        
        ObjectMap[] engineEvents = Stream.of(eventTypes.split(","))
                .map(this::engineEvent)
                .toArray(ObjectMap[]::new);
        
        Map<String, Object> payload = payload(engineEvents);
        
        String message = objectMapper.writeValueAsString(payload);

        stepVerifier.expectNext(message);
    }    
        
    @SuppressWarnings("serial")
    @Then("MESSAGE_RECEIVED and MESSAGE_SENT notifications are received")
    public void verifyMessageReceivedSentNotifications() throws Exception {
        ProcessInstance processInstnace = processInstanceRef.get();
        
        Map<String, Object> receiveMessagePayload = new ObjectMap() {{
            put("payload", new ObjectMap() {{
                put("data", new ObjectMap() {{
                    put("engineEvents",
                        Arrays.array(new ObjectMap() {{
                            put("serviceName", notificationsSteps.getRuntimeBundleServiceName());
                            put("processDefinitionKey", processInstnace.getProcessDefinitionKey());
                            put("eventType", "MESSAGE_RECEIVED");
                        }},                                     
                        new ObjectMap() {{
                            put("serviceName", notificationsSteps.getRuntimeBundleServiceName());
                            put("processDefinitionKey", processInstnace.getProcessDefinitionKey());
                            put("eventType", "MESSAGE_SENT");
                        }}));
                }});
            }});
            put("id","1");
            put("type", "data");
        }};

        String completeProcessMessage = objectMapper.writeValueAsString(receiveMessagePayload);
        
        notificationsSteps.verifyData(data, completeProcessMessage);
    }    
            
    
    private Consumer<Subscription> startProcessAction(String processName, CountDownLatch countDownLatch, AtomicReference<Subscription> subscriptionRef) {
        return (s) -> {
            subscriptionRef.set(s);

            Mono.just(s)
                .delaySubscription(Duration.ofSeconds(2))
                .doOnError(e -> subscriptionRef.get()
                                               .cancel())
                .subscribe(it -> { 
                    try {
                        String processDefinitionKey = processDefinitionKeyMatcher(processName);
                        
                        processInstanceRef.set(processRuntimeBundleSteps.startProcess(processDefinitionKey, true));
                        
                        countDownLatch.countDown();
                    } catch (Exception cause) {
                        throw new RuntimeException(cause);
                    }
                });
        };
    }
    
    private Consumer<Subscription> countDownLatchAction(CountDownLatch countDownLatch, 
                                                        AtomicReference<Subscription> subscriptionRef,
                                                        Duration duration,
                                                        Runnable action) {
        return (s) -> {
            subscriptionRef.set(s);

            Mono.just(s)
                .delaySubscription(duration)
                .doOnError(e -> subscriptionRef.get()
                                               .cancel())
                .subscribe(it -> { 
                    try {
                        countDownLatch.countDown();
                        
                        action.run();
                    } catch (Exception cause) {
                        throw new RuntimeException(cause);
                    }
                });
        };
    }

    private long sessionTimeoutSeconds() {
        long timeoutSeconds = Serenity.sessionVariableCalled("sessionTimeoutSeconds");
        
        if (timeoutSeconds  < 0) {
            timeoutSeconds = 0;
        }
        
        return timeoutSeconds;

    }
    
    private long subscriptionTimeoutSeconds() {
        long timeoutSeconds = Serenity.sessionVariableCalled("subscriptionTimeoutSeconds");
        
        if (timeoutSeconds  < 0) {
            timeoutSeconds = 0;
        }
        
        return timeoutSeconds;

    }
    
    @SuppressWarnings("serial")
    private ObjectMap engineEvent(String eventType) {
        ProcessInstance processInstance = processInstanceRef.get();

        return new ObjectMap() {{
                put("serviceName", notificationsSteps.getRuntimeBundleServiceName());
                put("processDefinitionKey", processInstance.getProcessDefinitionKey());
                put("eventType", eventType);
            }}; 
    }
    
    @SuppressWarnings("serial")
    private ObjectMap payload(ObjectMap[] engineEvents) {
        return new ObjectMap() {{
            put("payload", new ObjectMap() {{
                put("data", new ObjectMap() {{
                    put("engineEvents", engineEvents);
                }});
            }});
            put("id","1");
            put("type", "data");
        }};
    }
    
    @SuppressWarnings("serial")
    class ObjectMap extends LinkedHashMap<String, Object> {
    }
}
