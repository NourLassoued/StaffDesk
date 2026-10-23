package com.northpeak.hrms.ui.views;

import com.northpeak.hrms.model.Employee;
import com.northpeak.hrms.model.EmployeeStatus;
import com.northpeak.hrms.service.HrService;
import com.northpeak.hrms.ui.Navigator;
import com.northpeak.hrms.ui.Ui;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/** Employees organised by department and job title, with status filter and search. */
public class TrackingView {

    /** Node of the department tree: department == null means "all", title == null means the whole department. */
    private record Scope(String department, String title, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    private final HrService service;
    private final Navigator nav;
    private final ObservableList<Employee> data = FXCollections.observableArrayList();
    private final FilteredList<Employee> filtered = new FilteredList<>(data, e -> true);
    private Scope scope = new Scope(null, null, "All departments");
    private EmployeeStatus statusFilter;
    private final TextField search = new TextField();
    private final Label shown = new Label();

    public TrackingView(HrService service, Navigator nav) {
        this.service = service;
        this.nav = nav;
    }

    public Node build() {
        List<Employee> all = service.employees();
        all.sort(Comparator.comparing(Employee::getDepartment).thenComparing(Employee::getLastName));
        data.setAll(all);

        TreeItem<Scope> root = new TreeItem<>(new Scope(null, null, "All departments (" + all.size() + ")"));
        root.setExpanded(true);
        for (String d : HrService.DEPARTMENTS) {
            List<Employee> inDept = all.stream().filter(e -> e.getDepartment().equals(d)).toList();
            TreeItem<Scope> dn = new TreeItem<>(new Scope(d, null, d + " (" + inDept.size() + ")"));
            for (String t : new TreeSet<>(inDept.stream().map(Employee::getJobTitle).toList())) {
                long n = inDept.stream().filter(e -> e.getJobTitle().equals(t)).count();
                dn.getChildren().add(new TreeItem<>(new Scope(d, t, t + " (" + n + ")")));
            }
            root.getChildren().add(dn);
        }
        TreeView<Scope> tree = new TreeView<>(root);
        tree.getStyleClass().add("dept-tree");
        tree.getSelectionModel().select(root);
        tree.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            if (b != null) {
                scope = b.getValue();
                b.setExpanded(true);
                apply();
            }
        });
        VBox treeCard = new VBox(tree);
        treeCard.getStyleClass().addAll("card", "table-card");
        treeCard.setPrefWidth(280);
        treeCard.setMinWidth(260);
        VBox.setVgrow(tree, Priority.ALWAYS);

        ToggleGroup g = new ToggleGroup();
        HBox seg = new HBox(statusButton(g, "All", null));
        for (EmployeeStatus s : EmployeeStatus.values()) seg.getChildren().add(statusButton(g, s.getLabel(), s));
        seg.getStyleClass().add("segmented");
        search.setPromptText("Search name, title or ID");
        search.setPrefWidth(220);
        search.textProperty().addListener((o, a, b) -> apply());
        HBox searchBox = new HBox(8, Ui.iconBox(Ui.IC_SEARCH, 15, "icon-muted"), search);
        searchBox.getStyleClass().add("search-field");
        searchBox.setAlignment(Pos.CENTER_LEFT);
        shown.getStyleClass().add("muted-small");
        HBox toolbar = new HBox(10, seg, searchBox, Ui.spacer(), shown);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().addAll("card", "toolbar");

        TableView<Employee> t = new TableView<>(filtered);
        t.getColumns().add(Ui.flexible(Ui.nodeCol("Employee", e -> Ui.person(e, null), 200)));
        t.getColumns().add(Ui.monoCol("ID", Employee::getId, 95));
        t.getColumns().add(Ui.textCol("Department", Employee::getDepartment, 130));
        t.getColumns().add(Ui.textCol("Job title", Employee::getJobTitle, 200));
        t.getColumns().add(Ui.fixed(Ui.textCol("Hire date", e -> Ui.date(e.getHireDate()), 118)));
        t.getColumns().add(Ui.numCol("Tenure", e -> String.format(Locale.ENGLISH, "%.1f yrs", HrService.tenureYears(e.getHireDate())), 80));
        t.getColumns().add(Ui.nodeCol("Status", e -> Ui.statusPill(e.getStatus()), 122));
        t.getColumns().add(Ui.nodeCol("", e -> {
            Button v = EmployeesView.iconButton(Ui.IC_EYE, "View details");
            v.setOnAction(ev -> nav.showEmployee(e.getId()));
            return v;
        }, 50));
        Ui.fit(t);
        t.setFixedCellSize(44);
        t.setPlaceholder(new Label("No employees match this selection."));
        VBox tableCard = new VBox(t);
        tableCard.getStyleClass().addAll("card", "table-card");
        VBox.setVgrow(t, Priority.ALWAYS);
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        VBox right = new VBox(14, toolbar, tableCard);
        HBox.setHgrow(right, Priority.ALWAYS);
        HBox split = new HBox(14, treeCard, right);
        VBox.setVgrow(split, Priority.ALWAYS);

        Label sub = new Label("Organised by department and job title");
        sub.getStyleClass().add("muted");
        VBox page = Ui.page();
        page.getChildren().addAll(Ui.pageHeader("Home / Employee Tracking", "Employee Tracking", sub), split);
        apply();
        return page;
    }

    private ToggleButton statusButton(ToggleGroup g, String text, EmployeeStatus s) {
        ToggleButton b = new ToggleButton(text);
        b.setToggleGroup(g);
        b.getStyleClass().add("seg-button");
        b.setContentDisplay(ContentDisplay.RIGHT);
        b.setSelected(s == null);
        b.setOnAction(e -> {
            if (!b.isSelected()) b.setSelected(true);
            statusFilter = s;
            apply();
        });
        return b;
    }

    private void apply() {
        String q = search.getText() == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
        filtered.setPredicate(e ->
                (scope.department() == null || e.getDepartment().equals(scope.department()))
                        && (scope.title() == null || e.getJobTitle().equals(scope.title()))
                        && (statusFilter == null || e.getStatus() == statusFilter)
                        && (q.isEmpty() || (e.getFullName() + " " + e.getJobTitle() + " " + e.getId()).toLowerCase(Locale.ROOT).contains(q)));
        shown.setText(filtered.size() + " shown");
    }
}
