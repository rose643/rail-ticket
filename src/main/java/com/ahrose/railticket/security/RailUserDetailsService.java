package com.ahrose.railticket.security;

import com.ahrose.railticket.bean.SysUser;
import com.ahrose.railticket.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** 从 sys_user 表加载 Spring Security 用户。 */
@Service
public class RailUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;

    public RailUserDetailsService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
        );
        if (user == null) {
            throw new UsernameNotFoundException("用户名或密码错误");
        }
        return new AuthenticatedUser(user);
    }
}
