package com.cqwu.cqwu_appointment_system.domain;

import com.cqwu.cqwu_appointment_system.entity.User;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * @description Spring Security 的核心用户对象 (UserDetails 的实现)。
 *              它封装了我们的业务用户实体 (User)，并提供了 Spring Security 所需的
 *              认证和授权信息（如用户名、权限等）。
 */
@Data
@NoArgsConstructor
public class LoginUser implements UserDetails {

    private User user;

    public LoginUser(User user) {
        this.user = user;
    }

    /**
     * 返回用户的权限集合。
     * Spring Security 的角色检查默认需要 "ROLE_" 前缀。
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase()));
    }

    /**
     * 返回用户的密码。
     * 在此系统中，认证由外部（如CAS）处理，不使用密码，因此返回null。
     */
    @Override
    public String getPassword() {
        return null;
    }

    /**
     * 返回用户的唯一标识，在此系统中为学号/工号。
     */
    @Override
    public String getUsername() {
        return user.getId();
    }

    // 以下为账户状态检查，当前默认全部返回true
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}