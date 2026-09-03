package com.abnamro.assignment.helper;

import com.jayway.jsonpath.DocumentContext;
import io.restassured.response.Response;

public class ScenarioContext {

    private DocumentContext requestContext;
    private Response response;
    private Number createdIssueIid;

    public DocumentContext getRequestContext() {
        return requestContext;
    }

    public void setRequestContext(DocumentContext requestContext) {
        this.requestContext = requestContext;
    }

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    public Number getCreatedIssueIid() {
        return createdIssueIid;
    }

    public void setCreatedIssueIid(Number createdIssueIid) {
        this.createdIssueIid = createdIssueIid;
    }

}