/*
 * Copyright 2019 Alfresco, Inc. and/or its affiliates.
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.activiti.cloud.qa.helpers.ProcessDefinitionRegistry.processDefinitionKeyMatcher;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import net.serenitybdd.core.Serenity;
import net.thucydides.core.annotations.Steps;
import org.activiti.api.model.shared.event.VariableEvent;
import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.cloud.acc.core.steps.audit.AuditSteps;
import org.activiti.cloud.acc.core.steps.query.ProcessQuerySteps;
import org.activiti.cloud.acc.core.steps.query.TaskQuerySteps;
import org.activiti.cloud.acc.core.steps.runtime.ProcessRuntimeBundleSteps;
import org.activiti.cloud.acc.core.steps.runtime.ProcessVariablesRuntimeBundleSteps;
import org.activiti.cloud.acc.core.steps.runtime.TaskRuntimeBundleSteps;
import org.activiti.cloud.acc.core.steps.runtime.admin.TaskRuntimeAdminSteps;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.api.task.model.CloudTask;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.springframework.hateoas.Resources;

public class ProcessInstanceVariablesMappingTypes {

    @Steps
    private ProcessRuntimeBundleSteps processRuntimeBundleSteps;
    @Steps
    private ProcessVariablesRuntimeBundleSteps processVariablesRuntimeBundleSteps;
    @Steps
    private ProcessQuerySteps processQuerySteps;
    @Steps
    private AuditSteps auditSteps;
    @Steps
    private TaskRuntimeBundleSteps taskRuntimeBundleSteps;
    @Steps
    private TaskRuntimeAdminSteps taskRuntimeAdminSteps;
    @Steps
    private TaskQuerySteps taskQuerySteps;

    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    SimpleDateFormat longFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    Date date = format.parse("2019-09-09");
    Date longDate = longFormat.parse("2019-09-09T10:20:30.000Z");
    String processVariableString = "process-variable-string";
    String variableValue1 = "stringValue1";
    String processVariableInteger = "process-variable-integer";
    Integer variableValue2 = Integer.valueOf(123);
    String processVariableBoolean = "process-variable-boolean";
    Boolean variableValue3 = Boolean.valueOf(true);
    String processVariableDate = "process-variable-date";
    String processVariableDateTime = "process-variable-datetime";

    String taskVariableString = "task-variable-string";
    String taskVariableDatetime = "task-variable-datetime";
    String taskTaskVariableInteger = "task-variable-integer";
    String taskVariableBoolean = "task-variable-boolean";
    String taskVariableDate = "task-variable-date";

    String taskName = "My task1";

    public ProcessInstanceVariablesMappingTypes() throws ParseException {
    }

    private Map<String, Object> getVariablesMap(String variableName1,
                                                Object variableValue1,
                                                String variableName2,
                                                Object variableValue2,
                                                String variableName3,
                                                Object variableValue3
//                                                String processVariableDate,
//                                                Object variableValue4,
//                                                String processVariableDateTime,
//                                                Object variableValue5
//
    ) {

        Map<String, Object> variables = new HashMap<>();
        variables.put(variableName1,
                      variableValue1);
        variables.put(variableName2,
                      variableValue2);
        variables.put(variableName3,
                      variableValue3);
//        variables.put(processVariableDate,
//                      variableValue4);
//        variables.put(processVariableDateTime,
//                      variableValue5);

        return variables;
    }

    @When("services are started")
    public void checkServicesStatus() {
        processRuntimeBundleSteps.checkServicesHealth();
        taskRuntimeBundleSteps.checkServicesHealth();
        processQuerySteps.checkServicesHealth();
        taskQuerySteps.checkServicesHealth();
        auditSteps.checkServicesHealth();
    }

    @When("the user starts the process $processName")
    public void startProcess(String processName) throws ParseException {

        Map<String, Object> variables = getVariablesMap(processVariableString,
                                                        variableValue1,
                                                        processVariableInteger,
                                                        variableValue2,
                                                        processVariableBoolean,
                                                        variableValue3
//                                                        processVariableDate,
//                                                        date,
//                                                        processVariableDateTime,
//                                                        longDate
        );

        ProcessInstance processInstance = processRuntimeBundleSteps.startProcessWithVariables(
                processDefinitionKeyMatcher(processName),
                variables);

        Serenity.setSessionVariable("processInstanceId").to(processInstance.getId());

        await().untilAsserted(() -> {
            assertThat(processVariableString).isNotNull();
            assertThat(processVariableInteger).isNotNull();
            assertThat(processVariableBoolean).isNotNull();
//            assertThat(processVariableDate).isNotNull();
//            assertThat(processVariableDateTime).isNotNull();

            final Resources<CloudVariableInstance> cloudVariableInstanceResource = getProcessVariables(processInstance.getId());

            assertThat(cloudVariableInstanceResource).isNotNull();
            assertThat(cloudVariableInstanceResource).isNotEmpty();
            assertThat(cloudVariableInstanceResource.getContent()).extracting(VariableInstance::getName,
                                                                              VariableInstance::getValue)
                    .contains(
                            tuple(processVariableString,
                                  variableValue1),
                            tuple(processVariableInteger,
                                  variableValue2),
                            tuple(processVariableBoolean,
                                  variableValue3)
//                            tuple(processVariableDate, variableValue4)
//                            tuple(processVariableBoolean, variableValue3)
                    );
        });
    }

    @When("variables have correct values")
    public void checkProcessInstanceVariables() {

        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");

        await().untilAsserted(() -> {
            assertThat(processVariableString).isNotNull();
            assertThat(processVariableInteger).isNotNull();
            assertThat(processVariableBoolean).isNotNull();
//            assertThat(processVariableDate).isNotNull();
//            assertThat(processVariableDateTime).isNotNull();

            final Resources<CloudVariableInstance> cloudVariableInstanceResource = getProcessVariables(processInstanceId);

            assertThat(cloudVariableInstanceResource).isNotNull();
            assertThat(cloudVariableInstanceResource).isNotEmpty();
            assertThat(cloudVariableInstanceResource.getContent()).extracting(VariableInstance::getName,
                                                                              VariableInstance::getValue)
                    .contains(
                            tuple(processVariableString,
                                  variableValue1),
                            tuple(processVariableInteger,
                                  variableValue2),
                            tuple(processVariableBoolean,
                                  variableValue3)
//                            tuple(processVariableDate, variableValue4)
//                            tuple(processVariableBoolean, variableValue3)
                    );
        });
    }

    @When("variables have correct types in rb")
    public void checkProcessInstanceVariablesTypes() {

        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");

        await().untilAsserted(() -> {
            assertThat(processVariableString).isNotNull();
            assertThat(processVariableInteger).isNotNull();
            assertThat(processVariableBoolean).isNotNull();
//            assertThat(processVariableDate).isNotNull();
//            assertThat(processVariableDateTime).isNotNull();

            final Resources<CloudVariableInstance> cloudVariableInstanceResource = getProcessVariables(processInstanceId);

            assertThat(cloudVariableInstanceResource).isNotNull();
            assertThat(cloudVariableInstanceResource).isNotEmpty();
            assertThat(cloudVariableInstanceResource.getContent()).extracting(VariableInstance::getName,
                                                                              VariableInstance::getType)
                    .contains(
                            tuple(processVariableString,
                                  "string"),
                            tuple(processVariableInteger,
                                  "integer"),
                            tuple(processVariableBoolean,
                                  "boolean")
//                            tuple(processVariableDate, "date")
//                            tuple(processVariableDateTime, "date")
                    );
        });
    }

    @When("the process variables are created")
    public void verifyProcessVariableCreated() {
        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");

        await().untilAsserted(() -> {
            final Resources<CloudVariableInstance> variableInstances = getProcessVariables(processInstanceId);
            assertThat(variableInstances).isNotNull();
            assertThat(variableInstances).isNotEmpty();
            assertThat(variableInstances.getContent()).extracting(VariableInstance::getName).contains(processVariableString);
            assertThat(variableInstances.getContent()).extracting(VariableInstance::getName).contains(processVariableInteger);
            assertThat(variableInstances.getContent()).extracting(VariableInstance::getName).contains(processVariableBoolean);
//            assertThat(variableInstances.getContent()).extracting(VariableInstance::getName).contains(processVariableDate);
//            assertThat(variableInstances.getContent()).extracting(VariableInstance::getName).contains(processVariableDateTime);
        });
    }

    public Resources<CloudVariableInstance> getProcessVariables(String processInstanceId) {
        return processVariablesRuntimeBundleSteps.getVariables(processInstanceId);
    }

    @When("check variables in query")
    public void checkQuerykProcessInstanceVariable() {

        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");

        // TODO add variable value check in processQuerySteps
        processQuerySteps.checkProcessInstanceHasVariable(processInstanceId,
                                                          processVariableString);
        processQuerySteps.checkProcessInstanceHasVariable(processInstanceId,
                                                          processVariableInteger);
        processQuerySteps.checkProcessInstanceHasVariable(processInstanceId,
                                                          processVariableBoolean);
//        processQuerySteps.checkProcessInstanceHasVariable(processInstanceId, processVariableDate);
//        processQuerySteps.checkProcessInstanceHasVariable(processInstanceId, processVariableDateTime);
    }

    @When("variables was created event in audit")
    public void verifyVariableCreated() throws Exception {
        String processId = Serenity.sessionVariableCalled("processInstanceId");

        auditSteps.checkProcessInstanceVariableEvent(processId,
                                                     processVariableString,
                                                     VariableEvent.VariableEvents.VARIABLE_CREATED);
        auditSteps.checkProcessInstanceVariableEvent(processId,
                                                     processVariableInteger,
                                                     VariableEvent.VariableEvents.VARIABLE_CREATED);
        auditSteps.checkProcessInstanceVariableEvent(processId,
                                                     processVariableBoolean,
                                                     VariableEvent.VariableEvents.VARIABLE_CREATED);
        auditSteps.checkProcessInstanceVariableEvent(processId,
                                                     processVariableDate,
                                                     VariableEvent.VariableEvents.VARIABLE_CREATED);
        auditSteps.checkProcessInstanceVariableEvent(processId,
                                                     processVariableDateTime,
                                                     VariableEvent.VariableEvents.VARIABLE_CREATED);
    }

    @When("variables values created in task with variable mapping are correct")
    public void verifyVariableCreatedInTask() throws Exception {
        String processId = Serenity.sessionVariableCalled("processInstanceId");
        String taskName = "My task1";

        Task task = processRuntimeBundleSteps.getTaskByProcessInstanceId(processId).stream().filter(t -> t.getName().equals(taskName)).findFirst().orElse(null);
        assertThat(task).isNotNull();
        assertThat(task.getStatus()).isEqualTo(Task.TaskStatus.CREATED);
        Resources<CloudVariableInstance> rbVariables = taskRuntimeBundleSteps.getVariables(task.getId());
        Serenity.setSessionVariable("taskId").to(task.getId());
        assertThat(rbVariables)
                .extracting(CloudVariableInstance::getName,
                            CloudVariableInstance::getValue)
                .contains(
                        tuple(taskVariableBoolean,
                              true),
                        tuple(taskVariableString,
                              "stringValue1"),
                        tuple(taskTaskVariableInteger,
                              123)
//                        tuple(taskVariableDatetime,"stringValue1"),
//                        tuple(taskVariableDate,"stringValue1")
                );
    }

    @When("variables types in task are correct")
    public void verifyVariableTypeInTask() throws Exception {
        String taskId = Serenity.sessionVariableCalled("taskId");

        Resources<CloudVariableInstance> rbVariables = taskRuntimeBundleSteps.getVariables(taskId);
        assertThat(rbVariables)
                .extracting(CloudVariableInstance::getName,
                            CloudVariableInstance::getType)
                .contains(
                        tuple(taskVariableBoolean,
                              "boolean"),
                        tuple(taskVariableString,
                              "string"),
                        tuple(taskTaskVariableInteger,
                              "integer"),
                        tuple(taskVariableDatetime,
                              "date"),
                        tuple(taskVariableDate,
                              "date")
                );
    }


    @When("update task variables")
    public void updateTaskVariables() throws Exception {
        String taskId = Serenity.sessionVariableCalled("taskId");
        taskRuntimeBundleSteps.updateVariable(taskId,taskVariableString,"string321");
        taskRuntimeBundleSteps.updateVariable(taskId,taskTaskVariableInteger,321);
        taskRuntimeBundleSteps.updateVariable(taskId,taskVariableBoolean,false);

    }

    @When("the user ask to claim the task")
    public void claimTask() throws Exception {
        String taskId = Serenity.sessionVariableCalled("taskId");
        claimTaskById(taskId);

    }
    @When("the user ask to complete the task")
    public void completeTask() throws Exception {
        String taskId = Serenity.sessionVariableCalled("taskId");
        completeTaskById(taskId);
    }


    @Then("variables have correct values in process")
    public void checkProcessInstanceVariablesAfterTask() {

        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");

        await().untilAsserted(() -> {
            assertThat(processVariableString).isNotNull();
            assertThat(processVariableInteger).isNotNull();
            assertThat(processVariableBoolean).isNotNull();
//            assertThat(processVariableDate).isNotNull();
//            assertThat(processVariableDateTime).isNotNull();

            final Resources<CloudVariableInstance> cloudVariableInstanceResource = getProcessVariables(processInstanceId);

            assertThat(cloudVariableInstanceResource).isNotNull();
            assertThat(cloudVariableInstanceResource).isNotEmpty();
            assertThat(cloudVariableInstanceResource.getContent()).extracting(VariableInstance::getName,
                                                                              VariableInstance::getValue)
                    .contains(
                            tuple(processVariableString,
                                  "string321"),
                            tuple(processVariableInteger,
                                  321),
                            tuple(processVariableBoolean,
                                  false)
//                            tuple(processVariableDate, variableValue4)
//                            tuple(processVariableBoolean, variableValue3)
                    );
        });
    }


    public void completeTaskById(String taskId) throws Exception {
        taskRuntimeBundleSteps.completeTask(taskId,
                                            TaskPayloadBuilder
                                                    .complete()
                                                    .withTaskId(taskId)
                                                    .build());
        checkTaskStatus(taskId,
                        Task.TaskStatus.COMPLETED);
    }

    public void claimTaskById(String taskId) throws Exception {
        taskRuntimeBundleSteps.claimTask(taskId);
        checkTaskStatus(taskId,
                        Task.TaskStatus.ASSIGNED);
    }

    public List<Task> getTasks() throws Exception {
        String processId = Serenity.sessionVariableCalled("processInstanceId");
        return new ArrayList<>(
                processRuntimeBundleSteps.getTaskByProcessInstanceId(processId));
    }

    public Task getTaskByName(String taskName) throws Exception {
        List<Task> tasks = getTasks();
        assertThat(tasks).isNotEmpty();

        return tasks.stream().filter(t -> t.getName().equals(taskName)).findFirst().orElse(null);
    }

    public void checkTaskStatus(String taskId,
                                Task.TaskStatus status) throws Exception {
        if (status != Task.TaskStatus.COMPLETED) {
            final CloudTask task = taskRuntimeBundleSteps.getTaskById(taskId);
            assertThat(task).isNotNull();
            assertThat(task.getStatus()).isEqualTo(status);
        }
        taskQuerySteps.checkTaskStatus(taskId,
                                       status);
    }
}
