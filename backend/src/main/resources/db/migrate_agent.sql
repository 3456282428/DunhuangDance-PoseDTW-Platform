-- =============================================================
-- AI Agent 智能体模块：操作审计日志表
-- @author liukang&&heyan
-- =============================================================

CREATE TABLE IF NOT EXISTS ai_agent_log (
    log_id          BIGINT      PRIMARY KEY AUTO_INCREMENT,
    session_id      VARCHAR(32) NOT NULL COMMENT '会话ID',
    user_id         INT         NOT NULL COMMENT '操作者用户ID',
    username        VARCHAR(64) NOT NULL COMMENT '操作者账号',
    user_message    VARCHAR(500) NULL COMMENT '用户原始输入',
    tool_name       VARCHAR(64) NULL COMMENT '触发工具名（纯对话为NULL）',
    tool_args       VARCHAR(500) NULL COMMENT '工具参数JSON',
    confirmed       TINYINT     NOT NULL DEFAULT 0 COMMENT '0无需确认 1已确认 2已拒绝',
    exec_status     VARCHAR(16) NOT NULL COMMENT 'SUCCESS/FAILED/REJECTED/PENDING/CHAT',
    result_summary  VARCHAR(500) NULL COMMENT '执行结果摘要',
    create_time     DATETIME    NOT NULL COMMENT '创建时间',
    KEY idx_session (session_id),
    KEY idx_user_time (user_id, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='Agent智能体操作审计日志';
