package com.northpeak.hrms.model;

public enum EmployeeStatus {
    ACTIVE("Active"),
    PROBATION("Probation"),
    ON_LEAVE("On Leave"),
    INACTIVE("Inactive");

    private final String label;

    EmployeeStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
