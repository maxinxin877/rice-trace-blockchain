-- =====================================================================
-- 冒烟测试数据复位脚本
--
-- 用途：跑完 scripts/smoke-test-new-modules.py 后，把测试产生的数据清掉，
--       并把被"召回发布"冻结的测试批次恢复，避免影响后续演示。
--
-- 用法：mysql -uroot -p qukuailian < back_code/scripts/reset-smoke-data.sql
--
-- 安全边界：只动冒烟测试创建的对象（smoke 前缀的幂等键 / 'smoke' 开头的原因 /
--           PROD-SMOKE-0910212002 这个测试批次），不触碰真实业务数据。
-- =====================================================================

USE qukuailian;

-- ---------------------------------------------------------------------
-- 1. 恢复被召回测试冻结的批次与防伪码
--    （如果该批次本来就是 LOCKED/DISABLED，下面的 WHERE 不会误改真实状态）
-- ---------------------------------------------------------------------
UPDATE rice_product_batch
SET status = 'ON_SALE'
WHERE product_batch_id = 'PROD-SMOKE-0910212002'
  AND status = 'LOCKED';

UPDATE rice_trace_code
SET status = 'ACTIVATED', risk_level = 'LOW'
WHERE product_batch_id = 'PROD-SMOKE-0910212002'
  AND status = 'DISABLED';

UPDATE rice_planting_batch p
SET p.status = 'STORED'
WHERE p.status = 'LOCKED'
  AND EXISTS (SELECT 1 FROM rice_storage_receipt r
              WHERE r.planting_batch_id = p.planting_batch_id
                AND r.grain_batch_id = 'GRAIN-SMOKE-0910212002');

-- ---------------------------------------------------------------------
-- 2. 清空包装单元（先删绑定关系，再删主表）
--    说明：init.sql 的种子数据里没有箱码/托盘码，这两张表的数据全部由
--    本模块测试产生，因此在开发/演示库中整体清空是安全的。
--    若你已手工录入了真实箱码，请把下面的语句改为按 product_batch_id 过滤。
-- ---------------------------------------------------------------------
DELETE FROM rice_package_unit_item;
DELETE FROM rice_package_unit;

-- ---------------------------------------------------------------------
-- 3. 删除测试召回（先明细，后任务）
-- ---------------------------------------------------------------------
DELETE FROM rice_recall_item
WHERE recall_task_id IN (SELECT recall_task_id FROM rice_recall_task WHERE recall_reason LIKE 'smoke%');

DELETE FROM rice_recall_task WHERE recall_reason LIKE 'smoke%';

-- ---------------------------------------------------------------------
-- 4. 删除测试处置单、采集记录、召回预警
-- ---------------------------------------------------------------------
DELETE FROM rice_nonconforming_disposal WHERE defect_desc LIKE 'smoke%';
DELETE FROM rice_collect_record WHERE idempotency_key LIKE 'smoke-%' OR idempotency_key LIKE 'dbg-%';
DELETE FROM rice_risk_warning WHERE warning_type = 'RECALL_TRIGGERED';

-- ---------------------------------------------------------------------
-- 5. 结果核对
-- ---------------------------------------------------------------------
SELECT 'recall_task' AS tbl, COUNT(*) AS cnt FROM rice_recall_task
UNION ALL SELECT 'recall_item', COUNT(*) FROM rice_recall_item
UNION ALL SELECT 'disposal', COUNT(*) FROM rice_nonconforming_disposal
UNION ALL SELECT 'collect', COUNT(*) FROM rice_collect_record
UNION ALL SELECT 'package_unit', COUNT(*) FROM rice_package_unit;
