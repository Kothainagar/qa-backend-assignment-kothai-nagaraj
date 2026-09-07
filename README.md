# GitLab Issues API Test Automation

## Solution Overview

This project provides automated tests for the GitLab Issues API using Java, Cucumber, TestNG, REST Assured, and Maven.

The tests cover:

- Create, retrieve, update, and delete operations
- Request and response field validation
- Authentication errors
- Invalid and non-existent identifiers
- Boundary conditions and observed API edge cases
- Cleanup of issues created during test execution

## Prerequisites

- Java 17
- Maven 3.6 or later
- A GitLab account and project
- A GitLab OAuth2 access token with the `api` scope

Verify Java and Maven:

```bash
java -version
mvn -version
```

## Configuration and Token Setup

The default configuration is stored at:

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

Replace `project-id` when running the tests against another project.

The access token is read from an environment variable and is not stored in the repository.

Set the token on macOS or Linux:

```bash
read -s "GITLAB_TOKEN?Enter GitLab token: "
export GITLAB_TOKEN
```

Remove it after execution:

```bash
unset GITLAB_TOKEN
```

An environment-specific configuration can be selected using:

```bash
mvn test -Denv=qa
```

This command loads:

```text
src/test/resources/configs/config-qa.yaml
```

## Build and Test Commands

Run the complete build and test suite:

```bash
mvn clean install
```

Run only the tests:

```bash
mvn test
```

Build without running tests:

```bash
mvn clean install -DskipTests
```

Run scenarios using a Cucumber tag:

```bash
mvn test -Dcucumber.filter.tags="@createIssue-HappyFlow"
```

Available tags include:

```text
@createIssue-HappyFlow
@createIssue-ErrorFlow
@retrieveIssue-HappyFlow
@retrieveIssue-ErrorFlow
@updateIssue-HappyFlow
@updateIssue-ErrorFlow
@deleteIssue-HappyFlow
@deleteIssue-ErrorFlow
```

Scenarios tagged with `@skip` are excluded from the standard test run.

The Cucumber HTML report is generated at:

```text
target/cucumber-reports/test-results.html
```

Additional Maven and TestNG results are generated under:

```text
target/surefire-reports
```

GitHub Actions runs the tests when changes are pushed to the `master` branch. The repository requires an Actions secret named `GITLAB_TOKEN`.

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

- The configured project already exists.
- The token owner has sufficient project permissions.
- The configured assignee and milestone IDs exist in the default project.
- Premium and Ultimate-only fields are outside the current scope.
- The `test_case` issue type is excluded because it is unavailable in the current project or subscription.
- API behavior may vary depending on project permissions, configuration, subscription, or server version.

## Trade-offs and Observations

- Configuration and environment variables are used to avoid hardcoding the access token in the test code.
- Cucumber `ScenarioContext` shares request and response data between step-definition classes.
- Request and response validation is intentionally kept straightforward for the assignment.
- The optional `fieldType` test-data column is used when a request value must be sent as a Boolean, integer, array, or another non-string JSON type.
- A Cucumber `@After` hook attempts to remove issues created during each scenario, including failed scenarios.
- A cleanup response of `404` is accepted when the issue was already deleted by the scenario.
- Invalid `start_date` and `due_date` values may be accepted but returned as `null`.
- Severity supplied for non-incident issue types remains `UNKNOWN`.
- Changing an issue to an incident and setting severity in the same request may leave severity as `UNKNOWN`. Severity can be set after converting the issue to an incident.
- Retrieving an issue without authentication may return `404 Project Not Found` instead of `401 Unauthorized`.
- Non-existent assignee and milestone IDs may be accepted during creation but silently ignored.
- Environment-specific or unresolved scenarios may be tagged with `@skip` to keep the standard test suite stable.