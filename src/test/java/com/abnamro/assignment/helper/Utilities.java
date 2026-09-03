package com.abnamro.assignment.helper;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import io.restassured.response.Response;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;

public class Utilities {

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

    public static String prettyPrint(Response response) {
        if (response == null) {
            throw new IllegalArgumentException("Response must not be null");
        }

        return response.asPrettyString();
    }


}
