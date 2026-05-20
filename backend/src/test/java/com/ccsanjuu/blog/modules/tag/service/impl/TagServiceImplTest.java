package com.ccsanjuu.blog.modules.tag.service.impl;

import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.modules.tag.mapper.TagMapper;
import com.ccsanjuu.blog.modules.tag.model.dto.AdminTagQueryDTO;
import com.ccsanjuu.blog.modules.tag.model.dto.TagUpsertRequestDTO;
import com.ccsanjuu.blog.modules.tag.model.entity.Tag;
import com.ccsanjuu.blog.modules.tag.model.enums.TagStatus;
import com.ccsanjuu.blog.modules.tag.model.vo.AdminTagItemVO;
import com.ccsanjuu.blog.modules.tag.model.vo.CreatedTagVO;
import com.ccsanjuu.blog.modules.tag.model.vo.UpdatedTagVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagServiceImplTest {

    private static final Long TAG_ID = 30001L;
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-05-20T10:00:00+08:00");
    private static final OffsetDateTime UPDATED_AT = OffsetDateTime.parse("2026-05-20T10:30:00+08:00");

    @Mock
    private TagMapper tagMapper;

    private TagServiceImpl tagService;

    @BeforeEach
    void setUp() {
        tagService = new TagServiceImpl(tagMapper);
    }

    @Test
    void getTagListShouldReturnItemsWithArticleCountPlaceholder() {
        when(tagMapper.selectList(any())).thenReturn(List.of(Tag.builder()
                .id(TAG_ID)
                .name("Codeforces")
                .description("Codeforces contests")
                .status(TagStatus.ENABLED)
                .createdAt(CREATED_AT)
                .build()));

        List<AdminTagItemVO> result = tagService.getTagList(AdminTagQueryDTO.builder()
                .keyword("  code  ")
                .status(TagStatus.ENABLED)
                .build());

        assertEquals(1, result.size());
        assertEquals(TAG_ID, result.getFirst().getId());
        assertEquals("Codeforces", result.getFirst().getName());
        assertEquals(TagStatus.ENABLED, result.getFirst().getStatus());
        assertEquals(0, result.getFirst().getArticleCount());
        assertEquals(CREATED_AT, result.getFirst().getCreatedAt());
        verify(tagMapper).selectList(any());
    }

    @Test
    void createTagShouldTrimNameAndReturnGeneratedFields() {
        when(tagMapper.exists(any())).thenReturn(false);
        when(tagMapper.insert(any(Tag.class))).thenAnswer(invocation -> {
            Tag tag = invocation.getArgument(0);
            tag.setId(TAG_ID);
            tag.setCreatedAt(CREATED_AT);
            return 1;
        });

        CreatedTagVO result = tagService.createTag(TagUpsertRequestDTO.builder()
                .name("  AtCoder  ")
                .description("AtCoder contests")
                .status(TagStatus.ENABLED)
                .build());

        ArgumentCaptor<Tag> tagCaptor = ArgumentCaptor.forClass(Tag.class);
        verify(tagMapper).insert(tagCaptor.capture());
        assertEquals("AtCoder", tagCaptor.getValue().getName());
        assertEquals("AtCoder", result.getName());
        assertEquals(TagStatus.ENABLED, result.getStatus());
        assertEquals(CREATED_AT, result.getCreatedAt());
    }

    @Test
    void createTagShouldRejectDuplicateName() {
        when(tagMapper.exists(any())).thenReturn(true);

        BizException exception = assertThrows(BizException.class,
                () -> tagService.createTag(TagUpsertRequestDTO.builder()
                        .name("Codeforces")
                        .status(TagStatus.ENABLED)
                        .build()));

        assertEquals(ResultCode.TAG_NAME_ALREADY_EXISTS, exception.getResultCode());
        verify(tagMapper, never()).insert(any(Tag.class));
    }

    @Test
    void updateTagShouldRejectMissingTag() {
        when(tagMapper.selectById(TAG_ID)).thenReturn(null);

        BizException exception = assertThrows(BizException.class,
                () -> tagService.updateTag(TAG_ID, TagUpsertRequestDTO.builder()
                        .name("Codeforces")
                        .status(TagStatus.ENABLED)
                        .build()));

        assertEquals(ResultCode.TAG_NOT_FOUND, exception.getResultCode());
        verify(tagMapper, never()).exists(any());
        verify(tagMapper, never()).updateById(any(Tag.class));
    }

    @Test
    void updateTagShouldTrimNameAndReturnReloadedTag() {
        when(tagMapper.selectById(TAG_ID)).thenReturn(
                Tag.builder()
                        .id(TAG_ID)
                        .name("Codeforces")
                        .status(TagStatus.ENABLED)
                        .build(),
                Tag.builder()
                        .id(TAG_ID)
                        .name("AtCoder")
                        .status(TagStatus.DISABLED)
                        .updatedAt(UPDATED_AT)
                        .build()
        );
        when(tagMapper.exists(any())).thenReturn(false);

        UpdatedTagVO result = tagService.updateTag(TAG_ID, TagUpsertRequestDTO.builder()
                .name("  AtCoder  ")
                .description("AtCoder contests")
                .status(TagStatus.DISABLED)
                .build());

        ArgumentCaptor<Tag> tagCaptor = ArgumentCaptor.forClass(Tag.class);
        verify(tagMapper).updateById(tagCaptor.capture());
        assertEquals(TAG_ID, tagCaptor.getValue().getId());
        assertEquals("AtCoder", tagCaptor.getValue().getName());
        assertEquals(TAG_ID, result.getId());
        assertEquals("AtCoder", result.getName());
        assertEquals(TagStatus.DISABLED, result.getStatus());
        assertEquals(UPDATED_AT, result.getUpdatedAt());
    }

    @Test
    void deleteTagShouldRejectMissingTag() {
        when(tagMapper.selectById(TAG_ID)).thenReturn(null);

        BizException exception = assertThrows(BizException.class,
                () -> tagService.deleteTag(TAG_ID));

        assertEquals(ResultCode.TAG_NOT_FOUND, exception.getResultCode());
        verify(tagMapper, never()).deleteById(eq(TAG_ID));
    }

    @Test
    void deleteTagShouldDeleteExistingTag() {
        when(tagMapper.selectById(TAG_ID)).thenReturn(Tag.builder()
                .id(TAG_ID)
                .name("Codeforces")
                .status(TagStatus.ENABLED)
                .build());

        tagService.deleteTag(TAG_ID);

        verify(tagMapper).deleteById(eq(TAG_ID));
    }
}
