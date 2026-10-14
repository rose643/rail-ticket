package com.ahrose.railticket.user.mapper;

import com.ahrose.railticket.bean.SysUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** 系统用户数据库访问接口。 */
@Mapper
public interface UserMapper extends BaseMapper<SysUser> {
}
