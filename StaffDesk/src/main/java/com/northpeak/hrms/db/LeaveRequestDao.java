package com.northpeak.hrms.db;

import com.northpeak.hrms.model.LeaveRequest;
import com.northpeak.hrms.model.LeaveStatus;
import com.northpeak.hrms.model.LeaveType;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** JDBC access to the {@code leave_request} table. */
public class LeaveRequestDao {

    private final Database db;

    public LeaveRequestDao(Database db) {
        this.db = db;
    }

    public List<LeaveRequest> findAll() {
        List<LeaveRequest> list = new ArrayList<>();
        try (Statement st = db.connection().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM leave_request ORDER BY id DESC")) {
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Cannot load leave requests", e);
        }
    }

    public String nextId() {
        try (Statement st = db.connection().createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT COALESCE(MAX(CAST(SUBSTRING(id, 4) AS INT)), 0) FROM leave_request")) {
            rs.next();
            return String.format("LR-%04d", rs.getInt(1) + 1);
        } catch (SQLException e) {
            throw new DataAccessException("Cannot compute next leave request id", e);
        }
    }

    public void insert(LeaveRequest l) {
        String sql = """
                INSERT INTO leave_request (id, employee_id, leave_type, start_date, end_date, reason, status, review_note)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)""";
        try (PreparedStatement ps = db.connection().prepareStatement(sql)) {
            ps.setString(1, l.getId());
            ps.setString(2, l.getEmployeeId());
            ps.setString(3, l.getType().name());
            ps.setDate(4, Date.valueOf(l.getStartDate()));
            ps.setDate(5, Date.valueOf(l.getEndDate()));
            ps.setString(6, l.getReason());
            ps.setString(7, l.getStatus().name());
            ps.setString(8, l.getReviewNote());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Cannot insert leave request " + l.getId(), e);
        }
    }

    public void updateStatus(String id, LeaveStatus status, String note) {
        try (PreparedStatement ps = db.connection().prepareStatement(
                "UPDATE leave_request SET status = ?, review_note = ? WHERE id = ?")) {
            ps.setString(1, status.name());
            ps.setString(2, note);
            ps.setString(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Cannot update leave request " + id, e);
        }
    }

    private LeaveRequest map(ResultSet rs) throws SQLException {
        return new LeaveRequest(
                rs.getString("id"),
                rs.getString("employee_id"),
                LeaveType.valueOf(rs.getString("leave_type")),
                rs.getDate("start_date").toLocalDate(),
                rs.getDate("end_date").toLocalDate(),
                rs.getString("reason"),
                LeaveStatus.valueOf(rs.getString("status")),
                rs.getString("review_note"));
    }
}
