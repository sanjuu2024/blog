package com.ccsanjuu.blog.modules.audit;

import com.ccsanjuu.blog.modules.article.controller.AdminArticleController;
import com.ccsanjuu.blog.modules.audit.annotation.AdminAudit;
import com.ccsanjuu.blog.modules.category.controller.AdminCategoryController;
import com.ccsanjuu.blog.modules.comment.controller.AdminCommentController;
import com.ccsanjuu.blog.modules.file.controller.AdminFileController;
import com.ccsanjuu.blog.modules.message.controller.AdminMessageController;
import com.ccsanjuu.blog.modules.tag.controller.AdminTagController;
import com.ccsanjuu.blog.modules.user.controller.AdminUserController;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class AdminAuditCoverageTest {

    @Test
    void everyAdminMutationEndpointShouldDeclareAuditMetadata() {
        List<Class<?>> controllers = List.of(
                AdminUserController.class,
                AdminArticleController.class,
                AdminCategoryController.class,
                AdminTagController.class,
                AdminCommentController.class,
                AdminMessageController.class,
                AdminFileController.class
        );

        for (Class<?> controller : controllers) {
            for (Method method : controller.getDeclaredMethods()) {
                if (!isMutationEndpoint(method)) {
                    continue;
                }
                assertNotNull(
                        method.getAnnotation(AdminAudit.class),
                        () -> controller.getSimpleName() + "." + method.getName() + " 缺少 @AdminAudit"
                );
            }
        }
    }

    private boolean isMutationEndpoint(Method method) {
        return method.isAnnotationPresent(PostMapping.class)
                || method.isAnnotationPresent(PutMapping.class)
                || method.isAnnotationPresent(PatchMapping.class)
                || method.isAnnotationPresent(DeleteMapping.class);
    }
}
