-- P1 后台管理操作审计日志：只追加、不提供业务更新和删除能力。
CREATE TABLE IF NOT EXISTS blog_admin_audit_log (
    id BIGSERIAL PRIMARY KEY,
    operator_id BIGINT NOT NULL,
    operator_username VARCHAR(20) NOT NULL,
    resource_type VARCHAR(30) NOT NULL,
    resource_id TEXT,
    action VARCHAR(30) NOT NULL,
    action_detail VARCHAR(255),
    result VARCHAR(20) NOT NULL
        CHECK (result IN ('SUCCESS', 'FAILURE')),
    failure_code INTEGER,
    failure_message VARCHAR(255),
    request_method VARCHAR(10) NOT NULL,
    request_path VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_blog_admin_audit_log_created_at
    ON blog_admin_audit_log (created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_blog_admin_audit_log_operator_created
    ON blog_admin_audit_log (operator_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_blog_admin_audit_log_resource_created
    ON blog_admin_audit_log (resource_type, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_blog_admin_audit_log_action_result_created
    ON blog_admin_audit_log (action, result, created_at DESC);
