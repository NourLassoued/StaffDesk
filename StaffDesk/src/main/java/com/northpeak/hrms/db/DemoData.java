package com.northpeak.hrms.db;

import com.northpeak.hrms.model.Employee;
import com.northpeak.hrms.model.EmployeeStatus;
import com.northpeak.hrms.model.LeaveRequest;
import com.northpeak.hrms.model.LeaveStatus;
import com.northpeak.hrms.model.LeaveType;

import java.text.Normalizer;
import java.time.LocalDate;

/**
 * Fictional demo dataset (24 employees, 25 leave requests, July - September 2023).
 */
final class DemoData {

    private DemoData() {
    }

    private static final Object[][] EMPLOYEES = {
            {"EMP-1001", "Amira", "Ben Salem", "Engineering", "Senior Software Engineer", "2021-03-15", "ACTIVE", 4, "Tunis"},
            {"EMP-1002", "Lucas", "Martin", "Engineering", "DevOps Engineer", "2022-06-01", "ACTIVE", 6, "Paris"},
            {"EMP-1003", "Sofia", "Rossi", "Marketing", "Marketing Manager", "2019-09-10", "ACTIVE", 3, "Paris"},
            {"EMP-1004", "Daniel", "Okafor", "Finance", "Financial Analyst", "2020-01-20", "ACTIVE", 8, "Lyon"},
            {"EMP-1005", "Emma", "Laurent", "Human Resources", "HR Business Partner", "2018-04-02", "ACTIVE", 7, "Paris"},
            {"EMP-1006", "Youssef", "Haddad", "Sales", "Account Executive", "2023-08-14", "PROBATION", 0, "Tunis"},
            {"EMP-1007", "Clara", "Weber", "Operations", "Operations Coordinator", "2021-11-08", "ON_LEAVE", 5, "Lyon"},
            {"EMP-1008", "Mehdi", "Trabelsi", "Engineering", "Frontend Developer", "2022-08-22", "ACTIVE", 10, "Tunis"},
            {"EMP-1009", "Olivia", "Brennan", "Sales", "Sales Director", "2017-05-29", "ACTIVE", 4, "Paris"},
            {"EMP-1010", "Karim", "Mansour", "Finance", "Accountant", "2022-10-03", "ACTIVE", 9, "Tunis"},
            {"EMP-1011", "Hannah", "Schulz", "Engineering", "QA Engineer", "2020-07-14", "ACTIVE", 6, "Remote"},
            {"EMP-1012", "Inès", "Ferreira", "Marketing", "Content Strategist", "2023-07-10", "PROBATION", 2, "Paris"},
            {"EMP-1013", "Thomas", "Dubois", "Operations", "Logistics Manager", "2016-03-01", "ACTIVE", 8, "Lyon"},
            {"EMP-1014", "Leila", "Chaabane", "Human Resources", "Talent Acquisition Specialist", "2022-03-28", "ACTIVE", 11, "Tunis"},
            {"EMP-1015", "Marco", "Bianchi", "Engineering", "Backend Developer", "2021-05-17", "ON_LEAVE", 6, "Remote"},
            {"EMP-1016", "Nadia", "Kowalski", "Sales", "Sales Representative", "2023-04-11", "ACTIVE", 12, "Paris"},
            {"EMP-1017", "Samuel", "Adeyemi", "Engineering", "Data Engineer", "2022-09-05", "ACTIVE", 7, "Lyon"},
            {"EMP-1018", "Chloé", "Moreau", "Finance", "Finance Manager", "2015-11-23", "ACTIVE", 10, "Paris"},
            {"EMP-1019", "Omar", "Jaziri", "Operations", "Facilities Supervisor", "2019-02-18", "INACTIVE", 0, "Tunis"},
            {"EMP-1020", "Julia", "Novak", "Marketing", "Graphic Designer", "2020-10-26", "ACTIVE", 9, "Remote"},
            {"EMP-1021", "Ahmed", "Belhadj", "Engineering", "Engineering Manager", "2017-08-07", "ACTIVE", 8, "Tunis"},
            {"EMP-1022", "Grace", "Liu", "Sales", "Customer Success Manager", "2021-01-11", "ACTIVE", 7, "Paris"},
            {"EMP-1023", "Paul", "Lefèvre", "Operations", "Procurement Officer", "2023-09-04", "PROBATION", 1, "Lyon"},
            {"EMP-1024", "Rania", "Gharbi", "Human Resources", "Payroll Specialist", "2019-06-24", "ACTIVE", 5, "Tunis"},
    };

    private static final String[][] LEAVES = {
            {"LR-0423", "EMP-1001", "ANNUAL", "2023-09-18", "2023-09-22", "Family trip to Tozeur", "PENDING", null},
            {"LR-0422", "EMP-1010", "ANNUAL", "2023-09-25", "2023-09-29", "Visiting family in Sfax", "PENDING", null},
            {"LR-0421", "EMP-1022", "PERSONAL", "2023-09-19", "2023-09-19", "Moving to a new apartment", "PENDING", null},
            {"LR-0420", "EMP-1011", "ANNUAL", "2023-09-20", "2023-09-29", "End-of-season holiday", "PENDING", null},
            {"LR-0419", "EMP-1006", "SICK", "2023-09-18", "2023-09-19", "Medical appointment and recovery", "PENDING", null},
            {"LR-0418", "EMP-1020", "TRAINING", "2023-09-27", "2023-09-28", "UX design conference in Lyon", "PENDING", null},
            {"LR-0410", "EMP-1015", "SICK", "2023-09-11", "2023-09-22", "Surgery recovery", "APPROVED", null},
            {"LR-0405", "EMP-1024", "ANNUAL", "2023-09-04", "2023-09-08", "Late summer break", "APPROVED", null},
            {"LR-0402", "EMP-1008", "PERSONAL", "2023-09-21", "2023-09-22", "Personal matters", "REJECTED", "Overlaps release week"},
            {"LR-0400", "EMP-1022", "SICK", "2023-09-05", "2023-09-06", "Flu", "APPROVED", null},
            {"LR-0397", "EMP-1016", "ANNUAL", "2023-09-18", "2023-09-29", "Holiday travel", "REJECTED", "Peak sales period"},
            {"LR-0395", "EMP-1006", "ANNUAL", "2023-08-28", "2023-09-08", "Extended trip", "REJECTED", "Exceeds probation allowance"},
            {"LR-0391", "EMP-1021", "PERSONAL", "2023-08-30", "2023-08-30", "Administrative appointment", "APPROVED", null},
            {"LR-0388", "EMP-1002", "TRAINING", "2023-08-22", "2023-08-23", "Kubernetes workshop", "APPROVED", null},
            {"LR-0384", "EMP-1007", "PARENTAL", "2023-07-17", "2023-09-29", "Parental leave", "APPROVED", null},
            {"LR-0380", "EMP-1013", "ANNUAL", "2023-08-07", "2023-08-11", "Hiking trip in the Alps", "APPROVED", null},
            {"LR-0374", "EMP-1009", "ANNUAL", "2023-08-14", "2023-08-25", "Family holiday", "APPROVED", null},
            {"LR-0369", "EMP-1003", "ANNUAL", "2023-07-31", "2023-08-11", "Summer holiday", "APPROVED", null},
            {"LR-0361", "EMP-1001", "ANNUAL", "2023-07-24", "2023-07-28", "Summer break", "APPROVED", null},
            {"LR-0355", "EMP-1018", "ANNUAL", "2023-07-10", "2023-07-14", "Family visit", "APPROVED", null},
            {"LR-0348", "EMP-1011", "ANNUAL", "2023-07-03", "2023-07-07", "Summer trip", "APPROVED", null},
            {"LR-0340", "EMP-1005", "ANNUAL", "2023-07-17", "2023-07-21", "Summer holiday", "APPROVED", null},
            {"LR-0333", "EMP-1017", "PERSONAL", "2023-07-20", "2023-07-21", "Wedding", "APPROVED", null},
            {"LR-0321", "EMP-1020", "SICK", "2023-08-01", "2023-08-03", "Seasonal flu", "APPROVED", null},
            {"LR-0310", "EMP-1019", "ANNUAL", "2023-07-03", "2023-07-07", "Summer break", "APPROVED", null},
    };

    static void seed(EmployeeDao employees, LeaveRequestDao leaves) {
        int i = 0;
        for (Object[] r : EMPLOYEES) {
            String first = (String) r[1];
            String last = (String) r[2];
            String email = slug(first) + "." + slug(last) + "@northpeak.example";
            String phone = String.format("+33 1 70 %02d %02d %02d", 10 + i * 3, 40 + i * 2, 11 + i);
            employees.insert(new Employee((String) r[0], first, last, (String) r[3], (String) r[4], email, phone,
                    (String) r[8], LocalDate.parse((String) r[5]), EmployeeStatus.valueOf((String) r[6]), (Integer) r[7]));
            i++;
        }
        for (String[] r : LEAVES) {
            leaves.insert(new LeaveRequest(r[0], r[1], LeaveType.valueOf(r[2]), LocalDate.parse(r[3]),
                    LocalDate.parse(r[4]), r[5], LeaveStatus.valueOf(r[6]), r[7]));
        }
    }

    private static String slug(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z]", "");
    }
}
