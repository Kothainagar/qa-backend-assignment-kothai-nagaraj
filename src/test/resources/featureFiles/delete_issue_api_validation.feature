Feature: GitLab Project issue deletion API validation

  @deleteIssue-HappyFlow
  Scenario: Happy Flow - Delete a GitLab issue with valid IID and Verify the issue is deleted successfully
    Given A GitLab project issue exists in the project
    When I send the request to delete the issue
    Then the issue should be deleted successfully
    And I send the request to retrieve the issue
    And the request should be rejected with status code 404 and error message 404 Not found

  @deleteIssue-ErrorFlow
  Scenario Outline: Error Flow - Delete a GitLab project issue with invalid IID - <test>
    When I send the request to delete the issue with below details
      | iid   |
      | <iid> |
    Then the request should be rejected with status code <statusCode> and error message <errormessage>
    Examples:
      | test                                        | iid       | statusCode | errormessage         |
      | Reject issue deletion with invalid IID      | 0b23      | 400        | issue_iid is invalid |
      | Reject issue deletion with non-existent IID | 994353499 | 404        | 404 Issue Not Found  |


  @deleteIssue-ErrorFlow
  Scenario Outline: Error Flow - Delete a GitLab issue with invalid authentication types - <test>
    When I send the request to delete the issue with below details
      | authType   | iid  |
      | <authType> | 1123 |
    Then the request should be rejected with status code 401 and error message 401 Unauthorized
    Examples:
      | test                                     | authType |
      | Reject issue deletion without token      | noauth   |
      | Reject issue deletion with invalid token | invalid  |