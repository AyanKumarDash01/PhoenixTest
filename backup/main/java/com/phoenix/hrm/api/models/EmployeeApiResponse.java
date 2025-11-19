package com.phoenix.hrm.api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Employee API Response POJO
 * 
 * Represents the response payload from Employee API operations
 * Handles both single employee and list responses with standard API structure
 * 
 * Features:
 * - JSON deserialization with snake_case naming
 * - Flexible structure for different response types
 * - Error handling support
 * - Validation utilities
 * 
 * @author Phoenix HRM Test Team
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmployeeApiResponse {
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("data")
    private EmployeeData data;
    
    @JsonProperty("employees")
    private List<EmployeeData> employees;
    
    @JsonProperty("error")
    private String error;
    
    @JsonProperty("code")
    private Integer code;
    
    @JsonProperty("timestamp")
    private String timestamp;
    
    @JsonProperty("path")
    private String path;
    
    // Default constructor
    public EmployeeApiResponse() {}
    
    // Constructor for error responses
    public EmployeeApiResponse(String status, String message, String error, Integer code) {
        this.status = status;
        this.message = message;
        this.error = error;
        this.code = code;
    }
    
    // Getters and Setters
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public EmployeeData getData() {
        return data;
    }
    
    public void setData(EmployeeData data) {
        this.data = data;
    }
    
    public List<EmployeeData> getEmployees() {
        return employees;
    }
    
    public void setEmployees(List<EmployeeData> employees) {
        this.employees = employees;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public Integer getCode() {
        return code;
    }
    
    public void setCode(Integer code) {
        this.code = code;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    // Utility methods
    
    /**
     * Checks if the response indicates success
     */
    public boolean isSuccess() {
        return "success".equalsIgnoreCase(status) || 
               (code != null && code >= 200 && code < 300);
    }
    
    /**
     * Checks if the response indicates an error
     */
    public boolean isError() {
        return "error".equalsIgnoreCase(status) || 
               (code != null && code >= 400) ||
               error != null;
    }
    
    /**
     * Gets the first employee from the response data
     */
    public EmployeeData getFirstEmployee() {
        if (data != null) {
            return data;
        }
        if (employees != null && !employees.isEmpty()) {
            return employees.get(0);
        }
        return null;
    }
    
    /**
     * Gets the total count of employees in the response
     */
    public int getEmployeeCount() {
        if (employees != null) {
            return employees.size();
        }
        return data != null ? 1 : 0;
    }
    
    /**
     * Checks if the response contains employee data
     */
    public boolean hasEmployeeData() {
        return data != null || (employees != null && !employees.isEmpty());
    }
    
    @Override
    public String toString() {
        return "EmployeeApiResponse{" +
                "status='" + status + '\'' +
                ", message='" + message + '\'' +
                ", data=" + data +
                ", employees=" + employees +
                ", error='" + error + '\'' +
                ", code=" + code +
                ", timestamp='" + timestamp + '\'' +
                ", path='" + path + '\'' +
                '}';
    }
    
    /**
     * Nested class representing individual employee data in the response
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EmployeeData {
        
        @JsonProperty("id")
        private String id;
        
        @JsonProperty("employee_name")
        private String employeeName;
        
        @JsonProperty("employee_salary")
        private String employeeSalary;
        
        @JsonProperty("employee_age")
        private String employeeAge;
        
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
        
        @JsonProperty("created_at")
        private String createdAt;
        
        @JsonProperty("updated_at")
        private String updatedAt;
        
        @JsonProperty("profile_image")
        private String profileImage;
        
        // Default constructor
        public EmployeeData() {}
        
        // Getters and Setters
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getEmployeeName() {
            return employeeName;
        }
        
        public void setEmployeeName(String employeeName) {
            this.employeeName = employeeName;
        }
        
        public String getEmployeeSalary() {
            return employeeSalary;
        }
        
        public void setEmployeeSalary(String employeeSalary) {
            this.employeeSalary = employeeSalary;
        }
        
        public String getEmployeeAge() {
            return employeeAge;
        }
        
        public void setEmployeeAge(String employeeAge) {
            this.employeeAge = employeeAge;
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
        
        public String getCreatedAt() {
            return createdAt;
        }
        
        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }
        
        public String getUpdatedAt() {
            return updatedAt;
        }
        
        public void setUpdatedAt(String updatedAt) {
            this.updatedAt = updatedAt;
        }
        
        public String getProfileImage() {
            return profileImage;
        }
        
        public void setProfileImage(String profileImage) {
            this.profileImage = profileImage;
        }
        
        // Utility methods
        
        /**
         * Validates that employee data has required fields
         */
        public boolean hasRequiredFields() {
            return employeeName != null && !employeeName.trim().isEmpty();
        }
        
        /**
         * Checks if employee has complete information
         */
        public boolean hasCompleteInfo() {
            return hasRequiredFields() && 
                   employeeSalary != null && 
                   employeeAge != null;
        }
        
        /**
         * Gets display name for the employee
         */
        public String getDisplayName() {
            return employeeName != null ? employeeName : "Unknown Employee";
        }
        
        /**
         * Formats salary with currency symbol
         */
        public String getFormattedSalary() {
            if (employeeSalary == null || employeeSalary.isEmpty()) {
                return "Not specified";
            }
            try {
                double salary = Double.parseDouble(employeeSalary);
                return String.format("$%.2f", salary);
            } catch (NumberFormatException e) {
                return employeeSalary;
            }
        }
        
        @Override
        public String toString() {
            return "EmployeeData{" +
                    "id='" + id + '\'' +
                    ", employeeName='" + employeeName + '\'' +
                    ", employeeSalary='" + employeeSalary + '\'' +
                    ", employeeAge='" + employeeAge + '\'' +
                    ", employeeId='" + employeeId + '\'' +
                    ", jobTitle='" + jobTitle + '\'' +
                    ", department='" + department + '\'' +
                    ", email='" + email + '\'' +
                    ", phone='" + phone + '\'' +
                    ", status='" + status + '\'' +
                    '}';
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            
            EmployeeData that = (EmployeeData) o;
            
            if (id != null ? !id.equals(that.id) : that.id != null) return false;
            if (employeeName != null ? !employeeName.equals(that.employeeName) : that.employeeName != null) return false;
            return employeeId != null ? employeeId.equals(that.employeeId) : that.employeeId == null;
        }
        
        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (employeeName != null ? employeeName.hashCode() : 0);
            result = 31 * result + (employeeId != null ? employeeId.hashCode() : 0);
            return result;
        }
    }
}