package com.ccsanjuu.blog.modules.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccsanjuu.blog.modules.article.model.entity.ArticleTag;
import com.ccsanjuu.blog.modules.article.model.bo.TagArticleCountBO;
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

    /**
     * 获取包含所有标签的文章列表
     *
     * @param tagIds 保证去重且非空
     * @param tagCount tagIds 去重后的长度
     * @return
     */
    List<Long> getArticleIdsByTagIds(@Param("tagIds") List<Long> tagIds, @Param("tagCount") Integer tagCount);

    /**
     * 根据所给的 tagId 们和给定的合法二级分类们，返回每一个 tag 所关联的文章数
     *
     * @param tagIds                 保证非空
     * @param validLevel2CategoryIds 有效的所有二级分类的 id 数组，为 null 或者空列表则不作为查询条件
     * @param published              为 true 则文章数只统计已发表文章，为 false 则文章数统计所有状态的文章
     * @return
     */
    List<TagArticleCountBO> getArticleCountByTagIds(@Param("tagIds") List<Long> tagIds, @Param("categoryIds") List<Long> validLevel2CategoryIds, @Param("published") boolean published);
}
