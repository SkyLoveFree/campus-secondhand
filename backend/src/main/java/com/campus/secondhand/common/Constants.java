package com.campus.secondhand.common;

/**
 * 全局公共常量：Session 属性名、角色值、BCrypt 强度。
 *
 * <p>集中定义避免魔法字符串；Session 属性仅后端读写，前端通过 Cookie 携带 JSESSIONID。</p>
 */
public final class Constants {

    /** Session 属性：当前用户 ID（Long） */
    public static final String SESSION_USER_ID = "currentUserId";

    /** Session 属性：当前用户名（String） */
    public static final String SESSION_USERNAME = "currentUsername";

    /** Session 属性：当前用户角色（String，USER/ADMIN） */
    public static final String SESSION_ROLE = "currentRole";

    /** 普通用户角色 */
    public static final String ROLE_USER = "USER";

    /** 管理员角色（由 sql/data.sql 种子数据创建，注册接口不可创建） */
    public static final String ROLE_ADMIN = "ADMIN";

    /** BCrypt 加密强度（cost），与 docs/database-design.md 约定一致 */
    public static final int BCRYPT_STRENGTH = 10;

    private Constants() {
    }
}
