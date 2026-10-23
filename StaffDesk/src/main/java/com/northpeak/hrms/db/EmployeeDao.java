package com.northpeak.hrms.db;

import com.northpeak.hrms.model.Employee;
import com.northpeak.hrms.model.EmployeeStatus;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC access to the {@code employee} table. */
public class EmployeeDao {

    private final Database db;

    public EmployeeDao(Database db) {
        this.db = db;
    }

    public List<Employee> findAll() {
        List<Employee> list = new ArrayList<>();
        try (Statement st = db.connection().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM employee ORDER BY id")) {
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Cannot load employees", e);
        }
    }

    public Optional<Employee> findById(String id) {
        try (PreparedStatement ps = db.connection().prepareStatement("SELECT * FROM employee WHERE id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Cannot load employee " + id, e);
        }
    }

    public boolean emailExists(String email, String excludeId) {
        try (PreparedStatement ps = db.connection().prepareStatement(
                "SELECT COUNT(*) FROM employee WHERE LOWER(email) = LOWER(?) AND id <> ?")) {
            ps.setString(1, email);
            ps.setString(2, excludeId == null ? "" : excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Cannot check email", e);
        }
    }

    public String nextId() {
        try (Statement st = db.connection().createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT COALESCE(MAX(CAST(SUBSTRING(id, 5) AS INT)), 1000) FROM employee")) {
            rs.next();
            return "EMP-" + (rs.getInt(1) + 1);
        } catch (SQLException e) {
            throw new DataAccessException("Cannot compute next employee id", e);
        }
    }

    public void insert(Employee e) {
        String sql = """
                INSERT INTO employee (id, first_name, last_name, department, job_title, email, phone,
                                      location, hire_date, status, annual_used_before)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""";
        try (PreparedStatement ps = db.connection().prepareStatement(sql)) {
            ps.setString(1, e.getId());
            bind(ps, e, 2);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("Cannot insert employee " + e.getId(), ex);
        }
    }

    public void update(Employee e) {
        String sql = """
                UPDATE employee SET first_name = ?, last_name = ?, department = ?, job_title = ?, email = ?,
                       phone = ?, location = ?, hire_date = ?, status = ?, annual_used_before = ?
                WHERE id = ?""";
        try (PreparedStatement ps = db.connection().prepareStatement(sql)) {
            bind(ps, e, 1);
            ps.setString(11, e.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("Cannot update employee " + e.getId(), ex);
        }
    }

    public void delete(String id) {
        try (PreparedStatement ps = db.connection().prepareStatement("DELETE FROM employee WHERE id = ?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Cannot delete employee " + id, e);
        }
    }

    private void bind(PreparedStatement ps, Employee e, int start) throws SQLException {
        ps.setString(start, e.getFirstName());
        ps.setString(start + 1, e.getLastName());
        ps.setString(start + 2, e.getDepartment());
        ps.setString(start + 3, e.getJobTitle());
        ps.setString(start + 4, e.getEmail());
        ps.setString(start + 5, e.getPhone());
        ps.setString(start + 6, e.getLocation());
        ps.setDate(start + 7, Date.valueOf(e.getHireDate()));
        ps.setString(start + 8, e.getStatus().name());
        ps.setInt(start + 9, e.getAnnualDaysUsedBefore());
    }

    private Employee map(ResultSet rs) throws SQLException {
        return new Employee(
                rs.getString("id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("department"),
                rs.getString("job_title"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("location"),
                rs.getDate("hire_date").toLocalDate(),
                EmployeeStatus.valueOf(rs.getString("status")),
                rs.getInt("annual_used_before"));
    }
}
