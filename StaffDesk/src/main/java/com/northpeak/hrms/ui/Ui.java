package com.northpeak.hrms.ui;

import com.northpeak.hrms.model.Employee;
import com.northpeak.hrms.model.EmployeeStatus;
import com.northpeak.hrms.model.LeaveStatus;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/** Small factory methods that keep every screen visually consistent. */
public final class Ui {

    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    public static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH);
    public static final DateTimeFormatter LONG = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH);

    public static final Map<String, String> DEPT_COLORS = Map.of(
            "Engineering", "#1B3360",
            "Finance", "#2D5AA8",
            "Human Resources", "#2A7F8F",
            "Marketing", "#5B8DD6",
            "Operations", "#7A8BAA",
            "Sales", "#3E6FB8");

    // Icon outlines (24x24 grid)
    public static final String IC_DASHBOARD = "M3 3h7v9H3z M14 3h7v5h-7z M14 12h7v9h-7z M3 16h7v5H3z";
    public static final String IC_USERS = "M9 4.5a3.5 3.5 0 1 1 0 7a3.5 3.5 0 1 1 0-7z M2.5 20c.8-3.6 3.4-5.5 6.5-5.5s5.7 1.9 6.5 5.5 M17 6.4a2.6 2.6 0 1 1 0 5.2 M16.5 14.6c2.6.1 4.4 1.8 5 4.4";
    public static final String IC_CALENDAR = "M3 5h18v16H3z M3 10h18 M8 3v4 M16 3v4 M8.5 15l2.2 2.2 4.8-4.6";
    public static final String IC_TREE = "M9 2.5h6v5H9z M2.5 16.5h6v5h-6z M15.5 16.5h6v5h-6z M12 7.5V12 M5.5 16.5V12h13v4.5";
    public static final String IC_CHART = "M4 20V10 M10 20V4 M16 20v-7 M22 20H2";
    public static final String IC_CLOCK = "M12 3a9 9 0 1 1 0 18a9 9 0 1 1 0-18z M12 7v5l3 2";
    public static final String IC_CHECK = "M5 12.5l4.5 4.5L19 7.5";
    public static final String IC_X = "M6 6l12 12 M18 6L6 18";
    public static final String IC_PLUS = "M12 5v14 M5 12h14";
    public static final String IC_EYE = "M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12z M12 9a3 3 0 1 1 0 6a3 3 0 1 1 0-6z";
    public static final String IC_EDIT = "M4 20h4L19 9l-4-4L4 16v4z M13.5 6.5l4 4";
    public static final String IC_TRASH = "M4 7h16 M10 11v6 M14 11v6 M6 7l1 13h10l1-13 M9 7V4h6v3";
    public static final String IC_BACK = "M15 5l-7 7 7 7";
    public static final String IC_SEARCH = "M11 4a7 7 0 1 1 0 14a7 7 0 1 1 0-14z M20 20l-3.5-3.5";
    public static final String IC_LOGO = "M3 20 L9.5 9 L13.3 15 L15.5 11.8 L21 20 Z";

    private Ui() {
    }

    public static SVGPath icon(String path, String... styleClasses) {
        SVGPath p = new SVGPath();
        p.setContent(path);
        p.getStyleClass().add("icon");
        p.getStyleClass().addAll(styleClasses);
        return p;
    }

    /** Icon scaled into a fixed square box. */
    public static StackPane iconBox(String path, double size, String... styleClasses) {
        SVGPath p = icon(path, styleClasses);
        double scale = size / 24.0;
        p.setScaleX(scale);
        p.setScaleY(scale);
        StackPane box = new StackPane(p);
        box.setMinSize(size, size);
        box.setPrefSize(size, size);
        box.setMaxSize(size, size);
        return box;
    }

    public static Label pill(String text, String kind) {
        Label l = new Label(text);
        l.getStyleClass().addAll("pill", "pill-" + kind);
        Circle dot = new Circle(3);
        dot.getStyleClass().add("pill-dot");
        l.setGraphic(dot);
        return l;
    }

    public static Label statusPill(EmployeeStatus s) {
        String kind = switch (s) {
            case ACTIVE -> "ok";
            case PROBATION -> "warn";
            case ON_LEAVE -> "info";
            case INACTIVE -> "idle";
        };
        return pill(s.getLabel(), kind);
    }

    public static Label leavePill(LeaveStatus s) {
        String kind = switch (s) {
            case APPROVED -> "ok";
            case PENDING -> "warn";
            case REJECTED -> "bad";
        };
        return pill(s.getLabel(), kind);
    }

    public static Label tag(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("tag");
        return l;
    }

    public static StackPane avatar(Employee e, double size) {
        Circle c = new Circle(size / 2, Color.web(DEPT_COLORS.getOrDefault(e.getDepartment(), "#2D5AA8")));
        Label l = new Label(e.getInitials());
        l.getStyleClass().add("avatar-text");
        l.setStyle("-fx-font-size: " + Math.round(size * 0.38) + "px;");
        StackPane p = new StackPane(c, l);
        p.setMinSize(size, size);
        p.setMaxSize(size, size);
        return p;
    }

    public static HBox person(Employee e, String subtitle) {
        Label name = new Label(e.getFullName());
        name.getStyleClass().add("person-name");
        VBox text = new VBox(name);
        if (subtitle != null) {
            Label sub = new Label(subtitle);
            sub.getStyleClass().add("muted-small");
            text.getChildren().add(sub);
        }
        text.setAlignment(Pos.CENTER_LEFT);
        HBox box = new HBox(9, avatar(e, 28), text);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    public static VBox card(String title, String subtitle, Node body) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        if (title != null) {
            Label t = new Label(title);
            t.getStyleClass().add("card-title");
            HBox head = new HBox(t, spacer());
            head.setAlignment(Pos.CENTER_LEFT);
            if (subtitle != null) {
                Label s = new Label(subtitle);
                s.getStyleClass().add("muted-small");
                head.getChildren().add(s);
            }
            card.getChildren().add(head);
        }
        if (body != null) {
            card.getChildren().add(body);
            VBox.setVgrow(body, Priority.ALWAYS);
        }
        return card;
    }

    public static VBox kpi(String label, String value, String foot, String iconPath, String tone) {
        Label l = new Label(label);
        l.getStyleClass().add("kpi-label");
        Label v = new Label(value);
        v.getStyleClass().add("kpi-value");
        Label f = new Label(foot);
        f.getStyleClass().add("muted-small");
        f.setWrapText(true);
        StackPane ic = iconBox(iconPath, 20, "icon-" + tone);
        StackPane chip = new StackPane(ic);
        chip.getStyleClass().addAll("kpi-icon", "kpi-icon-" + tone);
        HBox top = new HBox(new VBox(2, l, v), spacer(), chip);
        top.setAlignment(Pos.TOP_LEFT);
        VBox box = new VBox(6, top, f);
        box.getStyleClass().addAll("card", "kpi");
        box.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    public static HBox pageHeader(String crumb, String title, Node... actions) {
        Label c = new Label(crumb);
        c.getStyleClass().add("crumb");
        Label t = new Label(title);
        t.getStyleClass().add("page-title");
        HBox actionBox = new HBox(10, actions);
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        HBox h = new HBox(new VBox(2, c, t), spacer(), actionBox);
        h.setAlignment(Pos.BOTTOM_LEFT);
        return h;
    }

    public static Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    public static VBox page() {
        VBox v = new VBox(16);
        v.getStyleClass().add("page");
        v.setPadding(new Insets(20, 24, 20, 24));
        return v;
    }

    public static String date(LocalDate d) {
        return d == null ? "" : DATE.format(d);
    }

    /** "18 – 22 Sep 2023", "28 Aug – 8 Sep 2023" or a single date. */
    public static String compactRange(LocalDate a, LocalDate b) {
        DateTimeFormatter dm = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);
        if (a.equals(b)) return DATE.format(a);
        if (a.getYear() != b.getYear()) return DATE.format(a) + " – " + DATE.format(b);
        String start = a.getMonth() == b.getMonth() ? String.valueOf(a.getDayOfMonth()) : dm.format(a);
        return start + " – " + dm.format(b) + " " + b.getYear();
    }

    public static String range(LocalDate a, LocalDate b) {
        return SHORT.format(a) + " – " + SHORT.format(b);
    }

    // ---------------------------------------------------------------- table helpers

    /**
     * Sizes table columns: columns marked fixed (IDs, numbers, pills, buttons) keep their width,
     * the other columns share the remaining width in proportion to their preferred widths.
     */
    public static void fit(javafx.scene.control.TableView<?> t) {
        t.setColumnResizePolicy(javafx.scene.control.TableView.UNCONSTRAINED_RESIZE_POLICY);
        double fixed = 0, flexSum = 0;
        for (TableColumn<?, ?> c : t.getColumns()) {
            if (isFixed(c)) fixed += c.getPrefWidth(); else flexSum += c.getPrefWidth();
        }
        for (TableColumn<?, ?> c : t.getColumns()) {
            if (isFixed(c)) {
                c.setMinWidth(c.getPrefWidth());
                c.setMaxWidth(c.getPrefWidth());
            } else {
                double share = c.getPrefWidth() / flexSum;
                c.setMinWidth(c.getPrefWidth() * 0.6);
                c.prefWidthProperty().bind(t.widthProperty().subtract(20 + fixed).multiply(share));
            }
        }
    }

    /** Lets a column (for example a person column) grow and shrink with the table. */
    public static <T, V> TableColumn<T, V> flexible(TableColumn<T, V> c) {
        c.getProperties().remove("fixed");
        return c;
    }

    /** Keeps a column at its preferred width (dates, short codes). */
    public static <T, V> TableColumn<T, V> fixed(TableColumn<T, V> c) {
        c.getProperties().put("fixed", true);
        return c;
    }

    private static boolean isFixed(TableColumn<?, ?> c) {
        return Boolean.TRUE.equals(c.getProperties().get("fixed"));
    }

    public static <T> TableColumn<T, String> textCol(String title, Function<T, String> value, double width) {
        TableColumn<T, String> c = new TableColumn<>(title);
        c.setCellValueFactory(cd -> new SimpleStringProperty(value.apply(cd.getValue())));
        c.setPrefWidth(width);
        return c;
    }

    public static <T> TableColumn<T, String> monoCol(String title, Function<T, String> value, double width) {
        TableColumn<T, String> c = textCol(title, value, width);
        c.getStyleClass().add("mono-col");
        c.getProperties().put("fixed", true);
        return c;
    }

    public static <T> TableColumn<T, String> numCol(String title, Function<T, String> value, double width) {
        TableColumn<T, String> c = textCol(title, value, width);
        c.getStyleClass().add("num-col");
        c.getProperties().put("fixed", true);
        return c;
    }

    public static <T> TableColumn<T, T> nodeCol(String title, Function<T, Node> graphic, double width) {
        TableColumn<T, T> c = new TableColumn<>(title);
        c.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue()));
        c.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty || item == null ? null : graphic.apply(item));
            }
        });
        c.setPrefWidth(width);
        c.setSortable(false);
        c.getProperties().put("fixed", true);
        return c;
    }
}
