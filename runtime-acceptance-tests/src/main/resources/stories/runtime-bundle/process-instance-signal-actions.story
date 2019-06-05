Meta:

Narrative:
As a user
I want to perform operations on process instances with throw catch signal

Scenario: process instances with throw catch signal
Given the user is authenticated as testuser
When the user starts a process with intermediate catch signal
And the user starts a process with intermediate throw signal
Then the process throwing a signal is completed
And the process catching a signal is completed
And the SIGNAL_RECEIVED event was catched up
