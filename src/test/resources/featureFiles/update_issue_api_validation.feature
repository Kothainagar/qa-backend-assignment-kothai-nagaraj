Feature: GitLab project issue update API validation

  @updateIssue-HappyFlow
  Scenario Outline: Update individual issue fields - <test>
    Given A GitLab project issue exists in the project
    And I prepare an update issue request with below details
      | fields   | values   |
      | <fields> | <values> |
    When I send the request to update the issue
    Then the issue should be updated successfully with the expected details

    Examples:
      | test                     | fields            | values                        |
      | Update title             | title             | Updated API issue             |
      | Update description       | description       | Updated API issue description |
      | Make issue confidential  | confidential      | true                          |
      | Lock issue discussion    | discussion_locked | true                          |
      | Update due date          | due_date          | 2026-09-20                    |
      | Update start date        | start_date        | 2026-09-10                    |
      | Update type to issue     | issue_type        | issue                         |
      | Update type to incident  | issue_type        | incident                      |
      | Replace labels           | labels            | api-test,automated            |
      | Add labels               | add_labels        | regression,backend            |
      | Assign user              | assignee_ids      | [42009381]                    |
      | Assign milestone by ID   | milestone_id      | 7586014                       |
      | Assign milestone by name | milestone         | API Test Milestone-1.0        |

  #TODO: possible bug - issueType is updated to incident but the severity remains UNKNOWN
  @skip
  Scenario Outline: Update incident severity - <severity>
    Given A GitLab project issue exists in the project
    And I prepare an update issue request with below details
      | fields               | values               |
      | issue_type\|severity | incident\|<severity> |
    When I send the request to update the issue
    Then the issue should be updated successfully with the expected details

    Examples:
      | severity |
      | unknown  |
      | low      |
      | medium   |
      | high     |
      | critical |


  @updateIssue-HappyFlow
  Scenario Outline: Update severity after converting an issue to incident - <severity>
    Given A GitLab project issue exists in the project
    And I prepare an update issue request with below details
      | fields     | values   |
      | issue_type | incident |
    When I send the request to update the issue
    Then the issue should be updated successfully with the expected details
    And I prepare an update issue request with below details
      | fields   | values     |
      | severity | <severity> |
    When I send the request to update the issue
    Then the issue should be updated successfully with the expected details

    Examples:
      | severity |
      | unknown  |
      | low      |
      | medium   |
      | high     |
      | critical |

  @updateIssue-HappyFlow
  Scenario: Update multiple fields in one request
    Given A GitLab project issue exists in the project
    And I prepare an update issue request with below details
      | fields                                                 | values                                                           |
      | title\|description\|confidential\|start_date\|due_date | Updated issue\|Updated description\|true\|2026-09-10\|2026-09-20 |
    When I send the request to update the issue
    Then the issue should be updated successfully with the expected details

  @updateIssue-HappyFlow
  Scenario: Remove a label from an issue
    Given A GitLab project issue exists in the project
    And I prepare an update issue request with below details
      | fields | values             |
      | labels | api-test,automated |
    When I send the request to update the issue
    Then the issue should be updated successfully with the expected details
    And I prepare an update issue request with below details
      | fields        | values   |
      | remove_labels | api-test |
    When I send the request to update the issue
    Then the issue should be updated successfully with the expected details

  @updateIssue-HappyFlow
  Scenario: Close and reopen an issue
    Given A GitLab project issue exists in the project
    And I prepare an update issue request with below details
      | fields      | values |
      | state_event | close  |
    When I send the request to update the issue
    Then the issue should be updated successfully with the expected details
    And I prepare an update issue request with below details
      | fields      | values |
      | state_event | reopen |
    When I send the request to update the issue
    Then the issue should be updated successfully with the expected details

  @updateIssue-ErrorFlow
  Scenario: Reject an update request containing only updated_at
    Given I prepare an update issue request with below details
      | fields     | values               | iid  |
      | updated_at | 2026-09-05T10:00:00Z | 1223 |
    When I send the request to update the issue
    Then the request should be rejected with status code 400 and error message assignee_id, assignee_ids, confidential, created_at, description, discussion_locked, due_date, start_date, labels, add_labels, remove_labels, milestone_id, milestone, severity, state_event, title, issue_type, weight, epic_id, epic_iid are missing, at least one parameter must be provided

  @updateIssue-ErrorFlow
  Scenario Outline: Reject issue update with invalid IID - <test>
    Given I prepare an update issue request with below details
      | fields | values        | iid   |
      | title  | Updated issue | <iid> |
    When I send the request to update the issue
    Then the request should be rejected with status code <statusCode> and error message <errorMessage>

    Examples:
      | test                                     | iid       | statusCode | errorMessage         |
      | Reject update with an invalid IID format | 0b23      | 400        | issue_iid is invalid |
      | Reject update with a non-existent IID    | 994353499 | 404        | 404 Not found        |

  @updateIssue-ErrorFlow
  Scenario Outline: Reject issue update with invalid authentication - <test>
    Given I prepare an update issue request with below details
      | fields | values        | authType   | iid  |
      | title  | Updated issue | <authType> | 1123 |
    When I send the request to update the issue
    Then the request should be rejected with status code 401 and error message 401 Unauthorized

    Examples:
      | test                                      | authType |
      | Reject issue update without a token       | noauth   |
      | Reject issue update with an invalid token | invalid  |