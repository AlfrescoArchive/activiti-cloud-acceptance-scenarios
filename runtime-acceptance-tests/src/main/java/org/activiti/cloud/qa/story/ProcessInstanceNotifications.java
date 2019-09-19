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
import java.util.Optional;
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
        notificationsSteps.checkServicesHealth();
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
    
    @When("the user subscribes to $eventTypes notifications")
    public void subscribeToEventTypesNotifications(String eventTypes) throws IOException, InterruptedException {

        String serviceName = notificationsSteps.getRuntimeBundleServiceName();
        String[] eventTypesArray = Arrays.array(eventTypes.split(","));
        long subscriptionTimeoutSeconds = subscriptionTimeoutSeconds();
        long sessionTimeoutSeconds = sessionTimeoutSeconds();
        
        subscriptionRef = new AtomicReference<>();

        CountDownLatch countDownLatch = new CountDownLatch(1);
        AuthToken authToken = TokenHolder.getAuthToken();

        // TODO: add processDefinitionKey when signal events are fixed
        String query = "subscription($serviceName: String!, $eventTypes: [EngineEventType!]) {" +
                        "  engineEvents(serviceName: [$serviceName], eventType: $eventTypes) {" +
                        "    serviceName " +
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
        
        assertThat(countDownLatch.await(sessionTimeoutSeconds, 
                                        TimeUnit.SECONDS))
                                 .as("should subscribe to notifications")
                                 .isTrue();
        
        stepVerifier = StepVerifier.create(data)
                                   .expectSubscription();
    }
    
    @When("the user starts a process $processName")
    public void startProcess(String processName) throws IOException, InterruptedException {
        String processDefinitionKey = processDefinitionKeyMatcher(processName);
        
        processInstanceRef =  new AtomicReference<>(processRuntimeBundleSteps.startProcess(processDefinitionKey, true));
    }

    @When("the user sends a start message named $messageName with businessKey value from session variable called $variableName")
    public void sendStartMessage(String messageName,
                                 String variableName) throws IOException, InterruptedException {

        processInstanceRef =  new AtomicReference<>();
        String variableValue = Serenity.sessionVariableCalled(variableName);
        
        StartMessagePayload payload = MessagePayloadBuilder.start(messageName)
                                                           .withBusinessKey(variableValue)
                                                           .build();
         
        processInstanceRef.set(processRuntimeBundleSteps.message(payload));
    }

    @Then("verify subscription started")
    public void verifySubscriptionStarted() {
        assertThat(subscriptionRef.get()).as("should start the subscription")
                                         .isNotNull();
    }
    
    @Then("verify process instance started response")
    public void verifyProcessInstanceStarted() {
        assertThat(processInstanceRef.get()).as("should receive process instance in the response")
                                            .isNotNull();
    }
    
    @Then("the user sends a message named $messageName with correlationKey value of session variable called $variableName")
    public void sendMessage(String messageName, 
                            String variableName) throws Exception {

        String variableValue = Serenity.sessionVariableCalled(variableName);

        ReceiveMessagePayload payload = MessagePayloadBuilder.receive(messageName)
                                                             .withCorrelationKey(variableValue)
                                                             .build();
        
        processRuntimeBundleSteps.message(payload);
    }    
    
    @Then("verify the status of the process is completed")
    public void verifyProcessCompleted() throws Exception {
        assertThat(processInstanceRef.get()).isNotNull();

        try {
            processQuerySteps.checkProcessInstanceStatus(processInstanceRef.get()
                                                                           .getId(),
                                                         ProcessInstance.ProcessInstanceStatus.COMPLETED);
        } catch(Error cause) {
            cancelSubscription();
            
            throw cause;
        }
    }
    
    @Then("the user completes the subscription")
    public void completeSubscription() {
        assertThat(subscriptionRef.get()).isNotNull();

        cancelSubscription();
    }

    @Then("verify all expected notifications are received")
    public void verifyAllNotificationsAreReceived() {
        long sessionTimeout = sessionTimeoutSeconds(); 

        stepVerifier.expectComplete()
                    .verify(Duration.ofSeconds(sessionTimeout));
    }
    
    @Then("the payload with $eventTypes notifications is expected")
    public void expectPayloadWithEventTypesNotification(String eventTypes) throws Exception {
        
        ObjectMap[] engineEvents = Stream.of(eventTypes.split(","))
                .map(this::engineEvent)
                .toArray(ObjectMap[]::new);
        
        Map<String, Object> payload = payload(engineEvents);
        
        String message = objectMapper.writeValueAsString(payload);

        stepVerifier.expectNext(message);
    }
    
    private void cancelSubscription() {
        // signal to stop receiving notifications 
        subscriptionRef.get()
                       .cancel();
        
    }
    
    private Consumer<Subscription> countDownLatchAction(CountDownLatch countDownLatch, 
                                                        AtomicReference<Subscription> subscriptionRef,
                                                        Duration duration,
                                                        Runnable action) {
        return (subscription) -> {
            subscriptionRef.set(subscription);

            Mono.just(subscription)
                .delaySubscription(duration)
                .doOnError(error -> subscriptionRef.get()
                                                   .cancel())
                .doOnSubscribe(it -> {
                    action.run();
                })
                .subscribe(it -> { 
                    countDownLatch.countDown();
                });
        };
    }

    private Long sessionTimeoutSeconds() {
        return sessionVariableCalled("sessionTimeoutSeconds", 
                                     Long.class).orElse(Long.valueOf(6));
    }
    
    private Long subscriptionTimeoutSeconds() {
        return sessionVariableCalled("subscriptionTimeoutSeconds", 
                                     Long.class).orElse(Long.valueOf(3));
    }
    
    private <T> Optional<T> sessionVariableCalled(String key, Class<T> clazz) {
        return Optional.ofNullable(Serenity.sessionVariableCalled(key));
    }
    
    @SuppressWarnings("serial")
    private ObjectMap engineEvent(String eventType) {
        return new ObjectMap() {{
                put("serviceName", notificationsSteps.getRuntimeBundleServiceName());
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
