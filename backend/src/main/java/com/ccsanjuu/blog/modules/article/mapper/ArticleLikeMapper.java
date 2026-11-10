package com.ccsanjuu.blog.modules.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccsanjuu.blog.modules.article.model.entity.Article;
import com.ccsanjuu.blog.modules.article.model.entity.ArticleLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ArticleLikeMapper extends BaseMapper<ArticleLike> {

    /**
     * 分页查询当前用户点赞且仍公开可见的文章。
     *
     * @param page 分页参数
     * @param userId 用户 ID
     * @return 文章分页结果
     */
    Page<Article> selectLikedArticlePage(Page<Article> page, @Param("userId") Long userId);

    /**
     * 查询文章当前主体的点赞记录。
     *
     * @param articleId 文章 ID
     * @param userId 登录用户 ID
     * @param visitorTokenHash 游客身份哈希
     * @return 点赞记录，不存在时返回 {@code null}
     */
    ArticleLike selectByActor(
            @Param("articleId") Long articleId,
            @Param("userId") Long userId,
            @Param("visitorTokenHash") String visitorTokenHash
    );

    /**
     * 插入点赞记录；同一主体重复点赞时保持幂等。
     *
     * @param articleLike 点赞记录
     * @return 插入行数
     */
    int insertIgnore(ArticleLike articleLike);

    /**
     * 删除文章当前主体的点赞记录。
     *
     * @param articleId 文章 ID
     * @param userId 登录用户 ID
     * @param visitorTokenHash 游客身份哈希
     * @return 删除行数
     */
    int deleteByActor(
            @Param("articleId") Long articleId,
            @Param("userId") Long userId,
            @Param("visitorTokenHash") String visitorTokenHash
    );
}
