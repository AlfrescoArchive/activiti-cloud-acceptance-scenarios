Meta:

Narrative:
As a user
I want to perform operations on process instances with subscriptions to receive notifications

Scenario: complete a process instance that uses a connector with subscription
Given the user is authenticated as testadmin
When the user starts a process CONNECTOR_PROCESS_INSTANCE with PROCESS_STARTED and PROCESS_COMPLETED events subscriptions
Then the status of the process is completed
Then PROCESS_STARTED and PROCESS_COMPLETED notifications are received

Scenario: complete a process instance that sends a signal with subscription 
Given the user is authenticated as testadmin
When the user starts a process SIGNAL_THROW_PROCESS_INSTANCE with SIGNAL_RECEIVED subscription
Then the status of the process is completed
And SIGNAL_RECEIVED notification with theStart signal event is received

Scenario: complete a process instance with intermediate timer subscription 
Given the user is authenticated as testadmin
When the user starts a process INTERMEDIATE_TIMER_EVENT_PROCESS with TIMER subscriptions
Then the status of the process is completed
And TIMER notifications are received

Scenario: complete a process instance with boundary timer subscription 
Given the user is authenticated as testadmin
When the user starts a process BOUNDARY_TIMER_EVENT_PROCESS with TIMER subscriptions
Then the status of the process is completed
And TIMER notifications are received

Scenario: complete a process instance by messages with subscriptions to MESSAGE_RECEIVED,MESSAGE_WAITING,MESSAGE_SENT event notifications
Given the user is authenticated as testadmin
And generated random value for session variable called businessId
And subscription timeout of 3 seconds
And session timeout of 5 seconds
When the user subscribes to the list of MESSAGE_RECEIVED,MESSAGE_WAITING,MESSAGE_SENT notifications
And the user sends message startMessage with businessKey value of businessId session variable
Then the list of MESSAGE_RECEIVED,MESSAGE_WAITING notifications are received
And the user sends a message named boundaryMessage with correlationKey value of businessId session variable
And the list of MESSAGE_RECEIVED,MESSAGE_WAITING notifications are received
And the user sends a message named catchMessage with correlationKey value of businessId session variable 
And the list of MESSAGE_RECEIVED,MESSAGE_SENT notifications are received
And the status of the process is completed
And the user completes subscription with all notifications received
