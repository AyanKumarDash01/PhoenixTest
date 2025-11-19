package com.phoenix.hrm.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.phoenix.hrm.models.Employee;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Employee API Request POJO
 * 
 * Represents the request payload for Employee API operations
 * Used for POST and PUT requests to create/update employee records
 * 
 * Features:
 * - JSON serialization with snake_case naming
 * - Validation annotations
 * - Builder pattern support
 * - Conversion utilities
 * 
 * @author Phoenix HRM Test Team
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmployeeApiRequest {
    
    @JsonProperty("name")
    @NotBlank(message = "Employee name is required")
    @Size(max = 100, message = "Employee name must not exceed 100 characters")
    private String name;
    
    @JsonProperty("salary")
    private String salary;
    
    @JsonProperty("age")
    private String age;
    
    @JsonProperty("employee_id")
    private String employeeId;
    
    @JsonProperty("job_title")
    private String jobTitle;
    
    @JsonProperty("department")
    private String department;
    
    @JsonProperty("email")
    private String email;
    
    @JsonProperty("phone")
    private String phone;
    
    @JsonProperty("hire_date")
    private String hireDate;
    
    @JsonProperty("status")
    private String status;
    
    // Default constructor
    public EmployeeApiRequest() {}
    
    // Constructor with required fields
    public EmployeeApiRequest(String name, String salary, String age) {
        this.name = name;
        this.salary = salary;
        this.age = age;
    }
    
    // Full constructor
    public EmployeeApiRequest(String name, String salary, String age, String employeeId, 
                            String jobTitle, String department, String email, String phone, 
                            String hireDate, String status) {
        this.name = name;
        this.salary = salary;
        this.age = age;
        this.employeeId = employeeId;
        this.jobTitle = jobTitle;
        this.department = department;
        this.email = email;
        this.phone = phone;
        this.hireDate = hireDate;
        this.status = status;
    }
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getSalary() {
        return salary;
    }
    
    public void setSalary(String salary) {
        this.salary = salary;
    }
    
    public String getAge() {
        return age;
    }
    
    public void setAge(String age) {
        this.age = age;
    }
    
    public String getEmployeeId() {
        return employeeId;
    }
    
    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }
    
    public String getJobTitle() {
        return jobTitle;
    }
    
    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }
    
    public String getDepartment() {
        return department;
    }
    
    public void setDepartment(String department) {
        this.department = department;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getHireDate() {
        return hireDate;
    }
    
    public void setHireDate(String hireDate) {
        this.hireDate = hireDate;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    // Builder pattern
    public static class Builder {
        private String name;
        private String salary;
        private String age;
        private String employeeId;
        private String jobTitle;
        private String department;
        private String email;
        private String phone;
        private String hireDate;
        private String status;
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder salary(String salary) {
            this.salary = salary;
            return this;
        }
        
        public Builder age(String age) {
            this.age = age;
            return this;
        }
        
        public Builder employeeId(String employeeId) {
            this.employeeId = employeeId;
            return this;
        }
        
        public Builder jobTitle(String jobTitle) {
            this.jobTitle = jobTitle;
            return this;
        }
        
        public Builder department(String department) {
            this.department = department;
            return this;
        }
        
        public Builder email(String email) {
            this.email = email;
            return this;
        }
        
        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }
        
        public Builder hireDate(String hireDate) {
            this.hireDate = hireDate;
            return this;
        }
        
        public Builder status(String status) {
            this.status = status;
            return this;
        }
        
        public EmployeeApiRequest build() {
            return new EmployeeApiRequest(name, salary, age, employeeId, jobTitle, 
                                        department, email, phone, hireDate, status);
        }
    }
    
    // Conversion utilities
    
    /**
     * Creates EmployeeApiRequest from Employee model
     */
    public static EmployeeApiRequest fromEmployee(Employee employee) {
        if (employee == null) {
            return null;
        }
        
        return new Builder()
                .name(employee.getFirstName() + " " + employee.getLastName())
                .employeeId(employee.getEmployeeId())
                .email(employee.getEmail())
                .phone(employee.getPhone())
                .hireDate(employee.getHireDate())
                .status("active")
                .build();
    }
    
    /**
     * Creates a sample employee request for testing
     */
    public static EmployeeApiRequest createSampleEmployee(String name) {
        return new Builder()
                .name(name)
                .salary("50000")
                .age("30")
                .employeeId("EMP" + System.currentTimeMillis())
                .jobTitle("Software Engineer")
                .department("IT")
                .email(name.toLowerCase().replace(" ", ".") + "@company.com")
                .phone("+1-555-0" + String.format("%03d", (int)(Math.random() * 1000)))
                .hireDate("2024-01-15")
                .status("active")
                .build();
    }
    
    /**
     * Creates a minimal employee request with only required fields
     */
    public static EmployeeApiRequest createMinimalEmployee(String name, String salary, String age) {
        return new EmployeeApiRequest(name, salary, age);
    }
    
    @Override
    public String toString() {
        return "EmployeeApiRequest{" +
                "name='" + name + '\'' +
                ", salary='" + salary + '\'' +
                ", age='" + age + '\'' +
                ", employeeId='" + employeeId + '\'' +
                ", jobTitle='" + jobTitle + '\'' +
                ", department='" + department + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", hireDate='" + hireDate + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        EmployeeApiRequest that = (EmployeeApiRequest) o;
        
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (salary != null ? !salary.equals(that.salary) : that.salary != null) return false;
        if (age != null ? !age.equals(that.age) : that.age != null) return false;
        if (employeeId != null ? !employeeId.equals(that.employeeId) : that.employeeId != null) return false;
        return email != null ? email.equals(that.email) : that.email == null;
    }
    
    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (salary != null ? salary.hashCode() : 0);
        result = 31 * result + (age != null ? age.hashCode() : 0);
        result = 31 * result + (employeeId != null ? employeeId.hashCode() : 0);
        result = 31 * result + (email != null ? email.hashCode() : 0);
        return result;
    }
}