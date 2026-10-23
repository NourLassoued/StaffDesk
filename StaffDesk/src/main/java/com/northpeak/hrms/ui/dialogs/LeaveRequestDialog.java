package com.northpeak.hrms.ui.dialogs;

import com.northpeak.hrms.model.Employee;
import com.northpeak.hrms.model.EmployeeStatus;
import com.northpeak.hrms.model.LeaveBalance;
import com.northpeak.hrms.model.LeaveRequest;
import com.northpeak.hrms.model.LeaveType;
import com.northpeak.hrms.service.AppClock;
import com.northpeak.hrms.service.HrService;
import com.northpeak.hrms.service.ValidationException;
import com.northpeak.hrms.ui.Dialogs;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/** Leave request form with a live working-day count and balance check. */
public class LeaveRequestDialog extends Dialog<LeaveRequest> {

    private final HrService service;
    private final ComboBox<Employee> employee = new ComboBox<>();
    private final ComboBox<LeaveType> type = new ComboBox<>();
    private final DatePicker start = new DatePicker();
    private final DatePicker end = new DatePicker();
    private final TextArea reason = new TextArea();
    private final Label info = new Label();
    private final Label error = new Label();
    private LeaveRequest saved;

    public LeaveRequestDialog(HrService service, Window owner, String preselectedEmployeeId) {
        this.service = service;
        initOwner(owner);
        setTitle("New leave request");
        setHeaderText("New leave request");

        List<Employee> employees = service.employees().stream().filter(e -> e.getStatus() != EmployeeStatus.INACTIVE).toList();
        employee.getItems().setAll(employees);
        employee.setConverter(new StringConverter<>() {
            @Override
            public String toString(Employee e) {
                return e == null ? "" : e.getFullName() + " · " + e.getId() + " · " + e.getDepartment();
            }

            @Override
            public Employee fromString(String s) {
                return null;
            }
        });
        employees.stream().filter(e -> e.getId().equals(preselectedEmployeeId)).findFirst()
                .ifPresentOrElse(employee::setValue, () -> employee.setValue(employees.isEmpty() ? null : employees.get(0)));
        type.getItems().setAll(LeaveType.values());
        type.setValue(LeaveType.ANNUAL);
        LocalDate monday = AppClock.today().plusDays(1);
        while (monday.getDayOfWeek() != DayOfWeek.MONDAY) monday = monday.plusDays(1);
        start.setValue(monday.plusWeeks(1));
        end.setValue(monday.plusWeeks(1).plusDays(2));
        reason.setPromptText("Short description for HR");
        reason.setPrefRowCount(3);
        reason.setWrapText(true);
        for (var c : new javafx.scene.control.Control[]{employee, type, start, end}) c.setMaxWidth(Double.MAX_VALUE);

        GridPane form = new GridPane();
        form.setHgap(14);
        form.setVgap(12);
        ColumnConstraints col = new ColumnConstraints();
        col.setPercentWidth(50);
        col.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(col, col);
        form.add(field("Employee", employee), 0, 0, 2, 1);
        form.add(field("Leave type", type), 0, 1);
        form.add(field("Start date", start), 0, 2);
        form.add(field("End date", end), 1, 2);
        form.add(field("Reason", reason), 0, 3, 2, 1);
        info.getStyleClass().add("info-line");
        info.setMaxWidth(Double.MAX_VALUE);
        info.setWrapText(true);
        form.add(info, 0, 4, 2, 1);
        error.getStyleClass().add("form-error");
        error.setWrapText(true);
        form.add(error, 0, 5, 2, 1);
        form.setPrefWidth(580);

        employee.valueProperty().addListener((o, a, b) -> refreshInfo());
        type.valueProperty().addListener((o, a, b) -> refreshInfo());
        start.valueProperty().addListener((o, a, b) -> refreshInfo());
        end.valueProperty().addListener((o, a, b) -> refreshInfo());
        refreshInfo();

        ButtonType submit = new ButtonType("Submit request", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().setAll(ButtonType.CANCEL, submit);
        getDialogPane().setContent(form);
        Dialogs.style(this);
        getDialogPane().lookupButton(submit).getStyleClass().add("btn-primary");
        getDialogPane().lookupButton(submit).addEventFilter(ActionEvent.ACTION, ev -> {
            try {
                Employee e = employee.getValue();
                saved = service.submitLeave(e == null ? null : e.getId(), type.getValue(), start.getValue(), end.getValue(), reason.getText());
            } catch (ValidationException ex) {
                error.setText(ex.getMessage());
                ev.consume();
            }
        });
        setResultConverter(bt -> bt == submit ? saved : null);
    }

    private void refreshInfo() {
        Employee e = employee.getValue();
        if (e == null) {
            info.setText("Choose an employee to see their balance.");
            return;
        }
        LeaveBalance b = service.balance(e.getId());
        int days = start.getValue() != null && end.getValue() != null && !end.getValue().isBefore(start.getValue())
                ? HrService.workingDays(start.getValue(), end.getValue()) : 0;
        StringBuilder sb = new StringBuilder();
        sb.append(days).append(" working day").append(days == 1 ? "" : "s").append(" requested · Annual balance: ")
                .append(b.remaining()).append(" of ").append(b.allowance()).append(" days remaining");
        if (b.pending() > 0) sb.append(" (").append(b.pending()).append(" pending)");
        if (type.getValue() == LeaveType.ANNUAL && days > 0) {
            sb.append(" · ").append(b.remainingAfterPending() - days).append(" left if approved");
        }
        info.setText(sb.toString());
    }

    private static Node field(String label, Node control) {
        Label l = new Label(label);
        l.getStyleClass().add("field-label");
        return new VBox(5, l, control);
    }
}
