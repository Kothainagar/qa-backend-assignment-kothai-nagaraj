package com.abnamro.assignment;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

@CucumberOptions(
        features = "classpath:featureFiles",
        glue = "com.abnamro.assignment.stepdefinitions",
        plugin = {
                "pretty",
                "html:target/cucumber-reports/test-results.html"
        },
        tags = "not @skip",
        monochrome = true
)

public class TestRunner extends AbstractTestNGCucumberTests {
}
