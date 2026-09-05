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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class Utilities {

    private static final Logger log = LoggerFactory.getLogger(Utilities.class);

    public static DocumentContext readJsonAsDocumentContext(String resourcePath) {
        try (InputStream inputStream = getResourceStream(resourcePath)) {
            return JsonPath.parse(inputStream);
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Unable to read JSON file: " + resourcePath,
                    exception
            );
        }
    }

    public static DocumentContext readJsonAsDocumentContext(Response response) {
        if (response == null) {
            throw new IllegalArgumentException("Response must not be null");
        }

        return JsonPath.parse(response.asString());
    }

    private static InputStream getResourceStream(String resourcePath) {
        InputStream inputStream = Utilities.class
                .getClassLoader()
                .getResourceAsStream(resourcePath);

        if (inputStream == null) {
            throw new IllegalArgumentException(
                    "Resource file not found: " + resourcePath
            );
        }

        return inputStream;
    }

    public static Map<String, Object> readYaml(String resourcePath) {
        try (InputStream inputStream = getResourceStream(resourcePath)) {
            Yaml yaml = new Yaml(
                    new SafeConstructor(new LoaderOptions())
            );

            Map<String, Object> yamlData = yaml.load(inputStream);

            if (yamlData == null) {
                throw new IllegalArgumentException(
                        "YAML file is empty: " + resourcePath
                );
            }

            return yamlData;
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Unable to read YAML file: " + resourcePath,
                    exception
            );
        }
    }

    public static String prettyPrint(DocumentContext documentContext) {
        if (documentContext == null) {
            throw new IllegalArgumentException("DocumentContext must not be null");
        }

        return io.restassured.path.json.JsonPath
                .from(documentContext.jsonString())
                .prettify();
    }

    public static Map<String, Object> dataTableToMap(DataTable dataTable) {
        Map<String, Object> values = new HashMap<>();

        if (dataTable == null || dataTable.isEmpty()) {
            return values;
        }

        List<Map<String, String>> rows =
                dataTable.asMaps(String.class, String.class);

        if (rows.size() != 1) {
            throw new IllegalArgumentException(
                    "Expected a header row and exactly one data row"
            );
        }

        values.putAll(rows.get(0));
        return values;
    }

    public static void removeField(DocumentContext documentContext, Map<String, Object> testData) {
        if (testData.containsKey("removeField")) {
            String removeFieldVal = String.valueOf(testData.get("removeField"));
            String[] fields = removeFieldVal.split("\\|");
            for (String f : fields) {
                String fieldName = f.trim().replace(" ", "_");
                if (!fieldName.isEmpty()) {
                    documentContext.delete("$." + fieldName);
                }
            }
        }
    }

    public static Map<String, String> prepareHeaders(String authType, String validToken) {
        return switch (authType) {
            case "noauth" -> Map.of();
            case "invalid" -> Map.of("Authorization", "Bearer invalid_token");
            default -> Map.of("Authorization", "Bearer " + validToken);
        };
    }

    public static void validateMatchingFields(
            DocumentContext requestContext,
            DocumentContext responseContext) {

        Set<String> requestFields = requestContext.read("$.keys()");

        Set<String> skippedFields = Set.of(
                "assignee_id",
                "assignee_ids",
                "labels",
                "add_labels",
                "remove_labels",
                "milestone",
                "milestone_id",
                "severity",
                "created_at",
                "updated_at",
                "state_event",
                "discussion_to_resolve",
                "merge_request_to_resolve_discussions_of",
                "start_date",
                "due_date"
        );

        for (String field : requestFields) {
            if (skippedFields.contains(field)) {
                continue;
            }

            Object expected = requestContext.read("$." + field);
            Object actual = responseContext.read("$." + field);

            log.info("Validating field: {}, expected: {}, actual: {}", field, expected, actual);

            assertEquals(actual, expected, "Mismatch for field: " + field);
        }

        if (requestFields.contains("severity")) {
            String issueType = responseContext.read("$.issue_type");
            String expectedSeverity;

            if ("incident".equals(issueType)) {
                expectedSeverity = requestContext.read("$.severity")
                        .toString()
                        .toUpperCase();
            } else {
                expectedSeverity = "UNKNOWN";
            }

            log.info(
                    "Validating field: severity, expected: {}, actual: {}",
                    expectedSeverity,
                    responseContext.read("$.severity")
            );

            assertEquals(responseContext.read("$.severity"), expectedSeverity, "Severity mismatch");
        }

        if (requestFields.contains("state_event")) {
            String stateEvent = requestContext.read("$.state_event");

            String expectedState = switch (stateEvent) {
                case "close" -> "closed";
                case "reopen" -> "opened";
                default -> throw new IllegalArgumentException(
                        "Unsupported state event: " + stateEvent
                );
            };

            log.info(
                    "Validating field: state, expected: {}, actual: {}",
                    expectedState,
                    responseContext.read("$.state")
            );

            assertEquals(responseContext.read("$.state"), expectedState, "Issue state mismatch");
        }

        if (requestFields.contains("milestone_id")) {
            Object expectedMilestone = requestContext.read("$.milestone_id");
            Object actualMilestone = responseContext.read("$.milestone.id");

            log.info(
                    "Validating field: milestone_id, expected: {}, actual: {}",
                    expectedMilestone,
                    actualMilestone
            );

            assertEquals(actualMilestone.toString(), expectedMilestone.toString(), "Milestone mismatch");
        }
    }

    public static Object convertValue(String value) {
        String trimmedValue = value.trim();

        if ("null".equalsIgnoreCase(trimmedValue)) {
            return null;
        }

        if ("true".equalsIgnoreCase(trimmedValue)
                || "false".equalsIgnoreCase(trimmedValue)) {
            return Boolean.valueOf(trimmedValue);
        }

        if (trimmedValue.matches("-?(0|[1-9]\\d*)")) {
            try {
                return Integer.valueOf(trimmedValue);
            } catch (NumberFormatException ignored) {
                try {
                    return Long.valueOf(trimmedValue);
                } catch (NumberFormatException exception) {
                    return new BigInteger(trimmedValue);
                }
            }
        }

        if (trimmedValue.matches(
                "-?(0|[1-9]\\d*)(\\.\\d+)?([eE][+-]?\\d+)?")) {
            return new BigDecimal(trimmedValue);
        }

        if ((trimmedValue.startsWith("[") && trimmedValue.endsWith("]"))
                || (trimmedValue.startsWith("{") && trimmedValue.endsWith("}"))) {
            return JsonPath.parse(trimmedValue).json();
        }

        return trimmedValue;
    }
}
