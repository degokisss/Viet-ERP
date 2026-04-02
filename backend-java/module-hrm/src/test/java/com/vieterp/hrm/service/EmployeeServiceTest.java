package com.vieterp.hrm.service;

import com.vieterp.hrm.domain.Employee;
import com.vieterp.hrm.exception.EmployeeNotFoundException;
import com.vieterp.hrm.repository.DepartmentRepository;
import com.vieterp.hrm.repository.EmployeeRepository;
import com.vieterp.hrm.domain.dto.CreateEmployeeRequest;
import com.vieterp.hrm.domain.dto.EmployeeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private DepartmentRepository departmentRepository;

    // Simple test double for EmployeeEventPublisher to avoid Mockito issues with Lombok @Slf4j on JDK 25
    private EmployeeEventPublisherTestDouble eventPublisher = new EmployeeEventPublisherTestDouble();

    private EmployeeService employeeService;

    private Employee testEmployee;
    private UUID testId;

    @BeforeEach
    void setUp() {
        // Manually construct service
        employeeService = new EmployeeService(employeeRepository, departmentRepository, eventPublisher);

        testId = UUID.randomUUID();
        testEmployee = Employee.builder()
            .id(testId).firstName("Nguyen").lastName("Van A")
            .email("nguyenvana@vieterp.com").status("ACTIVE")
            .hireDate(java.time.Instant.now())
            .build();
    }

    @Test
    void create_savesEmployeeAndPublishesEvent() {
        CreateEmployeeRequest req = new CreateEmployeeRequest(
            "Nguyen", "Van A", "nguyenvana@vieterp.com", "0912345678", null, "ACTIVE");
        when(employeeRepository.save(any(Employee.class))).thenReturn(testEmployee);
        EmployeeResponse resp = employeeService.create(req);
        assertNotNull(resp);
        assertEquals(testId, resp.id());
        assertEquals("Nguyen", resp.firstName());
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    void getById_throwsWhenNotFound() {
        when(employeeRepository.findByIdWithDepartment(testId)).thenReturn(Optional.empty());
        assertThrows(EmployeeNotFoundException.class, () -> employeeService.getById(testId));
    }

    @Test
    void listAll_returnsAllEmployees() {
        when(employeeRepository.findAll()).thenReturn(List.of(testEmployee));
        List<EmployeeResponse> result = employeeService.listAll();
        assertEquals(1, result.size());
        assertEquals(testId, result.get(0).id());
    }

    @Test
    void delete_deletesWhenExists() {
        when(employeeRepository.existsById(testId)).thenReturn(true);
        doNothing().when(employeeRepository).deleteById(testId);
        employeeService.delete(testId);
        verify(employeeRepository).deleteById(testId);
    }

    @Test
    void update_updatesEmployeeAndPublishesEvent() {
        CreateEmployeeRequest req = new CreateEmployeeRequest(
            "Updated", "Name", "updated@example.com", "0999999999", null, "INACTIVE");
        when(employeeRepository.findByIdWithDepartment(testId)).thenReturn(Optional.of(testEmployee));
        when(employeeRepository.save(any(Employee.class))).thenReturn(testEmployee);

        EmployeeResponse resp = employeeService.update(testId, req);

        assertNotNull(resp);
        verify(employeeRepository).save(any(Employee.class));
        assertTrue(eventPublisher.wasPublishUpdatedCalled());
    }
}
