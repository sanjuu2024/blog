package com.ccsanjuu.blog.modules.auth.service;

/**
 * 管理用户 Access Token 的版本号。
 */
public interface TokenVersionService {

    /**
     * 获取用户当前 tokenVersion；优先读取 Redis，缓存未命中时回源数据库。
     *
     * @param userId 用户 ID
     * @return 当前版本；用户不存在时返回 null
     */
    Long getCurrentVersion(Long userId);

    /**
     * 原子递增用户 tokenVersion，并同步更新 Redis 缓存。
     *
     * @param userId 用户 ID
     * @return 递增后的版本；用户不存在时返回 null
     */
    Long incrementVersion(Long userId);
}
