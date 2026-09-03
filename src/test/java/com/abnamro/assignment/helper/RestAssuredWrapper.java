package com.abnamro.assignment.helper;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Method;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Map;

public class RestAssuredWrapper {
    private final String baseUrl;


    public RestAssuredWrapper(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Response sendRequestWithHeaderAndBody(
            Method method,
            String endpoint,
            Map<String, String> headers,
            String requestBody) {

        return sendRequest(method, endpoint, headers, requestBody);
    }

    public Response sendRequestWithHeaderOnly(
            Method method,
            String endpoint,
            Map<String, String> headers) {

        return sendRequest(method, endpoint, headers, null);
    }

    public Response sendRequestWithoutHeader(
            Method method,
            String endpoint,
            String requestBody) {

        return sendRequest(method, endpoint, Map.of(), requestBody);
    }

    public Response sendRequestWithoutHeaderAndBody(
            Method method,
            String endpoint) {

        return sendRequest(method, endpoint, Map.of(), null);
    }

    private Response sendRequest(
            Method method,
            String endpoint,
            Map<String, String> headers,
            String requestBody) {

        RequestSpecification request = RestAssured.given()
                .baseUri(baseUrl);

        if (headers != null && !headers.isEmpty()) {
            request.headers(headers);
        }

        if (requestBody != null) {
            request.contentType(ContentType.JSON)
                    .body(requestBody);
        }

        return request.request(method, endpoint);
    }

}
