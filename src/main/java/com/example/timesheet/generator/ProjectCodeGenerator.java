package com.example.timesheet.generator;

public class ProjectCodeGenerator extends AbstractDocNoGenerator {
    @Override protected String dbFunction() { return "next_project_code()"; }
}
