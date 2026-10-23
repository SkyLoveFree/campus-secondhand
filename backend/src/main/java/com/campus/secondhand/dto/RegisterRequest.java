package com.campus.secondhand.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 注册请求体：{@code username} 3-20 位字母/数字/下划线，{@code password} 6-20 位且无空白字符。
 *
 * <p>username 在 setter 中先 trim，保证 @Valid 校验与入库使用同一份去空格数据；
 * 密码不做 trim（密码中的空白字符本身不合法，由校验拒绝）。</p>
 */
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度需为 3-20 位")
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度需为 6-20 位")
    @Pattern(regexp = "^\\S+$", message = "密码不能包含空白字符")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = trim(username);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
