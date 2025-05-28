package com.example.timesheet.generator;

public class CostCenterCodeGenerator extends AbstractDocNoGenerator {
    @Override protected String dbFunction() {
        return "next_cost_center_code()";
    }
}

