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

import static com.abnamro.assignment.helper.Utilities.dataTableToMap;
import static com.abnamro.assignment.helper.Utilities.prepareHeaders;
import static com.abnamro.assignment.helper.Utilities.readJsonAsDocumentContext;
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
        // Convert DataTable to a map (Utilities.dataTableToMap handles null/empty DataTable)
        testData = dataTableToMap(dataTable);
        String iid = context.iid;
        if (testData.containsKey("iid")) {
            iid = testData.get("iid").toString();
        }
        performRetrieve(iid);
    }

    @Then("the issue should be retrieved successfully with the expected details")
    public void validateRetrievedIssue() {
        log.info("Validating retrieved issue response for IID: {}", context.iid);
        // Validate the response status code and parse the response body
        assertNotNull(context.response, "No retrieve response is available");
        assertEquals(context.response.statusCode(), 200, "Unexpected retrieve status");
        responseContext = readJsonAsDocumentContext(context.response);
        context.retrieveIssueResponse = responseContext;

        assertNotNull(responseContext.read("$.title"), "Title is missing");
        assertNotNull(responseContext.read("$.id"), "Issue ID is missing");
        assertNotNull(responseContext.read("$.iid"), "Issue IID is missing");
        assertEquals(responseContext.read("$.state"), "opened", "Unexpected issue state");
        assertEquals(responseContext.read("$.project_id").toString(), projectId, "Project mismatch");
        log.info("Retrieved issue validation successful for IID: {}", context.iid);

    }

    @Then("the retrieve response should match the created issue")
    public void validateRetrieveResponseMatchesCreateResponse() {
        log.info("Validating that the retrieve response matches the created issue for IID: {}", context.iid);

        assertNotNull(context.response, "No retrieve response is available");
        assertEquals(context.response.statusCode(), 200, "Unexpected retrieve status");
        responseContext = readJsonAsDocumentContext(context.response);
        context.retrieveIssueResponse = responseContext;

        DocumentContext createResponseContext = context.createIssueResponse;
        assertNotNull(createResponseContext, "Original create response is missing");

        String[] paths = {
                "$.id", "$.iid", "$.project_id", "$.title", "$.description",
                "$.state", "$.issue_type", "$.confidential", "$.created_at"
        };

        for (String path : paths) {
            Object actual = responseContext.read(path);
            Object expected = createResponseContext.read(path);

            assertEquals(actual, expected, "Mismatch for response field: " + path);
        }
        log.info("Retrieve response matches the created issue for IID: {}", context.iid);
    }

    private void performRetrieve(String iid) {
        log.info("Preparing to send request to retrieve issue with IID: {}", iid);
        assertNotNull(iid, "No issue IID is available");

        // Construct the endpoint for retrieving the issue
        String endpoint = "/projects/" + projectId + "/issues/" + iid;

        log.info("Sending GET request to endpoint: {}", endpoint);

        //prepare Headers
        String authType = testData.getOrDefault("authType", "valid").toString();
        Map<String, String> headers = prepareHeaders(authType, token);

        context.response = restAssuredWrapper.sendRequest(
                Method.GET, endpoint, headers, null
        );

        log.info("Get response status: {}, body: {}", context.response.statusCode(), context.response.asPrettyString());
    }

}