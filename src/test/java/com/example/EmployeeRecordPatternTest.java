package com.example;

import com.example.model.Department;
import com.example.model.Employee;
import com.example.service.EmployeeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Employee Record Patterns - Java 25")
class EmployeeRecordPatternTest {

    private final EmployeeService employeeService = new EmployeeService();

    @Test
    @DisplayName("should destructure nested employee and department records")
    void testNestedRecordDestructuring() {
        Employee employee = new Employee("EMP002", "Jane Smith", new Department("Sales", "300000"));

        if (employee instanceof Employee(String id, String name, Department(String deptName, String budget))) {
            assertThat(id).isEqualTo("EMP002");
            assertThat(name).isEqualTo("Jane Smith");
            assertThat(deptName).isEqualTo("Sales");
            assertThat(budget).isEqualTo("300000");
        } else {
            throw new AssertionError("Should destructure nested Employee and Department");
        }
    }

    @Test
    @DisplayName("should classify employees by department using pattern matching with guards")
    void testEmployeeClassificationByDepartment() {
        Employee eng = new Employee("EMP001", "John", new Department("Engineering", "500000"));
        Employee sales = new Employee("EMP002", "Jane", new Department("Sales", "300000"));
        Employee hr = new Employee("EMP003", "Bob", new Department("HR", "200000"));

        assertThat(employeeService.classifyEmployee(eng)).isEqualTo("Senior - Engineering");
        assertThat(employeeService.classifyEmployee(sales)).isEqualTo("Mid-level - Sales");
        assertThat(employeeService.classifyEmployee(hr)).isEqualTo("Junior - HR");
    }
}
