package com.ccsanjuu.blog.modules.friendlink.controller;

import com.ccsanjuu.blog.common.api.Result;
import com.ccsanjuu.blog.modules.friendlink.model.vo.PublicFriendLinkSectionVO;
import com.ccsanjuu.blog.modules.friendlink.model.vo.PublicFriendLinkSiteProfileVO;
import com.ccsanjuu.blog.modules.friendlink.service.FriendLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/friend-links")
@RequiredArgsConstructor
public class FriendLinkController {

    private final FriendLinkService friendLinkService;

    /**
     * 获取固定分区顺序的启用友链。
     *
     * @return 三个固定分区及其启用友链
     */
    @GetMapping
    public ResponseEntity<Result<List<PublicFriendLinkSectionVO>>> getPublicFriendLinks() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Result.success(friendLinkService.getPublicFriendLinkSections()));
    }

    /**
     * 获取友链申请页展示的本站资料。
     *
     * @return 本站名称、地址、描述和 Logo
     */
    @GetMapping("/site-profile")
    public ResponseEntity<Result<PublicFriendLinkSiteProfileVO>> getPublicSiteProfile() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Result.success(friendLinkService.getPublicFriendLinkSiteProfile()));
    }
}
