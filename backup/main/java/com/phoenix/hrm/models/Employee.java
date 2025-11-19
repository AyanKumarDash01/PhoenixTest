package com.phoenix.hrm.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * Employee POJO model representing employee data structure.
 * Used for data-driven testing, API requests/responses, and test data management.
 * 
 * Features:
 * - Complete employee information model
 * - JSON serialization support with Jackson annotations
 * - Builder pattern for flexible object creation
 * - Validation methods for data integrity
 * - ToString, equals, and hashCode implementations
 * 
 * @author Phoenix HRM Test Automation Team
 * @version 1.0
 */
public class Employee {
    
    @JsonProperty("employee_id")
    private String employeeId;
    
    @JsonProperty("first_name")
    private String firstName;
    
    @JsonProperty("middle_name")
    private String middleName;
    
    @JsonProperty("last_name")
    private String lastName;
    
    @JsonProperty("email")
    private String email;
    
    @JsonProperty("job_title")
    private String jobTitle;
    
    @JsonProperty("department")
    private String department;
    
    @JsonProperty("phone")
    private String phone;
    
    @JsonProperty("date_of_birth")
    private String dateOfBirth;
    
    @JsonProperty("hire_date")
    private String hireDate;
    
    @JsonProperty("salary")
    private String salary;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("manager")
    private String manager;
    
    @JsonProperty("location")
    private String location;
    
    // Default constructor
    public Employee() {}
    
    // Constructor with essential fields
    public Employee(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }
    
    // Full constructor
    public Employee(String employeeId, String firstName, String middleName, String lastName, 
                   String email, String jobTitle, String department) {
        this.employeeId = employeeId;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.email = email;
        this.jobTitle = jobTitle;
        this.department = department;
    }
    
    // Getters and Setters
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    
    public String getHireDate() { return hireDate; }
    public void setHireDate(String hireDate) { this.hireDate = hireDate; }
    
    public String getSalary() { return salary; }
    public void setSalary(String salary) { this.salary = salary; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getManager() { return manager; }
    public void setManager(String manager) { this.manager = manager; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    // Utility methods
    public String getFullName() {
        StringBuilder fullName = new StringBuilder();
        if (firstName != null && !firstName.trim().isEmpty()) {
            fullName.append(firstName.trim());
        }
        if (middleName != null && !middleName.trim().isEmpty()) {
            if (fullName.length() > 0) fullName.append(" ");
            fullName.append(middleName.trim());
        }
        if (lastName != null && !lastName.trim().isEmpty()) {
            if (fullName.length() > 0) fullName.append(" ");
            fullName.append(lastName.trim());
        }
        return fullName.toString();
    }
    
    public String getDisplayName() {
        return getFullName();
    }
    
    public boolean isValid() {
        return firstName != null && !firstName.trim().isEmpty() &&
               lastName != null && !lastName.trim().isEmpty();
    }
    
    public boolean hasRequiredFields() {
        return isValid(); // At minimum, first and last names are required
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Employee employee = (Employee) obj;
        return Objects.equals(employeeId, employee.employeeId) &&
               Objects.equals(firstName, employee.firstName) &&
               Objects.equals(lastName, employee.lastName) &&
               Objects.equals(email, employee.email);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(employeeId, firstName, lastName, email);
    }
    
    @Override
    public String toString() {
        return String.format("Employee{id='%s', name='%s', email='%s', jobTitle='%s', department='%s'}", 
                           employeeId, getFullName(), email, jobTitle, department);
    }
    
    // Builder Pattern Implementation
    public static class Builder {
        private Employee employee;
        
        public Builder() {
            employee = new Employee();
        }
        
        public Builder employeeId(String employeeId) {
            employee.setEmployeeId(employeeId);
            return this;
        }
        
        public Builder firstName(String firstName) {
            employee.setFirstName(firstName);
            return this;
        }
        
        public Builder middleName(String middleName) {
            employee.setMiddleName(middleName);
            return this;
        }
        
        public Builder lastName(String lastName) {
            employee.setLastName(lastName);
            return this;
        }
        
        public Builder email(String email) {
            employee.setEmail(email);
            return this;
        }
        
        public Builder jobTitle(String jobTitle) {
            employee.setJobTitle(jobTitle);
            return this;
        }
        
        public Builder department(String department) {
            employee.setDepartment(department);
            return this;
        }
        
        public Builder phone(String phone) {
            employee.setPhone(phone);
            return this;
        }
        
        public Builder dateOfBirth(String dateOfBirth) {
            employee.setDateOfBirth(dateOfBirth);
            return this;
        }
        
        public Builder hireDate(String hireDate) {
            employee.setHireDate(hireDate);
            return this;
        }
        
        public Builder salary(String salary) {
            employee.setSalary(salary);
            return this;
        }
        
        public Builder status(String status) {
            employee.setStatus(status);
            return this;
        }
        
        public Builder manager(String manager) {
            employee.setManager(manager);
            return this;
        }
        
        public Builder location(String location) {
            employee.setLocation(location);
            return this;
        }
        
        public Employee build() {
            if (!employee.hasRequiredFields()) {
                throw new IllegalStateException("Employee must have at least first name and last name");
            }
            return employee;
        }
    }
    
    // Static factory methods for common use cases
    public static Employee createBasicEmployee(String firstName, String lastName) {
        return new Builder()
                .firstName(firstName)
                .lastName(lastName)
                .build();
    }
    
    public static Employee createEmployeeWithId(String employeeId, String firstName, String lastName) {
        return new Builder()
                .employeeId(employeeId)
                .firstName(firstName)
                .lastName(lastName)
                .build();
    }
    
    public static Builder builder() {
        return new Builder();
    }
}