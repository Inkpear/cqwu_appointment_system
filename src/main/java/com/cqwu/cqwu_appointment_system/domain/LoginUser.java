package com.cqwu.cqwu_appointment_system.domain;

import com.cqwu.cqwu_appointment_system.entity.User;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Data
@NoArgsConstructor
public class LoginUser implements UserDetails {

    private User user;
    // (可选) 可以添加 token, loginTime 等信息

    public LoginUser(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 将我们的 role 字符串（如 "admin"）转换为 Spring Security 的权限对象
        // Spring Security 的角色检查默认需要 "ROLE_" 前缀
        return Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase()));
    }

    @Override
    public String getPassword() {
        // 我们不使用密码认证，返回 null
        return null;
    }

    @Override
    public String getUsername() {
        return user.getId(); // 使用学号/工号作为 Spring Security 的 username
    }

    // 账户状态，可以根据需要从 User 实体中获取
    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    @Override
    public boolean isEnabled() { return true; }
}