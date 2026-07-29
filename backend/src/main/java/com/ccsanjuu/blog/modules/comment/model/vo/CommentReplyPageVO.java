package com.ccsanjuu.blog.modules.comment.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentReplyPageVO {

    private List<CommentReplyItemVO> records;

    private String nextCursor;

    private boolean hasNext;
}
