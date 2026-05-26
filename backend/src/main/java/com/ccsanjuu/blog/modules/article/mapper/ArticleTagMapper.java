package com.ccsanjuu.blog.modules.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccsanjuu.blog.modules.article.model.entity.ArticleTag;
import jakarta.validation.constraints.Positive;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ArticleTagMapper extends BaseMapper<ArticleTag> {
    /**
     * 批量插入 article_tag 关联关系
     *
     * @param articleId
     * @param tagIds
     */
    void insertBatch(@Param("articleId") Long articleId, @Param("tagIds") List<@Positive(message = "标签 ID 必须大于 0") Long> tagIds);
}
