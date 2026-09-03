package com.abnamro.assignment.stepdefinitions;

import com.abnamro.assignment.BaseSetup;
import com.abnamro.assignment.helper.ScenarioContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.Method;
import io.restassured.response.Response;

import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class DeleteIssueStepDefinitions extends BaseSetup {

    private final ScenarioContext context;
    private Response deleteResponse;
    private final CreateIssueStepDefinitions createIssueSteps;

    public DeleteIssueStepDefinitions(ScenarioContext context, CreateIssueStepDefinitions createIssueSteps) {
        this.context = context;
        this.createIssueSteps = createIssueSteps;
    }

    @Given("A GitLab issue exists in the project")
    public void a_git_lab_issue_exists_in_the_project() {
        log.info("Ensuring a GitLab issue exists in the project before deletion");
        createIssueSteps.createMandatoryIssue();
    }
    @When("I send the request to delete the issue")
    public void i_send_the_request_to_delete_the_issue() {
        performDelete();
    }
    @Then("the issue should be deleted successfully")
    public void validateDeletion() {
        log.info("Validating deletion response for issue IID: {}", context.getCreatedIssueIid());
        assertNotNull(deleteResponse, "No delete response is available");
        assertEquals(deleteResponse.statusCode(), 204,
                "Unexpected delete-issue status");
        assertEquals(deleteResponse.asString(), "",
                "Expected empty response body for delete operation");
        log.info("Issue with IID: {} deleted successfully", context.getCreatedIssueIid());
    }


    @Then("I verify that the issue is no longer available")
    public void i_verify_that_the_issue_is_no_longer_available() {
        // Write code here that turns the phrase above into concrete actions
        throw new io.cucumber.java.PendingException();
    }

    @Then("I delete the created issue")
    public void deleteCreatedIssue() {
        performDelete();
        validateDeletion();
    }

    private void performDelete() {
        String endpoint = issueEndpoint();
        log.info("Sending request to delete issue with endpoint: {} and IID: {}", endpoint, context.getCreatedIssueIid());
        deleteResponse = restAssuredWrapper.sendRequestWithHeaderOnly(
                Method.DELETE,
                endpoint,
                Map.of("Authorization", "Bearer " + token)
        );
        log.info("Delete response status: {}, body: {}", deleteResponse.statusCode(), deleteResponse.asString());
    }

    private String issueEndpoint() {
        assertNotNull(context.getCreatedIssueIid(),
                "No issue IID is available");

        return "/projects/" + projectId
                + "/issues/" + context.getCreatedIssueIid();
    }

}
