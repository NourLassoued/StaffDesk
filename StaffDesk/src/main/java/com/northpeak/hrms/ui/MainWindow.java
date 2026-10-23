package com.northpeak.hrms.ui;

import com.northpeak.hrms.db.Database;
import com.northpeak.hrms.model.LeaveStatus;
import com.northpeak.hrms.service.HrService;
import com.northpeak.hrms.ui.dialogs.EmployeeDialog;
import com.northpeak.hrms.ui.dialogs.LeaveRequestDialog;
import com.northpeak.hrms.ui.views.DashboardView;
import com.northpeak.hrms.ui.views.EmployeeDetailView;
import com.northpeak.hrms.ui.views.EmployeesView;
import com.northpeak.hrms.ui.views.LeaveView;
import com.northpeak.hrms.ui.views.ReportsView;
import com.northpeak.hrms.ui.views.TrackingView;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.LinkedHashMap;
import java.util.Map;

/** Application shell: menu bar, sidebar navigation, content area, status bar and toasts. */
public class MainWindow implements Navigator {

    private final HrService service;
    private final Database database;

    private final StackPane root = new StackPane();
    private final BorderPane shell = new BorderPane();
    private final StackPane content = new StackPane();
    private final VBox toasts = new VBox(8);
    private final Map<String, Button> navButtons = new LinkedHashMap<>();
    private final Label pendingBadge = new Label();
    private final Label statusView = new Label();
    private final Label statusCount = new Label();

    public MainWindow(HrService service, Database database) {
        this.service = service;
        this.database = database;

        content.getStyleClass().add("content");
        shell.setTop(buildMenuBar());
        shell.setLeft(buildSidebar());
        shell.setCenter(content);
        shell.setBottom(buildStatusBar());

        toasts.setAlignment(Pos.BOTTOM_RIGHT);
        toasts.setPickOnBounds(false);
        toasts.setPadding(new Insets(0, 22, 40, 0));
        toasts.setMouseTransparent(true);

        root.getChildren().addAll(shell, toasts);
        showDashboard();
    }

    public Parent getRoot() {
        return root;
    }

    // ---------------------------------------------------------------- chrome

    private Node buildMenuBar() {
        Menu file = new Menu("_File");
        MenuItem reset = new MenuItem("Reset demo data");
        reset.setOnAction(e -> resetData());
        MenuItem exit = new MenuItem("Exit");
        exit.setAccelerator(KeyCombination.keyCombination("Shortcut+Q"));
        exit.setOnAction(e -> Platform.exit());
        file.getItems().addAll(reset, new SeparatorMenuItem(), exit);

        Menu view = new Menu("_View");
        view.getItems().addAll(item("Dashboard", "Shortcut+1", this::showDashboard),
                item("Employees", "Shortcut+2", this::showEmployees),
                item("Leave requests", "Shortcut+3", () -> showLeave(LeaveStatus.PENDING)),
                item("Employee tracking", "Shortcut+4", this::showTracking),
                item("Reports", "Shortcut+5", this::showReports));

        Menu employees = new Menu("E_mployees");
        employees.getItems().add(item("Add employee…", "Shortcut+N", this::addEmployee));

        Menu leave = new Menu("_Leave");
        leave.getItems().add(item("New leave request…", "Shortcut+L", () -> openNewLeaveRequest(null)));

        Menu reports = new Menu("_Reports");
        reports.getItems().add(item("Open reports", null, this::showReports));

        Menu help = new Menu("_Help");
        help.getItems().add(item("About NorthPeak HRMS", null, this::about));

        MenuBar bar = new MenuBar(file, view, employees, leave, reports, help);
        bar.getStyleClass().add("app-menu");

        Label who = new Label("Signed in as s.mitchell · HR Administrator");
        who.getStyleClass().add("menu-user");

        HBox box = new HBox(bar, Ui.spacer(), who);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("menu-row");
        HBox.setHgrow(bar, Priority.NEVER);
        return box;
    }

    private MenuItem item(String text, String accel, Runnable action) {
        MenuItem m = new MenuItem(text);
        if (accel != null) m.setAccelerator(KeyCombination.keyCombination(accel));
        m.setOnAction(e -> action.run());
        return m;
    }

    private Node buildSidebar() {
        StackPane logo = new StackPane(Ui.iconBox(Ui.IC_LOGO, 20, "logo-mark"));
        logo.getStyleClass().add("logo-tile");
        Label name = new Label("NorthPeak");
        name.getStyleClass().add("brand-name");
        Label ver = new Label("HRMS 2.4");
        ver.getStyleClass().add("brand-sub");
        HBox brand = new HBox(10, logo, new VBox(name, ver));
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.getStyleClass().add("brand");

        Label section = new Label("WORKSPACE");
        section.getStyleClass().add("nav-label");

        pendingBadge.getStyleClass().add("nav-badge");

        VBox nav = new VBox(4,
                navButton("dashboard", "Dashboard", Ui.IC_DASHBOARD, this::showDashboard, null),
                navButton("employees", "Employees", Ui.IC_USERS, this::showEmployees, null),
                navButton("leave", "Leave Requests", Ui.IC_CALENDAR, () -> showLeave(LeaveStatus.PENDING), pendingBadge),
                navButton("tracking", "Employee Tracking", Ui.IC_TREE, this::showTracking, null),
                navButton("reports", "Reports", Ui.IC_CHART, this::showReports, null));

        Label av = new Label("SM");
        av.getStyleClass().add("admin-avatar");
        Label an = new Label("Sarah Mitchell");
        an.getStyleClass().add("admin-name");
        Label ar = new Label("HR Administrator");
        ar.getStyleClass().add("admin-role");
        HBox admin = new HBox(10, av, new VBox(an, ar));
        admin.setAlignment(Pos.CENTER_LEFT);
        admin.getStyleClass().add("admin");

        Region grow = new Region();
        VBox.setVgrow(grow, Priority.ALWAYS);
        VBox side = new VBox(brand, section, nav, grow, admin);
        side.getStyleClass().add("sidebar");
        side.setPrefWidth(236);
        return side;
    }

    private Button navButton(String key, String text, String icon, Runnable action, Label badge) {
        Button b = new Button();
        Label label = new Label(text);
        label.getStyleClass().add("nav-text");
        label.setMinWidth(Region.USE_PREF_SIZE);
        HBox inner = new HBox(11, Ui.iconBox(icon, 18, "nav-icon"), label, Ui.spacer());
        if (badge != null) inner.getChildren().add(badge);
        inner.setAlignment(Pos.CENTER_LEFT);
        inner.prefWidthProperty().bind(b.widthProperty().subtract(22));
        b.setGraphic(inner);
        b.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
        b.getStyleClass().add("nav-item");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setOnAction(e -> action.run());
        navButtons.put(key, b);
        return b;
    }

    private Node buildStatusBar() {
        Circle dot = new Circle(3.5);
        dot.getStyleClass().add("status-dot");
        Label ready = new Label("Ready", dot);
        Label right = new Label("Embedded H2 database · data/northpeak-hrms.mv.db");
        right.getStyleClass().add("status-right");
        HBox bar = new HBox(18, ready, statusView, statusCount, Ui.spacer(), right);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("statusbar");
        return bar;
    }

    // ---------------------------------------------------------------- navigation

    private void setView(String navKey, String title, Node view) {
        content.getChildren().setAll(view);
        view.setOpacity(0);
        view.setTranslateY(8);
        FadeTransition fade = new FadeTransition(Duration.millis(220), view);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(260), view);
        slide.setToY(0);
        new ParallelTransition(fade, slide).play();

        navButtons.forEach((k, b) -> b.getStyleClass().remove("active"));
        Button active = navButtons.get(navKey);
        if (active != null) active.getStyleClass().add("active");

        long pending = service.leaveCounts().get(LeaveStatus.PENDING);
        pendingBadge.setText(String.valueOf(pending));
        pendingBadge.setVisible(pending > 0);
        statusView.setText(title);
        statusCount.setText(service.employees().size() + " employee records");
    }

    @Override
    public void showDashboard() {
        setView("dashboard", "Dashboard", new DashboardView(service, this).build());
    }

    @Override
    public void showEmployees() {
        setView("employees", "Employee Directory", new EmployeesView(service, this).build());
    }

    @Override
    public void showEmployee(String employeeId) {
        setView("employees", "Employee Profile", new EmployeeDetailView(service, this, employeeId).build());
    }

    @Override
    public void showLeave(LeaveStatus filter) {
        setView("leave", "Leave Management", new LeaveView(service, this, filter).build());
    }

    @Override
    public void showTracking() {
        setView("tracking", "Employee Tracking", new TrackingView(service, this).build());
    }

    @Override
    public void showReports() {
        setView("reports", "Reports", new ReportsView(service).build());
    }

    @Override
    public void openNewLeaveRequest(String preselectedEmployeeId) {
        new LeaveRequestDialog(service, window(), preselectedEmployeeId).showAndWait().ifPresent(l -> {
            toast("Leave request " + l.getId() + " submitted for review");
            showLeave(LeaveStatus.PENDING);
        });
    }

    private void addEmployee() {
        new EmployeeDialog(service, window(), null).showAndWait().ifPresent(e -> {
            toast(e.getFullName() + " added as " + e.getId());
            showEmployees();
        });
    }

    @Override
    public Window window() {
        return root.getScene() == null ? null : root.getScene().getWindow();
    }

    @Override
    public void toast(String message) {
        Label t = new Label(message, Ui.iconBox(Ui.IC_CHECK, 16, "toast-icon"));
        t.getStyleClass().add("toast");
        t.setGraphicTextGap(10);
        toasts.getChildren().add(t);
        t.setOpacity(0);
        FadeTransition in = new FadeTransition(Duration.millis(200), t);
        in.setToValue(1);
        FadeTransition out = new FadeTransition(Duration.millis(400), t);
        out.setToValue(0);
        SequentialTransition seq = new SequentialTransition(in, new PauseTransition(Duration.seconds(3)), out);
        seq.setOnFinished(e -> toasts.getChildren().remove(t));
        seq.play();
    }

    private void resetData() {
        Alert a = Dialogs.confirm(window(), "Reset demo data",
                "Delete every change and reload the 24 demo employees and 25 leave requests?",
                "Reset data", true);
        a.showAndWait().filter(b -> b.getButtonData() == ButtonType.OK.getButtonData()).ifPresent(b -> {
            database.resetDemoData();
            toast("Demo data reloaded");
            showDashboard();
        });
    }

    private void about() {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.initOwner(window());
        a.setTitle("About");
        a.setHeaderText("NorthPeak HRMS 2.4");
        a.setContentText("Desktop Human Resources Management System.\nJava 17 · JavaFX 17 · H2 (JDBC).\nAll employee data is fictional.");
        Dialogs.style(a);
        a.showAndWait();
    }
}
