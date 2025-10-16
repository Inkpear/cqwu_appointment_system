package com.cqwu.cqwu_appointment_system.service.impl;

import com.cqwu.cqwu_appointment_system.domain.LoginUser;
import com.cqwu.cqwu_appointment_system.entity.User;
import com.cqwu.cqwu_appointment_system.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userService.selectUserById(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户 '" + username + "' 不存在");
        }
        return new LoginUser(user);
    }
}