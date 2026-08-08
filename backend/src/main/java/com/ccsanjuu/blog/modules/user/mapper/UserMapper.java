package com.ccsanjuu.blog.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import org.apache.ibatis.annotations.Param;

public interface UserMapper extends BaseMapper<User> {

    /**
     * 查询并锁定用户记录，串行化刷新登录态与账号安全操作。
     *
     * @param userId 用户 ID
     * @return 用户，不存在时返回 null
     */
    User selectByIdForUpdate(@Param("userId") Long userId);

    /**
     * 原子递增用户的 Access Token 版本号。
     *
     * @param userId 用户 ID
     * @return 递增后的版本号，用户不存在时返回 null
     */
    Long incrementTokenVersion(@Param("userId") Long userId);

}
