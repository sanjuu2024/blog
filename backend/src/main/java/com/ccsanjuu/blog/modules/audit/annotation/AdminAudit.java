package com.ccsanjuu.blog.modules.audit.annotation;

import com.ccsanjuu.blog.modules.audit.model.enums.AdminAuditAction;
import com.ccsanjuu.blog.modules.audit.model.enums.AdminAuditResourceType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要写入后台操作审计日志的 Controller 方法。
 *
 * <p>{@code resourceId} 和 {@code detail} 使用 Spring EL，可从方法参数或返回结果中提取
 * 已明确允许记录的非敏感信息，例如 {@code #p0}、{@code #p1.action} 或
 * {@code #result.data.id}。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AdminAudit {

    /**
     * 被操作的资源类型。
     *
     * @return 资源类型
     */
    AdminAuditResourceType resourceType();

    /**
     * 管理操作类型。
     *
     * @return 操作类型
     */
    AdminAuditAction action();

    /**
     * 用于提取目标资源标识的 Spring EL；为空时不记录目标标识。
     *
     * @return 目标资源标识表达式
     */
    String resourceId() default "";

    /**
     * 用于提取状态、审核动作等安全操作明细的 Spring EL；为空时不记录明细。
     *
     * @return 操作明细表达式
     */
    String detail() default "";
}
