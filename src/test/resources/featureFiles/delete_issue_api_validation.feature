Feature: GitLab issue creation

  Scenario: Delete a GitLab issue with valid IID and Verify the issue is deleted successfully
    Given A GitLab issue exists in the project
    When I send the request to delete the issue
    Then the issue should be deleted successfully
    And I verify that the issue is no longer available