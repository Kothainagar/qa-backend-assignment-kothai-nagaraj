package com.abnamro.assignment.helper;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import io.cucumber.datatable.DataTable;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class Utilities {

    private static final Logger log = LoggerFactory.getLogger(Utilities.class);

    public static DocumentContext readJsonAsDocumentContext(String resourcePath) {
        try (InputStream inputStream = getResourceStream(resourcePath)) {
            return JsonPath.parse(inputStream);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read JSON file: " + resourcePath, exception);
        }
    }

    public static DocumentContext readJsonAsDocumentContext(Response response) {
        if (response == null) {
            throw new IllegalArgumentException("Response must not be null");
        }
        return JsonPath.parse(response.asString());
    }

    private static InputStream getResourceStream(String resourcePath) {
        InputStream inputStream = Utilities.class.getClassLoader().getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new IllegalArgumentException("Resource file not found: " + resourcePath);
        }
        return inputStream;
    }

    public static Map<String, Object> readYaml(String resourcePath) {
        try (InputStream inputStream = getResourceStream(resourcePath)) {
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));

            Map<String, Object> yamlData = yaml.load(inputStream);

            if (yamlData == null) {
                throw new IllegalArgumentException("YAML file is empty: " + resourcePath);
            }

            return yamlData;
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read YAML file: " + resourcePath, exception);
        }
    }

    public static String prettyPrint(DocumentContext documentContext) {
        if (documentContext == null) {
            throw new IllegalArgumentException("DocumentContext must not be null");
        }

        return io.restassured.path.json.JsonPath.from(documentContext.jsonString()).prettify();
    }

    /**
     * Converts a Cucumber DataTable containing one data row into a map.
     * Returns an empty map when no table is provided.
     *
     * @param dataTable scenario data containing a header row and one data row
     * @return scenario data mapped by column name
     */
    public static Map<String, Object> prepareTestData(DataTable dataTable) {
        Map<String, Object> values = new HashMap<>();

        if (dataTable == null || dataTable.isEmpty()) {
            return values;
        }

        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);

        if (rows.size() != 1) {
            throw new IllegalArgumentException("Expected a header row and exactly one data row");
        }

        values.putAll(rows.get(0));
        return values;
    }


    /**
     * Removes fields listed in the test data from the request body.
     * Multiple field names can be separated using a pipe character.
     *
     * @param documentContext request body to modify
     * @param testData        scenario data containing the optional removeField value
     */
    public static void removeField(DocumentContext documentContext, Map<String, Object> testData) {
        if (!testData.containsKey("removeField")) {
            return;
        }
        Set<String> existingFields = documentContext.read("$.keys()");
        String removeFieldValue = String.valueOf(testData.get("removeField"));

        for (String field : removeFieldValue.split("\\|")) {
            String fieldName = field.trim().replace(" ", "_");

            if (!fieldName.isEmpty() && existingFields.contains(fieldName)) {
                documentContext.delete("$." + fieldName);
                existingFields.remove(fieldName);
            }
        }
    }

    public static String resolveProjectId(Map<String,Object> testData, String defaultProjectId){
        return testData.getOrDefault("projectId", defaultProjectId).toString();
    }

    public static String resolveIid(Map<String, Object> testData, String defaultIid) {
        Object iid = testData.getOrDefault("iid", defaultIid);
        return iid == null ? null : iid.toString();
    }

    public static Map<String, String> resolveHeaders(Map<String, Object> testData, String token) {
        String authType = testData.getOrDefault("authType", "valid").toString();
        return switch (authType) {
            case "valid" -> Map.of("Authorization", "Bearer " + token);
            case "noauth" -> Map.of();
            case "invalid" -> Map.of("Authorization", "Bearer invalid_token");
            default -> throw new IllegalArgumentException("Unsupported authentication type: " + authType);
        };
    }

    /**
     * Validates that fields supplied in the request match the corresponding
     * values returned in the response.
     *
     * @param requestContext  prepared request body
     * @param responseContext parsed response body
     */
    public static void validateMatchingFields(DocumentContext requestContext, DocumentContext responseContext, Map<String, Object> testData) {

        Set<String> requestFields = requestContext.read("$.keys()");

        for (String field : requestFields) {
            Object expected = requestContext.read("$." + field);

            switch (field) {
                case "discussion_to_resolve",
                     "merge_request_to_resolve_discussions_of" ->
                        log.info("Skipping field not returned in response: {}", field);

                case "severity" -> {
                    String issueType = responseContext.read("$.issue_type");
                    String expectedSeverity = "incident".equals(issueType)
                            ? expected.toString().toUpperCase()
                            : "UNKNOWN";
                    Object actual = responseContext.read("$.severity");

                    log.info("Validating {}: expected={}, actual={}", field, expectedSeverity, actual);
                    assertEquals(actual, expectedSeverity, "Severity mismatch");
                }

                case "state_event" -> {
                    String expectedState = switch (expected.toString()) {
                        case "close" -> "closed";
                        case "reopen" -> "opened";
                        default -> throw new IllegalArgumentException("Unsupported state event: " + expected);
                    };

                    Object actual = responseContext.read("$.state");
                    log.info("Validating state: expected={}, actual={}", expectedState, actual);
                    assertEquals(actual, expectedState, "Issue state mismatch");
                }

                case "milestone_id", "milestone" ->
                        validateMilestone(field, expected, responseContext, expectPopulated(field, testData));

                case "assignee_id", "assignee_ids" ->
                        validateAssignee(field, expected, responseContext, expectPopulated(field, testData));

                case "labels" ->
                        validateLabelFields("labels", expected, responseContext);

                case "add_labels" ->
                        validateLabelFields("add_labels", expected, responseContext);

                case "remove_labels" ->
                        validateLabelFields("remove_labels", expected, responseContext);

                case "confidential" -> {
                    Object expectedConfidential = convertValue(expected.toString(), "Boolean");
                    Object actualConfidential = responseContext.read("$.confidential");

                    log.info("Validating confidential: expected={}, actual={}",
                            expectedConfidential,
                            actualConfidential
                    );

                    assertEquals(actualConfidential, expectedConfidential, "Confidential mismatch");
                }

                case "created_at", "updated_at" -> {
                    Instant expectedTime = Instant.parse(expected.toString());
                    Instant actualTime = Instant.parse(
                            responseContext.read("$." + field).toString()
                    );

                    log.info("Validating {}: expected={}, actual={}", field, expectedTime, actualTime);
                    assertEquals(actualTime, expectedTime, field + " mismatch");
                }

                case "start_date", "due_date" ->
                        validateDateField(field, expected, responseContext, expectPopulated(field, testData));

                default -> {
                    Object actual = responseContext.read("$." + field);
                    log.info("Validating {}: expected={}, actual={}", field, expected, actual);
                    assertEquals(actual, expected, "Mismatch for field: " + field);
                }
            }
        }
    }

    private static boolean expectPopulated(String field, Map<String, Object> testData) {
        String unpopulatedFields = testData.getOrDefault("unpopulatedFields", "").toString();

        for (String unpopulatedField : unpopulatedFields.split("\\|")) {
            if (unpopulatedField.trim().equals(field)) {
                return false;
            }
        }
        return true;
    }

    private static List<String> parseCommaList(String value) {
        List<String> result = new ArrayList<>();
        for (String item : value.split(",")) {
            result.add(item.trim());
        }
        return result;
    }

    private static void validateDateField(String field, Object expected, DocumentContext responseContext, boolean expectPopulated) {
        Object actual = responseContext.read("$." + field);

        if (expectPopulated) {
            assertNotNull(actual, "Expected " + field + " to be populated with '" + expected + "', but it was null");
            log.info("Validating {}: expected={}, actual={}", field, expected, actual);
            assertEquals(actual, expected, field + " mismatch");
        } else {
            log.info("Validating {}: expecting null (invalid input), actual={}", field, actual);
            assertNull(actual, "Expected " + field + " to be null for invalid input '" + expected + "', but got: " + actual);
        }
    }

    private static void validateLabelFields(String field, Object expected, DocumentContext responseContext) {

        List<String> expectedLabels = parseCommaList(expected.toString());
        List<String> actualLabels = responseContext.read("$.labels");

        log.info("Validating field: {}, expected: {}, actual: {}", field, expectedLabels, actualLabels);

        switch (field) {
            case "labels" -> {
                    assertEquals(actualLabels.size(), expectedLabels.size(), "Unexpected number of labels");
                    assertTrue(actualLabels.containsAll(expectedLabels), "Labels mismatch");
            }
            case "add_labels" ->
                    assertTrue(actualLabels.containsAll(expectedLabels), "Added labels are missing from the response");

            case "remove_labels" ->
                    assertTrue(actualLabels.stream().noneMatch(expectedLabels::contains), "Removed labels are still present in the response");

            default -> throw new IllegalArgumentException("Unsupported label field: " + field);
        }
    }

    private static void validateMilestone(String field, Object expected, DocumentContext responseContext, boolean expectPopulated) {

        Object milestone = responseContext.read("$.milestone");

        if (expectPopulated) {
            assertNotNull(milestone, "Expected milestone to be populated for " + field + " '" + expected + "', but it was null");
            String responsePath = "milestone_id".equals(field) ? "$.milestone.id" : "$.milestone.title";
            Object actual = responseContext.read(responsePath);
            log.info("Validating {}: expected={}, actual={}", field, expected, actual);
            assertEquals(actual.toString(), expected.toString(), field + " mismatch");
        } else {
            log.info("Validating {}: expecting null (non-existent reference), actual={}", field, milestone);
            assertNull(milestone, "Expected milestone to be null for non-existent " + field + " '" + expected + "', but got: " + milestone);
        }
    }

    private static void validateAssignee(String field, Object expected, DocumentContext responseContext, boolean expectPopulated) {

        List<Integer> actualAssigneeIds = responseContext.read("$.assignees[*].id");

        if (expectPopulated) {
            assertFalse(actualAssigneeIds.isEmpty(), "Expected assignee(s) to be populated for " + field + " '" + expected + "', but assignees list was empty");
            log.info("Validating {}: expected={}, actual={}", field, expected, actualAssigneeIds);

            if ("assignee_id".equals(field)) {
                assertEquals(actualAssigneeIds.get(0).toString(), expected.toString(), "Assignee ID mismatch");
            } else {
                assertEquals(actualAssigneeIds, expected, "Assignee IDs mismatch");
            }
        } else {
            log.info("Validating {}: expecting empty (non-existent reference), actual={}", field, actualAssigneeIds);
            assertTrue(actualAssigneeIds.isEmpty(), "Expected assignee(s) to be empty for non-existent " + field + " '" + expected + "', but got: " + actualAssigneeIds);
        }
    }

    /**
     * Converts a DataTable string value into the requested JSON-compatible type.
     *
     * @param value value to convert
     * @param type  target type, such as string, boolean, integer, long,
     *              jsonarray, or jsonobject
     * @return converted value
     */
    public static Object convertValue(String value, String type) {
        String trimmedValue = value.trim();

        if (type == null || type.isBlank()) {
            return trimmedValue;
        }

        return switch (type.trim().toLowerCase()) {
            case "string" -> trimmedValue;
            case "boolean" -> Boolean.valueOf(trimmedValue);
            case "integer" -> Integer.valueOf(trimmedValue);
            case "long" -> Long.valueOf(trimmedValue);
            case "jsonarray", "jsonobject" -> JsonPath.parse(trimmedValue).json();
            default -> throw new IllegalArgumentException("Unsupported type: " + type);
        };
    }
}
