package com.northpeak.hrms.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Owns the JDBC connection to the embedded H2 database and creates the schema.
 */
public class Database implements AutoCloseable {

    private static final String SCHEMA_EMPLOYEE = """
            CREATE TABLE IF NOT EXISTS employee (
                id                 VARCHAR(12)  PRIMARY KEY,
                first_name         VARCHAR(60)  NOT NULL,
                last_name          VARCHAR(60)  NOT NULL,
                department         VARCHAR(40)  NOT NULL,
                job_title          VARCHAR(80)  NOT NULL,
                email              VARCHAR(120) NOT NULL UNIQUE,
                phone              VARCHAR(30),
                location           VARCHAR(40),
                hire_date          DATE         NOT NULL,
                status             VARCHAR(12)  NOT NULL,
                annual_used_before INT          DEFAULT 0 NOT NULL
            )""";

    private static final String SCHEMA_LEAVE = """
            CREATE TABLE IF NOT EXISTS leave_request (
                id           VARCHAR(12)  PRIMARY KEY,
                employee_id  VARCHAR(12)  NOT NULL REFERENCES employee(id) ON DELETE CASCADE,
                leave_type   VARCHAR(12)  NOT NULL,
                start_date   DATE         NOT NULL,
                end_date     DATE         NOT NULL,
                reason       VARCHAR(255),
                status       VARCHAR(10)  NOT NULL,
                review_note  VARCHAR(255)
            )""";

    private final String url;
    private Connection connection;

    public Database(String url) {
        this.url = url;
    }

    public synchronized Connection connection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(url, "sa", "");
            }
            return connection;
        } catch (SQLException e) {
            throw new DataAccessException("Cannot open the database at " + url, e);
        }
    }

    /** Creates the tables and loads the demo dataset the first time the application starts. */
    public void initialize() {
        execute(SCHEMA_EMPLOYEE);
        execute(SCHEMA_LEAVE);
        if (countEmployees() == 0) {
            DemoData.seed(new EmployeeDao(this), new LeaveRequestDao(this));
        }
    }

    /** Deletes every record and reloads the demo dataset. */
    public void resetDemoData() {
        execute("DELETE FROM leave_request");
        execute("DELETE FROM employee");
        DemoData.seed(new EmployeeDao(this), new LeaveRequestDao(this));
    }

    private int countEmployees() {
        try (Statement st = connection().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM employee")) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new DataAccessException("Cannot count employees", e);
        }
    }

    private void execute(String sql) {
        try (Statement st = connection().createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            throw new DataAccessException("SQL error: " + sql, e);
        }
    }

    @Override
    public synchronized void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {
            // closing on shutdown, nothing else to do
        }
    }
}
