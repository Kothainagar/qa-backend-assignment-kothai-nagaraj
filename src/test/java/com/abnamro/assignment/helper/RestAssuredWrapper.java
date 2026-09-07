package com.abnamro.assignment.helper;

import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
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

        RequestSpecification request = RestAssured
                .given()
                .config(RestAssuredConfig.config()
                        .httpClient(HttpClientConfig.httpClientConfig()
                                .setParam("http.connection.timeout", 10000)
                                .setParam("http.socket.timeout", 30000)
                                .setParam("http.connection-manager.timeout", 10000L)))
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
