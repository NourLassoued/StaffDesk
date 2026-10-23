package com.northpeak.hrms.ui.views;

import com.northpeak.hrms.model.Employee;
import com.northpeak.hrms.model.EmployeeStatus;
import com.northpeak.hrms.service.HrService;
import com.northpeak.hrms.ui.Dialogs;
import com.northpeak.hrms.ui.Navigator;
import com.northpeak.hrms.ui.Ui;
import com.northpeak.hrms.ui.dialogs.EmployeeDialog;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Searchable employee directory with add / view / edit / delete. */
public class EmployeesView {

    private static final String ALL_DEPTS = "All departments";
    private static final String ALL_STATUSES = "All statuses";

    private final HrService service;
    private final Navigator nav;
    private final ObservableList<Employee> data = FXCollections.observableArrayList();
    private final FilteredList<Employee> filtered = new FilteredList<>(data, e -> true);
    private final TextField search = new TextField();
    private final ComboBox<String> dept = new ComboBox<>();
    private final ComboBox<String> status = new ComboBox<>();
    private final Label count = new Label();

    public EmployeesView(HrService service, Navigator nav) {
        this.service = service;
        this.nav = nav;
    }

    public Node build() {
        data.setAll(service.employees());

        Button add = new Button("Add employee", Ui.iconBox(Ui.IC_PLUS, 15, "icon-white"));
        add.getStyleClass().add("btn-primary");
        add.setOnAction(e -> openEditor(null));

        search.setPromptText("Search by name, ID, department or job title");
        search.setPrefWidth(320);
        HBox searchBox = new HBox(8, Ui.iconBox(Ui.IC_SEARCH, 15, "icon-muted"), search);
        searchBox.getStyleClass().add("search-field");
        searchBox.setAlignment(Pos.CENTER_LEFT);

        List<String> depts = new ArrayList<>();
        depts.add(ALL_DEPTS);
        depts.addAll(HrService.DEPARTMENTS);
        dept.getItems().setAll(depts);
        dept.setValue(ALL_DEPTS);
        List<String> statuses = new ArrayList<>();
        statuses.add(ALL_STATUSES);
        for (EmployeeStatus s : EmployeeStatus.values()) statuses.add(s.getLabel());
        status.getItems().setAll(statuses);
        status.setValue(ALL_STATUSES);

        search.textProperty().addListener((o, a, b) -> applyFilter());
        dept.valueProperty().addListener((o, a, b) -> applyFilter());
        status.valueProperty().addListener((o, a, b) -> applyFilter());
        count.getStyleClass().add("muted-small");

        HBox toolbar = new HBox(10, searchBox, dept, status, Ui.spacer(), count);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().addAll("card", "toolbar");

        TableView<Employee> table = buildTable();
        VBox tableCard = new VBox(table);
        tableCard.getStyleClass().addAll("card", "table-card");
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        VBox page = Ui.page();
        page.getChildren().addAll(Ui.pageHeader("Home / Employees", "Employee Directory", add), toolbar, tableCard);
        applyFilter();
        return page;
    }

    private TableView<Employee> buildTable() {
        SortedList<Employee> sorted = new SortedList<>(filtered);
        TableView<Employee> t = new TableView<>(sorted);
        sorted.comparatorProperty().bind(t.comparatorProperty());

        t.getColumns().add(Ui.monoCol("Employee ID", Employee::getId, 110));
        var name = Ui.flexible(Ui.<Employee>nodeCol("Full name", e -> Ui.person(e, null), 210));
        t.getColumns().add(name);
        t.getColumns().add(Ui.textCol("Department", Employee::getDepartment, 140));
        t.getColumns().add(Ui.textCol("Job title", Employee::getJobTitle, 200));
        t.getColumns().add(Ui.textCol("Email", Employee::getEmail, 260));
        var hire = Ui.fixed(Ui.<Employee>textCol("Hire date", e -> Ui.date(e.getHireDate()), 115));
        hire.setComparator((a, b) -> java.time.LocalDate.parse(a, Ui.DATE).compareTo(java.time.LocalDate.parse(b, Ui.DATE)));
        t.getColumns().add(hire);
        t.getColumns().add(Ui.nodeCol("Status", e -> Ui.statusPill(e.getStatus()), 122));
        t.getColumns().add(Ui.nodeCol("Actions", this::actions, 130));
        Ui.fit(t);
        t.setFixedCellSize(44);
        t.setPlaceholder(new Label("No employees match these filters. Clear the search or choose another department."));
        t.setRowFactory(tv -> {
            TableRow<Employee> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) nav.showEmployee(row.getItem().getId());
            });
            return row;
        });
        return t;
    }

    private Node actions(Employee e) {
        Button view = iconButton(Ui.IC_EYE, "View details");
        view.setOnAction(ev -> nav.showEmployee(e.getId()));
        Button edit = iconButton(Ui.IC_EDIT, "Edit");
        edit.setOnAction(ev -> openEditor(e));
        Button del = iconButton(Ui.IC_TRASH, "Delete");
        del.getStyleClass().add("icon-btn-danger");
        del.setOnAction(ev -> confirmDelete(e));
        HBox box = new HBox(2, view, edit, del);
        box.setAlignment(Pos.CENTER_RIGHT);
        return box;
    }

    static Button iconButton(String path, String tip) {
        Button b = new Button();
        b.setGraphic(Ui.iconBox(path, 15, "icon-muted"));
        b.getStyleClass().add("icon-btn");
        b.setTooltip(new Tooltip(tip));
        return b;
    }

    private void openEditor(Employee e) {
        new EmployeeDialog(service, nav.window(), e).showAndWait().ifPresent(saved -> {
            nav.toast(e == null ? saved.getFullName() + " added as " + saved.getId() : "Changes saved for " + saved.getFullName());
            data.setAll(service.employees());
            applyFilter();
        });
    }

    private void confirmDelete(Employee e) {
        Dialogs.confirm(nav.window(), "Delete employee record",
                        "Delete " + e.getFullName() + " (" + e.getId() + ") and their leave history? This cannot be undone.",
                        "Delete record", true)
                .showAndWait()
                .filter(b -> b.getButtonData() == ButtonBar.ButtonData.OK_DONE)
                .ifPresent(b -> {
                    service.deleteEmployee(e.getId());
                    nav.toast(e.getFullName() + " deleted");
                    nav.showEmployees();
                });
    }

    private void applyFilter() {
        String q = search.getText() == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
        String d = dept.getValue();
        String s = status.getValue();
        filtered.setPredicate(e ->
                (d == null || ALL_DEPTS.equals(d) || e.getDepartment().equals(d))
                        && (s == null || ALL_STATUSES.equals(s) || e.getStatus().getLabel().equals(s))
                        && (q.isEmpty() || String.join(" ", e.getFullName(), e.getId(), e.getDepartment(), e.getJobTitle(), e.getEmail())
                        .toLowerCase(Locale.ROOT).contains(q)));
        count.setText(filtered.size() + " of " + data.size() + " employees");
    }
}
