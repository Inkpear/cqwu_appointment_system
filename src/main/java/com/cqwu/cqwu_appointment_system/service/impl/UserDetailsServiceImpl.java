package com.cqwu.cqwu_appointment_system.service.impl;

import com.cqwu.cqwu_appointment_system.domain.LoginUser;
import com.cqwu.cqwu_appointment_system.entity.User;
import com.cqwu.cqwu_appointment_system.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("Spring Security 开始根据用户名加载用户信息, username: {}", username);

        User user = userService.selectUserById(username);

        if (user == null) {
            log.warn("加载用户信息失败，用户 '{}' 不存在", username);
            throw new UsernameNotFoundException("用户 '" + username + "' 不存在");
        }

        log.info("成功加载到用户 '{}' 的信息", username);
        return new LoginUser(user);
    }
}