package com.northpeak.hrms.service;

import com.northpeak.hrms.db.Database;
import com.northpeak.hrms.db.EmployeeDao;
import com.northpeak.hrms.db.LeaveRequestDao;
import com.northpeak.hrms.model.Employee;
import com.northpeak.hrms.model.EmployeeStatus;
import com.northpeak.hrms.model.LeaveBalance;
import com.northpeak.hrms.model.LeaveRequest;
import com.northpeak.hrms.model.LeaveStatus;
import com.northpeak.hrms.model.LeaveType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HrServiceTest {

    private Database db;
    private HrService service;

    @BeforeEach
    void setUp() {
        db = new Database("jdbc:h2:mem:test" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        db.initialize();
        service = new HrService(new EmployeeDao(db), new LeaveRequestDao(db));
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void seedsDemoData() {
        assertEquals(24, service.employees().size());
        assertEquals(6, service.leaveCounts().get(LeaveStatus.PENDING));
        assertEquals(16, service.leaveCounts().get(LeaveStatus.APPROVED));
        assertEquals(3, service.leaveCounts().get(LeaveStatus.REJECTED));
    }

    @Test
    void countsWorkingDaysOnly() {
        // Monday 18 Sep 2023 -> Sunday 24 Sep 2023 = 5 working days
        assertEquals(5, HrService.workingDays(LocalDate.of(2023, 9, 18), LocalDate.of(2023, 9, 24)));
        assertEquals(0, HrService.workingDays(LocalDate.of(2023, 9, 16), LocalDate.of(2023, 9, 17)));
    }

    @Test
    void approvingUpdatesBalance() {
        LeaveBalance before = service.balance("EMP-1001");
        assertEquals(9, before.used());
        assertEquals(5, before.pending());
        assertEquals(16, before.remaining());

        service.approve("LR-0423");

        LeaveBalance after = service.balance("EMP-1001");
        assertEquals(14, after.used());
        assertEquals(0, after.pending());
        assertEquals(11, after.remaining());
        assertEquals(5, service.leaveCounts().get(LeaveStatus.PENDING));
        assertEquals(17, service.leaveCounts().get(LeaveStatus.APPROVED));
    }

    @Test
    void rejectStoresReason() {
        service.reject("LR-0422", "Quarter-end closing");
        LeaveRequest l = service.leaveRequests().stream().filter(r -> r.getId().equals("LR-0422")).findFirst().orElseThrow();
        assertEquals(LeaveStatus.REJECTED, l.getStatus());
        assertEquals("Quarter-end closing", l.getReviewNote());
    }

    @Test
    void refusesAnnualLeaveAboveBalance() {
        ValidationException ex = assertThrows(ValidationException.class, () ->
                service.submitLeave("EMP-1001", LeaveType.ANNUAL, LocalDate.of(2023, 10, 2), LocalDate.of(2023, 10, 31), "Long trip"));
        assertTrue(ex.getMessage().contains("annual days are available"));
    }

    @Test
    void submitsValidRequest() {
        LeaveRequest l = service.submitLeave("EMP-1002", LeaveType.PERSONAL, LocalDate.of(2023, 9, 26),
                LocalDate.of(2023, 9, 26), "Doctor appointment");
        assertEquals("LR-0424", l.getId());
        assertEquals(LeaveStatus.PENDING, l.getStatus());
    }

    @Test
    void addEmployeeValidatesEmail() {
        Employee e = new Employee(null, "Test", "User", "Finance", "Analyst", "not-an-email", "", "Paris",
                LocalDate.of(2023, 9, 1), EmployeeStatus.PROBATION, 0);
        assertThrows(ValidationException.class, () -> service.addEmployee(e));
        e.setEmail("test.user@northpeak.example");
        Employee saved = service.addEmployee(e);
        assertEquals("EMP-1025", saved.getId());
        assertEquals(25, service.employees().size());
    }

    @Test
    void deletingEmployeeRemovesTheirLeave() {
        service.deleteEmployee("EMP-1001");
        assertTrue(service.leaveRequestsFor("EMP-1001").isEmpty());
        assertEquals(23, service.employees().size());
    }
}
