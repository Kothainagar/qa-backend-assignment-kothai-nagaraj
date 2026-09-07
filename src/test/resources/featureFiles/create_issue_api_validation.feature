Feature: GitLab project issue creation API validation

  @createIssue-HappyFlow
  Scenario: Happy Flow - Create a GitLab issue with mandatory fields and verify the issue is created successfully
    Given I prepare a create issue request with the mandatory fields
    When I send the request to create the issue
    Then the issue should be created successfully with the expected details

  @createIssue-HappyFlow
  Scenario: Happy Flow - Create a GitLab issue with optional fields and verify the issue is created successfully
    Given I prepare a create issue request with the optional fields
    When I send the request to create the issue
    Then the issue should be created successfully with the expected details

  @createIssue-HappyFlow
  Scenario Outline: Happy Flow - Verify <field> field - <test>
    # accepted (201) rather than rejected — see "Observed API Behaviour" in README.
    Given I prepare a create issue request with the optional fields with below details
      | <field>       | fieldType   | unpopulatedFields |
      | <field_value> | <fieldType> | <field>           |
    When I send the request to create the issue
    Then the issue should be created successfully with the expected details
    Examples:
      | test                                           | field        | fieldType | field_value |
      | Accept non-existent assignee_id without error  | assignee_id  | integer   | 999999999   |
      | Accept non-existent milestone_id without error | milestone_id | integer   | 999999999   |

  @createIssue-HappyFlow
  Scenario Outline: Happy Flow - Verify issue_type and severity fields with valid inputs - <test>
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

  @createIssue-HappyFlow
  Scenario Outline: Happy Flow - Invalid date input is accepted but not applied - <test>
    Given I prepare a create issue request with the optional fields with below details
      | <fields> | unpopulatedFields |
      | <values> | <fields>          |
    When I send the request to create the issue
    Then the issue should be created successfully with the expected details
    Examples:
      | test                                            | fields     | values     |
      | create issue with non-date start date           | start_date | not-a-date |
      | create issue with impossible start date         | start_date | 2026-02-30 |
      | create issue with start date with invalid month | start_date | 2026-13-01 |
      | create issue with non-date due date             | due_date   | not-a-date |
      | create issue with impossible due date           | due_date   | 2026-02-30 |
      | create issue with due date with invalid month   | due_date   | 2026-13-01 |

  @createIssue-HappyFlow
  Scenario Outline: Happy Flow - Verify Confidential field with valid values - <test>
    Given I prepare a create issue request with the optional fields with below details
      | confidential | fieldType |
      | <values>     | boolean   |
    When I send the request to create the issue
    Then the issue should be created successfully with the expected details
    Examples:
      | test                                           | values |
      | Create an issue with confidential set to true  | true   |
      | Create an issue with confidential set to false | false  |

  @createIssue-HappyFlow
  Scenario: Happy Flow - Create an issue using valid milestone title
    Given I prepare a create issue request with the optional fields with below details
      | removeField  | milestone              |
      | milestone_id | API Test Milestone-1.0 |
    When I send the request to create the issue
    Then the issue should be created successfully with the expected details

  @createIssue-ErrorFlow
  Scenario Outline: Error Flow - Verify title Field - <test>
    Given I prepare a create issue request with the mandatory fields with below details
      | title   |
      | <title> |
    When I send the request to create the issue
    Then the request should be rejected with status code 400 and error message <errorMessage>
    Examples:
      | test                                                    | title                                                                                                                                                                                                                                                                                                                                                                         | errorMessage                            |
      | Send Request with null Title                            |                                                                                                                                                                                                                                                                                                                                                                               | can't be blank                          |
      | Send Request with empty Title                           | [empty]                                                                                                                                                                                                                                                                                                                                                                       | can't be blank                          |
      | send Request with title having more than 255 characters | Create Issue - This is an intentionally generated GitLab issue title containing more than two hundred and fifty-five characters for API boundary validation, ensuring the create issue endpoint correctly handles, rejects, truncates, or reports an appropriate validation error when the supplied title exceeds the documented maximum permitted length for an issue title. | is too long (maximum is 255 characters) |

  @createIssue-ErrorFlow
  Scenario: Error Flow - Verify title Field - Send Request with Missing Title
    Given I prepare a create issue request with the mandatory fields with below details
      | removeField |
      | title       |
    When I send the request to create the issue
    Then the request should be rejected with status code 400 and error message title is missing

  @createIssue-ErrorFlow
  Scenario Outline: Error Flow - Verify Authentication - <test>
    Given I prepare a create issue request with the mandatory fields with below details
      | authType   |
      | <authType> |
    When I send the request to create the issue
    Then the request should be rejected with status code 401 and error message 401 Unauthorized
    Examples:
      | test                                     | authType |
      | Reject issue creation without token      | noauth   |
      | Reject issue creation with invalid token | invalid  |

  @createIssue-ErrorFlow
  Scenario: Error Flow - Reject issue creation against a non-existent project ID
    Given I prepare a create issue request with the mandatory fields with below details
      | projectId |
      | 999999999 |
    When I send the request to create the issue
    Then the request should be rejected with status code 404 and error message 404 Project Not Found

  @createIssue-ErrorFlow
  Scenario Outline: Error Flow - Verify issueType Field - <test>
    Given I prepare a create issue request with the optional fields with below details
      | <fields> |
      | <values> |
    When I send the request to create the issue
    Then the request should be rejected with status code 400 and error message issue_type does not have a valid value
    Examples:
      | test                                  | fields     | values      |
      | Reject uppercase ISSUE issue type     | issue_type | ISSUE       |
      | Reject uppercase INCIDENT issue type  | issue_type | INCIDENT    |
      | Reject uppercase TASK issue type      | issue_type | TASK        |
      | Reject unsupported issue type         | issue_type | story       |
      | Reject numeric-string issue type      | issue_type | 123         |
      | Reject issue type with invalid suffix | issue_type | test_case-1 |

  @createIssue-ErrorFlow
  Scenario Outline: Error Flow -  Verify Severity Field - <test>
    Given I prepare a create issue request with the optional fields with below details
      | <fields> | issue_type |
      | <values> | incident   |
    When I send the request to create the issue
    Then the request should be rejected with status code 400 and error message severity does not have a valid value
    Examples:
      | test                               | fields   | values   |
      | Reject uppercase HIGH severity     | severity | HIGH     |
      | Reject uppercase MEDIUM severity   | severity | MEDIUM   |
      | Reject uppercase LOW severity      | severity | LOW      |
      | Reject uppercase CRITICAL severity | severity | CRITICAL |
      | Reject uppercase UNKNOWN severity  | severity | UNKNOWN  |
      | Reject unsupported severity        | severity | urgent   |
      | Reject numeric-string severity     | severity | 123      |

  @createIssue-ErrorFlow
  Scenario Outline: Error Flow - Verify <fields> Field - <test>
    Given I prepare a create issue request with the optional fields with below details
      | <fields> |
      | <values> |
    When I send the request to create the issue
    Then the request should be rejected with status code 400 and error message created_at is invalid

    Examples:
      | test                                   | fields     | values               |
      | Reject non-date creation timestamp     | created_at | not-a-date           |
      | Reject impossible creation date        | created_at | 2026-02-30T10:00:00Z |
      | Reject invalid creation timestamp hour | created_at | 2026-09-04T25:00:00Z |

  @createIssue-ErrorFlow
  Scenario Outline: Error Flow - Verify Confidential field value- <test>
    Given I prepare a create issue request with the optional fields with below details
      | confidential | fieldType   |
      | <values>     | <fieldType> |
    When I send the request to create the issue
    Then the request should be rejected with status code 400 and error message confidential is invalid

    Examples:
      | test                                  | fieldType | values        |
      | Reject non-boolean confidential value | String    | not-a-boolean |
      | Reject numeric confidential value     | Integer   | 123           |
      #TODO: GitLab docs specify confidential as type boolean, but string "TRUE"/"FALSE"
      # are accepted (201) instead of rejected (400) as expected. Possible API-side
      # validation gap — uncommented pending confirmation, not a test defect.
#      | Reject string confidential value as TRUE  | String  | TRUE          |
#      | Reject string confidential value as FALSE | String  | FALSE         |

  @createIssue-ErrorFlow
  Scenario: Error Flow - Verify Duplicate Request scenario
    Given A GitLab project issue exists in the project
    When I prepare a create issue request with the optional fields with below details
      | iid                       |
      | use_from_previous_request |
    When I send the request to create the issue
    Then the request should be rejected with status code 409 and error message Duplicated issue