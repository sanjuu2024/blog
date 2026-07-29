package com.ccsanjuu.blog.modules.comment.model.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentReplyCursorBO {

    private OffsetDateTime createdAt;

    private Long id;
}
