package com.campus.secondhand.config;

import com.campus.secondhand.common.Constants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码加密配置：只使用 spring-security-crypto 的 BCrypt，不引入 Spring Security 完整框架。
 */
@Configuration
public class PasswordConfig {

    /**
     * BCrypt 密码编码器，cost 固定 10（与 docs/database-design.md 种子哈希一致）。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(Constants.BCRYPT_STRENGTH);
    }
}
