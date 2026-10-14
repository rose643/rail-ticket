package com.ahrose.railticket.identity.mapper;

import com.ahrose.railticket.bean.UserIdentity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** 用户实名认证数据库访问接口。 */
@Mapper
public interface UserIdentityMapper extends BaseMapper<UserIdentity> {
}
