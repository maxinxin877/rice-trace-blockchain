-- =====================================================================
-- 基于区块链的水稻产品质量安全溯源系统 - 数据库初始化脚本
-- 对应接口文档 v1.1 第 3 节核心数据对象
-- MySQL 8.0+
-- 说明：种子用户密码均为 123456（MD5(盐+密码)，盐=a1b2c3d4）
-- =====================================================================

CREATE DATABASE IF NOT EXISTS qukuailian DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE qukuailian;

-- =====================================================================
-- 1. 管理端用户（公共认证模块）
-- =====================================================================
DROP TABLE IF EXISTS user;
CREATE TABLE `user` (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(50)     NOT NULL                COMMENT '用户名',
    password    VARCHAR(100)    NOT NULL                COMMENT '密码 MD5(盐+密码)',
    salt        VARCHAR(32)     NOT NULL                COMMENT '加盐',
    nickname    VARCHAR(50)     NOT NULL DEFAULT ''     COMMENT '昵称',
    phone       VARCHAR(20)     NOT NULL DEFAULT ''     COMMENT '手机号',
    role        VARCHAR(30)     NOT NULL DEFAULT 'FARMER' COMMENT '角色: RICE_ADMIN/FARMER/WAREHOUSE_KEEPER/PROCESSING_FACTORY/BRAND_OPERATOR/REGULATOR',
    status      TINYINT         NOT NULL DEFAULT 1      COMMENT '状态: 1正常 0禁用',
    deleted     TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除: 0否 1是',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='管理端用户表';

-- =====================================================================
-- 2. 地块档案 RiceField
-- =====================================================================
DROP TABLE IF EXISTS rice_field;
CREATE TABLE rice_field (
    field_id           VARCHAR(32)     NOT NULL                COMMENT '地块ID',
    field_code         VARCHAR(50)     NOT NULL                COMMENT '地块编号(租户内唯一)',
    field_name         VARCHAR(100)    NOT NULL                COMMENT '地块名称',
    farmer_id          VARCHAR(32)     NOT NULL                COMMENT '种植户ID',
    farmer_name        VARCHAR(50)     NOT NULL                COMMENT '种植户名称',
    province           VARCHAR(50)     NOT NULL                COMMENT '省',
    city               VARCHAR(50)     NOT NULL                COMMENT '市',
    district           VARCHAR(50)     NOT NULL                COMMENT '区县',
    address            VARCHAR(200)    NOT NULL                COMMENT '详细地址',
    area_mu            DECIMAL(12, 2)  NOT NULL                COMMENT '面积(亩)',
    gis_boundary       JSON            NOT NULL                COMMENT 'GIS边界坐标(经纬度点数组)',
    soil_type          VARCHAR(50)     NOT NULL DEFAULT ''     COMMENT '土壤类型',
    base_photo_file_ids JSON           NULL                    COMMENT '基地实景照片文件ID数组',
    coordinate_hash    VARCHAR(100)    NOT NULL DEFAULT ''     COMMENT '地块坐标SHA-256哈希',
    chain_status       VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '上链状态: PENDING/SUCCESS/FAILED',
    tx_id              VARCHAR(100)    NOT NULL DEFAULT ''     COMMENT '区块链交易ID',
    block_height       BIGINT          NOT NULL DEFAULT 0      COMMENT '区块高度',
    chain_time         DATETIME        NULL                    COMMENT '链上时间',
    chain_error        VARCHAR(500)    NOT NULL DEFAULT ''     COMMENT '上链失败原因',
    create_time        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (field_id),
    UNIQUE KEY uk_field_code (field_code),
    KEY idx_farmer (farmer_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='地块档案';

-- =====================================================================
-- 3. 种植批次 RicePlantingBatch
-- =====================================================================
DROP TABLE IF EXISTS rice_planting_batch;
CREATE TABLE rice_planting_batch (
    planting_batch_id       VARCHAR(32)     NOT NULL                COMMENT '种植批次ID',
    field_id                VARCHAR(32)     NOT NULL                COMMENT '地块ID',
    rice_variety            VARCHAR(50)     NOT NULL                COMMENT '水稻品种',
    seed_source             VARCHAR(200)    NOT NULL                COMMENT '种子来源',
    seed_batch_no           VARCHAR(50)     NOT NULL DEFAULT ''     COMMENT '种子批号',
    seed_certificate_file_id VARCHAR(32)    NOT NULL DEFAULT ''     COMMENT '种子证明文件ID',
    sowing_date             DATE            NOT NULL                COMMENT '播种日期',
    expected_harvest_date   DATE            NULL                    COMMENT '预计收割日期',
    actual_harvest_date     DATE            NULL                    COMMENT '实际收割日期',
    status                  VARCHAR(20)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PLANTED/HARVESTED/STORED/MILLING/PACKAGED/ON_SALE/LOCKED',
    organic_certified       TINYINT         NOT NULL DEFAULT 0      COMMENT '是否有机认证',
    green_certified         TINYINT         NOT NULL DEFAULT 0      COMMENT '是否绿色认证',
    certification_file_ids  JSON            NULL                    COMMENT '认证证书文件ID数组',
    chain_status            VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    tx_id                   VARCHAR(100)    NOT NULL DEFAULT '',
    block_height            BIGINT          NOT NULL DEFAULT 0,
    chain_time              DATETIME        NULL,
    chain_error             VARCHAR(500)    NOT NULL DEFAULT '',
    create_time             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (planting_batch_id),
    KEY idx_field (field_id),
    KEY idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='水稻种植批次';

-- =====================================================================
-- 4. 农事记录 RiceFarmingLog
-- =====================================================================
DROP TABLE IF EXISTS rice_farming_log;
CREATE TABLE rice_farming_log (
    log_id             VARCHAR(32)     NOT NULL                COMMENT '农事记录ID',
    planting_batch_id  VARCHAR(32)     NOT NULL                COMMENT '种植批次ID',
    operation_type     VARCHAR(30)     NOT NULL                COMMENT '农事类型: SOWING/FERTILIZATION/PESTICIDE/IRRIGATION/WEEDING/HARVEST',
    operation_time     DATETIME        NOT NULL                COMMENT '操作时间',
    operator_id        VARCHAR(32)     NOT NULL                COMMENT '操作人ID',
    operator_name      VARCHAR(50)     NOT NULL                COMMENT '操作人名称',
    material_name      VARCHAR(100)    NOT NULL DEFAULT ''     COMMENT '投入品名称',
    material_batch_no  VARCHAR(50)     NOT NULL DEFAULT ''     COMMENT '投入品批号',
    material_dosage    DECIMAL(12, 2)  NULL                    COMMENT '使用量',
    material_unit      VARCHAR(20)     NOT NULL DEFAULT ''     COMMENT '单位',
    safe_interval_days INT             NULL                    COMMENT '安全间隔期',
    description        VARCHAR(500)    NOT NULL DEFAULT ''     COMMENT '操作说明',
    proof_file_ids     JSON            NULL                    COMMENT '现场照片/采购凭证文件ID数组',
    data_hash          VARCHAR(100)    NOT NULL DEFAULT ''     COMMENT '记录摘要哈希',
    chain_status       VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    tx_id              VARCHAR(100)    NOT NULL DEFAULT '',
    block_height       BIGINT          NOT NULL DEFAULT 0,
    chain_time         DATETIME        NULL,
    chain_error        VARCHAR(500)    NOT NULL DEFAULT '',
    create_time        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (log_id),
    KEY idx_batch (planting_batch_id),
    KEY idx_type (operation_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='农事记录';

-- =====================================================================
-- 5. 环境数据 RiceEnvironmentRecord
-- =====================================================================
DROP TABLE IF EXISTS rice_environment_record;
CREATE TABLE rice_environment_record (
    environment_record_id VARCHAR(32)     NOT NULL                COMMENT '环境记录ID',
    planting_batch_id     VARCHAR(32)     NOT NULL                COMMENT '种植批次ID',
    source_type           VARCHAR(20)     NOT NULL                COMMENT '数据来源: IOT/DRONE/MANUAL',
    record_time           DATETIME        NOT NULL                COMMENT '记录时间',
    air_temperature       DECIMAL(6, 2)   NULL                    COMMENT '空气温度(℃)',
    air_humidity          DECIMAL(6, 2)   NULL                    COMMENT '空气湿度(%)',
    soil_moisture         DECIMAL(6, 2)   NULL                    COMMENT '土壤湿度(%)',
    rainfall              DECIMAL(8, 2)   NULL                    COMMENT '降雨量(mm)',
    wind_speed            DECIMAL(6, 2)   NULL                    COMMENT '风速(m/s)',
    image_file_ids        JSON            NULL                    COMMENT '无人机/现场图片文件ID数组',
    remark                VARCHAR(500)    NOT NULL DEFAULT ''     COMMENT '备注',
    create_time           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (environment_record_id),
    KEY idx_batch (planting_batch_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='稻田环境数据';

-- =====================================================================
-- 6. 收储入库单 RiceStorageReceipt
-- =====================================================================
DROP TABLE IF EXISTS rice_storage_receipt;
CREATE TABLE rice_storage_receipt (
    storage_receipt_id   VARCHAR(32)     NOT NULL                COMMENT '入库单ID',
    planting_batch_id    VARCHAR(32)     NOT NULL                COMMENT '种植批次ID',
    grain_batch_id       VARCHAR(32)     NOT NULL                COMMENT '原粮批次ID(业务唯一)',
    warehouse_id         VARCHAR(32)     NOT NULL                COMMENT '仓库ID',
    warehouse_code       VARCHAR(50)     NOT NULL                COMMENT '入仓编号',
    harvest_area_mu      DECIMAL(12, 2)  NOT NULL                COMMENT '收割面积(亩)',
    wet_grain_weight_kg  DECIMAL(14, 2)  NOT NULL                COMMENT '湿谷重量(kg)',
    moisture_percent     DECIMAL(6, 2)   NOT NULL                COMMENT '水分含量(%)',
    impurity_percent     DECIMAL(6, 2)   NOT NULL                COMMENT '杂质含量(%)',
    grain_grade          VARCHAR(20)     NOT NULL                COMMENT '原粮等级',
    storage_time         DATETIME        NOT NULL                COMMENT '入库时间',
    temperature          DECIMAL(6, 2)   NULL                    COMMENT '仓储温度(℃)',
    humidity             DECIMAL(6, 2)   NULL                    COMMENT '仓储湿度(%)',
    keeper_signature     VARCHAR(200)    NOT NULL DEFAULT ''     COMMENT '仓库方电子签名',
    farmer_signature     VARCHAR(200)    NOT NULL DEFAULT ''     COMMENT '种植方电子签名',
    quality_report_file_id VARCHAR(32)   NOT NULL DEFAULT ''     COMMENT '入库质检报告文件ID',
    report_hash          VARCHAR(100)    NOT NULL DEFAULT ''     COMMENT '报告文件哈希',
    chain_status         VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    tx_id                VARCHAR(100)    NOT NULL DEFAULT '',
    block_height         BIGINT          NOT NULL DEFAULT 0,
    chain_time           DATETIME        NULL,
    chain_error          VARCHAR(500)    NOT NULL DEFAULT '',
    create_time          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (storage_receipt_id),
    UNIQUE KEY uk_grain_batch (grain_batch_id),
    KEY idx_planting_batch (planting_batch_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='收储入库单';

-- =====================================================================
-- 7. 质检记录 RiceQualityTest
-- =====================================================================
DROP TABLE IF EXISTS rice_quality_test_item;
DROP TABLE IF EXISTS rice_quality_test;
CREATE TABLE rice_quality_test (
    quality_test_id  VARCHAR(32)     NOT NULL                COMMENT '质检记录ID',
    business_type    VARCHAR(20)     NOT NULL                COMMENT '质检归属: STORAGE/MILLING',
    business_id      VARCHAR(32)     NOT NULL                COMMENT '入库单ID或加工批次ID',
    test_agency      VARCHAR(100)    NOT NULL                COMMENT '检测机构',
    test_time        DATETIME        NOT NULL                COMMENT '检测时间',
    overall_result   VARCHAR(20)     NOT NULL                COMMENT '总体结果: PASS/FAIL/PENDING',
    report_file_id   VARCHAR(32)     NOT NULL DEFAULT ''     COMMENT '质检报告文件ID',
    report_hash      VARCHAR(100)    NOT NULL DEFAULT ''     COMMENT '报告SHA-256',
    remark           VARCHAR(500)    NOT NULL DEFAULT ''     COMMENT '备注',
    chain_status     VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    tx_id            VARCHAR(100)    NOT NULL DEFAULT '',
    block_height     BIGINT          NOT NULL DEFAULT 0,
    chain_time       DATETIME        NULL,
    chain_error      VARCHAR(500)    NOT NULL DEFAULT '',
    create_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (quality_test_id),
    KEY idx_business (business_type, business_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='质检记录';

CREATE TABLE rice_quality_test_item (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    quality_test_id  VARCHAR(32)     NOT NULL                COMMENT '质检记录ID',
    item_name        VARCHAR(50)     NOT NULL                COMMENT '检测项目',
    item_value       DECIMAL(12, 3)  NOT NULL                COMMENT '检测值',
    unit             VARCHAR(20)     NOT NULL DEFAULT ''     COMMENT '单位',
    standard_value   VARCHAR(50)     NOT NULL DEFAULT ''     COMMENT '标准值',
    result           VARCHAR(20)     NOT NULL                COMMENT '单项结果: PASS/FAIL',
    PRIMARY KEY (id),
    KEY idx_test (quality_test_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='质检项目明细';

-- =====================================================================
-- 8. 碾米加工批次 RiceMillingBatch
-- =====================================================================
DROP TABLE IF EXISTS rice_milling_batch;
CREATE TABLE rice_milling_batch (
    milling_batch_id      VARCHAR(32)     NOT NULL                COMMENT '加工批次ID',
    grain_batch_id        VARCHAR(32)     NOT NULL                COMMENT '原粮批次ID',
    product_batch_id      VARCHAR(32)     NOT NULL                COMMENT '成品米批次ID',
    factory_id            VARCHAR(32)     NOT NULL                COMMENT '加工厂ID',
    grain_out_weight_kg   DECIMAL(14, 2)  NOT NULL                COMMENT '稻谷出库量(kg)',
    rice_output_weight_kg DECIMAL(14, 2)  NULL                    COMMENT '精米产出量(kg)',
    yield_rate            DECIMAL(6, 2)   NULL                    COMMENT '精米产出率(%)',
    process_start_time    DATETIME        NOT NULL                COMMENT '加工开始时间',
    process_end_time      DATETIME        NULL                    COMMENT '加工结束时间',
    process_params        JSON            NOT NULL                COMMENT '脱壳/抛光/色选/包装参数',
    quality_summary       VARCHAR(500)    NOT NULL DEFAULT ''     COMMENT '出厂质检摘要',
    quality_report_file_id VARCHAR(32)    NOT NULL DEFAULT ''     COMMENT '出厂质检报告文件ID',
    chain_status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    tx_id                 VARCHAR(100)    NOT NULL DEFAULT '',
    block_height          BIGINT          NOT NULL DEFAULT 0,
    chain_time            DATETIME        NULL,
    chain_error           VARCHAR(500)    NOT NULL DEFAULT '',
    create_time           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (milling_batch_id),
    KEY idx_grain (grain_batch_id),
    KEY idx_product (product_batch_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='碾米加工批次';

-- =====================================================================
-- 9. 成品米批次 RiceProductBatch
-- =====================================================================
DROP TABLE IF EXISTS rice_product_batch;
CREATE TABLE rice_product_batch (
    product_batch_id       VARCHAR(32)     NOT NULL                COMMENT '成品米批次ID(业务唯一)',
    product_name           VARCHAR(100)    NOT NULL                COMMENT '产品名称',
    rice_variety           VARCHAR(50)     NOT NULL                COMMENT '水稻品种',
    brand_name             VARCHAR(100)    NOT NULL                COMMENT '品牌名称',
    package_spec           VARCHAR(50)     NOT NULL                COMMENT '包装规格',
    standard_no            VARCHAR(50)     NOT NULL                COMMENT '执行标准号',
    nutrition_facts        JSON            NULL                    COMMENT '营养成分',
    certification_file_ids JSON            NULL                    COMMENT '认证证书文件ID数组',
    expected_sale_region   VARCHAR(200)    NOT NULL DEFAULT ''     COMMENT '预期销售区域',
    status                 VARCHAR(20)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PACKAGED/ON_SALE',
    create_time            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (product_batch_id),
    KEY idx_brand (brand_name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='成品米批次';

-- =====================================================================
-- 10. 防伪码 RiceTraceCode
-- =====================================================================
DROP TABLE IF EXISTS rice_trace_code;
CREATE TABLE rice_trace_code (
    id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    trace_code           VARCHAR(50)     NOT NULL                COMMENT '防伪码',
    qr_code_url          VARCHAR(255)    NOT NULL DEFAULT ''     COMMENT '二维码图片URL',
    product_batch_id     VARCHAR(32)     NOT NULL                COMMENT '成品米批次ID',
    package_spec         VARCHAR(50)     NOT NULL                COMMENT '包装规格',
    status               VARCHAR(20)     NOT NULL DEFAULT 'GENERATED' COMMENT '状态: GENERATED/ACTIVATED/DISABLED/RISK',
    expire_days          INT             NOT NULL DEFAULT 0      COMMENT '二维码有效期天数, 0不限',
    activated_at         DATETIME        NULL                    COMMENT '激活时间',
    first_scanned_at     DATETIME        NULL                    COMMENT '首次扫码时间',
    scan_count           INT             NOT NULL DEFAULT 0      COMMENT '累计扫码次数',
    expected_sale_region VARCHAR(200)    NOT NULL DEFAULT ''     COMMENT '预期销售区域',
    last_scan_region     VARCHAR(100)    NOT NULL DEFAULT ''     COMMENT '最近扫码区域',
    risk_level           VARCHAR(20)     NOT NULL DEFAULT 'LOW'  COMMENT '风险等级: LOW/MEDIUM/HIGH',
    chain_status         VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    tx_id                VARCHAR(100)    NOT NULL DEFAULT '',
    block_height         BIGINT          NOT NULL DEFAULT 0,
    chain_time           DATETIME        NULL,
    chain_error          VARCHAR(500)    NOT NULL DEFAULT '',
    create_time          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_trace_code (trace_code),
    KEY idx_product (product_batch_id),
    KEY idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='防伪码';

-- =====================================================================
-- 11. 扫码日志 RiceScanLog
-- =====================================================================
DROP TABLE IF EXISTS rice_scan_log;
CREATE TABLE rice_scan_log (
    scan_log_id      VARCHAR(32)     NOT NULL                COMMENT '扫码日志ID',
    trace_code       VARCHAR(50)     NOT NULL                COMMENT '防伪码',
    product_batch_id VARCHAR(32)     NOT NULL DEFAULT ''     COMMENT '成品米批次ID',
    scan_time        DATETIME        NOT NULL                COMMENT '扫码时间',
    lng              DECIMAL(10, 6)  NULL                    COMMENT '扫码经度',
    lat              DECIMAL(10, 6)  NULL                    COMMENT '扫码纬度',
    region           VARCHAR(100)    NOT NULL DEFAULT ''     COMMENT '扫码地区',
    device_id        VARCHAR(100)    NOT NULL DEFAULT ''     COMMENT '设备指纹',
    openid           VARCHAR(64)     NOT NULL DEFAULT ''     COMMENT '微信openid',
    scene            VARCHAR(50)     NOT NULL DEFAULT ''     COMMENT '扫码场景',
    authentic        TINYINT         NOT NULL DEFAULT 1      COMMENT '是否正品',
    first_scan       TINYINT         NOT NULL DEFAULT 0      COMMENT '是否首次扫码',
    risk_level       VARCHAR(20)     NOT NULL DEFAULT 'LOW'  COMMENT '风险等级',
    create_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (scan_log_id),
    KEY idx_trace_code (trace_code),
    KEY idx_time (scan_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='扫码日志';

-- =====================================================================
-- 12. 风险预警 RiceRiskWarning
-- =====================================================================
DROP TABLE IF EXISTS rice_risk_warning;
CREATE TABLE rice_risk_warning (
    warning_id       VARCHAR(32)     NOT NULL                COMMENT '预警ID',
    warning_type     VARCHAR(30)     NOT NULL                COMMENT '预警类型: YIELD_BALANCE/CHANNEL_CONFLICT/REPEAT_SCAN/CHAIN_VERIFY_FAILED',
    risk_level       VARCHAR(20)     NOT NULL                COMMENT '风险等级: LOW/MEDIUM/HIGH',
    business_type    VARCHAR(30)     NOT NULL                COMMENT '关联业务类型',
    business_id      VARCHAR(32)     NOT NULL                COMMENT '关联业务ID',
    warning_content  VARCHAR(500)    NOT NULL                COMMENT '预警内容',
    handled          TINYINT         NOT NULL DEFAULT 0      COMMENT '是否已处理',
    handled_by       VARCHAR(50)     NOT NULL DEFAULT ''     COMMENT '处理人',
    handled_at       DATETIME        NULL                    COMMENT '处理时间',
    handle_result    VARCHAR(500)    NOT NULL DEFAULT ''     COMMENT '处理结果',
    chain_status     VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    tx_id            VARCHAR(100)    NOT NULL DEFAULT '',
    block_height     BIGINT          NOT NULL DEFAULT 0,
    chain_time       DATETIME        NULL,
    chain_error      VARCHAR(500)    NOT NULL DEFAULT '',
    create_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (warning_id),
    KEY idx_type (warning_type),
    KEY idx_handled (handled)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='风险预警';

-- =====================================================================
-- 13. 审计日志 RiceAuditLog
-- =====================================================================
DROP TABLE IF EXISTS rice_audit_log;
CREATE TABLE rice_audit_log (
    audit_id       VARCHAR(32)     NOT NULL                COMMENT '审计日志ID',
    business_type  VARCHAR(30)     NOT NULL                COMMENT '业务类型',
    business_id    VARCHAR(32)     NOT NULL                COMMENT '业务ID',
    operation_type VARCHAR(20)     NOT NULL                COMMENT '操作类型: CREATE/UPDATE/DELETE/VERIFY/HANDLE',
    operator_id    VARCHAR(32)     NOT NULL                COMMENT '操作人ID',
    operator_name  VARCHAR(50)     NOT NULL                COMMENT '操作人名称',
    before_hash    VARCHAR(100)    NOT NULL DEFAULT ''     COMMENT '修改前摘要',
    after_hash     VARCHAR(100)    NOT NULL DEFAULT ''     COMMENT '修改后摘要',
    reason         VARCHAR(500)    NOT NULL DEFAULT ''     COMMENT '修改原因',
    ip             VARCHAR(50)     NOT NULL DEFAULT ''     COMMENT '操作IP',
    operation_time DATETIME        NOT NULL                COMMENT '操作时间',
    tx_id          VARCHAR(100)    NOT NULL DEFAULT ''     COMMENT '审计摘要上链交易ID',
    create_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (audit_id),
    KEY idx_business (business_type, business_id),
    KEY idx_time (operation_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='审计日志';

-- =====================================================================
-- 14. 链上存证 RiceChainProof
-- =====================================================================
DROP TABLE IF EXISTS rice_chain_proof;
CREATE TABLE rice_chain_proof (
    chain_proof_id  VARCHAR(32)     NOT NULL                COMMENT '存证记录ID',
    business_type   VARCHAR(30)     NOT NULL                COMMENT '业务类型: FIELD/PLANTING_BATCH/FARMING_LOG/STORAGE_RECEIPT/QUALITY_TEST/MILLING_BATCH/TRACE_CODE/RISK_WARNING/YIELD_BALANCE/AUDIT_LOG',
    business_id     VARCHAR(32)     NOT NULL                COMMENT '业务ID',
    data_hash       VARCHAR(100)    NOT NULL                COMMENT '业务摘要哈希',
    file_hashes     JSON            NULL                    COMMENT '关联文件哈希数组',
    tx_id           VARCHAR(100)    NOT NULL DEFAULT ''     COMMENT '区块链交易ID',
    block_height    BIGINT          NOT NULL DEFAULT 0      COMMENT '区块高度',
    chain_time      DATETIME        NULL                    COMMENT '链上时间',
    chain_status    VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '上链状态: PENDING/SUCCESS/FAILED',
    chain_error     VARCHAR(500)    NOT NULL DEFAULT ''     COMMENT '上链失败原因',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (chain_proof_id),
    UNIQUE KEY uk_business (business_type, business_id),
    KEY idx_status (chain_status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='链上存证';

-- =====================================================================
-- 15. 文件资源 FileResource
-- =====================================================================
DROP TABLE IF EXISTS file_resource;
CREATE TABLE file_resource (
    file_id      VARCHAR(32)     NOT NULL                COMMENT '文件ID',
    file_name    VARCHAR(200)    NOT NULL                COMMENT '原始文件名',
    file_type    VARCHAR(100)    NOT NULL                COMMENT 'MIME类型',
    file_size    BIGINT          NOT NULL                COMMENT '文件大小(字节)',
    file_url     VARCHAR(255)    NOT NULL                COMMENT '文件访问地址',
    sha256       VARCHAR(100)    NOT NULL                COMMENT '文件SHA-256',
    storage_type VARCHAR(20)     NOT NULL DEFAULT 'LOCAL' COMMENT '存储类型: LOCAL/MINIO/IPFS',
    biz_type     VARCHAR(30)     NOT NULL                COMMENT '文件业务类型: FIELD_PHOTO/SEED_CERT/INPUT_PROOF/QUALITY_REPORT/CERTIFICATION',
    biz_id       VARCHAR(32)     NOT NULL DEFAULT ''     COMMENT '关联业务ID',
    create_time  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (file_id),
    UNIQUE KEY uk_sha256 (sha256),
    KEY idx_biz (biz_type, biz_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='文件资源';

-- =====================================================================
-- 16. 产量平衡校验结果
-- =====================================================================
DROP TABLE IF EXISTS rice_yield_balance;
CREATE TABLE rice_yield_balance (
    check_id           VARCHAR(32)     NOT NULL                COMMENT '校验ID',
    check_scope        VARCHAR(30)     NOT NULL                COMMENT '校验范围: PLANTING_TO_STORAGE/STORAGE_TO_MILLING/FULL_CHAIN',
    planting_batch_id  VARCHAR(32)     NOT NULL DEFAULT ''     COMMENT '种植批次ID',
    grain_batch_id     VARCHAR(32)     NOT NULL DEFAULT ''     COMMENT '原粮批次ID',
    product_batch_id   VARCHAR(32)     NOT NULL DEFAULT ''     COMMENT '成品米批次ID',
    result             VARCHAR(20)     NOT NULL                COMMENT '结果: PASS/FAIL',
    items              JSON            NULL                    COMMENT '校验明细',
    chain_status       VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    tx_id              VARCHAR(100)    NOT NULL DEFAULT '',
    block_height       BIGINT          NOT NULL DEFAULT 0,
    chain_time         DATETIME        NULL,
    chain_error        VARCHAR(500)    NOT NULL DEFAULT '',
    create_time        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (check_id),
    KEY idx_result (result)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='产量平衡校验结果';

-- =====================================================================
-- 17. 防伪码生成任务
-- =====================================================================
DROP TABLE IF EXISTS rice_generate_task;
CREATE TABLE rice_generate_task (
    generate_task_id     VARCHAR(32)     NOT NULL                COMMENT '生成任务ID',
    product_batch_id     VARCHAR(32)     NOT NULL                COMMENT '成品米批次ID',
    quantity             INT             NOT NULL                COMMENT '生成数量',
    package_spec         VARCHAR(50)     NOT NULL                COMMENT '包装规格',
    expected_sale_region VARCHAR(200)    NOT NULL DEFAULT ''     COMMENT '预期销售区域',
    expire_days          INT             NOT NULL DEFAULT 0      COMMENT '二维码有效期天数',
    status               VARCHAR(20)     NOT NULL DEFAULT 'PROCESSING' COMMENT '状态: PROCESSING/SUCCESS/FAILED',
    create_time          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (generate_task_id),
    KEY idx_product (product_batch_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='防伪码生成任务';

-- =====================================================================
-- 种子数据（密码均为 123456）
-- =====================================================================
INSERT INTO `user` (id, username, password, salt, nickname, phone, role, status) VALUES
    (1, 'admin',      'c9cc715c0c48834383a7f56a7233eac9', 'a1b2c3d4', '系统管理员', '13800000000', 'RICE_ADMIN',          1),
    (2, 'farmer1',    'c9cc715c0c48834383a7f56a7233eac9', 'a1b2c3d4', '张稻农',     '13800000001', 'FARMER',              1),
    (3, 'keeper1',    'c9cc715c0c48834383a7f56a7233eac9', 'a1b2c3d4', '李仓管',     '13800000002', 'WAREHOUSE_KEEPER',    1),
    (4, 'factory1',   'c9cc715c0c48834383a7f56a7233eac9', 'a1b2c3d4', '王加工',     '13800000003', 'PROCESSING_FACTORY',  1),
    (5, 'brand1',     'c9cc715c0c48834383a7f56a7233eac9', 'a1b2c3d4', '赵品牌',     '13800000004', 'BRAND_OPERATOR',      1),
    (6, 'regulator1', 'c9cc715c0c48834383a7f56a7233eac9', 'a1b2c3d4', '孙监管',     '13800000005', 'REGULATOR',           1);

-- 示例地块（便于前期联调）
INSERT INTO rice_field (field_id, field_code, field_name, farmer_id, farmer_name, province, city, district,
                        address, area_mu, gis_boundary, soil_type, coordinate_hash) VALUES
    ('FIELD202609010001', 'FIELD-WC-001', '五常核心产区1号田', 'FARMER10001', '张稻农',
     '黑龙江省', '哈尔滨市', '五常市', '民乐乡示范基地', 120.50,
     JSON_ARRAY(
        JSON_OBJECT('lng', 127.158001, 'lat', 44.931001),
        JSON_OBJECT('lng', 127.160112, 'lat', 44.931256),
        JSON_OBJECT('lng', 127.160025, 'lat', 44.929811)
     ),
     '黑土', '');

-- 示例种植批次（便于前期联调）
INSERT INTO rice_planting_batch (planting_batch_id, field_id, rice_variety, seed_source, seed_batch_no,
                                 sowing_date, expected_harvest_date, status, organic_certified, green_certified) VALUES
    ('RPB202609010001', 'FIELD202609010001', '稻花香2号', '五常市某种业有限公司', 'SEED20260401001',
     '2026-05-10', '2026-10-05', 'PLANTED', 1, 1);
