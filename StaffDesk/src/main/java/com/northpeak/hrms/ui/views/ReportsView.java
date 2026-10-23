package com.northpeak.hrms.ui.views;

import com.northpeak.hrms.model.Employee;
import com.northpeak.hrms.model.EmployeeStatus;
import com.northpeak.hrms.model.LeaveRequest;
import com.northpeak.hrms.model.LeaveStatus;
import com.northpeak.hrms.service.HrService;
import com.northpeak.hrms.ui.Ui;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Workforce statistics and leave reports. */
public class ReportsView {

    private record DeptRow(String department, long headcount, long active, long probation, long onLeave,
                           int leaveDays, long pending, String avgBalance) {
    }

    private final HrService service;

    public ReportsView(HrService service) {
        this.service = service;
    }

    public Node build() {
        List<Employee> employees = service.employees();
        List<LeaveRequest> leaves = service.leaveRequests();
        Map<LeaveStatus, Long> counts = service.leaveCounts();
        long approved = counts.get(LeaveStatus.APPROVED);
        long rejected = counts.get(LeaveStatus.REJECTED);
        int approvedDays = leaves.stream().filter(l -> l.getStatus() == LeaveStatus.APPROVED)
                .mapToInt(l -> HrService.workingDays(l.getStartDate(), l.getEndDate())).sum();
        long rate = Math.round(approved * 100.0 / Math.max(1, approved + rejected));

        HBox kpis = new HBox(14,
                Ui.kpi("Average tenure", String.format(Locale.ENGLISH, "%.1f yrs", service.averageTenureYears()),
                        "Across " + employees.size() + " employee records", Ui.IC_USERS, "navy"),
                Ui.kpi("Leave approval rate", rate + "%", approved + " approved · " + rejected + " rejected", Ui.IC_CHECK, "green"),
                Ui.kpi("Leave days approved", String.valueOf(approvedDays),
                        String.format(Locale.ENGLISH, "%.1f working days per request", approvedDays / (double) Math.max(1, approved)), Ui.IC_CALENDAR, "blue"),
                Ui.kpi("Open requests", String.valueOf(counts.get(LeaveStatus.PENDING)), "Pending HR decision", Ui.IC_CLOCK, "amber"));

        VBox hires = Ui.card("Hires by year", "Current employee records", hiresChart());
        VBox byType = Ui.card("Approved leave days by type", "Working days", typeChart());
        HBox charts = new HBox(14, hires, byType);
        HBox.setHgrow(hires, Priority.ALWAYS);
        HBox.setHgrow(byType, Priority.ALWAYS);
        hires.setPrefWidth(600);
        byType.setPrefWidth(600);

        VBox dept = Ui.card("Department summary", "Headcount and leave", departmentTable(employees, leaves));

        VBox page = Ui.page();
        page.getChildren().addAll(Ui.pageHeader("Home / Reports", "Reports"), kpis, charts, dept);
        ScrollPane sp = new ScrollPane(page);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("page-scroll");
        return sp;
    }

    private Node hiresChart() {
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();
        y.setTickUnit(1);
        y.setMinorTickVisible(false);
        BarChart<String, Number> chart = new BarChart<>(x, y);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setPrefHeight(230);
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        service.hiresByYear().forEach((year, n) -> s.getData().add(new XYChart.Data<>(String.valueOf(year), n)));
        chart.getData().add(s);
        s.getData().forEach(d -> DashboardView.colorNode(d, "#2D5AA8"));
        return chart;
    }

    private Node typeChart() {
        NumberAxis x = new NumberAxis();
        x.setMinorTickVisible(false);
        CategoryAxis y = new CategoryAxis();
        BarChart<Number, String> chart = new BarChart<>(x, y);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setPrefHeight(230);
        XYChart.Series<Number, String> s = new XYChart.Series<>();
        service.approvedDaysByType().entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(en -> s.getData().add(new XYChart.Data<>(en.getValue(), en.getKey().getLabel())));
        chart.getData().add(s);
        s.getData().forEach(d -> DashboardView.colorNode(d, "#1B3360"));
        return chart;
    }

    private Node departmentTable(List<Employee> employees, List<LeaveRequest> leaves) {
        List<DeptRow> rows = new ArrayList<>();
        for (String d : HrService.DEPARTMENTS) {
            List<Employee> es = employees.stream().filter(e -> e.getDepartment().equals(d)).toList();
            Set<String> ids = es.stream().map(Employee::getId).collect(Collectors.toSet());
            List<LeaveRequest> ls = leaves.stream().filter(l -> ids.contains(l.getEmployeeId())).toList();
            int days = ls.stream().filter(l -> l.getStatus() == LeaveStatus.APPROVED)
                    .mapToInt(l -> HrService.workingDays(l.getStartDate(), l.getEndDate())).sum();
            double avg = es.stream().filter(e -> e.getStatus() != EmployeeStatus.INACTIVE)
                    .mapToInt(e -> service.balance(e.getId()).remaining()).average().orElse(Double.NaN);
            rows.add(new DeptRow(d, es.size(),
                    es.stream().filter(e -> e.getStatus() == EmployeeStatus.ACTIVE).count(),
                    es.stream().filter(e -> e.getStatus() == EmployeeStatus.PROBATION).count(),
                    es.stream().filter(e -> e.getStatus() == EmployeeStatus.ON_LEAVE).count(),
                    days,
                    ls.stream().filter(l -> l.getStatus() == LeaveStatus.PENDING).count(),
                    Double.isNaN(avg) ? "—" : String.format(Locale.ENGLISH, "%.1f days", avg)));
        }
        TableView<DeptRow> t = new TableView<>(FXCollections.observableArrayList(rows));
        t.getColumns().add(Ui.textCol("Department", DeptRow::department, 180));
        t.getColumns().add(Ui.numCol("Headcount", r -> String.valueOf(r.headcount()), 100));
        t.getColumns().add(Ui.numCol("Active", r -> String.valueOf(r.active()), 90));
        t.getColumns().add(Ui.numCol("Probation", r -> String.valueOf(r.probation()), 90));
        t.getColumns().add(Ui.numCol("On leave", r -> String.valueOf(r.onLeave()), 90));
        t.getColumns().add(Ui.numCol("Leave days approved", r -> String.valueOf(r.leaveDays()), 150));
        t.getColumns().add(Ui.numCol("Pending requests", r -> String.valueOf(r.pending()), 130));
        t.getColumns().add(Ui.numCol("Avg. balance left", DeptRow::avgBalance, 130));
        Ui.fit(t);
        t.setFixedCellSize(38);
        t.setPrefHeight(38 * rows.size() + 38);
        return t;
    }
}
