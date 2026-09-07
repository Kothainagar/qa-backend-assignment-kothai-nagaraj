package com.abnamro.assignment.helper;

import com.jayway.jsonpath.DocumentContext;
import io.restassured.response.Response;

public class ScenarioContext {

    // Stores the latest API response for common status and error validation.
    public Response response;

    public DocumentContext createIssueRequest;
    public DocumentContext createIssueResponse;

    public DocumentContext retrieveIssueResponse;

    public DocumentContext updateIssueRequest;
    public DocumentContext updateIssueResponse;

    // Stores the created issue IID for retrieve, update and cleanup operations.
    public String iid;

}