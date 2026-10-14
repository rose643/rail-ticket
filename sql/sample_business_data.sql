-- RailTicket 管理端完整业务样例数据（MySQL 8.0+）
-- 使用前请先执行 init_rail_ticket.sql。
-- 数据规模：60 个车站、25 个车次、每车 8 个经停站、连续 7 天运行计划，
--           所有合法起止区间的三种席别票价，以及每个相邻区段的三种席别库存。
-- 除预置管理员外，不写入普通用户、实名、乘车人、订单、消息或通知数据。
-- 固定 BIGINT 仅用于样例数据之间建立稳定关联；应用新增数据仍使用雪花 ID。
-- 本脚本会先删除 rail_ticket 库所有业务数据，再重新写入样例数据。
-- 删除不可恢复，请仅在本地开发或专用测试数据库执行。

USE rail_ticket;

START TRANSACTION;

-- =========================================================
-- 0. 清空所有业务数据（按照依赖关系从子表到父表）
-- 当前表结构没有声明物理外键，但仍保持业务依赖顺序，便于后续增加外键。
-- =========================================================
DELETE FROM user_notification;
DELETE FROM message_consume_record;
DELETE FROM message_outbox;
DELETE FROM order_status_log;
DELETE FROM ticket_order;
DELETE FROM seat_inventory;
DELETE FROM train_fare;
DELETE FROM train_schedule;
DELETE FROM train_stop;
DELETE FROM passenger;
DELETE FROM user_identity;
DELETE FROM train;
DELETE FROM station;
DELETE FROM sys_user;

SET @sample_base_date = DATE_ADD(CURDATE(), INTERVAL 1 DAY);

-- =========================================================
-- 1. 预置管理员
-- 用户名：rose；密码：zhang2580
-- =========================================================
INSERT INTO sys_user (id, username, password_hash, phone, role, status)
VALUES (
    1900000000000000001,
    'rose',
    '$2b$12$PvIoDlfBpUtZcOUt0IOiGeco98DM5CqPDkvBK.Yu4CrdhckLz.25O',
    NULL,
    'ADMIN',
    1
)
ON DUPLICATE KEY UPDATE
    password_hash = VALUES(password_hash),
    role = VALUES(role),
    status = VALUES(status);

-- =========================================================
-- 2. 车站：60 个
-- 保留原有常用编码，例如 VNP=北京南、AOH=上海虹桥。
-- =========================================================
INSERT INTO station (id, code, name, city, status, version)
VALUES
    (2000000000000000001, 'BJP', '北京站',       '北京',   1, 0),
    (2000000000000000002, 'VNP', '北京南站',     '北京',   1, 0),
    (2000000000000000003, 'BXP', '北京西站',     '北京',   1, 0),
    (2000000000000000004, 'TJP', '天津站',       '天津',   1, 0),
    (2000000000000000005, 'TXP', '天津西站',     '天津',   1, 0),
    (2000000000000000006, 'SJW', '石家庄站',     '石家庄', 1, 0),
    (2000000000000000007, 'BMP', '保定东站',     '保定',   1, 0),
    (2000000000000000008, 'CBP', '沧州西站',     '沧州',   1, 0),
    (2000000000000000009, 'DIP', '德州东站',     '德州',   1, 0),
    (2000000000000000010, 'JNK', '济南站',       '济南',   1, 0),
    (2000000000000000011, 'JGK', '济南西站',     '济南',   1, 0),
    (2000000000000000012, 'TMK', '泰安站',       '泰安',   1, 0),
    (2000000000000000013, 'QAK', '曲阜东站',     '济宁',   1, 0),
    (2000000000000000014, 'ZEK', '枣庄站',       '枣庄',   1, 0),
    (2000000000000000015, 'XCH', '徐州东站',     '徐州',   1, 0),
    (2000000000000000016, 'BMH', '蚌埠南站',     '蚌埠',   1, 0),
    (2000000000000000017, 'NKH', '南京南站',     '南京',   1, 0),
    (2000000000000000018, 'ZEH', '镇江南站',     '镇江',   1, 0),
    (2000000000000000019, 'ESH', '常州北站',     '常州',   1, 0),
    (2000000000000000020, 'WGH', '无锡东站',     '无锡',   1, 0),
    (2000000000000000021, 'OHH', '苏州北站',     '苏州',   1, 0),
    (2000000000000000022, 'AOH', '上海虹桥站',   '上海',   1, 0),
    (2000000000000000023, 'HZH', '杭州东站',     '杭州',   1, 0),
    (2000000000000000024, 'EPH', '嘉兴南站',     '嘉兴',   1, 0),
    (2000000000000000025, 'NHH', '宁波站',       '宁波',   1, 0),
    (2000000000000000026, 'SLH', '绍兴北站',     '绍兴',   1, 0),
    (2000000000000000027, 'YWH', '义乌站',       '义乌',   1, 0),
    (2000000000000000028, 'JBH', '金华站',       '金华',   1, 0),
    (2000000000000000029, 'VRH', '温州南站',     '温州',   1, 0),
    (2000000000000000030, 'HFH', '合肥南站',     '合肥',   1, 0),
    (2000000000000000031, 'UAH', '六安站',       '六安',   1, 0),
    (2000000000000000032, 'WHN', '武汉站',       '武汉',   1, 0),
    (2000000000000000033, 'OYN', '信阳东站',     '信阳',   1, 0),
    (2000000000000000034, 'ZAF', '郑州东站',     '郑州',   1, 0),
    (2000000000000000035, 'LLF', '洛阳龙门站',   '洛阳',   1, 0),
    (2000000000000000036, 'EAY', '西安北站',     '西安',   1, 0),
    (2000000000000000037, 'HDY', '华山北站',     '渭南',   1, 0),
    (2000000000000000038, 'CWQ', '长沙南站',     '长沙',   1, 0),
    (2000000000000000039, 'YIQ', '岳阳东站',     '岳阳',   1, 0),
    (2000000000000000040, 'IZQ', '广州南站',     '广州',   1, 0),
    (2000000000000000041, 'IOQ', '深圳北站',     '深圳',   1, 0),
    (2000000000000000042, 'VQZ', '惠州北站',     '惠州',   1, 0),
    (2000000000000000043, 'NXG', '南昌西站',     '南昌',   1, 0),
    (2000000000000000044, 'SRG', '上饶站',       '上饶',   1, 0),
    (2000000000000000045, 'FZS', '福州站',       '福州',   1, 0),
    (2000000000000000046, 'XKS', '厦门北站',     '厦门',   1, 0),
    (2000000000000000047, 'QYS', '泉州站',       '泉州',   1, 0),
    (2000000000000000048, 'QDK', '青岛站',       '青岛',   1, 0),
    (2000000000000000049, 'QHK', '青岛北站',     '青岛',   1, 0),
    (2000000000000000050, 'WFK', '潍坊站',       '潍坊',   1, 0),
    (2000000000000000051, 'ZBK', '淄博站',       '淄博',   1, 0),
    (2000000000000000052, 'UYK', '临沂北站',     '临沂',   1, 0),
    (2000000000000000053, 'KZK', '日照西站',     '日照',   1, 0),
    (2000000000000000054, 'MDK', '济南东站',     '济南',   1, 0),
    (2000000000000000055, 'SBT', '沈阳北站',     '沈阳',   1, 0),
    (2000000000000000056, 'QTP', '秦皇岛站',     '秦皇岛', 1, 0),
    (2000000000000000057, 'TSP', '唐山站',       '唐山',   1, 0),
    (2000000000000000058, 'DFT', '大连北站',     '大连',   1, 0),
    (2000000000000000059, 'VAB', '哈尔滨西站',   '哈尔滨', 1, 0),
    (2000000000000000060, 'CRT', '长春西站',     '长春',   1, 0);

-- =========================================================
-- 3. 车次：25 个
-- =========================================================
INSERT INTO train (id, train_no, name, status)
VALUES
    (2100000000000000001, 'G101',  '北京南—上海虹桥', 1),
    (2100000000000000002, 'G105',  '北京南—杭州东',   1),
    (2100000000000000003, 'G79',   '北京西—深圳北',   1),
    (2100000000000000004, 'D3108', '上海虹桥—宁波',   1),
    (2100000000000000005, 'G1724', '武汉—上海虹桥',   1),
    (2100000000000000006, 'G1631', '上海虹桥—厦门北', 1),
    (2100000000000000007, 'G1061', '北京—青岛北',     1),
    (2100000000000000008, 'G232',  '青岛—上海虹桥',   1),
    (2100000000000000009, 'G55',   '北京西—西安北',   1),
    (2100000000000000010, 'G1918', '西安北—上海虹桥', 1),
    (2100000000000000011, 'G2055', '郑州东—深圳北',   1),
    (2100000000000000012, 'G7684', '合肥南—宁波',     1),
    (2100000000000000013, 'G1382', '南昌西—上海虹桥', 1),
    (2100000000000000014, 'G1638', '福州—上海虹桥',   1),
    (2100000000000000015, 'D3234', '厦门北—杭州东',   1),
    (2100000000000000016, 'G902',  '哈尔滨西—北京南', 1),
    (2100000000000000017, 'G1226', '沈阳北—上海虹桥', 1),
    (2100000000000000018, 'G3501', '大连北—北京',     1),
    (2100000000000000019, 'G1190', '深圳北—郑州东',   1),
    (2100000000000000020, 'G1302', '广州南—上海虹桥', 1),
    (2100000000000000021, 'G260',  '青岛—武汉',       1),
    (2100000000000000022, 'G5556', '临沂北—杭州东',   1),
    (2100000000000000023, 'D2288', '宁波—广州南',     1),
    (2100000000000000024, 'G1842', '济南—西安北',     1),
    (2100000000000000025, 'G99',   '上海虹桥—深圳北', 1);

-- =========================================================
-- 4. 路线定义：每个车次恰好 8 个经停站
-- 使用 JSON_TABLE 将紧凑的路线数组展开，stationSequence 对应上方车站序号。
-- =========================================================
DROP TEMPORARY TABLE IF EXISTS sample_train_route;
CREATE TEMPORARY TABLE sample_train_route (
    train_sequence INT NOT NULL,
    stop_index INT NOT NULL,
    station_sequence INT NOT NULL,
    stop_count INT NOT NULL,
    PRIMARY KEY (train_sequence, stop_index)
) ENGINE = MEMORY;

INSERT INTO sample_train_route (train_sequence, stop_index, station_sequence, stop_count)
SELECT route.train_sequence,
       route.stop_ordinal - 1,
       route.station_sequence,
       8
FROM JSON_TABLE(
    '[
      {"train":1,  "stations":[2,5,11,15,16,17,21,22]},
      {"train":2,  "stations":[2,5,11,15,17,21,22,23]},
      {"train":3,  "stations":[3,6,34,32,38,40,42,41]},
      {"train":4,  "stations":[22,21,20,24,23,26,27,25]},
      {"train":5,  "stations":[32,31,30,17,19,20,21,22]},
      {"train":6,  "stations":[22,24,23,26,28,29,45,46]},
      {"train":7,  "stations":[1,57,4,8,11,51,50,49]},
      {"train":8,  "stations":[48,50,51,10,52,15,17,22]},
      {"train":9,  "stations":[3,7,6,33,34,35,37,36]},
      {"train":10, "stations":[36,37,35,34,15,17,21,22]},
      {"train":11, "stations":[34,33,32,39,38,40,42,41]},
      {"train":12, "stations":[30,17,18,19,20,21,23,25]},
      {"train":13, "stations":[43,44,28,27,23,24,21,22]},
      {"train":14, "stations":[45,44,28,27,23,24,21,22]},
      {"train":15, "stations":[46,47,45,44,28,27,26,23]},
      {"train":16, "stations":[59,60,55,56,57,4,5,2]},
      {"train":17, "stations":[55,56,57,4,11,15,17,22]},
      {"train":18, "stations":[58,55,56,57,4,5,2,1]},
      {"train":19, "stations":[41,42,40,38,39,32,33,34]},
      {"train":20, "stations":[40,38,32,43,44,23,21,22]},
      {"train":21, "stations":[48,50,51,10,52,34,33,32]},
      {"train":22, "stations":[52,53,15,16,17,22,24,23]},
      {"train":23, "stations":[25,29,45,47,46,43,38,40]},
      {"train":24, "stations":[10,11,12,13,15,34,35,36]},
      {"train":25, "stations":[22,23,27,28,44,45,40,41]}
    ]',
    '$[*]' COLUMNS (
        train_sequence INT PATH '$.train',
        NESTED PATH '$.stations[*]' COLUMNS (
            stop_ordinal FOR ORDINALITY,
            station_sequence INT PATH '$'
        )
    )
) AS route;

-- =========================================================
-- 5. 经停站：200 条
-- 时间为相对始发日 00:00 的分钟数；前端显示为当日/次日 HH:mm。
-- 每个车次的始发时间不同，相邻站约 55 分钟，停站 5 分钟。
-- =========================================================
INSERT INTO train_stop
    (id, train_id, station_id, stop_index, arrival_offset_minutes, departure_offset_minutes)
SELECT 2200000000000000000 + route.train_sequence * 100 + route.stop_index,
       2100000000000000000 + route.train_sequence,
       2000000000000000000 + route.station_sequence,
       route.stop_index,
       CASE
           WHEN route.stop_index = 0 THEN NULL
           ELSE 360 + MOD(route.train_sequence - 1, 8) * 45 + route.stop_index * 55
       END,
       CASE
           WHEN route.stop_index = route.stop_count - 1 THEN NULL
           ELSE 360 + MOD(route.train_sequence - 1, 8) * 45 + route.stop_index * 55 +
                CASE WHEN route.stop_index = 0 THEN 0 ELSE 5 END
       END
FROM sample_train_route AS route;

-- =========================================================
-- 6. 运行计划：25 个车次 × 连续 7 天 = 175 条
-- 第 5 个车次的第 4 个运行日设为停售，用于验证状态过滤。
-- =========================================================
INSERT INTO train_schedule (id, train_id, travel_date, status)
SELECT 2300000000000000000 + train_sequence * 100 + sample_day.day_offset,
       2100000000000000000 + train_sequence,
       DATE_ADD(@sample_base_date, INTERVAL sample_day.day_offset DAY),
       CASE WHEN train_sequence = 5 AND sample_day.day_offset = 3 THEN 0 ELSE 1 END
FROM (
    SELECT 1 AS train_sequence UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL
    SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL
    SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10 UNION ALL SELECT 11 UNION ALL
    SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15 UNION ALL
    SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL
    SELECT 20 UNION ALL SELECT 21 UNION ALL SELECT 22 UNION ALL SELECT 23 UNION ALL
    SELECT 24 UNION ALL SELECT 25
) AS sample_train
CROSS JOIN (
    SELECT 0 AS day_offset UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL
    SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6
) AS sample_day;

-- =========================================================
-- 7. 区间票价：每车 28 个合法区间 × 3 种席别 × 25 车 = 2100 条
-- =========================================================
INSERT INTO train_fare
    (id, train_id, seat_type_code, from_stop_index, to_stop_index, price)
SELECT 2400000000000000000
           + (from_stop.train_id - 2100000000000000000) * 100000
           + from_stop.stop_index * 1000
           + to_stop.stop_index * 10
           + seat_type.seat_no,
       from_stop.train_id,
       seat_type.seat_type_code,
       from_stop.stop_index,
       to_stop.stop_index,
       ROUND(
           (to_stop.stop_index - from_stop.stop_index)
               * (55 + MOD(from_stop.train_id - 2100000000000000000, 7) * 5)
               * seat_type.price_factor,
           2
       )
FROM train_stop AS from_stop
INNER JOIN train_stop AS to_stop
        ON to_stop.train_id = from_stop.train_id
       AND to_stop.stop_index > from_stop.stop_index
CROSS JOIN (
    SELECT 1 AS seat_no, 'SECOND_CLASS' AS seat_type_code, 1.00 AS price_factor
    UNION ALL SELECT 2, 'FIRST_CLASS', 1.60
    UNION ALL SELECT 3, 'BUSINESS_CLASS', 3.10
) AS seat_type
WHERE from_stop.train_id BETWEEN 2100000000000000001 AND 2100000000000000025;

-- =========================================================
-- 8. 区段库存：175 个运行计划 × 7 个相邻区段 × 3 席别 = 3675 条
-- =========================================================
INSERT INTO seat_inventory
    (id, schedule_id, seat_type_code, segment_index, total, available)
SELECT 2500000000000000000
           + (schedule.id - 2300000000000000000) * 1000
           + current_stop.stop_index * 10
           + capacity.seat_no,
       schedule.id,
       capacity.seat_type_code,
       current_stop.stop_index,
       CASE capacity.seat_type_code
           WHEN 'SECOND_CLASS' THEN 560 + MOD(schedule.train_id - 2100000000000000000, 5) * 20
           WHEN 'FIRST_CLASS' THEN 80 + MOD(schedule.train_id - 2100000000000000000, 4) * 10
           ELSE 24 + MOD(schedule.train_id - 2100000000000000000, 3) * 4
       END,
       CASE capacity.seat_type_code
           WHEN 'SECOND_CLASS' THEN 560 + MOD(schedule.train_id - 2100000000000000000, 5) * 20
           WHEN 'FIRST_CLASS' THEN 80 + MOD(schedule.train_id - 2100000000000000000, 4) * 10
           ELSE 24 + MOD(schedule.train_id - 2100000000000000000, 3) * 4
       END
FROM train_schedule AS schedule
INNER JOIN train_stop AS current_stop ON current_stop.train_id = schedule.train_id
INNER JOIN train_stop AS next_stop
        ON next_stop.train_id = current_stop.train_id
       AND next_stop.stop_index = current_stop.stop_index + 1
CROSS JOIN (
    SELECT 1 AS seat_no, 'SECOND_CLASS' AS seat_type_code
    UNION ALL SELECT 2, 'FIRST_CLASS'
    UNION ALL SELECT 3, 'BUSINESS_CLASS'
) AS capacity
WHERE schedule.train_id BETWEEN 2100000000000000001 AND 2100000000000000025;

DROP TEMPORARY TABLE sample_train_route;

COMMIT;

-- =========================================================
-- 9. 执行结果检查
-- 预期：60 station、25 train、200 train_stop、175 train_schedule、
--       2100 train_fare、3675 seat_inventory；每个车次最少 8 站。
-- =========================================================
SELECT @sample_base_date AS first_travel_date,
       DATE_ADD(@sample_base_date, INTERVAL 6 DAY) AS last_travel_date;

SELECT 'admin_user' AS table_name, COUNT(*) AS row_count
FROM sys_user
WHERE username = 'rose' AND role = 'ADMIN'
UNION ALL
SELECT 'station', COUNT(*) FROM station
WHERE id BETWEEN 2000000000000000001 AND 2000000000000000060
UNION ALL
SELECT 'train', COUNT(*) FROM train
WHERE id BETWEEN 2100000000000000001 AND 2100000000000000025
UNION ALL
SELECT 'train_stop', COUNT(*) FROM train_stop
WHERE train_id BETWEEN 2100000000000000001 AND 2100000000000000025
UNION ALL
SELECT 'train_schedule', COUNT(*) FROM train_schedule
WHERE train_id BETWEEN 2100000000000000001 AND 2100000000000000025
UNION ALL
SELECT 'train_fare', COUNT(*) FROM train_fare
WHERE train_id BETWEEN 2100000000000000001 AND 2100000000000000025
UNION ALL
SELECT 'seat_inventory', COUNT(*)
FROM seat_inventory AS inventory
INNER JOIN train_schedule AS schedule ON schedule.id = inventory.schedule_id
WHERE schedule.train_id BETWEEN 2100000000000000001 AND 2100000000000000025;

SELECT train.train_no,
       train.name,
       COUNT(train_stop.id) AS stop_count,
       MIN(train_stop.stop_index) AS first_stop_index,
       MAX(train_stop.stop_index) AS last_stop_index
FROM train
INNER JOIN train_stop ON train_stop.train_id = train.id
WHERE train.id BETWEEN 2100000000000000001 AND 2100000000000000025
GROUP BY train.id, train.train_no, train.name
ORDER BY train.train_no;

SELECT MIN(route_summary.stop_count) AS minimum_stops_per_train,
       MAX(route_summary.stop_count) AS maximum_stops_per_train
FROM (
    SELECT train_id, COUNT(*) AS stop_count
    FROM train_stop
    WHERE train_id BETWEEN 2100000000000000001 AND 2100000000000000025
    GROUP BY train_id
) AS route_summary;
