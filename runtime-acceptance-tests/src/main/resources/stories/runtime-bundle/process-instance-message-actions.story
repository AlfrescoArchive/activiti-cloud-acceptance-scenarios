Meta:

Narrative:
As a user
I want to perform operations on process instance having message events

Scenario: deliver messages via process runtime Rest Api
Given the user is authenticated as hruser
When the user sends a start message named startMessage with businessKey value businessId
Then MESSAGE_RECEIVED event is emitted for the message 'startMessage' and timeout 5 seconds
Then MESSAGE_WAITING event is emitted for the message 'boundaryMessage' and timeout 5 seconds
Then the sends a message named boundaryMessage with correlationKey value businessId
Then MESSAGE_RECEIVED event is emitted for the message 'boundaryMessage' and timeout 5 seconds
Then MESSAGE_WAITING event is emitted for the message 'catchMessage' and timeout 5 seconds
Then the sends a message named catchMessage with correlationKey value businessId
Then MESSAGE_RECEIVED event is emitted for the message 'catchMessage' and timeout 5 seconds
Then MESSAGE_SENT event is emitted for the message 'endMessage' and timeout 5 seconds
And the process with message events is completed
