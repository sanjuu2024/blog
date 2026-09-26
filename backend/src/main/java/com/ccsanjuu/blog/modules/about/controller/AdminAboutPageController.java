package com.ccsanjuu.blog.modules.about.controller;

import com.ccsanjuu.blog.common.api.Result;
import com.ccsanjuu.blog.modules.audit.annotation.AdminAudit;
import com.ccsanjuu.blog.modules.audit.model.enums.AdminAuditAction;
import com.ccsanjuu.blog.modules.audit.model.enums.AdminAuditResourceType;
import com.ccsanjuu.blog.modules.auth.model.security.JwtPrincipal;
import com.ccsanjuu.blog.modules.about.model.dto.UpdateAboutPageRequestDTO;
import com.ccsanjuu.blog.modules.about.model.vo.AdminAboutPageVO;
import com.ccsanjuu.blog.modules.about.service.AboutPageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/about")
@RequiredArgsConstructor
@Tag(name = "关于页管理接口")
public class AdminAboutPageController {

    private final AboutPageService aboutPageService;

    /**
     * 获取关于页编辑数据
     *
     * @return
     */
    @GetMapping
    @Operation(description = "获取关于页编辑数据")
    public Result<AdminAboutPageVO> getAboutPage() {
        return Result.success(aboutPageService.getAdminAboutPage());
    }

    /**
     * 保存关于页
     *
     * @param requestDTO
     * @param jwtPrincipal
     * @return
     */
    @PutMapping
    @Operation(description = "保存关于页")
    @AdminAudit(
            resourceType = AdminAuditResourceType.ABOUT_PAGE,
            action = AdminAuditAction.UPDATE,
            resourceId = "'ABOUT'"
    )
    public Result<AdminAboutPageVO> upsertAboutPage(
            @Valid @RequestBody UpdateAboutPageRequestDTO requestDTO,
            @AuthenticationPrincipal JwtPrincipal jwtPrincipal
    ) {
        return Result.success(aboutPageService.upsertAboutPage(jwtPrincipal.userId(), requestDTO));
    }
}
