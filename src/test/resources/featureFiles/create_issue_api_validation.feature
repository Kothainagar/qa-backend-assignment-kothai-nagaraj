Feature: GitLab issue creation

  Scenario: Create a GitLab issue with mandatory fields and verify the issue is created successfully
    Given I prepare a create issue request with the mandatory fields
    When I send the request to create the issue
    Then the issue should be created successfully with the expected details
    And I delete the created issue
