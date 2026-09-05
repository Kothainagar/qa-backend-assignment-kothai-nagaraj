package com.abnamro.assignment.stepdefinitions;

import com.abnamro.assignment.BaseSetup;
import com.abnamro.assignment.helper.ScenarioContext;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.Method;

import java.util.HashMap;
import java.util.Map;

import static com.abnamro.assignment.helper.Utilities.dataTableToMap;
import static com.abnamro.assignment.helper.Utilities.prepareHeaders;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class DeleteIssueStepDefinitions extends BaseSetup {

    private final ScenarioContext context;
    private Map<String, Object> testData = new HashMap<>();


    public DeleteIssueStepDefinitions(ScenarioContext context) {
        this.context = context;
    }


    @When("I send the request to delete the issue")
    public void sendDeleteIssueRequest() {
        sendDeleteIssueRequestWithDetails(null);
    }

    @When("I send the request to delete the issue with below details")
    public void sendDeleteIssueRequestWithDetails(DataTable dataTable) {
        // Convert DataTable to a map (Utilities.dataTableToMap handles null/empty DataTable)
        testData = dataTableToMap(dataTable);
        String iid = context.iid;

        if (testData.containsKey("iid")) {
            iid = testData.get("iid").toString();
        }

        performDelete(iid);
    }


    @Then("the issue should be deleted successfully")
    public void validateDeletion() {
        log.info("Validating deletion response for issue IID: {}", context.iid);
        assertNotNull(context.response, "No delete response is available");
        assertEquals(context.response.statusCode(), 204,
                "Unexpected delete-issue status");
        assertEquals(context.response.asString(), "",
                "Expected empty response body for delete operation");
        log.info("Issue with IID: {} deleted successfully", context.iid);
    }


    private void performDelete(String issueIid) {
        assertNotNull(issueIid, "No issue IID is available");

        String endpoint = "/projects/" + projectId + "/issues/" + issueIid;

        String authType = testData.getOrDefault("authType", "valid").toString();
        Map<String, String> headers = prepareHeaders(authType, token);

        log.info("Sending DELETE request to endpoint: {}", endpoint);

        context.response = restAssuredWrapper.sendRequest(
                Method.DELETE,
                endpoint,
                headers,
                null
        );

        log.info("Delete response status: {}, body: {}", context.response.statusCode(), context.response.asPrettyString());
    }
}
