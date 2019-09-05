Meta:
@current

Narrative:
As a user
I want to perform operations on process instance variables

Scenario: variable types are correct for variables
Given the user is authenticated as hruser
When the user starts the process TASK_DATE_VAR_MAPPING
Then the process variable process-variable-date is created