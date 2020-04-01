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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import java.util.Collection;

import org.activiti.cloud.acc.core.steps.query.ProcessQuerySteps;
import org.activiti.cloud.acc.core.steps.runtime.ProcessRuntimeBundleSteps;
import org.activiti.cloud.acc.core.steps.runtime.ProcessVariablesRuntimeBundleSteps;
import org.activiti.cloud.acc.shared.steps.VariableBufferSteps;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.api.task.model.CloudTask;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.springframework.hateoas.Resources;

import net.serenitybdd.core.Serenity;
import net.thucydides.core.annotations.Steps;

public class ProcessInstanceConnectors {

    @Steps
    private VariableBufferSteps variableBufferSteps;

    @Steps
    private ProcessRuntimeBundleSteps processRuntimeBundleSteps;

    @Steps
    private ProcessVariablesRuntimeBundleSteps processVariablesRuntimeBundleSteps;

    @Steps
    private ProcessQuerySteps processQuerySteps;

    private CloudProcessInstance processInstance;

    @Given("the user provides a variable named $variableName with value $variableValue")
    public void givenVariable(String variableName,
                              String variableValue) {
        variableBufferSteps.addVariable(variableName,
                                        variableValue);
    }

    @Given("the user provides an integer variable named $variableName with value $variableValue")
    public void givenVariable(String variableName,
                              Integer variableValue) {
        variableBufferSteps.addVariable(variableName,
                                        variableValue);
    }

    @When("the user starts an instance of process called $processDefinitionKey with the provided variables")
    public void startProcessWithAvailableVariables(String processDefinitionKey) {
        processInstance = processRuntimeBundleSteps.startProcessWithVariables(processDefinitionKey,
                                                                              variableBufferSteps.availableVariables());

        Serenity.setSessionVariable("processInstanceId").to(processInstance.getId());
    }

    @Then("the process instance has a task named $taskName")
    public void processHasTask(String taskName) {

        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");

        await().untilAsserted(
                () ->
                {
                    assertThat(processInstance).isNotNull();
                    Collection<CloudTask> tasks = processRuntimeBundleSteps.getTaskByProcessInstanceId(processInstanceId);
                    assertThat(tasks).isNotNull();
                    assertThat(tasks)
                            .extracting(CloudTask::getName)
                            .contains(taskName);
                }
        );
    }

    @Then("the process instance has a variable named $variableName with value $variableValue")
    public void assertThatHasVariable(String variableName,
                                      String variableValue) {

        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");

        assertThat(processInstanceId).isNotNull();
        await().untilAsserted(() -> {
            Resources<CloudVariableInstance> processVariables = processVariablesRuntimeBundleSteps.getVariables(processInstanceId);
            assertThat(processVariables.getContent()).isNotNull();
            assertThat(processVariables.getContent())
                    .extracting(CloudVariableInstance::getName,
                                CloudVariableInstance::getValue)
                    .contains(tuple(variableName,
                                    variableValue));
        });
    }

    @Then("the query process instance has an integer variable named $variableName with value $variableValue")
    public void assertThatQueryHasVariable(String variableName,
                                           Integer variableValue) {
        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");

        processQuerySteps.checkProcessInstanceHasVariableValue(processInstanceId,
                                                               variableName,
                                                               variableValue);
    }

}
