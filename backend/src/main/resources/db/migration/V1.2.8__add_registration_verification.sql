CREATE TABLE blog_privacy_policy_version (
    version VARCHAR(71) PRIMARY KEY,
    content_md TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_blog_privacy_policy_version_format
        CHECK (version ~ '^sha256:[0-9a-f]{64}$'),
    CONSTRAINT chk_blog_privacy_policy_version_content
        CHECK (char_length(content_md) > 0)
);

ALTER TABLE blog_user
    ADD COLUMN privacy_policy_version VARCHAR(71),
    ADD COLUMN privacy_policy_accepted_at TIMESTAMPTZ,
    ADD CONSTRAINT chk_blog_user_privacy_policy_version_format
        CHECK (
            privacy_policy_version IS NULL
            OR privacy_policy_version ~ '^sha256:[0-9a-f]{64}$'
        ),
    ADD CONSTRAINT chk_blog_user_privacy_policy_acceptance_pair
        CHECK (
            (privacy_policy_version IS NULL AND privacy_policy_accepted_at IS NULL)
            OR (privacy_policy_version IS NOT NULL AND privacy_policy_accepted_at IS NOT NULL)
        );
