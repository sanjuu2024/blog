package com.ccsanjuu.blog.modules.article.model.bo;

public record ArticleLikeIdentity(Long userId, String visitorToken) {

    /**
     * 判断当前主体是否为登录用户。
     *
     * @return 登录用户返回 {@code true}
     */
    public boolean isAuthenticated() {
        return userId != null;
    }
}
