package com.abnamro.assignment.stepdefinitions;

import com.abnamro.assignment.BaseSetup;
import com.abnamro.assignment.helper.ScenarioContext;
import io.cucumber.java.After;
import io.cucumber.java.Scenario;
import io.restassured.http.Method;
import io.restassured.response.Response;

import java.util.Map;

import static com.abnamro.assignment.helper.Utilities.resolveHeaders;

public class Hooks extends BaseSetup {

    private final ScenarioContext context;

    public Hooks(ScenarioContext context) {
        this.context = context;
    }

    /**
     * Removes an issue created during the scenario.
     * Cleanup is attempted whether the scenario passes or fails.
     * A 404 response means the issue was already deleted by the scenario.
     */
    @After
    public void cleanupCreatedIssue(Scenario scenario) {
        String createdIid = context.iid;

        if (createdIid == null) {
            log.info("No issue was created in scenario '{}'; nothing to clean up.", scenario.getName());
            return;
        }

        String endpoint = "/projects/" + projectId + "/issues/" + createdIid;
        Map<String, String> headers = resolveHeaders(Map.of(), token);

        log.info("Cleaning up issue IID {} after scenario '{}'. Scenario status: {}",
                createdIid,
                scenario.getName(),
                scenario.getStatus()
        );

        try {
            Response response = restAssuredWrapper.sendRequest(
                    Method.DELETE,
                    endpoint,
                    headers,
                    null
            );

            if (response.statusCode() == 204) {
                log.info("Successfully cleaned up issue IID: {}", createdIid);
            } else if (response.statusCode() == 404) {
                log.info("Issue IID {} was already deleted; cleanup is complete.", createdIid);
            } else {
                log.warn("Cleanup for issue IID {} returned unexpected status: {}", createdIid, response.statusCode());
            }
        } catch (Exception exception) {
            log.warn("Cleanup failed for issue IID {}: {}", createdIid, exception.getMessage());
        } finally {
            context.iid = null;
        }
    }
}
