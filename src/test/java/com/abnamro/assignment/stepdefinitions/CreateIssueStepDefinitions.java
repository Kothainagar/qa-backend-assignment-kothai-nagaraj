package com.abnamro.assignment.stepdefinitions;

import com.abnamro.assignment.BaseSetup;
import com.abnamro.assignment.helper.ScenarioContext;
import com.jayway.jsonpath.DocumentContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.Method;
import io.restassured.response.Response;

import java.util.Map;
import java.util.UUID;

import static com.abnamro.assignment.helper.Utilities.prettyPrint;
import static com.abnamro.assignment.helper.Utilities.readJsonAsDocumentContext;
import static org.testng.Assert.assertEquals;

public class CreateIssueStepDefinitions extends BaseSetup {

    private final ScenarioContext context;
    private DocumentContext requestContext;
    private DocumentContext responseContext;
    private Response response;

    public CreateIssueStepDefinitions(ScenarioContext context) {
        this.context = context;
    }

    @Given("I prepare a create issue request with the {word} fields")
    public void i_prepare_a_create_issue_request_with_the_fields(String fieldType) {
        log.info("Preparing create issue request with {} fields", fieldType);
        if(!fieldType.equals("mandatory") && !fieldType.equals("optional")) {
            throw new IllegalArgumentException(
                    "Invalid field type: " + fieldType + ". Expected 'mandatory' or 'optional'."
            );
        }
        String resourcePath = "testData/issues/create-issue_request_with_" + fieldType + "_fields.json";
        requestContext = readJsonAsDocumentContext(resourcePath);
        requestContext.set("$.title","Create Issue - " + UUID.randomUUID());
        context.setRequestContext(requestContext);
    }

    @When("I send the request to create the issue")
    public void i_send_the_request_to_create_the_issue() {
        String endpoint = "/projects/" + projectId + "/issues";
        log.info("Sending request to create issue for project ID with endpoint: {}, Project ID: {}", endpoint, projectId);
        response = restAssuredWrapper.sendRequestWithHeaderAndBody(
                Method.POST,
                endpoint,
                Map.of("Authorization", "Bearer " + token),
                requestContext.jsonString()
        );
        assertEquals(response.statusCode(), 201, "Unexpected create-issue status");
        log.info("Response body: {}", prettyPrint(response));
        context.setResponse(response);
        context.setCreatedIssueIid(response.jsonPath().get("iid"));
    }

    @Then("the issue should be created successfully with the expected details")
    public void the_issue_should_be_created_successfully_with_the_expected_details() {
        log.info("Verifying issue creation response");
        responseContext = readJsonAsDocumentContext(response);
        assertEquals(responseContext.read("$.title").toString(), requestContext.read("$.title").toString(), "Title mismatch");
    }

    public void createMandatoryIssue() {
        i_prepare_a_create_issue_request_with_the_fields("mandatory");
        i_send_the_request_to_create_the_issue();
    }

}
