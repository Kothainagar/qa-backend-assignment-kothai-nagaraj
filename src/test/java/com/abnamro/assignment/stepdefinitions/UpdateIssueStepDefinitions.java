package com.abnamro.assignment.stepdefinitions;

import com.abnamro.assignment.BaseSetup;
import com.abnamro.assignment.helper.ScenarioContext;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.Method;

import java.util.HashMap;
import java.util.Map;

import static com.abnamro.assignment.helper.Utilities.convertValue;
import static com.abnamro.assignment.helper.Utilities.dataTableToMap;
import static com.abnamro.assignment.helper.Utilities.prepareHeaders;
import static com.abnamro.assignment.helper.Utilities.readJsonAsDocumentContext;
import static com.abnamro.assignment.helper.Utilities.removeField;
import static com.abnamro.assignment.helper.Utilities.validateMatchingFields;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class UpdateIssueStepDefinitions extends BaseSetup {

    private final ScenarioContext context;
    private DocumentContext requestContext;
    private DocumentContext responseContext;
    private Map<String, Object> testData = new HashMap<>();
    private String iid;

    public UpdateIssueStepDefinitions(ScenarioContext context) {
        this.context = context;
    }


    @Given("I prepare an update issue request with below details")
    public void prepareUpdateIssueRequestWithDetails(DataTable dataTable) {
        log.info("Preparing the update issue request");

        testData = dataTableToMap(dataTable);

        requestContext = JsonPath.parse("{}");

        iid = context.iid;
        if (testData.containsKey("iid")) {
            iid = testData.get("iid").toString();
        }

        applyTestDataToRequest();

        removeField(requestContext, testData);

        context.updateIssueRequest = requestContext;

        log.info("Prepared update request body: {}", requestContext.jsonString());
    }

    @When("I send the request to update the issue")
    public void sendUpdateIssueRequest() {
        assertNotNull(iid, "No issue IID is available");
        assertNotNull(requestContext, "No update request is available");

        String endpoint = "/projects/" + projectId + "/issues/" + iid;

        String authType = testData.getOrDefault("authType", "valid").toString();
        Map<String, String> headers = prepareHeaders(authType, token);

        log.info("Sending PUT request to endpoint: {}", endpoint);

        context.response = restAssuredWrapper.sendRequest(
                Method.PUT,
                endpoint,
                headers,
                requestContext.jsonString()
        );

        log.info("Update response status: {}, body: {}", context.response.statusCode(), context.response.asPrettyString());

        if (context.response.statusCode() == 200) {
            responseContext = readJsonAsDocumentContext(context.response);
            context.updateIssueResponse = responseContext;
        }
    }

    @Then("the issue should be updated successfully with the expected details")
    public void validateUpdatedIssue() {
        assertNotNull(context.response, "No update response is available");
        assertEquals(context.response.statusCode(), 200, "Unexpected update status");

        assertNotNull(context.updateIssueResponse, "Update response context is missing");
        responseContext = context.updateIssueResponse;

        assertEquals(
                responseContext.read("$.iid").toString(),
                context.iid,
                "Issue IID mismatch"
        );

        assertEquals(
                responseContext.read("$.project_id").toString(),
                projectId,
                "Project ID mismatch"
        );

        validateUpdatedFields();

        log.info("Issue updated successfully for IID: {}", context.iid);
    }

    private void applyTestDataToRequest() {
        String fieldsValue = testData.get("fields").toString();
        String valuesValue = testData.get("values").toString();

        String[] fields = fieldsValue.split("\\|", -1);
        String[] values = valuesValue.split("\\|", -1);

        if (fields.length != values.length) {
            throw new IllegalArgumentException(
                    "The number of update fields must match the number of values"
            );
        }

        requestContext = JsonPath.parse("{}");

        for (int index = 0; index < fields.length; index++) {
            String field = fields[index].trim();
            Object value = convertValue(values[index].trim());

            requestContext.put("$", field, value);
        }
    }

    private void validateUpdatedFields() {
        validateMatchingFields(requestContext, responseContext);
    }
}