package com.abnamro.assignment.stepdefinitions;

import com.abnamro.assignment.BaseSetup;
import com.abnamro.assignment.helper.ScenarioContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;

import static org.testng.Assert.assertEquals;

public class SharedStepDefinitions extends BaseSetup {

    private final ScenarioContext context;
    private final CreateIssueStepDefinitions createIssueSteps;

    public SharedStepDefinitions(ScenarioContext context, CreateIssueStepDefinitions createIssueSteps) {
        this.context = context;
        this.createIssueSteps = createIssueSteps;
    }

    @Given("A GitLab project issue exists in the project")
    public void ensureGitLabIssueExists() {
        log.info("Ensuring a GitLab project issue exists in the project before deletion");
        createIssueSteps.createMandatoryIssue();
    }

    @Then("the request should be rejected with status code {} and error message {}")
    public void validateRejectedRequest(int expectedStatus,
                                        String expectedError) {
        log.info("Verifying rejection response with expected status: {} and error message: {}", expectedStatus, expectedError);
        assertEquals(context.response.statusCode(), expectedStatus, "Unexpected rejection status");
        String actualError = "";

        switch (expectedError) {
            case "can't be blank":
                actualError = context.response.jsonPath().getString("message.title[0]");
                break;
            case "401 Unauthorized":
            case "404 Not found":
            case "404 Issue Not Found":
            case "Duplicated issue":
                actualError = context.response.jsonPath().getString("message");
                break;
            default:
                actualError = context.response.jsonPath().getString("error");
        }

        assertEquals(actualError, expectedError, "Unexpected error message");

    }

}
