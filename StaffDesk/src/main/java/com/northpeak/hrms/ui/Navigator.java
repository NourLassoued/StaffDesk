package com.northpeak.hrms.ui;

import com.northpeak.hrms.model.LeaveStatus;
import javafx.stage.Window;

/** Navigation and feedback callbacks shared by every screen. */
public interface Navigator {

    void showDashboard();

    void showEmployees();

    void showEmployee(String employeeId);

    /** @param filter status tab to open, or {@code null} for "All" */
    void showLeave(LeaveStatus filter);

    void showTracking();

    void showReports();

    void openNewLeaveRequest(String preselectedEmployeeId);

    void toast(String message);

    Window window();
}
