package com.example.service;

import com.example.model.Employee;

public class EmployeeService {

    public String classifyEmployee(Employee employee) {
        return switch (employee) {
            case Employee(String id, String name, var dept) when dept.name().equals("Engineering") -> "Senior - Engineering";
            case Employee(String id, String name, var dept) when dept.name().equals("Sales") -> "Mid-level - Sales";
            case Employee(String id, String name, var dept) when dept.name().equals("HR") -> "Junior - HR";
            default -> "Unknown";
        };
    }
}
