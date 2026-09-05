package com.abnamro.assignment.helper;

import com.jayway.jsonpath.DocumentContext;
import io.restassured.response.Response;

public class ScenarioContext {

    public Response response;

    public DocumentContext createIssueRequest;
    public DocumentContext createIssueResponse;

    public DocumentContext retrieveIssueResponse;

    public DocumentContext updateIssueRequest;
    public DocumentContext updateIssueResponse;

    public DocumentContext deleteIssueResponse;

    public String iid;

}