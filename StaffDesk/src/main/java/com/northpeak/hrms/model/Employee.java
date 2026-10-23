package com.northpeak.hrms.model;

import java.time.LocalDate;
import java.util.Objects;

public class Employee {

    private String id;
    private String firstName;
    private String lastName;
    private String department;
    private String jobTitle;
    private String email;
    private String phone;
    private String location;
    private LocalDate hireDate;
    private EmployeeStatus status;
    /** Annual leave days already taken this year before the requests stored in the system. */
    private int annualDaysUsedBefore;

    public Employee() {
    }

    public Employee(String id, String firstName, String lastName, String department, String jobTitle,
                    String email, String phone, String location, LocalDate hireDate,
                    EmployeeStatus status, int annualDaysUsedBefore) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.department = department;
        this.jobTitle = jobTitle;
        this.email = email;
        this.phone = phone;
        this.location = location;
        this.hireDate = hireDate;
        this.status = status;
        this.annualDaysUsedBefore = annualDaysUsedBefore;
    }

    public Employee copy() {
        return new Employee(id, firstName, lastName, department, jobTitle, email, phone, location,
                hireDate, status, annualDaysUsedBefore);
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getInitials() {
        String last = lastName.startsWith("Ben ") ? lastName.substring(4) : lastName;
        return (firstName.substring(0, 1) + last.substring(0, 1)).toUpperCase();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }
    public EmployeeStatus getStatus() { return status; }
    public void setStatus(EmployeeStatus status) { this.status = status; }
    public int getAnnualDaysUsedBefore() { return annualDaysUsedBefore; }
    public void setAnnualDaysUsedBefore(int annualDaysUsedBefore) { this.annualDaysUsedBefore = annualDaysUsedBefore; }

    @Override
    public boolean equals(Object o) {
        return o instanceof Employee other && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return getFullName() + " · " + id;
    }
}
