-- =====================================================================
-- 演示用全链路种子数据（每张表 5 条，互相联通）
-- 链路: 地块 -> 种植批次 -> 农事记录/环境记录 -> 入库单 -> 入库质检(+明细)
--       -> 加工批次 -> 成品批次 -> 防伪码(+生成任务) -> 扫码日志
--       -> 风险预警 / 产量平衡校验 / 审计日志 / 链上存证 / 文件资源
-- 幂等: 可重复执行，先按 20260917 前缀清理本脚本数据，不影响其他数据
-- ID 前缀日期统一为 20260917；哈希用 SHA2() 生成；上链状态模拟 SUCCESS
-- =====================================================================
USE qukuailian;
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 0. 清理旧演示数据（先子后主，无外键约束，顺序仅为可读性）
-- ---------------------------------------------------------------------
DELETE FROM rice_quality_test_item  WHERE quality_test_id    LIKE 'QT20260917%';
DELETE FROM rice_quality_test       WHERE quality_test_id    LIKE 'QT20260917%';
DELETE FROM rice_scan_log           WHERE scan_log_id        LIKE 'SL20260917%';
DELETE FROM rice_trace_code         WHERE trace_code         LIKE 'RC20260917%';
DELETE FROM rice_risk_warning       WHERE warning_id         LIKE 'RW20260917%';
DELETE FROM rice_audit_log          WHERE audit_id           LIKE 'AUD20260917%';
DELETE FROM rice_chain_proof        WHERE chain_proof_id     LIKE 'CP20260917%';
DELETE FROM rice_yield_balance      WHERE check_id           LIKE 'YB20260917%';
DELETE FROM rice_generate_task      WHERE generate_task_id   LIKE 'GT20260917%';
DELETE FROM file_resource           WHERE file_id            LIKE 'FR20260917%';
DELETE FROM rice_milling_batch      WHERE milling_batch_id   LIKE 'MB20260917%';
DELETE FROM rice_storage_receipt    WHERE storage_receipt_id LIKE 'SR20260917%';
DELETE FROM rice_environment_record WHERE environment_record_id LIKE 'ENV20260917%';
DELETE FROM rice_farming_log        WHERE log_id             LIKE 'FLOG20260917%';
DELETE FROM rice_product_batch      WHERE product_batch_id   LIKE 'PB20260917%';
DELETE FROM rice_planting_batch     WHERE planting_batch_id  LIKE 'RPB20260917%';
DELETE FROM rice_field              WHERE field_id           LIKE 'FIELD20260917%';

-- =====================================================================
-- 1. 地块档案 rice_field（5 块，东北五大产区）
-- =====================================================================
INSERT INTO rice_field
(field_id, field_code, field_name, farmer_id, farmer_name, province, city, district, address,
 area_mu, gis_boundary, soil_type, base_photo_file_ids, coordinate_hash,
 chain_status, tx_id, block_height, chain_time) VALUES
('FIELD202609170001','FIELD-DEMO-01','五常民乐核心示范田','FARMER10001','张稻农','黑龙江省','哈尔滨市','五常市','民乐朝鲜族乡示范基地01号田',128.50,
 JSON_ARRAY(JSON_OBJECT('lng',127.158001,'lat',44.931001),JSON_OBJECT('lng',127.160112,'lat',44.931256),JSON_OBJECT('lng',127.160025,'lat',44.929811)),
 '黑土',JSON_ARRAY('FR202609170001'),SHA2('gis-FIELD202609170001',256),
 'SUCCESS',CONCAT('0x',SHA2('tx-FIELD202609170001',256)),862101,'2026-05-06 09:10:00'),
('FIELD202609170002','FIELD-DEMO-02','建三江创业农场水稻基地','FARMER10002','李春生','黑龙江省','佳木斯市','富锦市','创业农场第三管理区',256.00,
 JSON_ARRAY(JSON_OBJECT('lng',132.102100,'lat',47.251300),JSON_OBJECT('lng',132.105600,'lat',47.252900),JSON_OBJECT('lng',132.107200,'lat',47.249600)),
 '草甸土',NULL,SHA2('gis-FIELD202609170002',256),
 'SUCCESS',CONCAT('0x',SHA2('tx-FIELD202609170002',256)),862102,'2026-05-06 10:20:00'),
('FIELD202609170003','FIELD-DEMO-03','盘锦蟹稻共生种植基地','FARMER10003','王水田','辽宁省','盘锦市','大洼区','唐家镇蟹田示范区',180.00,
 JSON_ARRAY(JSON_OBJECT('lng',122.187500,'lat',40.962300),JSON_OBJECT('lng',122.191200,'lat',40.964800),JSON_OBJECT('lng',122.193600,'lat',40.961100)),
 '盐碱改良土',NULL,SHA2('gis-FIELD202609170003',256),
 'SUCCESS',CONCAT('0x',SHA2('tx-FIELD202609170003',256)),862103,'2026-05-07 08:40:00'),
('FIELD202609170004','FIELD-DEMO-04','舒兰溪河有机水稻基地','FARMER10004','赵良田','吉林省','吉林市','舒兰市','溪河镇有机种植园',96.80,
 JSON_ARRAY(JSON_OBJECT('lng',126.952100,'lat',44.286500),JSON_OBJECT('lng',126.955800,'lat',44.288200),JSON_OBJECT('lng',126.957400,'lat',44.284900)),
 '黑钙土',NULL,SHA2('gis-FIELD202609170004',256),
 'SUCCESS',CONCAT('0x',SHA2('tx-FIELD202609170004',256)),862104,'2026-05-07 11:30:00'),
('FIELD202609170005','FIELD-DEMO-05','宁安响水火山岩稻田','FARMER10005','陈守田','黑龙江省','牡丹江市','宁安市','渤海镇响水灌区石板田',112.30,
 JSON_ARRAY(JSON_OBJECT('lng',129.468200,'lat',44.102600),JSON_OBJECT('lng',129.471900,'lat',44.104300),JSON_OBJECT('lng',129.473500,'lat',44.101200)),
 '火山岩台地土',NULL,SHA2('gis-FIELD202609170005',256),
 'SUCCESS',CONCAT('0x',SHA2('tx-FIELD202609170005',256)),862105,'2026-05-08 09:00:00');

-- =====================================================================
-- 2. 种植批次 rice_planting_batch（5 个，前4个已上市，第3个已包装）
-- =====================================================================
INSERT INTO rice_planting_batch
(planting_batch_id, field_id, rice_variety, seed_source, seed_batch_no, seed_certificate_file_id,
 sowing_date, expected_harvest_date, actual_harvest_date, status,
 organic_certified, green_certified, certification_file_ids,
 chain_status, tx_id, block_height, chain_time) VALUES
('RPB202609170001','FIELD202609170001','稻花香2号','五常市利元种业有限公司','SEED20260418001','FR202609170002',
 '2026-05-08','2026-10-02','2026-09-01','ON_SALE',1,1,JSON_ARRAY('FR202609170005'),
 'SUCCESS',CONCAT('0x',SHA2('tx-RPB202609170001',256)),862201,'2026-05-08 18:00:00'),
('RPB202609170002','FIELD202609170002','绥粳18','富锦市七星种业有限公司','SEED20260422007','',
 '2026-05-10','2026-10-04','2026-09-02','ON_SALE',0,1,NULL,
 'SUCCESS',CONCAT('0x',SHA2('tx-RPB202609170002',256)),862202,'2026-05-10 18:00:00'),
('RPB202609170003','FIELD202609170003','盐丰47','盘锦市北方种业有限公司','SEED20260502013','',
 '2026-05-12','2026-10-06','2026-09-03','PACKAGED',0,1,NULL,
 'SUCCESS',CONCAT('0x',SHA2('tx-RPB202609170003',256)),862203,'2026-05-12 18:00:00'),
('RPB202609170004','FIELD202609170004','吉宏6号','舒兰市粮油种业有限公司','SEED20260428009','',
 '2026-05-09','2026-10-03','2026-09-04','ON_SALE',1,1,NULL,
 'SUCCESS',CONCAT('0x',SHA2('tx-RPB202609170004',256)),862204,'2026-05-09 18:00:00'),
('RPB202609170005','FIELD202609170005','五优稻4号','宁安市响水种业有限公司','SEED20260506021','',
 '2026-05-11','2026-10-05','2026-09-05','ON_SALE',0,1,NULL,
 'SUCCESS',CONCAT('0x',SHA2('tx-RPB202609170005',256)),862205,'2026-05-11 18:00:00');

-- =====================================================================
-- 3. 农事记录 rice_farming_log（5 条，挂在批次1，完整展示耕种管收）
-- =====================================================================
INSERT INTO rice_farming_log
(log_id, planting_batch_id, operation_type, operation_time, operator_id, operator_name,
 material_name, material_batch_no, material_dosage, material_unit, safe_interval_days,
 description, proof_file_ids, data_hash,
 chain_status, tx_id, block_height, chain_time) VALUES
('FLOG202609170001','RPB202609170001','SOWING','2026-05-08 06:30:00','2','张稻农',
 '稻花香2号稻种','SEED20260418001',320.00,'kg',NULL,
 '机械精量直播，亩用种2.5kg，播深2cm，行距30cm',NULL,SHA2('data-FLOG202609170001',256),
 'SUCCESS',CONCAT('0x',SHA2('tx-FLOG202609170001',256)),862301,'2026-05-08 07:00:00'),
('FLOG202609170002','RPB202609170001','FERTILIZING','2026-06-02 07:10:00','2','张稻农',
 '腐熟羊粪有机肥','ORG20260528006',12800.00,'kg',NULL,
 '分蘖肥：施用腐熟羊粪有机肥，亩施100kg，人工撒施后浅水层自然落干',NULL,SHA2('data-FLOG202609170002',256),
 'SUCCESS',CONCAT('0x',SHA2('tx-FLOG202609170002',256)),862302,'2026-06-02 08:00:00'),
('FLOG202609170003','RPB202609170001','PESTICIDE','2026-07-10 08:00:00','2','张稻农',
 '氯虫苯甲酰胺悬浮剂','PES20260701003',60.00,'L',14,
 '防治二化螟，按1:500兑水无人机飞防，严格遵守安全间隔期14天',JSON_ARRAY('FR202609170003'),SHA2('data-FLOG202609170003',256),
 'SUCCESS',CONCAT('0x',SHA2('tx-FLOG202609170003',256)),862303,'2026-07-10 09:00:00'),
('FLOG202609170004','RPB202609170001','IRRIGATION','2026-07-25 06:00:00','2','张稻农',
 '','',NULL,'',NULL,
 '抽穗扬花期抽松花江水自流灌溉，保持3-5cm浅水层，灌后记录水层深度',NULL,SHA2('data-FLOG202609170004',256),
 'SUCCESS',CONCAT('0x',SHA2('tx-FLOG202609170004',256)),862304,'2026-07-25 07:00:00'),
('FLOG202609170005','RPB202609170001','HARVESTING','2026-09-01 09:15:00','2','张稻农',
 '','',NULL,'',NULL,
 '联合收割机机械化收割，籽粒含水率24.8%，收割后2小时内运抵烘干中心',NULL,SHA2('data-FLOG202609170005',256),
 'SUCCESS',CONCAT('0x',SHA2('tx-FLOG202609170005',256)),862305,'2026-09-01 10:00:00'),
-- 批次2 建三江（绥粳18，李春生，256亩，绿色食品）
('FLOG202609170006','RPB202609170002','SOWING','2026-05-10 06:20:00','FARMER10002','李春生',
 '绥粳18稻种','SEED20260422007',640.00,'kg',NULL,
 '三江平原水田机械精量直播，亩用种2.5kg，播深2cm，行距30cm，一播全苗',NULL,SHA2('data-FLOG202609170006',256),
 'SUCCESS',CONCAT('0x',SHA2('tx-FLOG202609170006',256)),862306,'2026-05-10 07:00:00'),
('FLOG202609170007','RPB202609170002','FERTILIZING','2026-06-05 07:00:00','FARMER10002','李春生',
 '缓释复合肥(绿色食品允许目录)','FTL20260530008',10240.00,'kg',NULL,
 '分蘖肥侧深施肥，亩施40kg，插秧机同步深施5cm，减少流失提高肥效',NULL,SHA2('data-FLOG202609170007',256),
 'SUCCESS',CONCAT('0x',SHA2('tx-FLOG202609170007',256)),862307,'2026-06-05 08:00:00'),
('FLOG202609170008','RPB202609170002','PESTICIDE','2026-07-12 08:30:00','FARMER10002','李春生',
 '吡蚜酮水分散粒剂','PES20260702005',51.20,'L',14,
 '防治稻飞虱，按1:750兑水无人机统防统治，严格遵守安全间隔期14天',NULL,SHA2('data-FLOG202609170008',256),
 'SUCCESS',CONCAT('0x',SHA2('tx-FLOG202609170008',256)),862308,'2026-07-12 09:00:00'),
('FLOG202609170009','RPB202609170002','IRRIGATION','2026-08-02 05:30:00','FARMER10002','李春生',
 '','',NULL,'',NULL,
 '抽穗扬花期采用浅湿干节水控灌，灌3cm浅水层自然落干后再复水，防低温冷害',NULL,SHA2('data-FLOG202609170009',256),
 'SUCCESS',CONCAT('0x',SHA2('tx-FLOG202609170009',256)),862309,'2026-08-02 06:30:00'),
('FLOG202609170010','RPB202609170002','HARVESTING','2026-09-02 09:10:00','FARMER10002','李春生',
 '','',NULL,'',NULL,
 '大型联合收割机分段收获，籽粒含水率25.6%，稻谷当天进场烘干至安全水分',NULL,SHA2('data-FLOG202609170010',256),
 'SUCCESS',CONCAT('0x',SHA2('tx-FLOG202609170010',256)),862310,'2026-09-02 10:00:00'),
-- 批次3 盘锦蟹田（盐丰47，王水田，180亩，蟹稻共生绿色种植）
('FLOG202609170011','RPB202609170003','SOWING','2026-05-12 06:40:00','FARMER10003','王水田',
 '盐丰47稻种','SEED20260502013',450.00,'kg',NULL,
 '蟹稻田机械直播，亩用种2.5kg，播前旋耕压青紫云英绿肥还田',NULL,SHA2('data-FLOG202609170011',256),
 'SUCCESS',CONCAT('0x',SHA2('tx-FLOG202609170011',256)),862311,'2026-05-12 07:30:00'),
('FLOG202609170012','RPB202609170003','FERTILIZING','2026-06-08 07:20:00','FARMER10003','王水田',
 '腐熟有机肥(蟹田专用)','ORG20260601012',18000.00,'kg',NULL,
 '亩施100kg腐熟有机肥配合紫云英绿肥，蟹稻共生不施化肥，蟹粪还田补充肥力',NULL,SHA2('data-FLOG202609170012',256),
 'SUCCESS',CONCAT('0x',SHA2('tx-FLOG202609170012',256)),862312,'2026-06-08 08:20:00'),
('FLOG202609170013','RPB202609170003','WEEDING','2026-06-25 08:00:00','FARMER10003','王水田',
 '','',NULL,'',NULL,
 '蟹稻共生除草：河蟹摄食杂草嫩芽与水生害虫，配合人工拔除稗草，全程不使用化学除草剂',NULL,SHA2('data-FLOG202609170013',256),
 'SUCCESS',CONCAT('0x',SHA2('tx-FLOG202609170013',256)),862313,'2026-06-25 09:00:00'),
('FLOG202609170014','RPB202609170003','IRRIGATION','2026-07-28 06:00:00','FARMER10003','王水田',
 '','',NULL,'',NULL,
 '引辽河生态水保持15cm深水层养蟹，田间投放扣蟹1500只/亩，稻蟹互利共生',NULL,SHA2('data-FLOG202609170014',256),
 'SUCCESS',CONCAT('0x',SHA2('tx-FLOG202609170014',256)),862314,'2026-07-28 07:00:00'),
('FLOG202609170015','RPB202609170003','HARVESTING','2026-09-03 09:00:00','FARMER10003','王水田',
 '','',NULL,'',NULL,
 '先捕捞成蟹后排水收稻，机械收割含水率24.2%，蟹田稻谷无农残单项检测合格',NULL,SHA2('data-FLOG202609170015',256),
 'SUCCESS',CONCAT('0x',SHA2('tx-FLOG202609170015',256)),862315,'2026-09-03 10:00:00'),
-- 批次4 舒兰有机（吉宏6号，赵良田，96.8亩，有机认证）
('FLOG202609170016','RPB202609170004','SOWING','2026-05-09 06:10:00','FARMER10004','赵良田',
 '吉宏6号稻种','SEED20260428009',242.00,'kg',NULL,
 '舒兰盆地黑土田机械精量直播，亩用种2.5kg，播前晒种浸种消毒',NULL,SHA2('data-FLOG202609170016',256),
 'SUCCESS',CONCAT('0x',SHA2('tx-FLOG202609170016',256)),862316,'2026-05-09 07:00:00'),
('FLOG202609170017','RPB202609170004','FERTILIZING','2026-06-03 07:00:00','FARMER10004','赵良田',
 '腐熟牛粪有机肥','ORG20260526015',9680.00,'kg',NULL,
 '有机种植全程不施化肥，亩施腐熟牛粪100kg作分蘖肥，人工撒施后浅水耘田',NULL,SHA2('data-FLOG202609170017',256),
 'SUCCESS',CONCAT('0x',SHA2('tx-FLOG202609170017',256)),862317,'2026-06-03 08:00:00'),
('FLOG202609170018','RPB202609170004','PESTICIDE','2026-07-15 08:00:00','FARMER10004','赵良田',
 '苦参碱生物农药','BIO20260705007',19.40,'L',7,
 '有机认证标准：苦参碱喷雾配合田间释放赤眼蜂防治二化螟，安全间隔期7天',NULL,SHA2('data-FLOG202609170018',256),
 'SUCCESS',CONCAT('0x',SHA2('tx-FLOG202609170018',256)),862318,'2026-07-15 09:00:00'),
('FLOG202609170019','RPB202609170004','IRRIGATION','2026-07-30 06:30:00','FARMER10004','赵良田',
 '','',NULL,'',NULL,
 '引拉林河支流溪河水自流灌溉，抽穗期保持5cm浅水层，山泉水温低米质好',NULL,SHA2('data-FLOG202609170019',256),
 'SUCCESS',CONCAT('0x',SHA2('tx-FLOG202609170019',256)),862319,'2026-07-30 07:30:00'),
('FLOG202609170020','RPB202609170004','HARVESTING','2026-09-04 09:20:00','FARMER10004','赵良田',
 '','',NULL,'',NULL,
 '有机稻谷人工查田后联合收割，含水率23.9%，单收单储防止与常规稻谷混杂',NULL,SHA2('data-FLOG202609170020',256),
 'SUCCESS',CONCAT('0x',SHA2('tx-FLOG202609170020',256)),862320,'2026-09-04 10:00:00'),
-- 批次5 宁安响水（五优稻4号，陈守田，112.3亩，火山岩石板田）
('FLOG202609170021','RPB202609170005','SOWING','2026-05-11 06:30:00','FARMER10005','陈守田',
 '五优稻4号稻种','SEED20260506021',281.00,'kg',NULL,
 '火山岩石板田人工辅助精量直播，亩用种2.5kg，石板地白天增温快利于扎根',NULL,SHA2('data-FLOG202609170021',256),
 'SUCCESS',CONCAT('0x',SHA2('tx-FLOG202609170021',256)),862321,'2026-05-11 07:30:00'),
('FLOG202609170022','RPB202609170005','FERTILIZING','2026-06-06 07:10:00','FARMER10005','陈守田',
 '有机无机复混肥','FTL20260530019',3369.00,'kg',NULL,
 '分蘖肥亩施30kg侧深施，火山岩台地保肥差，采用少量多次追肥防流失',NULL,SHA2('data-FLOG202609170022',256),
 'SUCCESS',CONCAT('0x',SHA2('tx-FLOG202609170022',256)),862322,'2026-06-06 08:10:00'),
('FLOG202609170023','RPB202609170005','PESTICIDE','2026-07-14 08:20:00','FARMER10005','陈守田',
 '氯虫·噻虫嗪','PES20260703011',22.50,'L',14,
 '防治负泥虫与稻螟虫，无人机飞防按1:1000兑水，遵守安全间隔期14天',NULL,SHA2('data-FLOG202609170023',256),
 'SUCCESS',CONCAT('0x',SHA2('tx-FLOG202609170023',256)),862323,'2026-07-14 09:20:00'),
('FLOG202609170024','RPB202609170005','IRRIGATION','2026-08-01 05:40:00','FARMER10005','陈守田',
 '','',NULL,'',NULL,
 '引镜泊湖火山岩泉水自流灌溉，水流经玄武岩岩层天然增温矿化，石板田昼夜积温高',NULL,SHA2('data-FLOG202609170024',256),
 'SUCCESS',CONCAT('0x',SHA2('tx-FLOG202609170024',256)),862324,'2026-08-01 06:40:00'),
('FLOG202609170025','RPB202609170005','HARVESTING','2026-09-05 09:15:00','FARMER10005','陈守田',
 '','',NULL,'',NULL,
 '石板田稻谷完熟后联合收割，含水率26.1%，响水大米成熟期一致、出米率高',NULL,SHA2('data-FLOG202609170025',256),
 'SUCCESS',CONCAT('0x',SHA2('tx-FLOG202609170025',256)),862325,'2026-09-05 10:00:00');

-- =====================================================================
-- 4. 环境数据 rice_environment_record（批次1~5，每批物联网/无人机/人工）
-- =====================================================================
INSERT INTO rice_environment_record
(environment_record_id, planting_batch_id, source_type, record_time,
 air_temperature, air_humidity, soil_moisture, rainfall, wind_speed, image_file_ids, remark) VALUES
('ENV202609170001','RPB202609170001','IOT','2026-06-15 10:00:00',24.60,68.20,72.40,0.00,2.10,NULL,'分蘖期物联网监测点自动采集'),
('ENV202609170002','RPB202609170001','IOT','2026-07-10 12:00:00',28.90,75.60,81.00,3.20,3.50,NULL,'拔节期田间气象站数据'),
('ENV202609170003','RPB202609170001','IOT','2026-08-05 14:00:00',30.10,80.20,85.60,12.80,2.80,NULL,'扬花期雷阵雨后上传，墒情偏高'),
('ENV202609170004','RPB202609170001','DRONE','2026-08-20 10:30:00',27.40,73.50,78.20,0.00,4.20,NULL,'无人机多光谱巡田，长势均匀无倒伏'),
('ENV202609170005','RPB202609170001','MANUAL','2026-08-28 16:00:00',25.80,70.40,74.60,0.00,1.80,NULL,'蜡熟期人工巡田记录，灌浆良好'),
-- 批次2 建三江
('ENV202609170006','RPB202609170002','IOT','2026-06-18 10:00:00',23.90,70.50,75.20,0.00,2.60,NULL,'分蘖期建三江田间气象站自动采集，积温充足'),
('ENV202609170007','RPB202609170002','DRONE','2026-08-08 10:30:00',26.80,76.40,80.10,0.00,3.80,NULL,'无人机多光谱巡田，大面积长势均匀无倒伏'),
('ENV202609170008','RPB202609170002','MANUAL','2026-08-26 15:30:00',24.50,68.90,72.00,0.00,2.20,NULL,'蜡熟期人工巡田，三江平原昼夜温差大利于灌浆'),
-- 批次3 盘锦蟹田
('ENV202609170009','RPB202609170003','IOT','2026-06-20 10:00:00',25.40,72.10,82.60,1.20,1.90,NULL,'蟹田水质物联网监测，水位与溶氧正常，河蟹活动良好'),
('ENV202609170010','RPB202609170003','DRONE','2026-08-10 10:00:00',27.60,78.80,84.20,0.00,3.20,NULL,'无人机巡田，稻蟹共生区水稻长势均匀、稻穗整齐'),
('ENV202609170011','RPB202609170003','MANUAL','2026-08-27 16:00:00',25.90,71.60,78.90,0.00,2.00,NULL,'人工记录河口小气候，黄熟期转色正常，无病害'),
-- 批次4 舒兰有机
('ENV202609170012','RPB202609170004','IOT','2026-06-17 10:00:00',22.80,74.30,70.80,0.00,1.80,NULL,'山区盆地物联网监测，黑土墒情适宜，有机质含量高'),
('ENV202609170013','RPB202609170004','DRONE','2026-08-09 10:30:00',26.20,77.50,76.40,6.40,2.70,NULL,'雷阵雨过后无人机巡田，有机稻田无病虫害发生'),
('ENV202609170014','RPB202609170004','MANUAL','2026-08-28 15:00:00',23.60,70.20,71.50,0.00,1.60,NULL,'舒兰盆地昼夜温差大，有机稻谷灌浆充实'),
-- 批次5 宁安响水
('ENV202609170015','RPB202609170005','IOT','2026-06-19 10:00:00',24.10,69.80,68.40,0.00,2.40,NULL,'火山岩台地物联网监测，石板田透水性强、地温偏高'),
('ENV202609170016','RPB202609170005','DRONE','2026-08-08 11:00:00',27.10,74.90,73.60,0.00,3.50,NULL,'无人机航拍响水片区，玄武岩石板田稻浪整齐'),
('ENV202609170017','RPB202609170005','MANUAL','2026-08-29 15:30:00',24.80,67.50,69.20,0.00,2.10,NULL,'镜泊湖小气候人工记录，成熟期一致、青壳乌米少');

-- =====================================================================
-- 5. 收储入库单 rice_storage_receipt（5 单，每批次一单）
-- =====================================================================
INSERT INTO rice_storage_receipt
(storage_receipt_id, planting_batch_id, grain_batch_id, warehouse_id, warehouse_code,
 harvest_area_mu, wet_grain_weight_kg, moisture_percent, impurity_percent, grain_grade,
 storage_time, temperature, humidity, keeper_signature, farmer_signature,
 quality_report_file_id, report_hash,
 chain_status, tx_id, block_height, chain_time) VALUES
('SR202609170001','RPB202609170001','GB202609170001','WH10001','WW-2026-0901',
 128.50,79670.00,24.80,1.20,'一等','2026-09-06 09:00:00',14.20,65.00,'李仓管(eSign-8f3a)','张稻农(eSign-2c1d)','FR202609170004',SHA2('report-SR202609170001',256),
 'SUCCESS',CONCAT('0x',SHA2('tx-SR202609170001',256)),862401,'2026-09-06 10:00:00'),
('SR202609170002','RPB202609170002','GB202609170002','WH10002','WW-2026-0902',
 256.00,153600.00,26.20,1.50,'一等','2026-09-06 11:00:00',15.10,66.40,'李仓管(eSign-8f3a)','李春生(eSign-5e7b)','',SHA2('report-SR202609170002',256),
 'SUCCESS',CONCAT('0x',SHA2('tx-SR202609170002',256)),862402,'2026-09-06 12:00:00'),
('SR202609170003','RPB202609170003','GB202609170003','WH10003','WW-2026-0903',
 180.00,117000.00,25.50,1.00,'二等','2026-09-06 13:30:00',16.30,68.10,'李仓管(eSign-8f3a)','王水田(eSign-9a2f)','',SHA2('report-SR202609170003',256),
 'SUCCESS',CONCAT('0x',SHA2('tx-SR202609170003',256)),862403,'2026-09-06 14:30:00'),
('SR202609170004','RPB202609170004','GB202609170004','WH10004','WW-2026-0904',
 96.80,59048.00,23.90,1.30,'一等','2026-09-06 15:00:00',13.80,63.70,'李仓管(eSign-8f3a)','赵良田(eSign-3b6e)','',SHA2('report-SR202609170004',256),
 'SUCCESS',CONCAT('0x',SHA2('tx-SR202609170004',256)),862404,'2026-09-06 16:00:00'),
('SR202609170005','RPB202609170005','GB202609170005','WH10005','WW-2026-0905',
 112.30,65134.00,27.10,1.60,'二等','2026-09-06 16:30:00',14.90,67.20,'李仓管(eSign-8f3a)','陈守田(eSign-6d4c)','',SHA2('report-SR202609170005',256),
 'SUCCESS',CONCAT('0x',SHA2('tx-SR202609170005',256)),862405,'2026-09-06 17:30:00');

-- =====================================================================
-- 6. 质检记录 rice_quality_test（5 单入库质检）
-- =====================================================================
INSERT INTO rice_quality_test
(quality_test_id, business_type, business_id, test_agency, test_time, overall_result,
 report_file_id, report_hash, remark,
 chain_status, tx_id, block_height, chain_time) VALUES
('QT202609170001','STORAGE','SR202609170001','五常市农产品质量安全检测中心','2026-09-06 14:00:00','PASS',
 'FR202609170004',SHA2('qreport-QT202609170001',256),'各项指标均符合GB 1350一级粳稻要求',
 'SUCCESS',CONCAT('0x',SHA2('tx-QT202609170001',256)),862501,'2026-09-06 15:00:00'),
('QT202609170002','STORAGE','SR202609170002','富锦市粮油质量检测站','2026-09-06 16:00:00','PASS',
 '',SHA2('qreport-QT202609170002',256),'建三江产区稻谷，出糙率高，加工品质好',
 'SUCCESS',CONCAT('0x',SHA2('tx-QT202609170002',256)),862502,'2026-09-06 17:00:00'),
('QT202609170003','STORAGE','SR202609170003','盘锦市食品检验检测中心','2026-09-07 09:30:00','PASS',
 '',SHA2('qreport-QT202609170003',256),'蟹田稻，农残未检出，水分偏高需烘干',
 'SUCCESS',CONCAT('0x',SHA2('tx-QT202609170003',256)),862503,'2026-09-07 10:30:00'),
('QT202609170004','STORAGE','SR202609170004','舒兰市农产品质量检测站','2026-09-07 10:30:00','PASS',
 '',SHA2('qreport-QT202609170004',256),'有机转换期批次，23项农残全部未检出',
 'SUCCESS',CONCAT('0x',SHA2('tx-QT202609170004',256)),862504,'2026-09-07 11:30:00'),
('QT202609170005','STORAGE','SR202609170005','宁安市质量计量检验检测所','2026-09-07 11:30:00','PASS',
 '',SHA2('qreport-QT202609170005',256),'响水石板田稻谷，直链淀粉含量16.8%，口感预期上佳',
 'SUCCESS',CONCAT('0x',SHA2('tx-QT202609170005',256)),862505,'2026-09-07 12:30:00');

-- 质检明细 5 项（挂在 QT01，展示完整检测项）
INSERT INTO rice_quality_test_item
(quality_test_id, item_name, item_value, unit, standard_value, result) VALUES
('QT202609170001','水分含量',14.200,'%','≤14.5','PASS'),
('QT202609170001','杂质含量',0.300,'%','≤1.0','PASS'),
('QT202609170001','出糙率',81.500,'%','≥79.0','PASS'),
('QT202609170001','整精米率',66.200,'%','≥61.0','PASS'),
('QT202609170001','镉(以Cd计)',0.082,'mg/kg','≤0.2','PASS');

-- =====================================================================
-- 7. 碾米加工批次 rice_milling_batch（5 批）
-- =====================================================================
INSERT INTO rice_milling_batch
(milling_batch_id, grain_batch_id, product_batch_id, factory_id,
 grain_out_weight_kg, rice_output_weight_kg, yield_rate,
 process_start_time, process_end_time, process_params, quality_summary, quality_report_file_id,
 chain_status, tx_id, block_height, chain_time) VALUES
('MB202609170001','GB202609170001','PB202609170001','FAC10001',
 75000.00,49500.00,66.00,'2026-09-08 08:00:00','2026-09-09 17:00:00',
 JSON_OBJECT('huskingRatePercent',82,'polishingTimes',2,'colorSortingPass',1,'packaging','真空充氮包装'),
 '出厂检验合格：加工精度精碾一级，碎米总量≤7.5%，垩白度≤2%','',
 'SUCCESS',CONCAT('0x',SHA2('tx-MB202609170001',256)),862601,'2026-09-09 18:00:00'),
('MB202609170002','GB202609170002','PB202609170002','FAC10001',
 145000.00,94250.00,65.00,'2026-09-08 09:00:00','2026-09-09 18:00:00',
 JSON_OBJECT('huskingRatePercent',81,'polishingTimes',1,'colorSortingPass',1,'packaging','普通编织袋'),
 '出厂检验合格：长粒香型，留胚率高，适合家庭装','',
 'SUCCESS',CONCAT('0x',SHA2('tx-MB202609170002',256)),862602,'2026-09-09 19:00:00'),
('MB202609170003','GB202609170003','PB202609170003','FAC10001',
 110000.00,72600.00,66.00,'2026-09-08 10:00:00','2026-09-09 16:00:00',
 JSON_OBJECT('huskingRatePercent',80,'polishingTimes',2,'colorSortingPass',2,'packaging','真空砖形装'),
 '出厂检验合格：蟹田米清白透明，胶稠度82mm','',
 'SUCCESS',CONCAT('0x',SHA2('tx-MB202609170003',256)),862603,'2026-09-09 17:00:00'),
('MB202609170004','GB202609170004','PB202609170004','FAC10001',
 56000.00,36400.00,65.00,'2026-09-08 11:00:00','2026-09-09 15:00:00',
 JSON_OBJECT('huskingRatePercent',82,'polishingTimes',3,'colorSortingPass',1,'packaging','礼盒充氮装'),
 '出厂检验合格：有机贡米，轻抛光留胚芽，礼盒装','',
 'SUCCESS',CONCAT('0x',SHA2('tx-MB202609170004',256)),862604,'2026-09-09 16:00:00'),
('MB202609170005','GB202609170005','PB202609170005','FAC10001',
 61000.00,40260.00,66.00,'2026-09-08 13:00:00','2026-09-09 19:00:00',
 JSON_OBJECT('huskingRatePercent',83,'polishingTimes',2,'colorSortingPass',1,'packaging','真空充氮包装'),
 '出厂检验合格：响水大米米粒青莹，蒸煮后油润香软','',
 'SUCCESS',CONCAT('0x',SHA2('tx-MB202609170005',256)),862605,'2026-09-09 20:00:00');

-- =====================================================================
-- 8. 成品米批次 rice_product_batch（5 批）
-- =====================================================================
INSERT INTO rice_product_batch
(product_batch_id, product_name, rice_variety, brand_name, package_spec, standard_no,
 nutrition_facts, certification_file_ids, expected_sale_region, status) VALUES
('PB202609170001','五常有机稻花香2号大米','稻花香2号','五常金禾','5kg真空袋装','GB/T 19266',
 JSON_OBJECT('energy_kj',1460,'protein_g',7.4,'fat_g',0.8,'carbohydrate_g',77.9,'sodium_mg',2),JSON_ARRAY('FR202609170005'),'北京市、上海市','ON_SALE'),
('PB202609170002','建三江长粒香米','绥粳18','建三江农垦','10kg编织袋装','GB/T 1354',
 JSON_OBJECT('energy_kj',1452,'protein_g',7.1,'fat_g',0.9,'carbohydrate_g',78.3,'sodium_mg',3),NULL,'广东省','ON_SALE'),
('PB202609170003','盘锦蟹田生态米','盐丰47','盘锦绿也','5kg真空砖形装','GB/T 18824',
 JSON_OBJECT('energy_kj',1448,'protein_g',7.2,'fat_g',0.7,'carbohydrate_g',78.0,'sodium_mg',2),NULL,'全国','PACKAGED'),
('PB202609170004','舒兰有机贡米','吉宏6号','舒兰净土','2.5kg礼盒装','GB/T 1354',
 JSON_OBJECT('energy_kj',1455,'protein_g',7.5,'fat_g',0.8,'carbohydrate_g',77.6,'sodium_mg',2),NULL,'黑龙江省、吉林省、辽宁省','ON_SALE'),
('PB202609170005','宁安响水火山岩石板米','五优稻4号','宁安响水米业','5kg真空袋装','GB/T 18824',
 JSON_OBJECT('energy_kj',1463,'protein_g',7.3,'fat_g',0.9,'carbohydrate_g',78.1,'sodium_mg',2),NULL,'浙江省、江苏省','ON_SALE');

-- =====================================================================
-- 9. 防伪码 rice_trace_code（5 枚，每成品 1 枚）
--    001 北京/上海被扫2次；002 广州1次；003 待激活；004 海南异地->风险；005 刚激活
-- =====================================================================
INSERT INTO rice_trace_code
(trace_code, qr_code_url, product_batch_id, package_spec, status, expire_days,
 activated_at, first_scanned_at, scan_count, expected_sale_region, last_scan_region, risk_level,
 chain_status, tx_id, block_height, chain_time) VALUES
('RC20260917000001','/uploads/qrcode/RC20260917000001.png','PB202609170001','5kg真空袋装','ACTIVATED',730,
 '2026-09-09 18:30:00','2026-09-10 10:12:00',2,'北京市、上海市','上海市','LOW',
 'SUCCESS',CONCAT('0x',SHA2('tx-RC20260917000001',256)),862701,'2026-09-09 18:31:00'),
('RC20260917000002','/uploads/qrcode/RC20260917000002.png','PB202609170002','10kg编织袋装','ACTIVATED',730,
 '2026-09-09 19:30:00','2026-09-11 09:20:00',1,'广东省','广东省广州市','LOW',
 'SUCCESS',CONCAT('0x',SHA2('tx-RC20260917000002',256)),862702,'2026-09-09 19:31:00'),
('RC20260917000003','/uploads/qrcode/RC20260917000003.png','PB202609170003','5kg真空砖形装','GENERATED',0,
 NULL,NULL,0,'全国','','LOW',
 'PENDING','',0,NULL),
('RC20260917000004','/uploads/qrcode/RC20260917000004.png','PB202609170004','2.5kg礼盒装','RISK',365,
 '2026-09-09 16:30:00','2026-09-13 14:32:00',2,'黑龙江省、吉林省、辽宁省','海南省三亚市','HIGH',
 'SUCCESS',CONCAT('0x',SHA2('tx-RC20260917000004',256)),862704,'2026-09-09 16:31:00'),
('RC20260917000005','/uploads/qrcode/RC20260917000005.png','PB202609170005','5kg真空袋装','ACTIVATED',0,
 '2026-09-10 09:00:00',NULL,0,'浙江省、江苏省','','LOW',
 'SUCCESS',CONCAT('0x',SHA2('tx-RC20260917000005',256)),862705,'2026-09-10 09:01:00');

-- =====================================================================
-- 10. 扫码日志 rice_scan_log（5 条）
-- =====================================================================
INSERT INTO rice_scan_log
(scan_log_id, trace_code, product_batch_id, scan_time, lng, lat, region,
 device_id, openid, scene, authentic, first_scan, risk_level) VALUES
('SL202609170001','RC20260917000001','PB202609170001','2026-09-10 10:12:00',116.407400,39.904200,'北京市朝阳区',
 'dev-8f3a2c1d','oA9xK1Qk7mPb','SCAN',1,1,'LOW'),
('SL202609170002','RC20260917000001','PB202609170001','2026-09-12 15:40:00',121.473700,31.230400,'上海市黄浦区',
 'dev-2c1d5e7b','oB7nL2Wr4tXc','SCAN',1,0,'LOW'),
('SL202609170003','RC20260917000002','PB202609170002','2026-09-11 09:20:00',113.264400,23.129100,'广东省广州市天河区',
 'dev-9a2f3b6e','oC5pM8Yu6vZd','SCAN',1,1,'LOW'),
('SL202609170004','RC20260917000004','PB202609170004','2026-09-13 14:32:00',109.511900,18.252800,'海南省三亚市吉阳区',
 'dev-6d4c8f0a','oD3qN1Xp8wQe','SCAN',1,1,'HIGH'),
('SL202609170005','RC20260917000004','PB202609170004','2026-09-14 11:05:00',110.331200,20.031700,'海南省海口市美兰区',
 'dev-5e7b1a9c','oE8rO4Zq2rTf','SCAN',1,0,'HIGH');

-- =====================================================================
-- 11. 风险预警 rice_risk_warning（5 条：窜货2 + 重复扫码2 + 产量平衡1）
-- =====================================================================
INSERT INTO rice_risk_warning
(warning_id, warning_type, risk_level, business_type, business_id, warning_content,
 handled, handled_by, handled_at, handle_result,
 chain_status, tx_id, block_height, chain_time) VALUES
('RW202609170001','CHANNEL_CONFLICT','HIGH','TRACE_CODE','RC20260917000004',
 '防伪码首次扫码地为海南省三亚市，与预期销售区域（黑龙江、吉林、辽宁）严重不符，疑似跨区窜货',
 0,'',NULL,'',
 'PENDING','',0,NULL),
('RW202609170002','CHANNEL_CONFLICT','MEDIUM','TRACE_CODE','RC20260917000004',
 '该防伪码再次出现于海南省海口市，扫码区域持续偏离授权销售区域',
 0,'',NULL,'',
 'PENDING','',0,NULL),
('RW202609170003','REPEAT_SCAN','LOW','TRACE_CODE','RC20260917000001',
 '防伪码在北京首扫后于上海再次扫码，跨城市重复扫码，需关注是否为二手流转',
 1,'孙监管','2026-09-13 10:20:00','经核验两次扫码设备不同，联系消费者确认为出差携带，排除窜货',
 'SUCCESS',CONCAT('0x',SHA2('tx-RW202609170003',256)),862803,'2026-09-13 10:25:00'),
('RW202609170004','REPEAT_SCAN','MEDIUM','TRACE_CODE','RC20260917000004',
 '高风险码在海南三亚、海口连续被扫，疑似批量异常流通',
 0,'',NULL,'',
 'PENDING','',0,NULL),
('RW202609170005','YIELD_BALANCE','MEDIUM','YIELD_BALANCE','YB202609170003',
 '盘锦蟹田批次收储到加工环节物料平衡偏差+7.9%，超出±5%预警阈值',
 1,'孙监管','2026-09-12 15:00:00','核实为高水分稻谷烘干减重折算口径差异，调取烘干记录与入库单复核一致，属正常损耗',
 'SUCCESS',CONCAT('0x',SHA2('tx-RW202609170005',256)),862805,'2026-09-12 15:05:00');

-- =====================================================================
-- 12. 审计日志 rice_audit_log（5 条，覆盖创建/核验/处理）
-- =====================================================================
INSERT INTO rice_audit_log
(audit_id, business_type, business_id, operation_type, operator_id, operator_name,
 before_hash, after_hash, reason, ip, operation_time, tx_id) VALUES
('AUD202609170001','FIELD','FIELD202609170001','CREATE','2','张稻农',
 '',SHA2('audit-after-FIELD202609170001',256),'新建地块档案并提交GIS坐标上链','192.168.10.21','2026-05-06 09:10:00',CONCAT('0x',SHA2('aud-AUD202609170001',256))),
('AUD202609170002','PLANTING_BATCH','RPB202609170001','CREATE','2','张稻农',
 '',SHA2('audit-after-RPB202609170001',256),'创建种植批次，登记品种与种子来源','192.168.10.21','2026-05-08 17:50:00',CONCAT('0x',SHA2('aud-AUD202609170002',256))),
('AUD202609170003','STORAGE_RECEIPT','SR202609170001','VERIFY','3','李仓管',
 '',SHA2('audit-after-SR202609170001',256),'核验湿谷过磅重量与质检报告后确认入库','192.168.10.35','2026-09-06 09:40:00',CONCAT('0x',SHA2('aud-AUD202609170003',256))),
('AUD202609170004','MILLING_BATCH','MB202609170001','CREATE','4','王加工',
 '',SHA2('audit-after-MB202609170001',256),'登记加工批次与工艺参数，成品批次同步生成','192.168.10.48','2026-09-08 08:10:00',CONCAT('0x',SHA2('aud-AUD202609170004',256))),
('AUD202609170005','RISK_WARNING','RW202609170003','HANDLE','6','孙监管',
 SHA2('audit-before-RW03',256),SHA2('audit-after-RW03',256),'重复扫码预警复核关闭','192.168.10.66','2026-09-13 10:20:00',CONCAT('0x',SHA2('aud-AUD202609170005',256)));

-- =====================================================================
-- 13. 链上存证 rice_chain_proof（29 条，覆盖 5 条链路的地块/种植/质检/入库/加工环节 + 已激活防伪码）
--     003 链路防伪码未激活，故意不插 RC20260917000003 存证，留现场激活演示
-- =====================================================================
INSERT INTO rice_chain_proof
(chain_proof_id, business_type, business_id, data_hash, file_hashes,
 tx_id, block_height, chain_time, chain_status) VALUES
-- 链路1 五常
('CP202609170001','FIELD','FIELD202609170001',SHA2('proof-FIELD202609170001',256),NULL,
 CONCAT('0x',SHA2('cptx-FIELD202609170001',256)),862101,'2026-05-06 09:10:00','SUCCESS'),
('CP202609170002','PLANTING_BATCH','RPB202609170001',SHA2('proof-RPB202609170001',256),NULL,
 CONCAT('0x',SHA2('cptx-RPB202609170001',256)),862201,'2026-05-08 18:00:00','SUCCESS'),
('CP202609170003','STORAGE_RECEIPT','SR202609170001',SHA2('proof-SR202609170001',256),JSON_ARRAY(SHA2('file-FR202609170004',256)),
 CONCAT('0x',SHA2('cptx-SR202609170001',256)),862401,'2026-09-06 10:00:00','SUCCESS'),
('CP202609170004','MILLING_BATCH','MB202609170001',SHA2('proof-MB202609170001',256),NULL,
 CONCAT('0x',SHA2('cptx-MB202609170001',256)),862601,'2026-09-09 18:00:00','SUCCESS'),
('CP202609170005','TRACE_CODE','RC20260917000001',SHA2('proof-RC20260917000001',256),NULL,
 CONCAT('0x',SHA2('cptx-RC20260917000001',256)),862701,'2026-09-09 18:31:00','SUCCESS'),
-- 地块 002~005
('CP202609170006','FIELD','FIELD202609170002',SHA2('proof-FIELD202609170002',256),NULL,
 CONCAT('0x',SHA2('cptx-FIELD202609170002',256)),862102,'2026-05-07 09:10:00','SUCCESS'),
('CP202609170007','FIELD','FIELD202609170003',SHA2('proof-FIELD202609170003',256),NULL,
 CONCAT('0x',SHA2('cptx-FIELD202609170003',256)),862103,'2026-05-08 09:10:00','SUCCESS'),
('CP202609170008','FIELD','FIELD202609170004',SHA2('proof-FIELD202609170004',256),NULL,
 CONCAT('0x',SHA2('cptx-FIELD202609170004',256)),862104,'2026-05-09 09:10:00','SUCCESS'),
('CP202609170009','FIELD','FIELD202609170005',SHA2('proof-FIELD202609170005',256),NULL,
 CONCAT('0x',SHA2('cptx-FIELD202609170005',256)),862105,'2026-05-10 09:10:00','SUCCESS'),
-- 种植批次 002~005
('CP202609170010','PLANTING_BATCH','RPB202609170002',SHA2('proof-RPB202609170002',256),NULL,
 CONCAT('0x',SHA2('cptx-RPB202609170002',256)),862202,'2026-05-09 18:00:00','SUCCESS'),
('CP202609170011','PLANTING_BATCH','RPB202609170003',SHA2('proof-RPB202609170003',256),NULL,
 CONCAT('0x',SHA2('cptx-RPB202609170003',256)),862203,'2026-05-10 18:00:00','SUCCESS'),
('CP202609170012','PLANTING_BATCH','RPB202609170004',SHA2('proof-RPB202609170004',256),NULL,
 CONCAT('0x',SHA2('cptx-RPB202609170004',256)),862204,'2026-05-11 18:00:00','SUCCESS'),
('CP202609170013','PLANTING_BATCH','RPB202609170005',SHA2('proof-RPB202609170005',256),NULL,
 CONCAT('0x',SHA2('cptx-RPB202609170005',256)),862205,'2026-05-12 18:00:00','SUCCESS'),
-- 质检报告 001~005
('CP202609170014','QUALITY_TEST','QT202609170001',SHA2('proof-QT202609170001',256),JSON_ARRAY(SHA2('file-FR202609170004',256)),
 CONCAT('0x',SHA2('cptx-QT202609170001',256)),862501,'2026-09-06 14:00:00','SUCCESS'),
('CP202609170015','QUALITY_TEST','QT202609170002',SHA2('proof-QT202609170002',256),NULL,
 CONCAT('0x',SHA2('cptx-QT202609170002',256)),862502,'2026-09-05 14:00:00','SUCCESS'),
('CP202609170016','QUALITY_TEST','QT202609170003',SHA2('proof-QT202609170003',256),NULL,
 CONCAT('0x',SHA2('cptx-QT202609170003',256)),862503,'2026-09-06 14:00:00','SUCCESS'),
('CP202609170017','QUALITY_TEST','QT202609170004',SHA2('proof-QT202609170004',256),NULL,
 CONCAT('0x',SHA2('cptx-QT202609170004',256)),862504,'2026-09-07 14:00:00','SUCCESS'),
('CP202609170018','QUALITY_TEST','QT202609170005',SHA2('proof-QT202609170005',256),NULL,
 CONCAT('0x',SHA2('cptx-QT202609170005',256)),862505,'2026-09-08 14:00:00','SUCCESS'),
-- 入库单 002~005
('CP202609170019','STORAGE_RECEIPT','SR202609170002',SHA2('proof-SR202609170002',256),NULL,
 CONCAT('0x',SHA2('cptx-SR202609170002',256)),862402,'2026-09-05 10:00:00','SUCCESS'),
('CP202609170020','STORAGE_RECEIPT','SR202609170003',SHA2('proof-SR202609170003',256),NULL,
 CONCAT('0x',SHA2('cptx-SR202609170003',256)),862403,'2026-09-06 10:00:00','SUCCESS'),
('CP202609170021','STORAGE_RECEIPT','SR202609170004',SHA2('proof-SR202609170004',256),NULL,
 CONCAT('0x',SHA2('cptx-SR202609170004',256)),862404,'2026-09-07 10:00:00','SUCCESS'),
('CP202609170022','STORAGE_RECEIPT','SR202609170005',SHA2('proof-SR202609170005',256),NULL,
 CONCAT('0x',SHA2('cptx-SR202609170005',256)),862405,'2026-09-08 10:00:00','SUCCESS'),
-- 加工批次 002~005
('CP202609170023','MILLING_BATCH','MB202609170002',SHA2('proof-MB202609170002',256),NULL,
 CONCAT('0x',SHA2('cptx-MB202609170002',256)),862602,'2026-09-07 18:00:00','SUCCESS'),
('CP202609170024','MILLING_BATCH','MB202609170003',SHA2('proof-MB202609170003',256),NULL,
 CONCAT('0x',SHA2('cptx-MB202609170003',256)),862603,'2026-09-08 18:00:00','SUCCESS'),
('CP202609170025','MILLING_BATCH','MB202609170004',SHA2('proof-MB202609170004',256),NULL,
 CONCAT('0x',SHA2('cptx-MB202609170004',256)),862604,'2026-09-09 18:00:00','SUCCESS'),
('CP202609170026','MILLING_BATCH','MB202609170005',SHA2('proof-MB202609170005',256),NULL,
 CONCAT('0x',SHA2('cptx-MB202609170005',256)),862605,'2026-09-10 18:00:00','SUCCESS'),
-- 已激活防伪码 002/004/005（003 留现场激活）
('CP202609170027','TRACE_CODE','RC20260917000002',SHA2('proof-RC20260917000002',256),NULL,
 CONCAT('0x',SHA2('cptx-RC20260917000002',256)),862702,'2026-09-07 18:31:00','SUCCESS'),
('CP202609170028','TRACE_CODE','RC20260917000004',SHA2('proof-RC20260917000004',256),NULL,
 CONCAT('0x',SHA2('cptx-RC20260917000004',256)),862704,'2026-09-09 18:31:00','SUCCESS'),
('CP202609170029','TRACE_CODE','RC20260917000005',SHA2('proof-RC20260917000005',256),NULL,
 CONCAT('0x',SHA2('cptx-RC20260917000005',256)),862705,'2026-09-10 18:31:00','SUCCESS');

-- =====================================================================
-- 14. 文件资源 file_resource（5 个，覆盖五类业务文件）
-- =====================================================================
INSERT INTO file_resource
(file_id, file_name, file_type, file_size, file_url, sha256, storage_type, biz_type, biz_id) VALUES
('FR202609170001','五常民乐基地实景-01.jpg','image/jpeg',2483200,'/uploads/files/2026/05/wuchang-base-01.jpg',SHA2('file-FR202609170001',256),'LOCAL','FIELD_PHOTO','FIELD202609170001'),
('FR202609170002','稻花香2号种子合格证.pdf','application/pdf',856320,'/uploads/files/2026/05/seed-cert-rpb01.pdf',SHA2('file-FR202609170002',256),'LOCAL','SEED_CERT','RPB202609170001'),
('FR202609170003','氯虫苯甲酰胺采购凭证.jpg','image/jpeg',1624500,'/uploads/files/2026/07/pesticide-proof.jpg',SHA2('file-FR202609170003',256),'LOCAL','INPUT_PROOF','FLOG202609170003'),
('FR202609170004','SR01入库质检报告.pdf','application/pdf',1240680,'/uploads/files/2026/09/quality-report-sr01.pdf',SHA2('file-FR202609170004',256),'LOCAL','QUALITY_REPORT','QT202609170001'),
('FR202609170005','有机产品认证证书.pdf','application/pdf',968420,'/uploads/files/2026/05/organic-cert-pb01.pdf',SHA2('file-FR202609170005',256),'LOCAL','CERTIFICATION','PB202609170001');

-- =====================================================================
-- 15. 产量平衡校验 rice_yield_balance（5 条，4 通过 + 1 异常）
-- =====================================================================
INSERT INTO rice_yield_balance
(check_id, check_scope, planting_batch_id, grain_batch_id, product_batch_id, result, items,
 chain_status, tx_id, block_height, chain_time) VALUES
('YB202609170001','FULL_CHAIN','RPB202609170001','GB202609170001','PB202609170001','PASS',
 JSON_ARRAY(
   JSON_OBJECT('stage','种植到收储','expectedKg',79670,'actualKg',79670,'deviationPercent',0.00,'result','PASS'),
   JSON_OBJECT('stage','收储到加工','expectedKg',75000,'actualKg',75000,'deviationPercent',0.00,'result','PASS'),
   JSON_OBJECT('stage','加工到成品','expectedKg',49500,'actualKg',49500,'deviationPercent',0.00,'result','PASS')),
 'SUCCESS',CONCAT('0x',SHA2('tx-YB202609170001',256)),862901,'2026-09-10 09:00:00'),
('YB202609170002','FULL_CHAIN','RPB202609170002','GB202609170002','PB202609170002','PASS',
 JSON_ARRAY(
   JSON_OBJECT('stage','种植到收储','expectedKg',153600,'actualKg',153600,'deviationPercent',0.00,'result','PASS'),
   JSON_OBJECT('stage','收储到加工','expectedKg',145000,'actualKg',145000,'deviationPercent',0.00,'result','PASS'),
   JSON_OBJECT('stage','加工到成品','expectedKg',94250,'actualKg',94250,'deviationPercent',0.00,'result','PASS')),
 'SUCCESS',CONCAT('0x',SHA2('tx-YB202609170002',256)),862902,'2026-09-10 09:10:00'),
('YB202609170003','STORAGE_TO_MILLING','RPB202609170003','GB202609170003','','FAIL',
 JSON_ARRAY(
   JSON_OBJECT('stage','湿谷折算干粮','expectedKg',101952,'actualKg',101952,'deviationPercent',0.00,'result','PASS'),
   JSON_OBJECT('stage','干粮加工出库','expectedKg',101952,'actualKg',110000,'deviationPercent',7.90,'result','FAIL')),
 'SUCCESS',CONCAT('0x',SHA2('tx-YB202609170003',256)),862903,'2026-09-10 09:20:00'),
('YB202609170004','FULL_CHAIN','RPB202609170004','GB202609170004','PB202609170004','PASS',
 JSON_ARRAY(
   JSON_OBJECT('stage','种植到收储','expectedKg',59048,'actualKg',59048,'deviationPercent',0.00,'result','PASS'),
   JSON_OBJECT('stage','收储到加工','expectedKg',56000,'actualKg',56000,'deviationPercent',0.00,'result','PASS'),
   JSON_OBJECT('stage','加工到成品','expectedKg',36400,'actualKg',36400,'deviationPercent',0.00,'result','PASS')),
 'SUCCESS',CONCAT('0x',SHA2('tx-YB202609170004',256)),862904,'2026-09-10 09:30:00'),
('YB202609170005','PLANTING_TO_STORAGE','RPB202609170005','GB202609170005','','PASS',
 JSON_ARRAY(
   JSON_OBJECT('stage','预估产量','expectedKg',65134,'actualKg',65134,'deviationPercent',0.00,'result','PASS'),
   JSON_OBJECT('stage','实际入库','expectedKg',65134,'actualKg',65134,'deviationPercent',0.00,'result','PASS')),
 'SUCCESS',CONCAT('0x',SHA2('tx-YB202609170005',256)),862905,'2026-09-10 09:40:00');

-- =====================================================================
-- 16. 防伪码生成任务 rice_generate_task（5 个）
-- =====================================================================
INSERT INTO rice_generate_task
(generate_task_id, product_batch_id, quantity, package_spec, expected_sale_region, expire_days, status) VALUES
('GT202609170001','PB202609170001',200,'5kg真空袋装','北京市、上海市',730,'SUCCESS'),
('GT202609170002','PB202609170002',150,'10kg编织袋装','广东省',730,'SUCCESS'),
('GT202609170003','PB202609170003',300,'5kg真空砖形装','全国',0,'SUCCESS'),
('GT202609170004','PB202609170004',100,'2.5kg礼盒装','黑龙江省、吉林省、辽宁省',365,'SUCCESS'),
('GT202609170005','PB202609170005',120,'5kg真空袋装','浙江省、江苏省',0,'SUCCESS');
