package com.ccsanjuu.blog.modules.user.service;

import com.ccsanjuu.blog.modules.user.model.dto.AdminBootstrapRequestDTO;

public interface ProductionAdminBootstrapService {

    /**
     * 在没有管理员的生产数据库中创建首个管理员。
     *
     * @param request 管理员初始化参数
     * @return 新管理员 ID
     */
    Long bootstrap(AdminBootstrapRequestDTO request);
}
