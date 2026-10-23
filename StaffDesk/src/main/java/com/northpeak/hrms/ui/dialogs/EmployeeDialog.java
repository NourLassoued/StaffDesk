package com.northpeak.hrms.ui.dialogs;

import com.northpeak.hrms.model.Employee;
import com.northpeak.hrms.model.EmployeeStatus;
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
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/** Add / edit employee form. The record is saved when the user confirms and validation passes. */
public class EmployeeDialog extends Dialog<Employee> {

    private final TextField first = new TextField();
    private final TextField last = new TextField();
    private final ComboBox<String> dept = new ComboBox<>();
    private final TextField title = new TextField();
    private final TextField email = new TextField();
    private final TextField phone = new TextField();
    private final ComboBox<String> location = new ComboBox<>();
    private final DatePicker hire = new DatePicker();
    private final ComboBox<EmployeeStatus> status = new ComboBox<>();
    private final Label error = new Label();
    private Employee saved;

    public EmployeeDialog(HrService service, Window owner, Employee existing) {
        boolean isNew = existing == null;
        initOwner(owner);
        setTitle(isNew ? "Add employee" : "Edit employee · " + existing.getId());
        setHeaderText(isNew ? "Add employee" : "Edit employee · " + existing.getId());

        dept.getItems().setAll(HrService.DEPARTMENTS);
        location.getItems().setAll("Paris", "Lyon", "Tunis", "Remote");
        status.getItems().setAll(EmployeeStatus.values());
        email.setPromptText("firstname.lastname@northpeak.example");
        for (var c : new javafx.scene.control.Control[]{dept, location, status, hire}) c.setMaxWidth(Double.MAX_VALUE);

        if (isNew) {
            dept.setValue("Engineering");
            location.setValue("Paris");
            status.setValue(EmployeeStatus.PROBATION);
            hire.setValue(AppClock.today());
        } else {
            first.setText(existing.getFirstName());
            last.setText(existing.getLastName());
            dept.setValue(existing.getDepartment());
            title.setText(existing.getJobTitle());
            email.setText(existing.getEmail());
            phone.setText(existing.getPhone());
            location.setValue(existing.getLocation());
            hire.setValue(existing.getHireDate());
            status.setValue(existing.getStatus());
        }

        GridPane form = new GridPane();
        form.setHgap(14);
        form.setVgap(12);
        ColumnConstraints col = new ColumnConstraints();
        col.setHgrow(Priority.ALWAYS);
        col.setPercentWidth(50);
        form.getColumnConstraints().addAll(col, col);
        form.add(field("First name", first), 0, 0);
        form.add(field("Last name", last), 1, 0);
        form.add(field("Department", dept), 0, 1);
        form.add(field("Job title", title), 1, 1);
        form.add(field("Work email", email), 0, 2, 2, 1);
        form.add(field("Phone", phone), 0, 3);
        form.add(field("Work location", location), 1, 3);
        form.add(field("Hire date", hire), 0, 4);
        form.add(field("Employment status", status), 1, 4);
        error.getStyleClass().add("form-error");
        error.setWrapText(true);
        form.add(error, 0, 5, 2, 1);
        form.setPrefWidth(560);

        ButtonType save = new ButtonType(isNew ? "Add employee" : "Save changes", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().setAll(ButtonType.CANCEL, save);
        getDialogPane().setContent(form);
        Dialogs.style(this);
        getDialogPane().lookupButton(save).getStyleClass().add("btn-primary");

        getDialogPane().lookupButton(save).addEventFilter(ActionEvent.ACTION, ev -> {
            Employee e = isNew ? new Employee() : existing.copy();
            e.setFirstName(first.getText());
            e.setLastName(last.getText());
            e.setDepartment(dept.getValue());
            e.setJobTitle(title.getText());
            e.setEmail(email.getText());
            e.setPhone(phone.getText());
            e.setLocation(location.getValue());
            e.setHireDate(hire.getValue());
            e.setStatus(status.getValue());
            try {
                saved = isNew ? service.addEmployee(e) : service.updateEmployee(e);
            } catch (ValidationException ex) {
                error.setText(ex.getMessage());
                ev.consume(); // keep the dialog open
            }
        });
        setResultConverter(bt -> bt == save ? saved : null);
    }

    private static Node field(String label, Node control) {
        Label l = new Label(label);
        l.getStyleClass().add("field-label");
        return new VBox(5, l, control);
    }
}
