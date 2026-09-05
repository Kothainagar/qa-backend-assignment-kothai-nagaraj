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


    public Response sendRequest(
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
