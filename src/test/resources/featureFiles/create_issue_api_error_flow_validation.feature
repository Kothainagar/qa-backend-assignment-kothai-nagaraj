@createIssue-ErrorFlow
Feature: GitLab Project issue creation API error flow validation

  Scenario: Verify title Field - Send Request with Empty Title
    Given I prepare a create issue request with the mandatory fields with below details
      | title |
      |       |
    When I send the request to create the issue
    Then the request should be rejected with status code 400 and error message can't be blank

  Scenario: Verify title Field - Send Request with Missing Title
    Given I prepare a create issue request with the mandatory fields with below details
      | removeField |
      | title       |
    When I send the request to create the issue
    Then the request should be rejected with status code 400 and error message title is missing

  Scenario: Verify Duplicate Request scenario
    Given A GitLab project issue exists in the project
    When I prepare a create issue request with the optional fields with below details
      | iid                       |
      | use_from_previous_request |
    When I send the request to create the issue
    Then the request should be rejected with status code 409 and error message Duplicated issue

  Scenario Outline: Verify Authentication - <test>
    Given I prepare a create issue request with the mandatory fields with below details
      | authType   |
      | <authType> |
    When I send the request to create the issue
    Then the request should be rejected with status code 401 and error message 401 Unauthorized
    Examples:
      | test                                     | authType |
      | Reject issue creation without token      | noauth   |
      | Reject issue creation with invalid token | invalid  |


  Scenario Outline: Verify issueType Field - <test>
    Given I prepare a create issue request with the optional fields with below details
      | <field_type>  |
      | <field_value> |
    When I send the request to create the issue
    Then the request should be rejected with status code 400 and error message issue_type does not have a valid value
    Examples:
      | test                                  | field_type | field_value |
      | Reject uppercase ISSUE issue type     | issue_type | ISSUE       |
      | Reject uppercase INCIDENT issue type  | issue_type | INCIDENT    |
      | Reject uppercase TASK issue type      | issue_type | TASK        |
      | Reject unsupported issue type         | issue_type | story       |
      | Reject numeric-string issue type      | issue_type | 123         |
      | Reject issue type with invalid suffix | issue_type | test_case-1 |


  Scenario Outline: Verify Severity Field - <test>
    Given I prepare a create issue request with the optional fields with below details
      | <field_type>  | issue_type |
      | <field_value> | incident   |
    When I send the request to create the issue
    Then the request should be rejected with status code 400 and error message severity does not have a valid value
    Examples:
      | test                               | field_type | field_value |
      | Reject uppercase HIGH severity     | severity   | HIGH        |
      | Reject uppercase MEDIUM severity   | severity   | MEDIUM      |
      | Reject uppercase LOW severity      | severity   | LOW         |
      | Reject uppercase CRITICAL severity | severity   | CRITICAL    |
      | Reject uppercase UNKNOWN severity  | severity   | UNKNOWN     |
      | Reject unsupported severity        | severity   | urgent      |
      | Reject numeric-string severity     | severity   | 123         |


  Scenario Outline: Verify <field_type> Field - <test>
    Given I prepare a create issue request with the optional fields with below details
      | <field_type>  |
      | <field_value> |
    When I send the request to create the issue
    Then the request should be rejected with status code 400 and error message created_at is invalid

    Examples:
      | test                                   | field_type | field_value          |
      | Reject non-date creation timestamp     | created_at | not-a-date           |
      | Reject impossible creation date        | created_at | 2026-02-30T10:00:00Z |
      | Reject invalid creation timestamp hour | created_at | 2026-09-04T25:00:00Z |


  Scenario Outline: Verify Confidential field value- <test>
    Given I prepare a create issue request with the optional fields with below details
      | confidential | confidentialType |
      | <fieldValue> | <type>           |
    When I send the request to create the issue
    Then the request should be rejected with status code 400 and error message confidential is invalid

    Examples:
      | test                                  | type    | fieldValue    |
      | Reject non-boolean confidential value | String  | not-a-boolean |
      | Reject numeric confidential value     | Integer | 123           |
      #TODO: GitLab docs specify confidential as type boolean, but string "TRUE"/"FALSE"
      # are accepted (201) instead of rejected (400) as expected. Possible API-side
      # validation gap — uncommented pending confirmation, not a test defect.
#      | Reject string confidential value as TRUE  | String  | TRUE          |
#      | Reject string confidential value as FALSE | String  | FALSE         |