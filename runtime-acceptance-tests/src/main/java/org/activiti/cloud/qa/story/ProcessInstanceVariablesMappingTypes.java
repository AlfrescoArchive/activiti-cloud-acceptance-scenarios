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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.activiti.cloud.qa.helpers.ProcessDefinitionRegistry.processDefinitionKeyMatcher;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import net.serenitybdd.core.Serenity;
import net.thucydides.core.annotations.Steps;
import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.payloads.SetProcessVariablesPayload;
import org.activiti.cloud.acc.core.steps.query.ProcessQuerySteps;
import org.activiti.cloud.acc.core.steps.runtime.ProcessRuntimeBundleSteps;
import org.activiti.cloud.acc.core.steps.runtime.ProcessVariablesRuntimeBundleSteps;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
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

    private Map<String, Object> getVariablesMap(String variableName1,
                                                Object variableValue1,
                                                String variableName2,
                                                Object variableValue2,
                                                String variableName3,
                                                Object variableValue3,
                                                String variableName4,
                                                Object variableValue4,
                                                String variableName5,
                                                Object variableValue5

    ) {

        Map<String, Object> variables = new HashMap<>();
        variables.put(variableName1,
                      variableValue1);
        variables.put(variableName2,
                      variableValue2);
        variables.put(variableName3,
                      variableValue3);
        variables.put(variableName4,
                      variableValue4);
        variables.put(variableName5,
                      variableValue5);

        return variables;
    }
    //Date date = dateFormatterProvider.convert2Date("2019-09-01");
    //Date datetime = dateFormatterProvider.convert2Date("2019-09-01T10:20:30.000Z");
    //
//#When the user starts the process TASK_DATE_VAR_MAPPING with var1 process-variable-string value string var2 process-variable-integer value 111 var3 process-variable-boolean val true var4 process-variable-date val 2019-09-01 var5 process-variable-datetime val 2019-09-02

//    @When("the user starts the process $processName with var1 $variableName1 value $variableValue1 var2 $variableName2 value $variableValue2 var3 $variableName3 val $variableValue3 var4 $variableName4 val $variableValue4 var5 $variableName5 val $variableValue5")

    //    public void startProcess(String processName,
//                             String variableName1,
//                             String variableValue1,
//                             String variableName2,
//                             Integer variableValue2,
//                             String variableName3,
//                             Boolean variableValue3,
//                             String variableName4,
//                             String variableValue4,
//                             String variableName5,
//                             String variableValue5
//
//    )
    @When("the user starts the process $processName")
    public void startProcess(String processName) {

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

        String dateString = format.format(new Date());
        Date date1 = null;
        try {
            date1 = format.parse("2019-09-01");
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Date date2 = null;
        try {
            date2 = format.parse("2019-09-02");
        } catch (ParseException e) {
            e.printStackTrace();
        }

        String variableName1 = "process-variable-string";
        String variableValue1 = "stringValue1";
        String variableName2 = "process-variable-integer";
        Integer variableValue2 = Integer.valueOf(123);
        String variableName3 = "process-variable-boolean";
        Boolean variableValue3 = Boolean.valueOf(true);
        String variableName4 = "process-variable-date";
        Date variableValue4 = date1;
        String variableName5 = "process-variable-datetime";
        Date variableValue5 = date2;

        Map<String, Object> variables = getVariablesMap(variableName1,
                                                        variableValue1,
                                                        variableName2,
                                                        variableValue2,
                                                        variableName3,
                                                        variableValue3,
                                                        variableName4,
                                                        date1,
                                                        variableName5,
                                                        date2
        );

        ProcessInstance processInstance = processRuntimeBundleSteps.startProcessWithVariables(
                processDefinitionKeyMatcher(processName),
                variables);

        Serenity.setSessionVariable("processInstanceId").to(processInstance.getId());
    }

    //    @Then("variable $variableName1 has value $value1 and $variableName2 has value $value2")
//    public void checkProcessInstanceVariables(String variableName1, String value1, String variableName2, String value2) {
//
//        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");
//
//        await().untilAsserted(() -> {
//            assertThat(variableName1).isNotNull();
//            assertThat(variableName2).isNotNull();
//
//            final Resources<CloudVariableInstance> cloudVariableInstanceResource = getProcessVariables(processInstanceId);
//
//            assertThat(cloudVariableInstanceResource).isNotNull();
//            assertThat(cloudVariableInstanceResource).isNotEmpty();
//
//            assertThat(cloudVariableInstanceResource.getContent()).extracting(VariableInstance::getName,
//                                                                              VariableInstance::getValue)
//                    .contains(
//                            tuple(variableName1, value1),
//                            tuple(variableName2, value2)
//                    );
//        });
//    }
//
//    @Then("the process variable $variableName is deleted")
//    @When("the process variable $variableName is deleted")
//    public void verifyProcessVariableDeleted(String variableName) {
//        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");
//
//        await().untilAsserted(() -> {
//            assertThat(variableName).isNotNull();
//            final Resources<CloudVariableInstance> variableInstances = getProcessVariables(processInstanceId);
//            if (variableInstances!=null) {
//                assertThat(variableInstances.getContent()).extracting(VariableInstance::getName).doesNotContain(variableName);
//            }
//        });
//    }
//
//    @Then("the process variable $variableName is created")
    @When("the process variable $variableName is created")
    public void verifyProcessVariableCreated(String variableName) {
        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");

        await().untilAsserted(() -> {
            assertThat(variableName).isNotNull();
            final Resources<CloudVariableInstance> variableInstances = getProcessVariables(processInstanceId);
            assertThat(variableInstances).isNotNull();
            assertThat(variableInstances).isNotEmpty();
            //one of the variables should have name matching variableName
            assertThat(variableInstances.getContent()).extracting(VariableInstance::getName).contains(variableName);
        });
    }

    //
//    @When("the user set the instance variable $variableName1 with value $value1")
//    public void setProcessVariables(String variableName1, String value1) {
//        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");
//
//        SetProcessVariablesPayload setProcessVariablesPayload = ProcessPayloadBuilder
//                .setVariables()
//                .withVariable(variableName1, value1)
//                .build();
//        processVariablesRuntimeBundleSteps.setVariables(processInstanceId, setProcessVariablesPayload);
//    }
//
    public Resources<CloudVariableInstance> getProcessVariables(String processInstanceId) {
        return processVariablesRuntimeBundleSteps.getVariables(processInstanceId);
    }
//
//    @Then("query process instance variable $variableName has value $value")
//    public void checkQuerykProcessInstanceVariable(String variableName, String value) {
//
//        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");
//
//        // TODO add variable value check in processQuerySteps
//        processQuerySteps.checkProcessInstanceHasVariable(processInstanceId, variableName);
//    }
}
