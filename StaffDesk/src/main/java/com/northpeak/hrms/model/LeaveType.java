package com.northpeak.hrms.model;

public enum LeaveType {
    ANNUAL("Annual"),
    SICK("Sick"),
    PERSONAL("Personal"),
    TRAINING("Training"),
    PARENTAL("Parental");

    private final String label;

    LeaveType(String label) {
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
