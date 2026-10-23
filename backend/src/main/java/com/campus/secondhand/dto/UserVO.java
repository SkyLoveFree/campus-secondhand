package com.campus.secondhand.dto;

/**
 * 当前用户视图对象：注册、登录、获取当前用户接口的统一响应数据。
 *
 * <p>只暴露 id/username/role，绝不包含密码或密码哈希。</p>
 */
public class UserVO {

    private final Long id;
    private final String username;
    private final String role;

    public UserVO(Long id, String username, String role) {
        this.id = id;
        this.username = username;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }
}
