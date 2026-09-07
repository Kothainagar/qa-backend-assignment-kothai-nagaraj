package com.abnamro.assignment.stepdefinitions;

import com.abnamro.assignment.BaseSetup;
import com.abnamro.assignment.helper.ScenarioContext;
import com.jayway.jsonpath.DocumentContext;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.Method;

import java.util.HashMap;
import java.util.Map;

import static com.abnamro.assignment.helper.Utilities.prepareTestData;
import static com.abnamro.assignment.helper.Utilities.readJsonAsDocumentContext;
import static com.abnamro.assignment.helper.Utilities.resolveHeaders;
import static com.abnamro.assignment.helper.Utilities.resolveIid;
import static com.abnamro.assignment.helper.Utilities.resolveProjectId;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class RetrieveIssueStepDefinitions extends BaseSetup {

    private final ScenarioContext context;
    private DocumentContext responseContext;
    private Map<String, Object> testData = new HashMap<>();

    public RetrieveIssueStepDefinitions(ScenarioContext context) {
        this.context = context;
    }

    @When("I send the request to retrieve the issue")
    public void sendRetrieveIssueRequest() {
        sendRetrieveIssueRequestWithDetails(null);
    }

    @When("I send the request to retrieve the issue with below details")
    public void sendRetrieveIssueRequestWithDetails(DataTable dataTable) {
        testData = prepareTestData(dataTable);
        String issueIid = resolveIid(testData, context.iid);

        performRetrieve(issueIid);
    }

    @Then("the issue should be retrieved successfully with the expected details")
    public void validateRetrievedIssue() {
        log.info("Validating retrieved issue for IID: {}", context.iid);

        prepareSuccessfulResponseContext();

        assertNotNull(responseContext.read("$.title"), "Title is missing");
        assertNotNull(responseContext.read("$.id"), "Issue ID is missing");
        assertNotNull(responseContext.read("$.iid"), "Issue IID is missing");

        assertEquals(responseContext.read("$.state"), "opened", "Unexpected issue state");

        assertEquals(responseContext.read("$.project_id").toString(), projectId, "Project ID mismatch");

        log.info("Retrieved issue validation completed for IID: {}", context.iid);
    }

    @Then("the retrieve response should match the created issue")
    public void validateRetrieveResponseMatchesCreateResponse() {
        log.info("Comparing retrieve and create responses for IID: {}", context.iid);

        prepareSuccessfulResponseContext();

        DocumentContext createResponseContext = context.createIssueResponse;
        assertNotNull(createResponseContext, "Original create response is missing");

        String[] matchingPaths = {
                "$.id",
                "$.iid",
                "$.project_id",
                "$.title",
                "$.description",
                "$.state",
                "$.issue_type",
                "$.confidential",
                "$.created_at"
        };

        for (String path : matchingPaths) {
            Object expected = createResponseContext.read(path);
            Object actual = responseContext.read(path);

            log.info("Validating path: {}, expected: {}, actual: {}", path, expected, actual);
            assertEquals(actual, expected, "Mismatch for response field: " + path);
        }

        log.info("Retrieve response matches create response for IID: {}", context.iid);
    }

    private void performRetrieve(String issueIid) {
        assertNotNull(issueIid, "No issue IID is available");

        String testProjectId = resolveProjectId(testData, projectId);
        String endpoint = "/projects/" + testProjectId + "/issues/" + issueIid;

        Map<String, String> headers = resolveHeaders(testData, token);

        log.info("Sending retrieve request to endpoint: {}", endpoint);

        context.response = restAssuredWrapper.sendRequest(
                Method.GET,
                endpoint,
                headers,
                null
        );

        log.info("Retrieve response status: {}, body: {}", context.response.statusCode(), context.response.asPrettyString());
    }

    private void prepareSuccessfulResponseContext() {
        assertNotNull(context.response, "No retrieve response is available");

        assertEquals(context.response.statusCode(), 200, "Unexpected retrieve status");

        responseContext = readJsonAsDocumentContext(context.response);

        // Store the parsed response so another step can access it if required.
        context.retrieveIssueResponse = responseContext;
    }
}