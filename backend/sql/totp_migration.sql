-- Google验证器(TOTP)字段迁移脚本
-- 执行前请备份数据库

ALTER TABLE sys_user
    ADD COLUMN totp_secret VARCHAR(64) DEFAULT NULL COMMENT 'Google验证器密钥(Base32)' AFTER pwd_update_date,
    ADD COLUMN totp_enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Google验证器是否启用(0否 1是)' AFTER totp_secret;
