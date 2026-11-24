-- =====================================================================
-- 扫码采集 / 不合格品处置 / 统一包装编码 增量脚本
--
-- 对应需求文档：
--   §九 CJ-01 扫码终端支持、CJ-02 工位扫码采集、CJ-05 采集校验与防重复、
--        CJ-06 离线采集与补传、CJ-07 人工补录审核
--   §九 FC-04 不合格品处置
--   §九 YM-01 统一编码体系（原料/批次/成品/单品/箱/托盘 + 绑定关系）
--
-- 执行顺序：init.sql → trace_recall_module.sql → 本脚本；可重复执行。
-- =====================================================================

USE qukuailian;

-- ---------------------------------------------------------------------
-- 1. 扫码采集记录 RiceCollectRecord
--    CJ-01/02：扫码枪、PDA、移动端、Web 手工统一落一张表
--    CJ-05：idempotency_key 唯一索引实现"重复提交不重复入库"
--    CJ-06：source=OFFLINE 标记离线补传
--    CJ-07：source=MANUAL 的记录必须走审核（status=PENDING_REVIEW）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS rice_collect_record (
    collect_id        VARCHAR(32)     NOT NULL                COMMENT '采集记录ID',
    idempotency_key   VARCHAR(120)    NOT NULL                COMMENT '幂等键(终端生成: 码+动作+时间戳)',
    collect_code      VARCHAR(64)     NOT NULL                COMMENT '扫到的码',
    code_type         VARCHAR(30)     NOT NULL                COMMENT '码类型: TRACE_CODE/BOX_CODE/PALLET_CODE/PRODUCT_BATCH/GRAIN_BATCH/PLANTING_BATCH/STORAGE_RECEIPT',
    action            VARCHAR(30)     NOT NULL                COMMENT '业务动作: STORE/MILL/ACTIVATE/OUTBOUND/SHELF/SELL/INSPECT',
    terminal_type     VARCHAR(20)     NOT NULL DEFAULT 'WEB'  COMMENT '终端: SCANNER/PDA/MOBILE/WEB',
    operator_name     VARCHAR(50)     NOT NULL DEFAULT ''     COMMENT '操作人',
    collect_time      DATETIME        NOT NULL                COMMENT '扫码时间(终端本地时间)',
    source            VARCHAR(20)     NOT NULL DEFAULT 'ONLINE' COMMENT '来源: ONLINE/OFFLINE/MANUAL',
    business_type     VARCHAR(30)     NOT NULL DEFAULT ''     COMMENT '带出的业务类型',
    business_id       VARCHAR(64)     NOT NULL DEFAULT ''     COMMENT '带出的业务ID',
    snapshot          JSON            NULL                    COMMENT '扫码自动带出的业务数据快照',
    check_result      VARCHAR(20)     NOT NULL DEFAULT 'PASS' COMMENT '校验结果: PASS/WARN/BLOCK',
    check_message     VARCHAR(1000)   NOT NULL DEFAULT ''     COMMENT '校验说明(多条用;分隔)',
    duplicated        TINYINT         NOT NULL DEFAULT 0      COMMENT '是否幂等命中(1=重复提交)',
    status            VARCHAR(20)     NOT NULL DEFAULT 'COLLECTED' COMMENT '状态: COLLECTED/PENDING_REVIEW/APPROVED/REJECTED',
    review_reason     VARCHAR(500)    NOT NULL DEFAULT ''     COMMENT '补录原因(人工补录必填)',
    reviewed_by       VARCHAR(50)     NOT NULL DEFAULT ''     COMMENT '审核人',
    reviewed_at       DATETIME        NULL                    COMMENT '审核时间',
    review_comment    VARCHAR(500)    NOT NULL DEFAULT ''     COMMENT '审核意见',
    create_time       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (collect_id),
    UNIQUE KEY uk_idempotency (idempotency_key),
    KEY idx_code (collect_code),
    KEY idx_action_time (action, collect_time),
    KEY idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='扫码采集记录';

-- ---------------------------------------------------------------------
-- 2. 不合格品处置 RiceNonconformingDisposal
--    FC-04：隔离 / 评审 / 返工 / 返修 / 报废 / 让步接收 / 解除隔离
--    审批通过即冻结批次（isolated=1 + 批次 LOCKED），执行后按结论决定是否解冻
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS rice_nonconforming_disposal (
    disposal_id     VARCHAR(32)     NOT NULL                COMMENT '处置单ID',
    disposal_no     VARCHAR(40)     NOT NULL                COMMENT '处置单号(对外可查)',
    source_type     VARCHAR(30)     NOT NULL                COMMENT '来源: QUALITY_TEST/SCAN_CHECK/INSPECTION/MANUAL/COMPLAINT',
    source_id       VARCHAR(64)     NOT NULL DEFAULT ''     COMMENT '来源单据ID(如质检单)',
    business_type   VARCHAR(30)     NOT NULL                COMMENT '问题对象类型: GRAIN_BATCH/PRODUCT_BATCH/MILLING_BATCH/TRACE_CODE',
    business_id     VARCHAR(64)     NOT NULL                COMMENT '问题对象ID',
    batch_no        VARCHAR(64)     NOT NULL DEFAULT ''     COMMENT '关联批次号',
    product_name    VARCHAR(100)    NOT NULL DEFAULT ''     COMMENT '产品名称',
    defect_type     VARCHAR(30)     NOT NULL                COMMENT '缺陷类型: MOISTURE/IMPURITY/RESIDUE/APPEARANCE/PACKAGE/TEMPERATURE/OTHER',
    defect_desc     VARCHAR(500)    NOT NULL                COMMENT '问题描述',
    risk_level      VARCHAR(20)     NOT NULL DEFAULT 'MEDIUM' COMMENT '风险等级: LOW/MEDIUM/HIGH',
    quantity        INT             NOT NULL DEFAULT 0      COMMENT '涉及数量',
    weight_kg       DECIMAL(14, 2)  NOT NULL DEFAULT 0      COMMENT '涉及重量(kg)',
    disposition     VARCHAR(20)     NOT NULL                COMMENT '处置方式: ISOLATE/REWORK/REPAIR/SCRAP/CONCESSION/RELEASE',
    status          VARCHAR(20)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PENDING_REVIEW/APPROVED/REJECTED/EXECUTED/CLOSED',
    isolated        TINYINT         NOT NULL DEFAULT 0      COMMENT '是否已隔离冻结(1=禁止流转)',
    applicant       VARCHAR(50)     NOT NULL DEFAULT ''     COMMENT '申请人',
    applied_at      DATETIME        NULL                    COMMENT '申请时间',
    reviewer        VARCHAR(50)     NOT NULL DEFAULT ''     COMMENT '审核人',
    reviewed_at     DATETIME        NULL                    COMMENT '审核时间',
    review_comment  VARCHAR(500)    NOT NULL DEFAULT ''     COMMENT '审核意见',
    executed_at     DATETIME        NULL                    COMMENT '执行时间',
    execute_remark  VARCHAR(500)    NOT NULL DEFAULT ''     COMMENT '执行说明',
    chain_status    VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    tx_id           VARCHAR(100)    NOT NULL DEFAULT '',
    block_height    BIGINT          NOT NULL DEFAULT 0,
    chain_time      DATETIME        NULL,
    chain_error     VARCHAR(500)    NOT NULL DEFAULT '',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (disposal_id),
    UNIQUE KEY uk_disposal_no (disposal_no),
    KEY idx_business (business_type, business_id),
    KEY idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='不合格品处置单';

-- ---------------------------------------------------------------------
-- 3. 包装单元（箱 / 托盘） RicePackageUnit
--    YM-01：箱码下挂 N 个单品码，托盘码下挂 N 个箱码，形成编码层级
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS rice_package_unit (
    unit_code           VARCHAR(64)     NOT NULL                COMMENT '箱码/托盘码(业务唯一)',
    unit_level          VARCHAR(10)     NOT NULL                COMMENT '层级: BOX/PALLET',
    parent_code         VARCHAR(64)     NOT NULL DEFAULT ''     COMMENT '上级码(箱→托盘), 空表示顶层',
    product_batch_id    VARCHAR(32)     NOT NULL                COMMENT '所属成品批次',
    package_spec        VARCHAR(50)     NOT NULL DEFAULT ''     COMMENT '包装规格(单袋规格)',
    child_level         VARCHAR(12)     NOT NULL                COMMENT '子级层级: TRACE_CODE/BOX',
    child_count         INT             NOT NULL DEFAULT 0      COMMENT '子码数量',
    total_quantity      INT             NOT NULL DEFAULT 0      COMMENT '折算单品数量',
    status              VARCHAR(20)     NOT NULL DEFAULT 'SEALED' COMMENT '状态: SEALED/ACTIVATED/DISABLED',
    expected_sale_region VARCHAR(200)   NOT NULL DEFAULT ''     COMMENT '预期销售区域',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (unit_code),
    KEY idx_batch (product_batch_id),
    KEY idx_parent (parent_code),
    KEY idx_level (unit_level)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='箱码/托盘码';

CREATE TABLE IF NOT EXISTS rice_package_unit_item (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    unit_code   VARCHAR(64)     NOT NULL                COMMENT '父级码',
    child_code  VARCHAR(64)     NOT NULL                COMMENT '子级码',
    child_level VARCHAR(12)     NOT NULL                COMMENT '子级层级: TRACE_CODE/BOX',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_unit_child (unit_code, child_code),
    KEY idx_child (child_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='包装单元绑定关系';
