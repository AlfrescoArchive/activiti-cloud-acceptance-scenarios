Meta:

Narrative:
As a user
I want to perform operations on process instances

Scenario: admin update process instance variables
Given the user is authenticated as hradmin
When the admin starts the process PROCESS_INSTANCE_WITH_VARIABLES with variables start1 and start2
And the admin update the instance variables start1 with value value1 and start2 with value value2
Then the list of errors messages is empty
And variable start1 has value value1 and variable2 has value value2

Scenario: admin delete process instance variables
Given the user is authenticated as hradmin
When the admin starts the process PROCESS_INSTANCE_WITH_VARIABLES with variables start1 and start2
And the admin delete the instance variable start1
Then the process variable start1 is deleted
