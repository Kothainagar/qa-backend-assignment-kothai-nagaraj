package com.abnamro.assignment;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

@CucumberOptions(
        features = "classpath:featureFiles",
        glue = "com.abnamro.assignment.stepdefinitions",
        plugin = {
                "pretty",
                "html:target/cucumber-reports/create-issue-report.html"
        },
        monochrome = true
)

public class TestRunner extends AbstractTestNGCucumberTests {
}
