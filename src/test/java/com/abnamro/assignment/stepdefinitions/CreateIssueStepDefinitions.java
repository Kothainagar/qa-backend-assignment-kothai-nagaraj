package com.abnamro.assignment.stepdefinitions;

import com.abnamro.assignment.BaseSetup;
import com.abnamro.assignment.helper.ScenarioContext;
import com.jayway.jsonpath.DocumentContext;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.Method;
import org.testng.Assert;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static com.abnamro.assignment.helper.Utilities.dataTableToMap;
import static com.abnamro.assignment.helper.Utilities.prepareHeaders;
import static com.abnamro.assignment.helper.Utilities.prettyPrint;
import static com.abnamro.assignment.helper.Utilities.readJsonAsDocumentContext;
import static com.abnamro.assignment.helper.Utilities.removeField;
import static com.abnamro.assignment.helper.Utilities.validateMatchingFields;
import static java.util.UUID.randomUUID;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class CreateIssueStepDefinitions extends BaseSetup {

    private final ScenarioContext context;
    private DocumentContext requestContext;
    private DocumentContext responseContext;
    private Map<String, Object> testData = new HashMap<>();
    private String requestType;

    public CreateIssueStepDefinitions(ScenarioContext context) {
        this.context = context;
    }

    @Given("I prepare a create issue request with the {} fields")
    public void prepareCreateIssue(String fieldType) {
        prepareCreateIssueWithDetails(fieldType, null);
    }

    @Given("I prepare a create issue request with the {} fields with below details")
    public void prepareCreateIssueWithDetails(String fieldType,
                                              DataTable dataTable) {
        log.info("Preparing create issue request with {} fields", fieldType);
        requestType = fieldType.toLowerCase();
        validateFieldType(fieldType);

        // Convert DataTable to a map (Utilities.dataTableToMap handles null/empty DataTable)
        testData = dataTableToMap(dataTable);

        //load the appropriate JSON template based on fieldType
        String resourcePath = "testData/issues/create-issue_request_with_" + fieldType + "_fields.json";
        requestContext = readJsonAsDocumentContext(resourcePath);

        // Set or generate a title
        requestContext.set("$.title", testData.getOrDefault("title", "Create Issue - " + randomUUID()));

        // Set optional fields in the request
        if ("optional".equals(fieldType)) {
            setOptionalFields(requestContext);
        }

        // Remove fields mentioned in the dataTable from the requestContext
        removeField(requestContext, testData);

        // Store the prepared requestContext in the scenario context for later use
        context.createIssueRequest = requestContext;
    }

    @When("I send the request to create the issue")
    public void sendCreateIssue() {
        // Construct the endpoint for creating an issue in the specified project
        String endpoint = "/projects/" + projectId + "/issues";
        log.info("Sending request to create issue for project ID with endpoint: {}, body: {}", endpoint, prettyPrint(requestContext));

        //prepare Headers
        String authType = testData.getOrDefault("authType", "valid").toString();
        Map<String, String> headers = prepareHeaders(authType, token);

        //sendRequest
        context.response = restAssuredWrapper.sendRequest(
                Method.POST,
                endpoint,
                headers,
                requestContext.jsonString()
        );

        //validate response and set context
        log.info("Create response status: {}, body: {}", context.response.statusCode(), context.response.asPrettyString());
        if (context.response.statusCode() == 201) {
            context.createIssueResponse = readJsonAsDocumentContext(context.response);
            context.iid = context.createIssueResponse.read("$.iid").toString();
        }
    }

    @Then("the issue should be created successfully with the expected details")
    public void validateCreateIssue() {
        log.info("Verifying issue creation response");
        // Validate the response status code and parse the response body
        assertNotNull(context.response, "No create response is available");
        assertEquals(context.response.statusCode(), 201, "Unexpected create-issue status");
        responseContext = readJsonAsDocumentContext(context.response);

        // Validate that the response contains the expected fields and values
        assertEquals(responseContext.read("$.title").toString(), requestContext.read("$.title").toString(), "Title mismatch");
        assertNotNull(responseContext.read("$.id"), "Issue ID is missing");
        assertNotNull(responseContext.read("$.iid"), "Issue IID is missing");
        assertEquals(responseContext.read("$.state").toString(), "opened", "Unexpected issue state");
        assertEquals(responseContext.read("$.project_id").toString(), projectId, "Project mismatch");

        // Validate optional fields if they were included in the request
        if ("optional".equals(requestType)) {
            validateResponseWithOptionalFields();
        }
        log.info("Response validation successful for issue creation");
    }

    private void validateFieldType(String fieldType) {
        if (!"mandatory".equals(fieldType) && !"optional".equals(fieldType)) {
            Assert.fail("Invalid field type: " + fieldType + ". Expected 'mandatory' or 'optional'.");
        }
    }

    public void createMandatoryIssue() {
        prepareCreateIssueWithDetails("mandatory", null);
        sendCreateIssue();
        assertEquals(context.response.statusCode(), 201, "Failed to create issue");
    }

    private void setOptionalFields(DocumentContext requestContext) {
        OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);

        requestContext.set("$.created_at",
                testData.getOrDefault("created_at", createdAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
        requestContext.set("$.start_date",
                testData.getOrDefault("start_date", createdAt.toLocalDate().plusDays(1).toString()));
        requestContext.set("$.due_date",
                testData.getOrDefault("due_date", createdAt.toLocalDate().plusDays(10).toString()));
        if (testData.containsKey("iid") && testData.get("iid").equals("use_from_previous_request")) {
            requestContext.set("$.iid", context.iid);
        } else {
            requestContext.set("$.iid",
                    testData.getOrDefault("iid", ThreadLocalRandom.current().nextInt(100_0, 1_000_0)));
        }
        requestContext.set("$.issue_type", testData.getOrDefault("issue_type", "issue"));
        requestContext.set("$.severity", testData.getOrDefault("severity", "unknown"));

        String confidentialType = testData.getOrDefault("confidentialType", "boolean").toString().trim().toLowerCase();
        Object confidentialValue = testData.get("confidential");
        Object confidential = switch (confidentialType) {
            case "integer" -> confidentialValue != null ? Integer.parseInt(confidentialValue.toString()) : 1;
            case "string" -> confidentialValue != null ? confidentialValue.toString() : "true";
            case "boolean" -> confidentialValue != null && Boolean.parseBoolean(confidentialValue.toString());
            default -> confidentialValue != null ? confidentialValue : false;
        };
        requestContext.set("$.confidential", confidential);

        // Ensure mutually-exclusive milestone fields are not both present
        requestContext.delete("$.milestone");
    }

    private void validateResponseWithOptionalFields() {
        validateMatchingFields(requestContext, responseContext);
    }
}