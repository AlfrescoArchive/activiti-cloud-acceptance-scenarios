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

import net.serenitybdd.core.Serenity;
import net.thucydides.core.annotations.Steps;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.payloads.SetProcessVariablesPayload;
import org.activiti.cloud.acc.core.steps.runtime.ProcessRuntimeBundleSteps;
import org.activiti.cloud.acc.core.steps.runtime.ProcessVariablesRuntimeBundleSteps;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.springframework.hateoas.Resources;

public class ProcessInstanceVariables {
    @Steps
    private ProcessRuntimeBundleSteps processRuntimeBundleSteps;

    @Steps
    private ProcessVariablesRuntimeBundleSteps processVariablesRuntimeBundleSteps;

    @Then("variable $variableName1 has value $value1 and $variableName2 has value $value2")
    public void checkProcessInstanceVariables(String variableName1, String value1, String variableName2, String value2) {
        String variableName,actualValue;
        boolean found1,found2;
        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");
        Resources<CloudVariableInstance> cloudVariableInstanceResource = getProcessVariables(processInstanceId);
        
        found1 = found2 = false;
        
        for (CloudVariableInstance cloudVariableInstance : cloudVariableInstanceResource.getContent()) {
            variableName = cloudVariableInstance.getName();
            actualValue = cloudVariableInstance.getValue();
            if (variableName.equals(variableName1)) {
                assertThat(value1).isEqualTo(actualValue);
                found1=true;
            } else {
                if (variableName.equals(variableName2)) {
                    assertThat(value2).isEqualTo(actualValue);
                    found2 = true;
                }
            }
        }
        
        assertThat(found1).isEqualTo(true);
        assertThat(found2).isEqualTo(true);
    }

    @Then("the process variable $variableName is deleted")
    public void verifyVariableDeleted(String variableName) {
        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");

        Resources<CloudVariableInstance> cloudVariableInstanceResource = getProcessVariables(processInstanceId);
        boolean found = false;
        for (CloudVariableInstance cloudVariableInstance : cloudVariableInstanceResource.getContent()) {
            if (cloudVariableInstance.getName().equals(variableName)) {
                found = true;
                break;
            } 
        }
        assertThat(found).isEqualTo(false);
    }
    
    @Then("the process variable $variableName is created")
    public void verifyVariableCreated(String variableName) {
        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");

        Resources<CloudVariableInstance> cloudVariableInstanceResource = getProcessVariables(processInstanceId);
        boolean found = false;
        for (CloudVariableInstance cloudVariableInstance : cloudVariableInstanceResource.getContent()) {
            if (cloudVariableInstance.getName().equals(variableName)) {
                found = true;
                break;
            } 
        }
        assertThat(found).isEqualTo(true);
    }
    
    @When("the user set the instance variable $variableName1 with value $value1")
    public void setVariables(String variableName1, String value1) {
        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");
        
        SetProcessVariablesPayload setProcessVariablesPayload = ProcessPayloadBuilder
                                                                .setVariables()
                                                                .withVariable(variableName1, value1)
                                                                .build();
        processVariablesRuntimeBundleSteps.setVariables(processInstanceId, setProcessVariablesPayload);
    }
    
    public Resources<CloudVariableInstance> getProcessVariables(String processInstanceId) {
        return  processVariablesRuntimeBundleSteps.getVariables(processInstanceId);
    } 

}