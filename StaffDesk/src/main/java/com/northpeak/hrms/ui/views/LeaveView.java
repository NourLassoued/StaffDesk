package com.northpeak.hrms.ui.views;

import com.northpeak.hrms.model.Employee;
import com.northpeak.hrms.model.LeaveBalance;
import com.northpeak.hrms.model.LeaveRequest;
import com.northpeak.hrms.model.LeaveStatus;
import com.northpeak.hrms.model.LeaveType;
import com.northpeak.hrms.service.HrService;
import com.northpeak.hrms.service.ValidationException;
import com.northpeak.hrms.ui.Dialogs;
import com.northpeak.hrms.ui.Navigator;
import com.northpeak.hrms.ui.Ui;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Leave management: status tabs, request table, approve and reject actions. */
public class LeaveView {

    private final HrService service;
    private final Navigator nav;
    private final LeaveStatus filter;
    private Map<String, Employee> employees;

    public LeaveView(HrService service, Navigator nav, LeaveStatus filter) {
        this.service = service;
        this.nav = nav;
        this.filter = filter;
    }

    public Node build() {
        employees = service.employees().stream().collect(Collectors.toMap(Employee::getId, Function.identity()));
        Map<LeaveStatus, Long> counts = service.leaveCounts();
        long all = counts.values().stream().mapToLong(Long::longValue).sum();

        Button newLeave = new Button("New leave request", Ui.iconBox(Ui.IC_PLUS, 15, "icon-white"));
        newLeave.getStyleClass().add("btn-primary");
        newLeave.setOnAction(e -> nav.openNewLeaveRequest(null));

        ToggleGroup group = new ToggleGroup();
        HBox tabs = new HBox(
                tab(group, "Pending", counts.get(LeaveStatus.PENDING), LeaveStatus.PENDING),
                tab(group, "Approved", counts.get(LeaveStatus.APPROVED), LeaveStatus.APPROVED),
                tab(group, "Rejected", counts.get(LeaveStatus.REJECTED), LeaveStatus.REJECTED),
                tab(group, "All", all, null));
        tabs.getStyleClass().add("segmented");
        Label hint = new Label("Annual allowance " + HrService.ANNUAL_ALLOWANCE + " days · balances in working days");
        hint.getStyleClass().add("muted-small");
        HBox toolbar = new HBox(10, tabs, Ui.spacer(), hint);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().addAll("card", "toolbar");

        TableView<LeaveRequest> t = new TableView<>(FXCollections.observableArrayList(service.leaveRequests(filter)));
        t.getColumns().add(Ui.monoCol("Request", LeaveRequest::getId, 90));
        t.getColumns().add(Ui.flexible(Ui.nodeCol("Employee", l -> {
            Employee e = employees.get(l.getEmployeeId());
            return e == null ? new Label("Removed employee") : Ui.person(e, e.getDepartment());
        }, 200)));
        t.getColumns().add(Ui.nodeCol("Type", l -> Ui.tag(l.getType().getLabel()), 108));
        t.getColumns().add(Ui.fixed(Ui.textCol("Dates", l -> Ui.compactRange(l.getStartDate(), l.getEndDate()), 150)));
        t.getColumns().add(Ui.numCol("Days", l -> String.valueOf(HrService.workingDays(l.getStartDate(), l.getEndDate())), 55));
        t.getColumns().add(Ui.textCol("Reason", LeaveRequest::getReason, 220));
        t.getColumns().add(Ui.numCol("Balance left", l -> service.balance(l.getEmployeeId()).remaining() + " / " + HrService.ANNUAL_ALLOWANCE, 100));
        t.getColumns().add(Ui.nodeCol("Status", l -> Ui.leavePill(l.getStatus()), 115));
        t.getColumns().add(Ui.nodeCol("Actions", this::actions, 200));
        Ui.fit(t);
        t.setFixedCellSize(52);
        t.setPlaceholder(new Label("No " + (filter == null ? "" : filter.getLabel().toLowerCase() + " ") + "requests."));
        VBox tableCard = new VBox(t);
        tableCard.getStyleClass().addAll("card", "table-card");
        VBox.setVgrow(t, Priority.ALWAYS);
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        VBox page = Ui.page();
        page.getChildren().addAll(Ui.pageHeader("Home / Leave Requests", "Leave Management", newLeave), toolbar, tableCard);
        return page;
    }

    private ToggleButton tab(ToggleGroup g, String text, long count, LeaveStatus status) {
        Label c = new Label(String.valueOf(count));
        c.getStyleClass().add("seg-count");
        ToggleButton b = new ToggleButton(text, c);
        b.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
        b.setToggleGroup(g);
        b.getStyleClass().add("seg-button");
        b.setSelected(status == filter);
        b.setOnAction(e -> nav.showLeave(status));
        return b;
    }

    private Node actions(LeaveRequest l) {
        if (l.getStatus() != LeaveStatus.PENDING) {
            Label note = new Label(l.getReviewNote() == null || l.getReviewNote().isBlank() ? "Reviewed by S. Mitchell" : l.getReviewNote());
            note.getStyleClass().add("muted-small");
            HBox box = new HBox(note);
            box.setAlignment(Pos.CENTER_RIGHT);
            return box;
        }
        Button approve = new Button("Approve", Ui.iconBox(Ui.IC_CHECK, 14, "icon-ok"));
        approve.getStyleClass().addAll("btn-small", "btn-ok-outline");
        approve.setOnAction(e -> confirmApprove(l));
        Button reject = new Button("Reject", Ui.iconBox(Ui.IC_X, 14, "icon-bad"));
        reject.getStyleClass().addAll("btn-small", "btn-bad-outline");
        reject.setOnAction(e -> confirmReject(l));
        HBox box = new HBox(6, approve, reject);
        box.setAlignment(Pos.CENTER_RIGHT);
        return box;
    }

    private GridPane summary(LeaveRequest l) {
        Employee e = employees.get(l.getEmployeeId());
        LeaveBalance b = service.balance(l.getEmployeeId());
        int d = HrService.workingDays(l.getStartDate(), l.getEndDate());
        GridPane g = new GridPane();
        g.setHgap(14);
        g.setVgap(6);
        g.getStyleClass().add("summary");
        g.setPadding(new Insets(10, 12, 10, 12));
        int r = 0;
        g.addRow(r++, muted("Employee"), bold(e == null ? l.getEmployeeId() : e.getFullName() + " · " + e.getId()));
        g.addRow(r++, muted("Leave type"), bold(l.getType().getLabel()));
        g.addRow(r++, muted("Dates"), bold(Ui.date(l.getStartDate()) + " – " + Ui.date(l.getEndDate()) + " (" + d + " working days)"));
        g.addRow(r++, muted("Reason"), bold(l.getReason()));
        if (l.getType() == LeaveType.ANNUAL) {
            g.addRow(r, muted("Balance"), bold(b.remaining() + " days now → " + (b.remaining() - d) + " after approval"));
        }
        return g;
    }

    private void confirmApprove(LeaveRequest l) {
        Alert a = Dialogs.confirm(nav.window(), "Approve leave request · " + l.getId(),
                "Approve this leave request? The employee's balance is updated straight away.", "Approve", false);
        Label msg = new Label("Approve this leave request? The employee's balance is updated straight away.");
        msg.setWrapText(true);
        a.getDialogPane().setContent(new VBox(12, msg, summary(l)));
        a.showAndWait().filter(b -> b.getButtonData() == ButtonBar.ButtonData.OK_DONE).ifPresent(b -> {
            try {
                service.approve(l.getId());
                Employee e = employees.get(l.getEmployeeId());
                nav.toast("Leave " + l.getId() + " approved" + (e == null ? "" : " for " + e.getFullName()));
            } catch (ValidationException ex) {
                nav.toast(ex.getMessage());
            }
            nav.showLeave(filter);
        });
    }

    private void confirmReject(LeaveRequest l) {
        Dialog<String> d = new Dialog<>();
        d.initOwner(nav.window());
        d.setTitle("Reject leave request · " + l.getId());
        d.setHeaderText("Reject leave request · " + l.getId());
        ButtonType reject = new ButtonType("Reject", ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().setAll(ButtonType.CANCEL, reject);
        TextField reason = new TextField();
        reason.setPromptText("For example: overlaps a release week");
        Label msg = new Label("Reject this leave request? The employee sees the reason you give.");
        Label rl = new Label("Reason for rejection");
        rl.getStyleClass().add("field-label");
        d.getDialogPane().setContent(new VBox(10, msg, summary(l), rl, reason));
        Dialogs.style(d);
        d.getDialogPane().lookupButton(reject).getStyleClass().add("btn-danger");
        d.setResultConverter(bt -> bt == reject ? reason.getText() : null);
        d.showAndWait().ifPresent(text -> {
            service.reject(l.getId(), text);
            nav.toast("Leave " + l.getId() + " rejected");
            nav.showLeave(filter);
        });
    }

    private static Label muted(String s) {
        Label l = new Label(s);
        l.getStyleClass().add("muted");
        return l;
    }

    private static Label bold(String s) {
        Label l = new Label(s);
        l.getStyleClass().add("value");
        l.setWrapText(true);
        return l;
    }
}
