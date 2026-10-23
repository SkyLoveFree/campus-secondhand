package com.campus.secondhand.entity;

import java.time.LocalDateTime;

/**
 * 用户实体：只映射数据库 {@code user} 表，不直接作为接口返回值（对外统一用 {@code UserVO}）。
 *
 * <p>passwordHash 仅用于登录校验，禁止出现在响应、日志与 toString 中。</p>
 */
public class User {

    private Long id;
    private String username;
    private String passwordHash;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * 日志友好输出：刻意不包含 passwordHash，避免哈希泄漏。
     */
    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', role='" + role + "'}";
    }
}
