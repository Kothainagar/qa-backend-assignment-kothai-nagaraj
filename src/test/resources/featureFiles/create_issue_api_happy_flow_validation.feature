@createIssue-HappyFlow
Feature: GitLab Project issue creation API happy flow validation

  Scenario: Create a GitLab issue with mandatory fields and verify the issue is created successfully
    Given I prepare a create issue request with the mandatory fields
    When I send the request to create the issue
    Then the issue should be created successfully with the expected details


  Scenario: Create a GitLab issue with optional fields and verify the issue is created successfully
    Given I prepare a create issue request with the optional fields
    When I send the request to create the issue
    Then the issue should be created successfully with the expected details


  Scenario Outline: Verify issue_type and severity fields with valid inputs - <test>
    Given I prepare a create issue request with the optional fields with below details
      | issue_type   | severity   |
      | <issue_type> | <severity> |
    When I send the request to create the issue
    Then the issue should be created successfully with the expected details

    Examples:
      | test                                         | issue_type | severity |
      | Create an issue with unknown severity input  | issue      | unknown  |
      | Create an issue with low severity input      | issue      | low      |
      | Create an issue with medium severity input   | issue      | medium   |
      | Create an issue with high severity input     | issue      | high     |
      | Create an issue with critical severity input | issue      | critical |
      | Create an incident with unknown severity     | incident   | unknown  |
      | Create an incident with low severity         | incident   | low      |
      | Create an incident with medium severity      | incident   | medium   |
      | Create an incident with high severity        | incident   | high     |
      | Create an incident with critical severity    | incident   | critical |
      | Create a task with unknown severity input    | task       | unknown  |
      | Create a task with low severity input        | task       | low      |
      | Create a task with medium severity input     | task       | medium   |
      | Create a task with high severity input       | task       | high     |
      | Create a task with critical severity input   | task       | critical |


  Scenario Outline: Invalid date input is accepted but not applied - <test>
    Given I prepare a create issue request with the optional fields with below details
      | <field_type>  |
      | <field_value> |
    When I send the request to create the issue
    Then the issue should be created successfully with the expected details

    Examples:
      | test                                            | field_type | field_value |
      | create issue with non-date start date           | start_date | not-a-date  |
      | create issue with impossible start date         | start_date | 2026-02-30  |
      | create issue with start date with invalid month | start_date | 2026-13-01  |
      | create issue with non-date due date             | due_date   | not-a-date  |
      | create issue with impossible due date           | due_date   | 2026-02-30  |
      | create issue with due date with invalid month   | due_date   | 2026-13-01  |


  Scenario Outline: Verify Confidential field with valid values - <test>
    Given I prepare a create issue request with the optional fields with below details
      | confidential | confidentialType |
      | <fieldValue> | boolean          |
    When I send the request to create the issue
    Then the issue should be created successfully with the expected details

    Examples:
      | test                                               | fieldValue |
      | Create issue with valid confidential boolean value | true       |
      | Create issue with valid confidential boolean value | false      |