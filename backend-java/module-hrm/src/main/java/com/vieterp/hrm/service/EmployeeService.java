package com.vieterp.hrm.service;

import com.vieterp.hrm.domain.Department;
import com.vieterp.hrm.domain.Employee;
import com.vieterp.hrm.event.EmployeeEventPublisher;
import com.vieterp.hrm.exception.EmployeeNotFoundException;
import com.vieterp.hrm.repository.DepartmentRepository;
import com.vieterp.hrm.repository.EmployeeRepository;
import com.vieterp.hrm.service.dto.CreateEmployeeRequest;
import com.vieterp.hrm.service.dto.DepartmentSummary;
import com.vieterp.hrm.service.dto.EmployeeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeEventPublisher eventPublisher;

    @Transactional
    public EmployeeResponse create(CreateEmployeeRequest req) {
        Employee employee = Employee.builder()
            .firstName(req.firstName())
            .lastName(req.lastName())
            .email(req.email())
            .phone(req.phone())
            .status(req.status() != null ? req.status() : "ACTIVE")
            .hireDate(java.time.Instant.now())
            .build();

        if (req.departmentId() != null) {
            Department dept = departmentRepository.findById(req.departmentId())
                .orElseThrow(() -> new IllegalArgumentException("Department not found: " + req.departmentId()));
            employee.setDepartment(dept);
        }

        Employee saved = employeeRepository.save(employee);
        eventPublisher.publishCreated(saved);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getById(UUID id) {
        Employee emp = employeeRepository.findByIdWithDepartment(id)
            .orElseThrow(() -> new EmployeeNotFoundException(id));
        return toResponse(emp);
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> listAll() {
        return employeeRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public EmployeeResponse update(UUID id, CreateEmployeeRequest req) {
        Employee emp = employeeRepository.findByIdWithDepartment(id)
            .orElseThrow(() -> new EmployeeNotFoundException(id));
        emp.setFirstName(req.firstName());
        emp.setLastName(req.lastName());
        emp.setEmail(req.email());
        emp.setPhone(req.phone());
        if (req.status() != null) emp.setStatus(req.status());
        if (req.departmentId() != null) {
            Department dept = departmentRepository.findById(req.departmentId())
                .orElseThrow(() -> new IllegalArgumentException("Department not found: " + req.departmentId()));
            emp.setDepartment(dept);
        }
        Employee saved = employeeRepository.save(emp);
        eventPublisher.publishUpdated(saved);
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        if (!employeeRepository.existsById(id)) {
            throw new EmployeeNotFoundException(id);
        }
        employeeRepository.deleteById(id);
        eventPublisher.publishDeleted(id);
    }

    private EmployeeResponse toResponse(Employee e) {
        DepartmentSummary deptSummary = null;
        if (e.getDepartment() != null) {
            deptSummary = new DepartmentSummary(
                e.getDepartment().getId(),
                e.getDepartment().getName(),
                e.getDepartment().getCode());
        }
        return new EmployeeResponse(
            e.getId(), e.getFirstName(), e.getLastName(), e.getEmail(),
            e.getPhone(), deptSummary, e.getHireDate(), e.getStatus(),
            e.getCreatedAt(), e.getUpdatedAt());
    }
}
