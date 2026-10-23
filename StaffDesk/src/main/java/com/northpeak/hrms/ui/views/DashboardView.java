package com.northpeak.hrms.ui.views;

import com.northpeak.hrms.model.Employee;
import com.northpeak.hrms.model.EmployeeStatus;
import com.northpeak.hrms.model.LeaveRequest;
import com.northpeak.hrms.model.LeaveStatus;
import com.northpeak.hrms.service.AppClock;
import com.northpeak.hrms.service.HrService;
import com.northpeak.hrms.ui.Navigator;
import com.northpeak.hrms.ui.Ui;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Admin dashboard: KPIs, charts, recent requests and employment status. */
public class DashboardView {

    private static final Map<EmployeeStatus, String> STATUS_COLORS = Map.of(
            EmployeeStatus.ACTIVE, "#1E8556",
            EmployeeStatus.PROBATION, "#D9A441",
            EmployeeStatus.ON_LEAVE, "#5B8DD6",
            EmployeeStatus.INACTIVE, "#C6CFDD");

    private final HrService service;
    private final Navigator nav;

    public DashboardView(HrService service, Navigator nav) {
        this.service = service;
        this.nav = nav;
    }

    public Node build() {
        List<Employee> employees = service.employees();
        Map<LeaveStatus, Long> counts = service.leaveCounts();
        long total = employees.size();
        long active = employees.stream().filter(e -> e.getStatus() == EmployeeStatus.ACTIVE).count();
        long onLeave = employees.stream().filter(e -> e.getStatus() == EmployeeStatus.ON_LEAVE).count();
        long newHires = employees.stream().filter(e -> e.getStatus() == EmployeeStatus.PROBATION).count();

        Button newLeave = new Button("New leave request", Ui.iconBox(Ui.IC_PLUS, 15, "icon-white"));
        newLeave.getStyleClass().add("btn-primary");
        newLeave.setOnAction(e -> nav.openNewLeaveRequest(null));
        Label today = new Label(Ui.LONG.format(AppClock.today()));
        today.getStyleClass().add("muted");

        VBox page = Ui.page();
        page.getChildren().add(Ui.pageHeader("Home / Dashboard", "Dashboard", today, newLeave));

        HBox kpis = new HBox(14,
                Ui.kpi("Total employees", String.valueOf(total), "+" + newHires + " new hires on probation", Ui.IC_USERS, "navy"),
                Ui.kpi("Active employees", String.valueOf(active),
                        Math.round(active * 100.0 / Math.max(1, total)) + "% of workforce · " + onLeave + " on leave", Ui.IC_USERS, "green"),
                Ui.kpi("Pending leave requests", String.valueOf(counts.get(LeaveStatus.PENDING)), "Awaiting HR review", Ui.IC_CLOCK, "amber"),
                Ui.kpi("Approved leaves", String.valueOf(counts.get(LeaveStatus.APPROVED)),
                        counts.get(LeaveStatus.REJECTED) + " rejected this quarter", Ui.IC_CALENDAR, "blue"));

        VBox dept = Ui.card("Employees by department", total + " people", departmentChart());
        VBox status = Ui.card("Leave by status", "This quarter", statusDonut(counts));
        VBox weekly = Ui.card("Leave days taken per week", "Approved working days", weeklyChart());
        HBox charts = new HBox(14, dept, status, weekly);
        HBox.setHgrow(dept, Priority.ALWAYS);
        HBox.setHgrow(status, Priority.ALWAYS);
        HBox.setHgrow(weekly, Priority.ALWAYS);
        dept.setPrefWidth(420);
        status.setPrefWidth(330);
        weekly.setPrefWidth(460);

        Button viewAll = new Button("View all");
        viewAll.getStyleClass().add("btn-small");
        viewAll.setOnAction(e -> nav.showLeave(null));
        VBox recent = Ui.card("Recent leave requests", null, recentTable(employees));
        ((HBox) recent.getChildren().get(0)).getChildren().add(viewAll);
        VBox empStatus = Ui.card("Employment status", "All records", employmentStatus(employees));
        HBox bottom = new HBox(14, recent, empStatus);
        HBox.setHgrow(recent, Priority.ALWAYS);
        recent.setPrefWidth(820);
        empStatus.setPrefWidth(360);
        empStatus.setMaxWidth(380);

        page.getChildren().addAll(kpis, charts, bottom);
        ScrollPane sp = new ScrollPane(page);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("page-scroll");
        return sp;
    }

    private Node departmentChart() {
        long max = service.headcountByDepartment().values().stream().mapToLong(Long::longValue).max().orElse(1);
        NumberAxis x = new NumberAxis(0, max + 1, 1);
        x.setMinorTickVisible(false);
        CategoryAxis y = new CategoryAxis();
        BarChart<Number, String> chart = new BarChart<>(x, y);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setPrefHeight(230);
        chart.setCategoryGap(8);
        XYChart.Series<Number, String> s = new XYChart.Series<>();
        // ascending, so the largest department is drawn at the top
        service.headcountByDepartment().entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(en -> s.getData().add(new XYChart.Data<>(en.getValue(), en.getKey())));
        chart.getData().add(s);
        for (XYChart.Data<Number, String> d : s.getData()) {
            colorNode(d, Ui.DEPT_COLORS.get(d.getYValue()));
        }
        return chart;
    }

    private Node statusDonut(Map<LeaveStatus, Long> counts) {
        PieChart pie = new PieChart();
        pie.setLabelsVisible(false);
        pie.setLegendVisible(false);
        pie.setAnimated(false);
        pie.setStartAngle(90);
        pie.setPrefSize(160, 160);
        pie.setMinSize(160, 160);
        pie.setMaxSize(160, 160);
        Map<LeaveStatus, String> colors = Map.of(LeaveStatus.APPROVED, "#1E8556", LeaveStatus.PENDING, "#D9A441", LeaveStatus.REJECTED, "#B8392E");
        for (LeaveStatus st : List.of(LeaveStatus.APPROVED, LeaveStatus.PENDING, LeaveStatus.REJECTED)) {
            PieChart.Data d = new PieChart.Data(st.getLabel(), counts.get(st));
            pie.getData().add(d);
            String c = colors.get(st);
            if (d.getNode() != null) d.getNode().setStyle("-fx-pie-color: " + c + ";");
            d.nodeProperty().addListener((o, a, n) -> { if (n != null) n.setStyle("-fx-pie-color: " + c + ";"); });
        }
        long all = counts.values().stream().mapToLong(Long::longValue).sum();
        Circle hole = new Circle(44);
        hole.getStyleClass().add("donut-hole");
        Label num = new Label(String.valueOf(all));
        num.getStyleClass().add("donut-value");
        Label sub = new Label("requests");
        sub.getStyleClass().add("muted-small");
        VBox center = new VBox(num, sub);
        center.setAlignment(Pos.CENTER);
        StackPane donut = new StackPane(pie, hole, center);

        VBox legend = new VBox(10);
        for (LeaveStatus st : List.of(LeaveStatus.APPROVED, LeaveStatus.PENDING, LeaveStatus.REJECTED)) {
            legend.getChildren().add(legendRow(colors.get(st), st.getLabel(), counts.get(st)));
        }
        legend.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(legend, Priority.ALWAYS);
        HBox box = new HBox(16, donut, legend);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private Node weeklyChart() {
        Map<String, Integer> weeks = service.approvedDaysPerWeek();
        int max = weeks.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        CategoryAxis x = new CategoryAxis();
        x.setLabel("ISO week");
        x.setTickLabelRotation(0);
        NumberAxis y = new NumberAxis(0, Math.max(5, Math.ceil(max / 5.0) * 5), 5);
        y.setMinorTickVisible(false);
        BarChart<String, Number> chart = new BarChart<>(x, y);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setPrefHeight(230);
        chart.setCategoryGap(6);
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        weeks.forEach((k, v) -> s.getData().add(new XYChart.Data<>(k.substring(1), v)));
        chart.getData().add(s);
        for (XYChart.Data<String, Number> d : s.getData()) colorNode(d, "#2D5AA8");
        return chart;
    }

    private Node recentTable(List<Employee> employees) {
        Map<String, Employee> byId = employees.stream().collect(Collectors.toMap(Employee::getId, Function.identity()));
        List<LeaveRequest> recent = new ArrayList<>(service.leaveRequests());
        TableView<LeaveRequest> t = new TableView<>(FXCollections.observableArrayList(recent.subList(0, Math.min(5, recent.size()))));
        t.getColumns().add(Ui.monoCol("Request", LeaveRequest::getId, 90));
        t.getColumns().add(Ui.flexible(Ui.nodeCol("Employee", l -> byId.containsKey(l.getEmployeeId())
                ? Ui.person(byId.get(l.getEmployeeId()), null) : new Label("—"), 210)));
        t.getColumns().add(Ui.nodeCol("Type", l -> Ui.tag(l.getType().getLabel()), 100));
        t.getColumns().add(Ui.fixed(Ui.textCol("Dates", l -> Ui.range(l.getStartDate(), l.getEndDate()), 130)));
        t.getColumns().add(Ui.numCol("Days", l -> String.valueOf(HrService.workingDays(l.getStartDate(), l.getEndDate())), 60));
        t.getColumns().add(Ui.nodeCol("Status", l -> Ui.leavePill(l.getStatus()), 115));
        Ui.fit(t);
        t.setFixedCellSize(42);
        t.setPrefHeight(42 * 5 + 36);
        t.setRowFactory(tv -> {
            var row = new javafx.scene.control.TableRow<LeaveRequest>();
            row.setOnMouseClicked(e -> { if (e.getClickCount() == 2 && !row.isEmpty()) nav.showLeave(row.getItem().getStatus()); });
            return row;
        });
        return t;
    }

    private Node employmentStatus(List<Employee> employees) {
        Map<EmployeeStatus, Long> by = service.headcountByStatus();
        double total = Math.max(1, employees.size());
        HBox bar = new HBox();
        bar.getStyleClass().add("stack-bar");
        bar.setMinWidth(0);
        VBox legend = new VBox(11);
        for (EmployeeStatus s : EmployeeStatus.values()) {
            Region seg = new Region();
            seg.setStyle("-fx-background-color: " + STATUS_COLORS.get(s) + ";");
            seg.prefWidthProperty().bind(bar.widthProperty().multiply(by.get(s) / total));
            seg.setMinWidth(0);
            bar.getChildren().add(seg);
            legend.getChildren().add(legendRow(STATUS_COLORS.get(s), s.getLabel(), by.get(s)));
        }
        Label note = new Label("Probation: recent hires. On leave: approved leave in progress.");
        note.getStyleClass().add("muted-small");
        note.setWrapText(true);
        return new VBox(14, bar, legend, note);
    }

    private HBox legendRow(String color, String label, long value) {
        Region sw = new Region();
        sw.getStyleClass().add("swatch");
        sw.setStyle("-fx-background-color: " + color + ";");
        Label l = new Label(label);
        l.setMinWidth(Region.USE_PREF_SIZE);
        Label v = new Label(String.valueOf(value));
        v.setMinWidth(Region.USE_PREF_SIZE);
        v.getStyleClass().add("legend-value");
        HBox row = new HBox(8, sw, l, Ui.spacer(), v);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    static <X, Y> void colorNode(XYChart.Data<X, Y> d, String color) {
        if (d.getNode() != null) d.getNode().setStyle("-fx-bar-fill: " + color + ";");
        d.nodeProperty().addListener((o, a, n) -> { if (n != null) n.setStyle("-fx-bar-fill: " + color + ";"); });
    }
}
