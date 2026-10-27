package com.ccsanjuu.blog.modules.friendlink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.modules.friendlink.mapper.FriendLinkMapper;
import com.ccsanjuu.blog.modules.friendlink.model.dto.AdminFriendLinkQueryDTO;
import com.ccsanjuu.blog.modules.friendlink.model.dto.FriendLinkUpsertRequestDTO;
import com.ccsanjuu.blog.modules.friendlink.model.dto.FriendLinkUpdateRequestDTO;
import com.ccsanjuu.blog.modules.friendlink.model.entity.FriendLink;
import com.ccsanjuu.blog.modules.friendlink.model.enums.FriendLinkSection;
import com.ccsanjuu.blog.modules.friendlink.model.enums.FriendLinkStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendLinkServiceImplTest {

    private static final long FRIEND_LINK_ID = 110001L;

    @Mock
    private FriendLinkMapper friendLinkMapper;

    private FriendLinkServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FriendLinkServiceImpl(friendLinkMapper);
    }

    @Test
    void createShouldNormalizeOptionalFieldsAndDefaultSortAndStatus() {
        FriendLink requestEntity = validEntity();
        when(friendLinkMapper.exists(any())).thenReturn(false);
        doAnswer(invocation -> {
            FriendLink inserted = invocation.getArgument(0);
            inserted.setId(FRIEND_LINK_ID);
            inserted.setCreatedAt(OffsetDateTime.parse("2026-10-26T00:00:00Z"));
            return 1;
        }).when(friendLinkMapper).insert(any(FriendLink.class));
        when(friendLinkMapper.selectById(FRIEND_LINK_ID)).thenAnswer(invocation -> requestEntity);

        var result = service.createFriendLink(request());

        assertEquals(FRIEND_LINK_ID, result.getId());
        assertEquals(100, requestEntity.getSortNo());
        assertEquals(FriendLinkStatus.ENABLED, requestEntity.getStatus());
        assertEquals("https://example.com/", requestEntity.getUrl());
        assertEquals("", requestEntity.getLogoUrl());
    }

    @Test
    void createShouldRejectUnsafeUrlBeforeDatabaseWrite() {
        FriendLinkUpsertRequestDTO request = request();
        request.setUrl("javascript:alert(1)");

        BizException exception = assertThrows(BizException.class, () -> service.createFriendLink(request));

        assertEquals(ResultCode.PARAM_INVALID, exception.getResultCode());
        verifyNoInteractions(friendLinkMapper);
    }

    @Test
    void createShouldRejectRelativeUrlBeforeDatabaseWrite() {
        FriendLinkUpsertRequestDTO request = request();
        request.setUrl("/relative/path");

        BizException exception = assertThrows(BizException.class, () -> service.createFriendLink(request));

        assertEquals(ResultCode.PARAM_INVALID, exception.getResultCode());
        verifyNoInteractions(friendLinkMapper);
    }

    @Test
    void createShouldRejectOverlongUrlBeforeDatabaseWrite() {
        FriendLinkUpsertRequestDTO request = request();
        request.setUrl("https://example.com/" + "a".repeat(500));

        BizException exception = assertThrows(BizException.class, () -> service.createFriendLink(request));

        assertEquals(ResultCode.PARAM_INVALID, exception.getResultCode());
        verifyNoInteractions(friendLinkMapper);
    }

    @Test
    void createShouldRejectDuplicateUrl() {
        when(friendLinkMapper.exists(any())).thenReturn(true);

        BizException exception = assertThrows(BizException.class, () -> service.createFriendLink(request()));

        assertEquals(ResultCode.FRIEND_LINK_URL_ALREADY_EXISTS, exception.getResultCode());
        verify(friendLinkMapper, never()).insert(any(FriendLink.class));
    }

    @Test
    void updateShouldReportMissingFriendLink() {
        when(friendLinkMapper.selectById(FRIEND_LINK_ID)).thenReturn(null);

        BizException exception = assertThrows(BizException.class,
                () -> service.updateFriendLink(FRIEND_LINK_ID, updateRequest()));

        assertEquals(ResultCode.FRIEND_LINK_NOT_FOUND, exception.getResultCode());
        verify(friendLinkMapper, never()).updateById(any(FriendLink.class));
    }

    @Test
    void deleteShouldRequireExistingFriendLink() {
        when(friendLinkMapper.selectById(FRIEND_LINK_ID)).thenReturn(null);

        BizException exception = assertThrows(BizException.class, () -> service.deleteFriendLink(FRIEND_LINK_ID));

        assertEquals(ResultCode.FRIEND_LINK_NOT_FOUND, exception.getResultCode());
        verify(friendLinkMapper, never()).deleteById(FRIEND_LINK_ID);
    }

    @Test
    void adminListShouldPageAndMapRecords() {
        FriendLink row = validEntity();
        row.setId(FRIEND_LINK_ID);
        when(friendLinkMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenAnswer(invocation -> {
            Page<FriendLink> page = invocation.getArgument(0);
            page.setTotal(1);
            page.setRecords(List.of(row));
            return page;
        });
        AdminFriendLinkQueryDTO query = new AdminFriendLinkQueryDTO();
        query.setKeyword(" example ");

        var result = service.getAdminFriendLinkList(query);

        assertEquals(1, result.getTotal());
        assertEquals(FRIEND_LINK_ID, result.getRecords().getFirst().getId());
    }

    private FriendLinkUpsertRequestDTO request() {
        FriendLinkUpsertRequestDTO request = new FriendLinkUpsertRequestDTO();
        request.setName(" Example ");
        request.setUrl(" https://example.com/ ");
        request.setLogoUrl(" ");
        request.setDescription(" Example site ");
        request.setSection(FriendLinkSection.FRIENDS);
        return request;
    }

    private FriendLinkUpdateRequestDTO updateRequest() {
        FriendLinkUpdateRequestDTO request = new FriendLinkUpdateRequestDTO();
        request.setName("Example");
        request.setUrl("https://example.com");
        request.setDescription("Example site");
        request.setSection(FriendLinkSection.FRIENDS);
        request.setSortNo(100);
        request.setStatus(FriendLinkStatus.ENABLED);
        return request;
    }

    private FriendLink validEntity() {
        return FriendLink.builder()
                .id(FRIEND_LINK_ID)
                .name("Example")
                .url(URI.create("https://example.com/").toString())
                .logoUrl("")
                .description("Example site")
                .section(FriendLinkSection.FRIENDS)
                .sortNo(100)
                .status(FriendLinkStatus.ENABLED)
                .createdAt(OffsetDateTime.parse("2026-10-26T00:00:00Z"))
                .updatedAt(OffsetDateTime.parse("2026-10-26T00:00:00Z"))
                .build();
    }
}
