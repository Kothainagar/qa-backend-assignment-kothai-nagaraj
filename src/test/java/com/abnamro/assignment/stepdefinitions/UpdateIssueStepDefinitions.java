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
import static com.abnamro.assignment.helper.Utilities.prepareTestData;
import static com.abnamro.assignment.helper.Utilities.prettyPrint;
import static com.abnamro.assignment.helper.Utilities.readJsonAsDocumentContext;
import static com.abnamro.assignment.helper.Utilities.removeField;
import static com.abnamro.assignment.helper.Utilities.resolveHeaders;
import static com.abnamro.assignment.helper.Utilities.resolveIid;
import static com.abnamro.assignment.helper.Utilities.resolveProjectId;
import static com.abnamro.assignment.helper.Utilities.validateMatchingFields;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class UpdateIssueStepDefinitions extends BaseSetup {

    private final ScenarioContext context;
    private DocumentContext requestContext;
    private DocumentContext responseContext;
    private Map<String, Object> testData = new HashMap<>();
    private String issueIid;
    private String requestProjectId;

    public UpdateIssueStepDefinitions(ScenarioContext context) {
        this.context = context;
    }

    @Given("I prepare an update issue request with below details")
    public void prepareUpdateIssueRequest(DataTable dataTable) {
        log.info("Preparing update request");

        testData = prepareTestData(dataTable);
        issueIid = resolveIid(testData, context.iid);
        requestProjectId = resolveProjectId(testData, projectId);

        buildRequestContext();

        removeField(requestContext, testData);

        context.updateIssueRequest = requestContext;

        log.info("Prepared update request body: {}", prettyPrint(requestContext));
    }

    @When("I send the request to update the issue")
    public void sendUpdateIssueRequest() {
        assertNotNull(issueIid, "No issue IID is available");
        assertNotNull(requestContext, "No update request is available");

        String endpoint = "/projects/" + requestProjectId + "/issues/" + issueIid;

        Map<String, String> headers = resolveHeaders(testData, token);

        log.info("Sending update request to endpoint: {}, body: {}", endpoint, prettyPrint(requestContext));

        context.response = restAssuredWrapper.sendRequest(
                Method.PUT,
                endpoint,
                headers,
                requestContext.jsonString()
        );

        log.info("Update response status: {}, body: {}", context.response.statusCode(), context.response.asPrettyString());

        // Store the parsed response only when the update succeeds.
        if (context.response.statusCode() == 200) {
            responseContext = readJsonAsDocumentContext(context.response);
            context.updateIssueResponse = responseContext;
        }
    }

    @Then("the issue should be updated successfully with the expected details")
    public void validateUpdatedIssue() {
        log.info("Validating update response for IID: {}", issueIid);

        assertNotNull(context.response, "No update response is available");

        assertEquals(context.response.statusCode(), 200, "Unexpected update status");

        assertNotNull(context.updateIssueResponse, "Update response context is missing");

        responseContext = context.updateIssueResponse;

        assertEquals(responseContext.read("$.iid").toString(), issueIid, "Issue IID mismatch");

        assertEquals(responseContext.read("$.project_id").toString(), requestProjectId, "Project ID mismatch");

        validateMatchingFields(requestContext, responseContext, testData);

        log.info("Update response validation completed for IID: {}", issueIid);
    }

    private void buildRequestContext() {
        String fieldsValue = getRequiredTestData("fields");
        String valuesValue = getRequiredTestData("values");
        Object fieldTypeValue = testData.get("fieldType");
        String typesValue = fieldTypeValue == null ? "" : fieldTypeValue.toString();

        String[] fields = fieldsValue.split("\\|");
        String[] values = valuesValue.split("\\|");
        String[] types = typesValue.split("\\|");

        validateFieldCounts(fields, values, types);

        requestContext = JsonPath.parse("{}");

        for (int index = 0; index < fields.length; index++) {
            String field = fields[index].trim();
            String value = values[index].trim();
            String type = types.length == 1
                    ? types[0].trim()
                    : types[index].trim();

            requestContext.put("$", field, convertValue(value, type));
        }
    }

    private String getRequiredTestData(String key) {
        Object value = testData.get(key);

        if (value == null) {
            throw new IllegalArgumentException("Missing required test data column: " + key);
        }

        return value.toString();
    }


    /**
     * Validates that each field has a corresponding value and field type.
     * A single field type can be applied to all fields, or a separate type
     * can be provided for every field.
     *
     * @param fields fields to include in the request
     * @param values values corresponding to the fields
     * @param types  one common type or one type for each field
     * @throws IllegalArgumentException if the number of fields, values, or types is invalid
     */
    private void validateFieldCounts(String[] fields, String[] values, String[] types) {
        if (fields.length != values.length) {
            throw new IllegalArgumentException("The number of fields must match the number of values");
        }

        if (types.length != 1 && types.length != fields.length) {
            throw new IllegalArgumentException("Provide either one common fieldType or one fieldType for each field");
        }
    }
}