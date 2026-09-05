# GitLab Issues API Test Automation

## Solution Overview

This project provides automated tests for the GitLab Issues API.

The tests cover:

- Create an issue
- Retrieve an issue
- Update an issue
- Delete an issue
- Authentication validation
- Field validation
- Invalid and non-existent issue identifiers
- Relevant edge cases

The framework uses Cucumber for readable scenarios, REST Assured for API requests, TestNG for execution, and Maven for build and dependency management.

## Prerequisites

- Java 17
- Maven 3.6 or later
- A GitLab account
- A GitLab project
- A GitLab access token with API permissions

Verify Java and Maven:

```bash
java -version
mvn -version
```

## Configuration

The GitLab configuration is stored in:

```text
src/test/resources/configs/config.yaml
```

Example:

```yaml
gitlab:
  base-url: "https://gitlab.com/api/v4"
  project-id: "86052777"
  token-environment-variable: "GITLAB_TOKEN"
```

Replace `project-id` when running the tests against another GitLab project.

The access token is read from an environment variable and is not stored in the repository.

Set the token on macOS or Linux:

```bash
read -s "GITLAB_TOKEN?Enter GitLab token: "
export GITLAB_TOKEN
```

Remove the token from the terminal session after execution:

```bash
unset GITLAB_TOKEN
```

## Build and Run

Run a clean build and execute the complete test suite:

```bash
mvn clean install
```

Run the tests without performing the complete Maven install phase:

```bash
mvn test
```

Run scenarios using a Cucumber tag:

```bash
mvn test -Dcucumber.filter.tags="@createIssue-HappyFlow"
```

Other available tags include:

```text
@createIssue-ErrorFlow
@retrieveIssue-HappyFlow
@retrieveIssue-ErrorFlow
@updateIssue-HappyFlow
@updateIssue-ErrorFlow
@deleteIssue-HappyFlow
@deleteIssue-ErrorFlow
```

Scenarios tagged with `@skip` are excluded from the standard test run.

Build the project without executing tests:

```bash
mvn clean install -DskipTests
```

## Test Report

The Cucumber HTML report is generated at:

```text
target/cucumber-reports/test-results.html
```

On macOS, open the report from the project directory with:

```bash
open target/cucumber-reports/test-results.html
```

The console output includes request endpoints, response status codes, response bodies, and field-validation details. Access tokens and authorization headers are not logged.

## Dependencies

The main dependencies are:

- REST Assured
- Cucumber Java
- Cucumber TestNG
- Cucumber PicoContainer
- TestNG
- Jayway JsonPath
- SnakeYAML
- SLF4J

Maven downloads these dependencies automatically.

## Assumptions

- The configured GitLab project already exists.
- The token owner has sufficient project permissions.
- Configured assignee and milestone IDs exist in the target project.
- Premium and Ultimate-only fields are outside the current test scope.
- The `test_case` issue type is excluded because it is unavailable in the current GitLab project or subscription.

## Special Notes and Trade-offs

- Scenario data is shared between step definitions using a lightweight scenario context.
- Issues created during tests are removed using a Cucumber `@After` hook.
- Cleanup is attempted whether a scenario passes or fails.
- A `404` cleanup response is accepted when the issue was already deleted by the scenario.
- Request and response validation is intentionally kept simple for the scope of the assignment.
- Fields with different request and response structures require separate validation.
- Configuration and environment variables are used so URLs, project IDs, and tokens are not hardcoded in the test implementation.

## Observed API Behaviour

The following behaviour was observed during testing:

- Invalid `start_date` and `due_date` values are accepted but returned as `null`.
- Changing an issue to an incident and setting severity in the same request leaves severity as `UNKNOWN`.
- Severity can be updated after the issue has first been converted to an incident.
- An update containing only `updated_at` returns `400`.
- String values such as `TRUE` and `FALSE` may be accepted for the Boolean `confidential` field.
- Retrieving an issue without authentication may return `404 Project Not Found` instead of `401 Unauthorized`.

Environment-specific or unresolved scenarios may be tagged with `@skip` to keep the standard suite stable.