package com.ccsanjuu.blog.modules.privacy.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("blog_privacy_policy_version")
public class PrivacyPolicyVersion {

    @TableId(type = IdType.INPUT)
    private String version;

    private String contentMd;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
}
