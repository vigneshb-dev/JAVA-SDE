package com.backendlearninglab.repository;

import com.backendlearninglab.exception.DatabaseOperationException;
import com.backendlearninglab.model.User;
import com.backendlearninglab.model.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Pure JDBC implementation of {@link UserRepository}.
 */
public class JdbcUserRepository implements UserRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcUserRepository.class);
    private final DataSource dataSource;

    public JdbcUserRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public User save(User user) {
        if (user.getId() == null) {
            return insert(user);
        } else {
            return update(user);
        }
    }

    private User insert(User user) {
        String sql = "INSERT INTO users (email, password_hash, full_name, role, status, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            Instant now = Instant.now();
            pstmt.setString(1, user.getEmail());
            pstmt.setString(2, user.getPasswordHash());
            pstmt.setString(3, user.getFullName());
            pstmt.setString(4, user.getRole().name());
            pstmt.setString(5, user.getStatus());
            pstmt.setTimestamp(6, Timestamp.from(now));
            pstmt.setTimestamp(7, Timestamp.from(now));

            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getLong(1));
                    user.setCreatedAt(now);
                    user.setUpdatedAt(now);
                } else {
                    throw new DatabaseOperationException("Creating user failed, no ID obtained.");
                }
            }
            return user;

        } catch (SQLException e) {
            throw new DatabaseOperationException("Error inserting user: " + user.getEmail(), e);
        }
    }

    private User update(User user) {
        String sql = "UPDATE users SET full_name = ?, role = ?, status = ?, updated_at = ? WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            Instant now = Instant.now();
            pstmt.setString(1, user.getFullName());
            pstmt.setString(2, user.getRole().name());
            pstmt.setString(3, user.getStatus());
            pstmt.setTimestamp(4, Timestamp.from(now));
            pstmt.setLong(5, user.getId());

            int affected = pstmt.executeUpdate();
            if (affected == 0) {
                throw new DatabaseOperationException("User update failed, user not found with id: " + user.getId());
            }
            user.setUpdatedAt(now);
            return user;

        } catch (SQLException e) {
            throw new DatabaseOperationException("Error updating user ID: " + user.getId(), e);
        }
    }

    @Override
    public Optional<User> findById(Long id) {
        String sql = "SELECT id, email, password_hash, full_name, role, status, created_at, updated_at " +
                     "FROM users WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding user by ID: " + id, e);
        }
    }

    @Override
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT id, email, password_hash, full_name, role, status, created_at, updated_at " +
                     "FROM users WHERE email = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding user by email: " + email, e);
        }
    }

    @Override
    public List<User> findAll() {
        String sql = "SELECT id, email, password_hash, full_name, role, status, created_at, updated_at FROM users ORDER BY id";
        List<User> list = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToUser(rs));
            }
            return list;

        } catch (SQLException e) {
            throw new DatabaseOperationException("Error fetching all users", e);
        }
    }

    @Override
    public boolean updateStatus(Long userId, String newStatus) {
        String sql = "UPDATE users SET status = ?, updated_at = ? WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newStatus);
            pstmt.setTimestamp(2, Timestamp.from(Instant.now()));
            pstmt.setLong(3, userId);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseOperationException("Error updating status for user ID: " + userId, e);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        String sql = "DELETE FROM users WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseOperationException("Error deleting user ID: " + id, e);
        }
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        Timestamp createdTs = rs.getTimestamp("created_at");
        Timestamp updatedTs = rs.getTimestamp("updated_at");

        return new User(
                rs.getLong("id"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getString("full_name"),
                UserRole.fromString(rs.getString("role")),
                rs.getString("status"),
                createdTs != null ? createdTs.toInstant() : Instant.now(),
                updatedTs != null ? updatedTs.toInstant() : Instant.now()
        );
    }
}
