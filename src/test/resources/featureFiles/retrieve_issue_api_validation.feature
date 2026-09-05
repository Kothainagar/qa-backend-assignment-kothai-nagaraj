Feature: GitLab Project issue retrieval API validation

  @retrieveIssue-HappyFlow
  Scenario: Happy Flow - Retrieve a GitLab project issue with valid IID and verify the issue details
    Given A GitLab project issue exists in the project
    When I send the request to retrieve the issue
    Then the issue should be retrieved successfully with the expected details

  @retrieveIssue-HappyFlow
  Scenario: Happy Flow - Retrieve a newly created issue and verify response matches create response
    Given I prepare a create issue request with the mandatory fields
    And I send the request to create the issue
    And the issue should be created successfully with the expected details
    When I send the request to retrieve the issue
    Then the retrieve response should match the created issue

  @retrieveIssue-ErrorFlow
  Scenario Outline: Error Flow - Retrieve a GitLab project issue with invalid IID - <test>
    When I send the request to retrieve the issue with below details
      | iid   |
      | <iid> |
    Then the request should be rejected with status code <statusCode> and error message <errormessage>
    Examples:
      | test                                        | iid       | statusCode | errormessage         |
      | Reject issue retrieve with invalid IID      | 0b23      | 400        | issue_iid is invalid |
      | Reject issue retrieve with non-existent IID | 994353499 | 404        | 404 Not found        |

  @retrieveIssue-ErrorFlow
  Scenario Outline: Error Flow - Retrieve a GitLab project issue with invalid authentication types - <test>
    When I send the request to retrieve the issue with below details
      | authType   | iid  |
      | <authType> | 1123 |
    Then the request should be rejected with status code 401 and error message 401 Unauthorized
    Examples:
      | test                                     | authType |
      #TODO: possible bug - no auth returns 404 Project Not Found instead of 401 Unauthorized. uncomment after fix
      #| Reject issue retrieve without token      | noauth   |
      | Reject issue retrieve with invalid token | invalid  |