package com.abnamro.assignment.stepdefinitions;

import com.abnamro.assignment.BaseSetup;
import com.abnamro.assignment.helper.ScenarioContext;
import com.jayway.jsonpath.DocumentContext;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.Method;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.abnamro.assignment.helper.Utilities.convertValue;
import static com.abnamro.assignment.helper.Utilities.prepareTestData;
import static com.abnamro.assignment.helper.Utilities.prettyPrint;
import static com.abnamro.assignment.helper.Utilities.readJsonAsDocumentContext;
import static com.abnamro.assignment.helper.Utilities.removeField;
import static com.abnamro.assignment.helper.Utilities.resolveHeaders;
import static com.abnamro.assignment.helper.Utilities.resolveProjectId;
import static com.abnamro.assignment.helper.Utilities.validateMatchingFields;
import static java.util.UUID.randomUUID;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class CreateIssueStepDefinitions extends BaseSetup {

    private final ScenarioContext context;
    private DocumentContext requestContext;
    private DocumentContext responseContext;
    private Map<String, Object> testData = new HashMap<>();
    private boolean optionalRequest;

    public CreateIssueStepDefinitions(ScenarioContext context) {
        this.context = context;
    }

    @Given("I prepare a create issue request with the {} fields")
    public void prepareCreateIssue(String fieldType) {
        prepareCreateIssueWithDetails(fieldType, null);
    }

    @Given("I prepare a create issue request with the {} fields with below details")
    public void prepareCreateIssueWithDetails(String fieldType, DataTable dataTable) {
        log.info("Preparing create issue request with {} fields", fieldType);

        validateFieldType(fieldType);
        optionalRequest = "optional".equals(fieldType);
        testData = prepareTestData(dataTable);

        String resourcePath =
                "testData/issues/create-issue_request_with_"
                        + fieldType
                        + "_fields.json";

        requestContext = readJsonAsDocumentContext(resourcePath);

        setTitle();

        if (optionalRequest) {
            setOptionalFields();
        }

        // Negative scenarios can request that specific fields are removed.
        removeField(requestContext, testData);
        context.createIssueRequest = requestContext;
    }

    @When("I send the request to create the issue")
    public void sendCreateIssue() {
        String testProjectId = resolveProjectId(testData, projectId);
        String endpoint = "/projects/" + testProjectId + "/issues";
        Map<String, String> headers = resolveHeaders(testData, token);

        log.info("Sending create request to endpoint: {}, body: {}", endpoint, prettyPrint(requestContext));

        context.response = restAssuredWrapper.sendRequest(
                Method.POST,
                endpoint,
                headers,
                requestContext.jsonString()
        );

        log.info("Create response status: {}, body: {}", context.response.statusCode(), context.response.asPrettyString());

        // Store successful responses so later steps can reuse the created IID.
        if (context.response.statusCode() == 201) {
            context.createIssueResponse = readJsonAsDocumentContext(context.response);
            context.iid = context.createIssueResponse.read("$.iid").toString();
        }
    }

    @Then("the issue should be created successfully with the expected details")
    public void validateCreateIssue() {
        log.info("Validating create response");

        assertNotNull(context.response, "No create response is available");
        assertEquals(context.response.statusCode(), 201, "Unexpected create status");

        responseContext = readJsonAsDocumentContext(context.response);
        assertEquals(
                responseContext.read("$.title").toString(),
                requestContext.read("$.title").toString(),
                "Title mismatch"
        );
        assertNotNull(responseContext.read("$.id"), "Issue ID is missing");
        assertNotNull(responseContext.read("$.iid"), "Issue IID is missing");
        assertEquals(responseContext.read("$.state"), "opened", "Unexpected issue state");
        assertEquals(responseContext.read("$.project_id").toString(), projectId, "Project ID mismatch");

        if (optionalRequest) {
            validateMatchingFields(requestContext, responseContext, testData);
        }

        log.info("Create response validation completed successfully");
    }

    public void createMandatoryIssue() {
        prepareCreateIssueWithDetails("mandatory", null);
        sendCreateIssue();

        assertEquals(context.response.statusCode(), 201, "Failed to create the required issue");
    }

    private void setOptionalFields() {
        ChronoUnit timePrecision = ChronoUnit.SECONDS;
        OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(timePrecision);

        setField("created_at", createdAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        setField("start_date", createdAt.toLocalDate().plusDays(1).toString());

        setField("due_date", createdAt.toLocalDate().plusDays(10).toString());

        setIssueIid();
        setField("issue_type", "issue");
        setField("severity", "unknown");
        setField("confidential", false);
        setMilestone();
        setField("assignee_id", requestContext.read("$.assignee_id"));
    }

    private void setIssueIid() {
        if (!testData.containsKey("iid")) {
            Set<String> requestFields = requestContext.read("$.keys()");

            if (requestFields.contains("iid")) {
                requestContext.delete("$.iid");
            }

            return;
        }

        Object iidValue = testData.get("iid");

        if ("use_from_previous_request".equals(iidValue)) {
            requestContext.set("$.iid", context.iid);
            return;
        }

        setField("iid", iidValue);
    }

    private void setMilestone() {
        if (!testData.containsKey("milestone")) {
            setField("milestone_id", requestContext.read("$.milestone_id"));
            return;
        }

        Set<String> requestFields = requestContext.read("$.keys()");

        // Only one milestone representation can be included in a request.
        if (requestFields.contains("milestone_id")) {
            requestContext.delete("$.milestone_id");
        }

        requestContext.put("$", "milestone", testData.get("milestone"));
    }

    private void setField(String field, Object defaultValue) {
        if (!testData.containsKey(field)) {
            requestContext.set("$." + field, defaultValue);
            return;
        }

        String value = testData.get(field).toString();
        String type = testData.getOrDefault("fieldType", "string").toString();

        requestContext.set("$." + field, convertValue(value, type));
    }

    private void validateFieldType(String fieldType) {
        if ("mandatory".equals(fieldType) || "optional".equals(fieldType)) {
            return;
        }
        throw new IllegalArgumentException("Invalid field type: " + fieldType + ". Expected 'mandatory' or 'optional'.");
    }

    private void setTitle() {
        Object title = testData.get("title");

        if ("[empty]".equals(title)) {
            title = "";
        } else if (!testData.containsKey("title")) {
            title = "Create Issue - " + randomUUID();
        }

        requestContext.set("$.title", title);
    }
}