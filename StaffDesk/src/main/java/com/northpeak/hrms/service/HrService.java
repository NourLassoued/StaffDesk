package com.northpeak.hrms.service;

import com.northpeak.hrms.db.EmployeeDao;
import com.northpeak.hrms.db.LeaveRequestDao;
import com.northpeak.hrms.model.Employee;
import com.northpeak.hrms.model.EmployeeStatus;
import com.northpeak.hrms.model.LeaveBalance;
import com.northpeak.hrms.model.LeaveRequest;
import com.northpeak.hrms.model.LeaveStatus;
import com.northpeak.hrms.model.LeaveType;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Business rules of the HRMS: employees, leave requests, balances and statistics.
 */
public class HrService {

    public static final int ANNUAL_ALLOWANCE = 25;
    public static final int SICK_ALLOWANCE = 10;
    public static final List<String> DEPARTMENTS =
            List.of("Engineering", "Finance", "Human Resources", "Marketing", "Operations", "Sales");
    public static final Map<String, String> MANAGERS = Map.of(
            "Engineering", "Ahmed Belhadj",
            "Finance", "Chloé Moreau",
            "Human Resources", "Emma Laurent",
            "Marketing", "Sofia Rossi",
            "Operations", "Thomas Dubois",
            "Sales", "Olivia Brennan");

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final EmployeeDao employeeDao;
    private final LeaveRequestDao leaveDao;

    public HrService(EmployeeDao employeeDao, LeaveRequestDao leaveDao) {
        this.employeeDao = employeeDao;
        this.leaveDao = leaveDao;
    }

    // ---------------------------------------------------------------- employees

    public List<Employee> employees() {
        return employeeDao.findAll();
    }

    public Optional<Employee> employee(String id) {
        return employeeDao.findById(id);
    }

    public Employee addEmployee(Employee e) {
        validate(e, null);
        e.setId(employeeDao.nextId());
        if (e.getLocation() == null || e.getLocation().isBlank()) {
            e.setLocation("Paris");
        }
        employeeDao.insert(e);
        return e;
    }

    public Employee updateEmployee(Employee e) {
        validate(e, e.getId());
        employeeDao.update(e);
        return e;
    }

    public void deleteEmployee(String id) {
        employeeDao.delete(id);
    }

    private void validate(Employee e, String currentId) {
        List<String> missing = new ArrayList<>();
        if (blank(e.getFirstName())) missing.add("first name");
        if (blank(e.getLastName())) missing.add("last name");
        if (blank(e.getJobTitle())) missing.add("job title");
        if (blank(e.getEmail())) missing.add("email");
        if (e.getHireDate() == null) missing.add("hire date");
        if (!missing.isEmpty()) {
            throw new ValidationException("Fill in every field before saving. Missing: " + String.join(", ", missing) + ".");
        }
        if (!EMAIL.matcher(e.getEmail().trim()).matches()) {
            throw new ValidationException("Enter a valid email address, for example name@northpeak.example.");
        }
        if (employeeDao.emailExists(e.getEmail().trim(), currentId)) {
            throw new ValidationException("Another employee already uses " + e.getEmail().trim() + ".");
        }
        if (e.getHireDate().isAfter(AppClock.today().plusMonths(6))) {
            throw new ValidationException("The hire date cannot be more than six months in the future.");
        }
        e.setFirstName(e.getFirstName().trim());
        e.setLastName(e.getLastName().trim());
        e.setJobTitle(e.getJobTitle().trim());
        e.setEmail(e.getEmail().trim());
    }

    // ---------------------------------------------------------------- leave

    public List<LeaveRequest> leaveRequests() {
        return leaveDao.findAll();
    }

    public List<LeaveRequest> leaveRequests(LeaveStatus status) {
        return leaveDao.findAll().stream()
                .filter(l -> status == null || l.getStatus() == status)
                .toList();
    }

    public List<LeaveRequest> leaveRequestsFor(String employeeId) {
        return leaveDao.findAll().stream().filter(l -> l.getEmployeeId().equals(employeeId)).toList();
    }

    public LeaveRequest submitLeave(String employeeId, LeaveType type, LocalDate start, LocalDate end, String reason) {
        if (employeeId == null) throw new ValidationException("Choose the employee who is requesting leave.");
        if (start == null || end == null) throw new ValidationException("Choose a start and an end date.");
        if (end.isBefore(start)) throw new ValidationException("The end date must be on or after the start date.");
        int days = workingDays(start, end);
        if (days == 0) throw new ValidationException("The selected dates fall on a weekend. Choose at least one working day.");
        if (type == LeaveType.ANNUAL) {
            LeaveBalance b = balance(employeeId);
            if (days > b.remainingAfterPending()) {
                throw new ValidationException("This request needs " + days + " days but only "
                        + b.remainingAfterPending() + " annual days are available.");
            }
        }
        if (blank(reason)) throw new ValidationException("Add a short reason so HR can review the request.");

        LeaveRequest l = new LeaveRequest(leaveDao.nextId(), employeeId, type, start, end, reason.trim(),
                LeaveStatus.PENDING, null);
        leaveDao.insert(l);
        return l;
    }

    public void approve(String leaveId) {
        LeaveRequest l = findLeave(leaveId);
        if (l.getStatus() != LeaveStatus.PENDING) {
            throw new ValidationException("Only pending requests can be approved.");
        }
        leaveDao.updateStatus(leaveId, LeaveStatus.APPROVED, null);
        LocalDate today = AppClock.today();
        employeeDao.findById(l.getEmployeeId()).ifPresent(e -> {
            boolean inProgress = !l.getStartDate().isAfter(today) && !l.getEndDate().isBefore(today);
            if (inProgress && e.getStatus() == EmployeeStatus.ACTIVE) {
                e.setStatus(EmployeeStatus.ON_LEAVE);
                employeeDao.update(e);
            }
        });
    }

    public void reject(String leaveId, String reason) {
        LeaveRequest l = findLeave(leaveId);
        if (l.getStatus() != LeaveStatus.PENDING) {
            throw new ValidationException("Only pending requests can be rejected.");
        }
        leaveDao.updateStatus(leaveId, LeaveStatus.REJECTED, blank(reason) ? "Rejected by S. Mitchell" : reason.trim());
    }

    private LeaveRequest findLeave(String id) {
        return leaveDao.findAll().stream().filter(l -> l.getId().equals(id)).findFirst()
                .orElseThrow(() -> new ValidationException("Leave request " + id + " no longer exists."));
    }

    public LeaveBalance balance(String employeeId) {
        int year = AppClock.today().getYear();
        int before = employeeDao.findById(employeeId).map(Employee::getAnnualDaysUsedBefore).orElse(0);
        int used = before, pending = 0, sick = 0;
        for (LeaveRequest l : leaveRequestsFor(employeeId)) {
            if (l.getStartDate().getYear() != year) continue;
            int d = workingDays(l.getStartDate(), l.getEndDate());
            if (l.getType() == LeaveType.ANNUAL && l.getStatus() == LeaveStatus.APPROVED) used += d;
            if (l.getType() == LeaveType.ANNUAL && l.getStatus() == LeaveStatus.PENDING) pending += d;
            if (l.getType() == LeaveType.SICK && l.getStatus() == LeaveStatus.APPROVED) sick += d;
        }
        return new LeaveBalance(ANNUAL_ALLOWANCE, used, pending, ANNUAL_ALLOWANCE - used, sick);
    }

    /** Counts Monday-Friday days between two dates, both included. */
    public static int workingDays(LocalDate start, LocalDate end) {
        int n = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            DayOfWeek w = d.getDayOfWeek();
            if (w != DayOfWeek.SATURDAY && w != DayOfWeek.SUNDAY) n++;
        }
        return n;
    }

    // ---------------------------------------------------------------- statistics

    public Map<LeaveStatus, Long> leaveCounts() {
        Map<LeaveStatus, Long> m = new LinkedHashMap<>();
        for (LeaveStatus s : LeaveStatus.values()) m.put(s, 0L);
        leaveDao.findAll().forEach(l -> m.merge(l.getStatus(), 1L, Long::sum));
        return m;
    }

    public Map<String, Long> headcountByDepartment() {
        Map<String, Long> m = new LinkedHashMap<>();
        DEPARTMENTS.forEach(d -> m.put(d, 0L));
        employees().forEach(e -> m.merge(e.getDepartment(), 1L, Long::sum));
        return m;
    }

    public Map<EmployeeStatus, Long> headcountByStatus() {
        Map<EmployeeStatus, Long> m = new LinkedHashMap<>();
        for (EmployeeStatus s : EmployeeStatus.values()) m.put(s, 0L);
        employees().forEach(e -> m.merge(e.getStatus(), 1L, Long::sum));
        return m;
    }

    /** Approved leave days per ISO week for the quarter containing "today" (13 weeks from the first Monday). */
    public Map<String, Integer> approvedDaysPerWeek() {
        LocalDate today = AppClock.today();
        LocalDate quarterStart = LocalDate.of(today.getYear(), ((today.getMonthValue() - 1) / 3) * 3 + 1, 1);
        LocalDate monday = quarterStart;
        while (monday.getDayOfWeek() != DayOfWeek.MONDAY) monday = monday.plusDays(1);
        int firstWeek = monday.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int[] days = new int[13];
        for (LeaveRequest l : leaveRequests(LeaveStatus.APPROVED)) {
            for (LocalDate d = l.getStartDate(); !d.isAfter(l.getEndDate()); d = d.plusDays(1)) {
                DayOfWeek w = d.getDayOfWeek();
                if (w == DayOfWeek.SATURDAY || w == DayOfWeek.SUNDAY) continue;
                long idx = java.time.temporal.ChronoUnit.DAYS.between(monday, d) / 7;
                if (!d.isBefore(monday) && idx < 13) days[(int) idx]++;
            }
        }
        Map<String, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i < 13; i++) m.put("W" + (firstWeek + i), days[i]);
        return m;
    }

    public Map<LeaveType, Integer> approvedDaysByType() {
        Map<LeaveType, Integer> m = new LinkedHashMap<>();
        for (LeaveType t : LeaveType.values()) m.put(t, 0);
        leaveRequests(LeaveStatus.APPROVED)
                .forEach(l -> m.merge(l.getType(), workingDays(l.getStartDate(), l.getEndDate()), Integer::sum));
        return m;
    }

    public Map<Integer, Long> hiresByYear() {
        Map<Integer, Long> m = new LinkedHashMap<>();
        List<Employee> all = employees();
        int first = all.stream().map(e -> e.getHireDate().getYear()).min(Comparator.naturalOrder()).orElse(AppClock.today().getYear());
        for (int y = first; y <= AppClock.today().getYear(); y++) m.put(y, 0L);
        all.forEach(e -> m.merge(e.getHireDate().getYear(), 1L, Long::sum));
        return m;
    }

    public double averageTenureYears() {
        List<Employee> all = employees();
        return all.stream().mapToDouble(e -> tenureYears(e.getHireDate())).average().orElse(0);
    }

    public static double tenureYears(LocalDate hire) {
        return java.time.temporal.ChronoUnit.DAYS.between(hire, AppClock.today()) / 365.25;
    }

    public static String tenureLabel(LocalDate hire) {
        java.time.Period p = java.time.Period.between(hire, AppClock.today());
        if (p.getYears() == 0) {
            int m = Math.max(1, p.getMonths());
            return m + " month" + (m > 1 ? "s" : "");
        }
        return p.getYears() + " yr" + (p.getYears() > 1 ? "s" : "") + (p.getMonths() > 0 ? " " + p.getMonths() + " mo" : "");
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
