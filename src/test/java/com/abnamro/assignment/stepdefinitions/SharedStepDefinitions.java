package com.abnamro.assignment.stepdefinitions;

import com.abnamro.assignment.BaseSetup;
import com.abnamro.assignment.helper.ScenarioContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.restassured.path.json.JsonPath;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class SharedStepDefinitions extends BaseSetup {

    private final ScenarioContext context;
    private final CreateIssueStepDefinitions createIssueSteps;

    public SharedStepDefinitions(
            ScenarioContext context,
            CreateIssueStepDefinitions createIssueSteps) {

        this.context = context;
        this.createIssueSteps = createIssueSteps;
    }

    @Given("A GitLab project issue exists in the project")
    public void ensureProjectIssueExists() {
        log.info("Creating an issue required for the scenario");
        createIssueSteps.createMandatoryIssue();
    }

    @Then("the request should be rejected with status code {} and error message {}")
    public void validateRejectedRequest(int expectedStatus, String expectedError) {
        log.info("Validating rejection response: expected status={}, error={}", expectedStatus, expectedError);

        assertNotNull(context.response, "No response is available");
        assertEquals(context.response.statusCode(), expectedStatus, "Unexpected rejection status");

        String actualError = readErrorMessage(context.response.jsonPath());

        log.info("Validating error message: expected={}, actual={}", expectedError, actualError);

        assertEquals(actualError, expectedError, "Unexpected error message");
    }

    private String readErrorMessage(JsonPath responseJson) {
        String error = responseJson.getString("error");

        if (error != null) {
            return error;
        }

        String fieldError = responseJson.getString("message.title[0]");

        if (fieldError != null) {
            return fieldError;
        }

        String message = responseJson.getString("message");

        if (message != null) {
            return message;
        }

        throw new IllegalArgumentException(
                "No supported error message was found in the response"
        );
    }
}