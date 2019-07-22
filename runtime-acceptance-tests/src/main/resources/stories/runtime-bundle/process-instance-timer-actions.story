Meta:

Narrative:
As a user
I want to perform operations on process instance having timer events

Scenario: check a process instance with intermediate timer event
Given the user is authenticated as hruser
When the user starts a process with timer events called INTERMEDIATE_TIMER_EVENT_PROCESS
Then TIMER_SCHEDULED events are emitted for the timer 'timer' and timeout 1 seconds 
And the process with timer events is completed
