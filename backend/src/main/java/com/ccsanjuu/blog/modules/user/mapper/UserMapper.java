package com.ccsanjuu.blog.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserMapper extends BaseMapper<User> {


    /**
     * 查询并锁定用户记录，串行化刷新登录态与账号安全操作。
     *
     * @param userId 用户 ID
     * @return 用户，不存在时返回 null
     */
    User selectByIdForUpdate(@Param("userId") Long userId);

    /**
     * 锁定当前仍可用的管理员账号，用于并发注销保护。
     *
     * @return 未注销且未禁用的管理员 ID
     */
    List<Long> selectActiveAdminIdsForUpdate();

}
