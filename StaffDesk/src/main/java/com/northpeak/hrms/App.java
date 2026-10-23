package com.northpeak.hrms;

import com.northpeak.hrms.db.Database;
import com.northpeak.hrms.db.EmployeeDao;
import com.northpeak.hrms.db.LeaveRequestDao;
import com.northpeak.hrms.service.HrService;
import com.northpeak.hrms.ui.MainWindow;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * NorthPeak HRMS - JavaFX entry point.
 * Data is stored in an embedded H2 database file (./data/northpeak-hrms.mv.db).
 */
public class App extends Application {

    public static final String DB_URL = "jdbc:h2:./data/northpeak-hrms";

    private Database database;

    @Override
    public void start(Stage stage) {
        database = new Database(DB_URL);
        database.initialize();

        HrService service = new HrService(new EmployeeDao(database), new LeaveRequestDao(database));
        MainWindow window = new MainWindow(service, database);

        Scene scene = new Scene(window.getRoot(), 1440, 880);
        scene.getStylesheets().add(App.class.getResource("/css/styles.css").toExternalForm());

        stage.setTitle("NorthPeak HRMS — Administrator Console");
        stage.setScene(scene);
        stage.setMinWidth(1200);
        stage.setMinHeight(760);
        stage.show();
    }

    @Override
    public void stop() {
        if (database != null) {
            database.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
