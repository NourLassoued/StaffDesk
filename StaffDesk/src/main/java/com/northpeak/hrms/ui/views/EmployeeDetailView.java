package com.northpeak.hrms.ui.views;

import com.northpeak.hrms.model.Employee;
import com.northpeak.hrms.model.LeaveBalance;
import com.northpeak.hrms.model.LeaveRequest;
import com.northpeak.hrms.service.AppClock;
import com.northpeak.hrms.service.HrService;
import com.northpeak.hrms.ui.Navigator;
import com.northpeak.hrms.ui.Ui;
import com.northpeak.hrms.ui.dialogs.EmployeeDialog;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

/** Employee profile: employment details, annual leave balance and leave history. */
public class EmployeeDetailView {

    private final HrService service;
    private final Navigator nav;
    private final String employeeId;

    public EmployeeDetailView(HrService service, Navigator nav, String employeeId) {
        this.service = service;
        this.nav = nav;
        this.employeeId = employeeId;
    }

    public Node build() {
        Employee e = service.employee(employeeId).orElse(null);
        if (e == null) {
            return new EmployeesView(service, nav).build();
        }
        LeaveBalance b = service.balance(e.getId());
        List<LeaveRequest> history = service.leaveRequestsFor(e.getId());

        Button back = new Button("Back to directory", Ui.iconBox(Ui.IC_BACK, 15, "icon-muted"));
        back.setOnAction(ev -> nav.showEmployees());

        // profile header
        Label name = new Label(e.getFullName());
        name.getStyleClass().add("profile-name");
        Label meta = new Label(e.getJobTitle() + "  ·  " + e.getDepartment() + "  ·  " + e.getId());
        meta.getStyleClass().add("muted");
        HBox metaRow = new HBox(10, meta, Ui.statusPill(e.getStatus()));
        metaRow.setAlignment(Pos.CENTER_LEFT);
        Button edit = new Button("Edit", Ui.iconBox(Ui.IC_EDIT, 15, "icon-muted"));
        edit.setOnAction(ev -> new EmployeeDialog(service, nav.window(), e).showAndWait().ifPresent(s -> {
            nav.toast("Changes saved for " + s.getFullName());
            nav.showEmployee(s.getId());
        }));
        Button request = new Button("Request leave", Ui.iconBox(Ui.IC_PLUS, 15, "icon-white"));
        request.getStyleClass().add("btn-primary");
        request.setOnAction(ev -> nav.openNewLeaveRequest(e.getId()));
        HBox profile = new HBox(18, Ui.avatar(e, 64), new VBox(4, name, metaRow), Ui.spacer(), edit, request);
        profile.setAlignment(Pos.CENTER_LEFT);
        profile.getStyleClass().addAll("card", "profile");

        // details grid
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(11);
        ColumnConstraints c0 = new ColumnConstraints(150);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c0, c1);
        String manager = HrService.MANAGERS.getOrDefault(e.getDepartment(), "—");
        if (manager.equals(e.getFullName())) manager = "Leadership team";
        int r = 0;
        r = row(grid, r, "Employee ID", value(e.getId(), true));
        r = row(grid, r, "Department", value(e.getDepartment(), false));
        r = row(grid, r, "Job title", value(e.getJobTitle(), false));
        r = row(grid, r, "Reports to", value(manager, false));
        r = row(grid, r, "Hire date", value(Ui.date(e.getHireDate()) + "  ·  " + HrService.tenureLabel(e.getHireDate()), false));
        r = row(grid, r, "Employment type", value(e.getStatus().name().equals("PROBATION") ? "Full-time · probation" : "Full-time", false));
        r = row(grid, r, "Work location", value(e.getLocation(), false));
        r = row(grid, r, "Work email", value(e.getEmail(), false));
        r = row(grid, r, "Phone", value(e.getPhone(), true));
        row(grid, r, "Status", Ui.statusPill(e.getStatus()));
        VBox details = Ui.card("Employment details", null, grid);

        // balance
        Label big = new Label(String.valueOf(b.remaining()));
        big.getStyleClass().add("balance-value");
        Label bigSub = new Label("days remaining");
        bigSub.getStyleClass().add("muted");
        HBox bigRow = new HBox(8, big, bigSub);
        bigRow.setAlignment(Pos.BASELINE_LEFT);
        HBox bar = new HBox();
        bar.getStyleClass().add("balance-bar");
        Region used = new Region();
        used.getStyleClass().add("bar-used");
        used.prefWidthProperty().bind(bar.widthProperty().multiply(b.used() / (double) b.allowance()));
        Region pend = new Region();
        pend.getStyleClass().add("bar-pending");
        pend.prefWidthProperty().bind(bar.widthProperty().multiply(b.pending() / (double) b.allowance()));
        bar.getChildren().addAll(used, pend);
        HBox figures = new HBox(figure(b.allowance(), "Allowance", null), figure(b.used(), "Used", "bar-used"),
                figure(b.pending(), "Pending", "bar-pending"), figure(b.remainingAfterPending(), "After pending", null));
        figures.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));
        Label sick = new Label("Sick leave: " + b.sickUsed() + " of " + HrService.SICK_ALLOWANCE + " days used");
        sick.getStyleClass().add("muted-small");
        VBox balance = Ui.card("Annual leave balance · " + AppClock.today().getYear(), b.allowance() + " days allowance",
                new VBox(12, bigRow, bar, figures, sick));

        // history
        TableView<LeaveRequest> t = new TableView<>(FXCollections.observableArrayList(history));
        t.getColumns().add(Ui.monoCol("Request", LeaveRequest::getId, 90));
        t.getColumns().add(Ui.nodeCol("Type", l -> Ui.tag(l.getType().getLabel()), 100));
        t.getColumns().add(Ui.fixed(Ui.textCol("Dates", l -> Ui.range(l.getStartDate(), l.getEndDate()), 130)));
        t.getColumns().add(Ui.numCol("Days", l -> String.valueOf(HrService.workingDays(l.getStartDate(), l.getEndDate())), 60));
        t.getColumns().add(Ui.nodeCol("Status", l -> Ui.leavePill(l.getStatus()), 115));
        Ui.fit(t);
        t.setFixedCellSize(38);
        t.setPrefHeight(38 * Math.max(3, history.size()) + 36);
        t.setPlaceholder(new Label("No leave requests yet."));
        VBox hist = Ui.card("Leave history", history.size() + " request" + (history.size() == 1 ? "" : "s"), t);

        VBox right = new VBox(14, balance, hist);
        HBox columns = new HBox(14, details, right);
        HBox.setHgrow(details, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        details.setPrefWidth(600);
        right.setPrefWidth(560);

        VBox page = Ui.page();
        page.getChildren().addAll(Ui.pageHeader("Home / Employees / " + e.getId(), "Employee Profile", back), profile, columns);
        ScrollPane sp = new ScrollPane(page);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("page-scroll");
        return sp;
    }

    private static int row(GridPane g, int r, String label, Node value) {
        Label l = new Label(label);
        l.getStyleClass().add("muted");
        g.add(l, 0, r);
        g.add(value, 1, r);
        return r + 1;
    }

    private static Label value(String text, boolean mono) {
        Label v = new Label(text);
        v.getStyleClass().add(mono ? "value-mono" : "value");
        return v;
    }

    private static VBox figure(int n, String label, String swatchClass) {
        Label num = new Label(String.valueOf(n));
        num.getStyleClass().add("figure-value");
        Label l = new Label(label);
        l.getStyleClass().add("muted-small");
        if (swatchClass != null) {
            Region sw = new Region();
            sw.getStyleClass().addAll("swatch", swatchClass);
            l.setGraphic(sw);
        }
        return new VBox(1, num, l);
    }
}
