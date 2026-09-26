package com.ccsanjuu.blog.modules.article.model.bo;

public record ArticleViewIdentity(Long userId, String visitorToken) {

    public boolean isAuthenticated() {
        return userId != null;
    }
}
