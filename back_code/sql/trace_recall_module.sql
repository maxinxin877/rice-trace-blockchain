-- =====================================================================
-- 追溯中心 / 召回管理 增量脚本
--
-- 对应需求文档：
--   §六 核心功能 3「数据检索与正反向追溯」
--   §六 核心功能 4「问题追溯与召回管理」
--   §九 ZS-01 正反向追溯、ZS-02 追溯可视化
--   §九 FC-05 召回冻结与销售拦截、FC-06 召回处置闭环
--   §九 JK-06 召回进度监控
--
-- 执行顺序：先执行 init.sql，再执行本脚本；脚本可重复执行（IF NOT EXISTS）。
-- =====================================================================

USE qukuailian;

-- ---------------------------------------------------------------------
-- 1. 召回任务 RiceRecallTask
--    一次召回 = 一条任务，覆盖「发起 → 审批 → 发布冻结 → 处置 → 关闭」全流程
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS rice_recall_task (
    recall_task_id        VARCHAR(32)     NOT NULL                COMMENT '召回任务ID',
    recall_no             VARCHAR(40)     NOT NULL                COMMENT '召回编号(对外公示)',
    trigger_type          VARCHAR(30)     NOT NULL                COMMENT '触发类型: QUALITY_FAIL/CHANNEL_RISK/COMPLAINT/REGULATOR/ENTERPRISE',
    trigger_business_type VARCHAR(30)     NOT NULL DEFAULT ''     COMMENT '触发业务类型',
    trigger_business_id   VARCHAR(32)     NOT NULL DEFAULT ''     COMMENT '触发业务ID(如不合格质检单/问题批次)',
    risk_level            VARCHAR(20)     NOT NULL DEFAULT 'MEDIUM' COMMENT '风险等级: LOW/MEDIUM/HIGH',
    recall_reason         VARCHAR(500)    NOT NULL                COMMENT '召回原因',
    -- 影响范围（由追溯服务自动计算，冗余存储便于列表展示与事后复核）
    affected_batch_count  INT             NOT NULL DEFAULT 0      COMMENT '受影响成品批次数',
    affected_code_count   INT             NOT NULL DEFAULT 0      COMMENT '受影响防伪码数量',
    affected_weight_kg    DECIMAL(14, 2)  NOT NULL DEFAULT 0      COMMENT '受影响产品重量(kg)',
    affected_regions      VARCHAR(500)    NOT NULL DEFAULT ''     COMMENT '受影响销售区域(逗号分隔)',
    -- 状态机：DRAFT -> SUBMITTED -> APPROVED -> RELEASED -> HANDLING -> CLOSED
    --         SUBMITTED -> REJECTED；非发布态可 CANCELLED
    status                VARCHAR(20)     NOT NULL DEFAULT 'DRAFT' COMMENT '任务状态',
    deadline              DATETIME        NULL                    COMMENT '处置截止时间',
    initiated_by          VARCHAR(50)     NOT NULL DEFAULT ''     COMMENT '发起人',
    initiated_at          DATETIME        NULL                    COMMENT '发起时间',
    approved_by           VARCHAR(50)     NOT NULL DEFAULT ''     COMMENT '审批人',
    approved_at           DATETIME        NULL                    COMMENT '审批时间',
    approve_comment       VARCHAR(500)    NOT NULL DEFAULT ''     COMMENT '审批意见',
    released_at           DATETIME        NULL                    COMMENT '发布(冻结生效)时间',
    closed_at             DATETIME        NULL                    COMMENT '关闭时间',
    close_result          VARCHAR(500)    NOT NULL DEFAULT ''     COMMENT '关闭结论',
    chain_status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '链上状态: PENDING/SUCCESS/FAILED',
    tx_id                 VARCHAR(100)    NOT NULL DEFAULT ''     COMMENT '召回指令上链交易ID',
    block_height          BIGINT          NOT NULL DEFAULT 0      COMMENT '区块高度',
    chain_time            DATETIME        NULL                    COMMENT '上链时间',
    chain_error           VARCHAR(500)    NOT NULL DEFAULT ''     COMMENT '上链异常信息',
    create_time           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (recall_task_id),
    UNIQUE KEY uk_recall_no (recall_no),
    KEY idx_status (status),
    KEY idx_trigger (trigger_business_type, trigger_business_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='召回任务';

-- ---------------------------------------------------------------------
-- 2. 召回明细 RiceRecallItem
--    一个任务下挂多条明细（成品批次级 + 防伪码级），逐条跟踪处置进度
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS rice_recall_item (
    item_id          VARCHAR(32)     NOT NULL                COMMENT '明细ID',
    recall_task_id   VARCHAR(32)     NOT NULL                COMMENT '召回任务ID',
    item_type        VARCHAR(20)     NOT NULL                COMMENT '明细类型: PRODUCT_BATCH/TRACE_CODE/GRAIN_BATCH',
    business_id      VARCHAR(64)     NOT NULL                COMMENT '业务ID(成品批次ID/防伪码/原粮批次ID)',
    product_name     VARCHAR(100)    NOT NULL DEFAULT ''     COMMENT '产品名称',
    batch_no         VARCHAR(64)     NOT NULL DEFAULT ''     COMMENT '关联批次号',
    package_spec     VARCHAR(50)     NOT NULL DEFAULT ''     COMMENT '包装规格',
    expected_region  VARCHAR(200)    NOT NULL DEFAULT ''     COMMENT '预期销售区域',
    actual_region    VARCHAR(100)    NOT NULL DEFAULT ''     COMMENT '实际扫码/签收区域',
    quantity         INT             NOT NULL DEFAULT 0      COMMENT '数量(袋/件)',
    weight_kg        DECIMAL(14, 2)  NOT NULL DEFAULT 0      COMMENT '重量(kg)',
    -- 通知与处置双状态：通知负责"告知不可抵赖"，处置负责"实物闭环"
    notify_status    VARCHAR(20)     NOT NULL DEFAULT 'PENDING'   COMMENT '通知状态: PENDING/SENT/CONFIRMED/FAILED',
    notify_channel   VARCHAR(100)    NOT NULL DEFAULT ''         COMMENT '通知渠道(短信/微信/门店APP)',
    notified_at      DATETIME        NULL                        COMMENT '通知时间',
    handle_status    VARCHAR(20)     NOT NULL DEFAULT 'PENDING'   COMMENT '处置状态: PENDING/OFF_SHELF/RECOVERED/ISOLATED/RETURNED/SCRAPPED/UNRECOVERED',
    handle_remark    VARCHAR(500)    NOT NULL DEFAULT ''         COMMENT '处置说明',
    handled_by       VARCHAR(50)     NOT NULL DEFAULT ''         COMMENT '处置人',
    handled_at       DATETIME        NULL                        COMMENT '处置时间',
    frozen           TINYINT         NOT NULL DEFAULT 0          COMMENT '是否已冻结(1=禁止出入库/销售)',
    create_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (item_id),
    KEY idx_task (recall_task_id),
    KEY idx_business (item_type, business_id),
    KEY idx_handle (handle_status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='召回明细';

-- ---------------------------------------------------------------------
-- 3. 风险预警类型扩容（召回类预警复用 rice_risk_warning）
--    warning_type 为 VARCHAR(30)，无需改表；此处仅补充说明：
--    RECALL_TRIGGERED / RECALL_OVERDUE 由召回服务写入。
-- ---------------------------------------------------------------------

-- ---------------------------------------------------------------------
-- 4. 正确性校验（可选）：召回明细必须挂在存在的任务下
--    这里不建外键，保持与 init.sql 其余业务表一致的"应用层约束"风格；
--    追溯查询用到的索引（grain_batch / product_batch / planting_batch /
--    trace_code）在 init.sql 建表时已存在，无需重复创建。
-- ---------------------------------------------------------------------
SELECT COUNT(*) AS recall_task_table_ready FROM information_schema.tables
WHERE table_schema = 'qukuailian' AND table_name = 'rice_recall_task';
