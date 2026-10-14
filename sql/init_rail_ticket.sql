-- RailTicket 完整初始化脚本（MySQL 8.0+）
-- 警告：执行本脚本会删除 rail_ticket 数据库及其中的全部数据。
-- 所有业务表主键均由 MyBatis-Plus ASSIGN_ID 生成，因此不使用 AUTO_INCREMENT。
-- 席别使用 Java 枚举 SeatType，数据库保存 seat_type_code，不创建 seat_type 表。

DROP DATABASE IF EXISTS rail_ticket;

CREATE DATABASE rail_ticket
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE rail_ticket;

-- ============================================================
-- 1. 系统用户
-- ============================================================
CREATE TABLE sys_user (
    id BIGINT NOT NULL COMMENT '雪花 ID',
    username VARCHAR(64) NOT NULL COMMENT '唯一登录账号，统一保存为小写',
    password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码摘要',
    phone VARCHAR(20) DEFAULT NULL COMMENT '账号绑定手机号',
    role VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色：USER、ADMIN',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-正常，0-禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username),
    UNIQUE KEY uk_sys_user_phone (phone),
    CONSTRAINT ck_sys_user_status CHECK (status IN (0, 1))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='系统用户';

-- ============================================================
-- 2. 用户实名认证
-- 一个账号只能绑定一个实名身份，同一证件只能绑定一个账号。
-- 本项目暂存证件号码明文以简化学习；生产环境应使用加密字段和检索哈希。
-- ============================================================
CREATE TABLE user_identity (
    id BIGINT NOT NULL COMMENT '雪花 ID',
    user_id BIGINT NOT NULL COMMENT '所属系统用户 ID',
    real_name VARCHAR(64) NOT NULL COMMENT '真实姓名',
    id_type VARCHAR(20) NOT NULL COMMENT '证件类型',
    id_no VARCHAR(64) NOT NULL COMMENT '证件号码',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '认证状态：1-已认证，0-认证失效',
    verified_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '认证时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_identity_user (user_id),
    UNIQUE KEY uk_user_identity_document (id_type, id_no),
    CONSTRAINT ck_user_identity_status CHECK (status IN (0, 1))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='用户实名认证';

-- ============================================================
-- 3. 常用乘车人
-- user_id 表示谁维护了该联系人，不代表乘车人自己的系统账号。
-- 删除联系人不会删除或影响已经生成的历史订单。
-- ============================================================
CREATE TABLE passenger (
    id BIGINT NOT NULL COMMENT '雪花 ID',
    user_id BIGINT NOT NULL COMMENT '联系人所属用户 ID',
    name VARCHAR(64) NOT NULL COMMENT '乘车人姓名',
    id_type VARCHAR(20) NOT NULL DEFAULT 'ID_CARD' COMMENT '证件类型',
    id_no VARCHAR(64) NOT NULL COMMENT '证件号码',
    phone VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_passenger_user_document (user_id, id_type, id_no),
    KEY idx_passenger_user_id (user_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='常用乘车人';

-- ============================================================
-- 4. 车站
-- ============================================================
CREATE TABLE station (
    id BIGINT NOT NULL COMMENT '雪花 ID',
    code VARCHAR(10) NOT NULL COMMENT '车站唯一编码，例如 BJP',
    name VARCHAR(64) NOT NULL COMMENT '车站名称',
    city VARCHAR(64) NOT NULL COMMENT '所属城市',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-停用',
    version INT NOT NULL DEFAULT 0 COMMENT 'MyBatis-Plus 乐观锁版本号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_station_code (code),
    KEY idx_station_city_name (city, name),
    CONSTRAINT ck_station_status CHECK (status IN (0, 1)),
    CONSTRAINT ck_station_version_nonnegative CHECK (version >= 0)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='车站';

-- ============================================================
-- 5. 车次基础信息
-- 本表只描述车次本身，起点、终点和经停站由 train_stop 决定。
-- ============================================================
CREATE TABLE train (
    id BIGINT NOT NULL COMMENT '雪花 ID',
    train_no VARCHAR(32) NOT NULL COMMENT '车次号，例如 G100',
    name VARCHAR(64) NOT NULL COMMENT '车次展示名称',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-停运',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_train_train_no (train_no),
    CONSTRAINT ck_train_status CHECK (status IN (0, 1))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='车次基础信息';

-- ============================================================
-- 6. 车次经停站
-- 时间偏移量相对于始发日 00:00，可超过 1440 表示次日到达或发车。
-- ============================================================
CREATE TABLE train_stop (
    id BIGINT NOT NULL COMMENT '雪花 ID',
    train_id BIGINT NOT NULL COMMENT '车次 ID',
    station_id BIGINT NOT NULL COMMENT '车站 ID',
    stop_index INT NOT NULL COMMENT '经停顺序，从 0 开始',
    arrival_offset_minutes INT DEFAULT NULL COMMENT '相对始发日 00:00 的到达分钟数；始发站可为空',
    departure_offset_minutes INT DEFAULT NULL COMMENT '相对始发日 00:00 的发车分钟数；终点站可为空',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_train_stop_train_index (train_id, stop_index),
    UNIQUE KEY uk_train_stop_train_station (train_id, station_id),
    KEY idx_train_stop_station_train (station_id, train_id),
    CONSTRAINT ck_train_stop_index_nonnegative CHECK (stop_index >= 0),
    CONSTRAINT ck_train_stop_arrival_nonnegative
        CHECK (arrival_offset_minutes IS NULL OR arrival_offset_minutes >= 0),
    CONSTRAINT ck_train_stop_departure_nonnegative
        CHECK (departure_offset_minutes IS NULL OR departure_offset_minutes >= 0)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='车次经停站';

-- ============================================================
-- 7. 车次运行日
-- travel_date 表示列车从始发站出发的日期。
-- ============================================================
CREATE TABLE train_schedule (
    id BIGINT NOT NULL COMMENT '雪花 ID',
    train_id BIGINT NOT NULL COMMENT '车次 ID',
    travel_date DATE NOT NULL COMMENT '始发日期',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-可售，0-停运或停售',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_train_schedule_train_date (train_id, travel_date),
    KEY idx_train_schedule_date_status (travel_date, status),
    CONSTRAINT ck_train_schedule_status CHECK (status IN (0, 1))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='车次运行日';

-- ============================================================
-- 8. 车次区间票价
-- ============================================================
CREATE TABLE train_fare (
    id BIGINT NOT NULL COMMENT '雪花 ID',
    train_id BIGINT NOT NULL COMMENT '车次 ID',
    seat_type_code VARCHAR(32) NOT NULL COMMENT '席别编码，对应 SeatType 枚举',
    from_stop_index INT NOT NULL COMMENT '出发站顺序',
    to_stop_index INT NOT NULL COMMENT '到达站顺序',
    price DECIMAL(10,2) NOT NULL COMMENT '票价',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_train_fare_route_seat (
        train_id,
        seat_type_code,
        from_stop_index,
        to_stop_index
    ),
    KEY idx_train_fare_train_route (train_id, from_stop_index, to_stop_index),
    CONSTRAINT ck_train_fare_stop_order CHECK (from_stop_index < to_stop_index),
    CONSTRAINT ck_train_fare_price_nonnegative CHECK (price >= 0)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='车次区间票价';

-- ============================================================
-- 9. 区段库存
-- segment_index=0 表示第 0 站到第 1 站之间的库存。
-- 购买 0 到 4 区间时，需要同时扣减 0、1、2、3 四个区段。
-- ============================================================
CREATE TABLE seat_inventory (
    id BIGINT NOT NULL COMMENT '雪花 ID',
    schedule_id BIGINT NOT NULL COMMENT '运行日 ID',
    seat_type_code VARCHAR(32) NOT NULL COMMENT '席别编码，对应 SeatType 枚举',
    segment_index INT NOT NULL COMMENT '区段起始站顺序',
    total INT NOT NULL COMMENT '总库存',
    available INT NOT NULL COMMENT '可售库存',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_seat_inventory_segment (
        schedule_id,
        seat_type_code,
        segment_index
    ),
    KEY idx_seat_inventory_schedule_seat (schedule_id, seat_type_code),
    CONSTRAINT ck_seat_inventory_segment_nonnegative CHECK (segment_index >= 0),
    CONSTRAINT ck_seat_inventory_total_nonnegative CHECK (total >= 0),
    CONSTRAINT ck_seat_inventory_available_range
        CHECK (available >= 0 AND available <= total)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='运行日席别区段库存';

-- ============================================================
-- 10. 车票订单（一期：一张订单对应一位乘车人的一张车票）
-- user_id 是购票人账号；passenger_user_id 是实际乘车人的账号。
-- passenger_user_id 在乘车人没有已认证账号时允许为空，后续实名认证时会按实名快照回填。
-- 乘车人快照用于保证联系人修改或删除后，历史订单内容仍保持不变。
-- ============================================================
CREATE TABLE ticket_order (
    id BIGINT NOT NULL COMMENT '雪花 ID',
    order_no VARCHAR(32) NOT NULL COMMENT '对外业务订单号',
    user_id BIGINT NOT NULL COMMENT '购票用户 ID，也是订单所有人',
    passenger_id BIGINT NOT NULL COMMENT '下单时选择的常用乘车人 ID，仅作来源记录',
    passenger_user_id BIGINT DEFAULT NULL COMMENT '实际乘车人的系统用户 ID，无对应实名账号时为空',
    passenger_name VARCHAR(64) NOT NULL COMMENT '下单时乘车人姓名快照',
    passenger_id_type VARCHAR(20) NOT NULL COMMENT '下单时证件类型快照',
    passenger_id_no VARCHAR(64) NOT NULL COMMENT '下单时证件号码快照',
    passenger_phone VARCHAR(20) DEFAULT NULL COMMENT '下单时联系电话快照',
    schedule_id BIGINT NOT NULL COMMENT '运行日 ID',
    seat_type_code VARCHAR(32) NOT NULL COMMENT '席别编码，对应 SeatType 枚举',
    from_stop_index INT NOT NULL COMMENT '出发站顺序',
    to_stop_index INT NOT NULL COMMENT '到达站顺序',
    amount DECIMAL(10,2) NOT NULL COMMENT '订单金额',
    status VARCHAR(20) NOT NULL COMMENT 'PENDING_PAYMENT、PAID、CLOSED、REFUNDED',
    request_id VARCHAR(64) NOT NULL COMMENT '客户端创建订单幂等键',
    expire_at DATETIME NOT NULL COMMENT '待支付订单过期时间',
    paid_at DATETIME DEFAULT NULL COMMENT '支付成功时间',
    refunded_at DATETIME DEFAULT NULL COMMENT '退票时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ticket_order_order_no (order_no),
    UNIQUE KEY uk_ticket_order_user_request (user_id, request_id),
    KEY idx_ticket_order_user_created (user_id, created_at),
    KEY idx_ticket_order_passenger_user_created (passenger_user_id, created_at),
    KEY idx_ticket_order_passenger_document_created (
        passenger_id_type,
        passenger_id_no,
        passenger_name,
        created_at
    ),
    KEY idx_ticket_order_status_expire (status, expire_at),
    KEY idx_ticket_order_schedule (schedule_id),
    CONSTRAINT ck_ticket_order_stop_order CHECK (from_stop_index < to_stop_index),
    CONSTRAINT ck_ticket_order_amount_nonnegative CHECK (amount >= 0),
    CONSTRAINT ck_ticket_order_status CHECK (
        status IN ('PENDING_PAYMENT', 'PAID', 'CLOSED', 'REFUNDED')
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='车票订单';

-- ============================================================
-- 11. 订单状态流转日志
-- ============================================================
CREATE TABLE order_status_log (
    id BIGINT NOT NULL COMMENT '雪花 ID',
    order_id BIGINT NOT NULL COMMENT '订单 ID',
    from_status VARCHAR(20) DEFAULT NULL COMMENT '变更前状态；创建订单时为空',
    to_status VARCHAR(20) NOT NULL COMMENT '变更后状态',
    reason VARCHAR(255) DEFAULT NULL COMMENT '状态变更原因',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_order_status_log_order_created (order_id, created_at),
    CONSTRAINT ck_order_status_log_from_status CHECK (
        from_status IS NULL
        OR from_status IN ('PENDING_PAYMENT', 'PAID', 'CLOSED', 'REFUNDED')
    ),
    CONSTRAINT ck_order_status_log_to_status CHECK (
        to_status IN ('PENDING_PAYMENT', 'PAID', 'CLOSED', 'REFUNDED')
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='订单状态流转日志';

-- ============================================================
-- 12. 事务消息表
-- 订单事务与待发送消息在同一 MySQL 事务中提交，RabbitMQ 不可用时可持续重试。
-- ============================================================
CREATE TABLE message_outbox (
    id BIGINT NOT NULL COMMENT '雪花 ID，同时作为 RabbitMQ messageId',
    aggregate_type VARCHAR(32) NOT NULL COMMENT '聚合类型，例如 TICKET_ORDER',
    aggregate_id BIGINT NOT NULL COMMENT '聚合根 ID，例如订单 ID',
    event_type VARCHAR(64) NOT NULL COMMENT '事件类型，例如 ORDER_TIMEOUT',
    payload VARCHAR(1000) NOT NULL COMMENT '消息载荷',
    status VARCHAR(20) NOT NULL DEFAULT 'NEW' COMMENT 'NEW、SENDING、SENT、FAILED',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '已重试次数',
    next_retry_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下次允许抢占或重试时间，同时作为发送租约截止时间',
    claim_token VARCHAR(64) DEFAULT NULL COMMENT '当前发布任务的抢占令牌',
    last_error VARCHAR(500) DEFAULT NULL COMMENT '最近一次发送失败原因',
    sent_at DATETIME DEFAULT NULL COMMENT '发送成功时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_message_outbox_publish (status, next_retry_at, created_at),
    UNIQUE KEY uk_message_outbox_aggregate_event (
        aggregate_type,
        aggregate_id,
        event_type
    ),
    CONSTRAINT ck_message_outbox_status CHECK (status IN ('NEW', 'SENDING', 'SENT', 'FAILED')),
    CONSTRAINT ck_message_outbox_retry_nonnegative CHECK (retry_count >= 0)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='事务消息表';

-- ============================================================
-- 13. MQ 消费记录表（Inbox）
-- 同一消息可以由多个消费者分别处理，同一消费者只能成功登记一次。
-- ============================================================
CREATE TABLE message_consume_record (
    id BIGINT NOT NULL COMMENT '雪花 ID',
    message_id BIGINT NOT NULL COMMENT '对应 message_outbox.id 或外部 MQ messageId',
    consumer_name VARCHAR(64) NOT NULL COMMENT '消费者名称，用于区分不同消费逻辑',
    status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING' COMMENT 'PROCESSING、SUCCESS',
    consumed_at DATETIME DEFAULT NULL COMMENT '消费成功时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_message_consume_consumer (message_id, consumer_name),
    KEY idx_message_consume_status_created (status, created_at),
    CONSTRAINT ck_message_consume_status CHECK (status IN ('PROCESSING', 'SUCCESS'))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='MQ 消费幂等记录表';

-- ============================================================
-- 14. 用户站内通知
-- event_id 使用 Outbox 消息 ID，同一用户不会重复生成同一事件的通知。
-- ============================================================
CREATE TABLE user_notification (
    id BIGINT NOT NULL COMMENT '雪花 ID',
    user_id BIGINT NOT NULL COMMENT '被通知用户 ID',
    notification_type VARCHAR(64) NOT NULL COMMENT '通知类型，例如 ORDER_PAID',
    title VARCHAR(128) NOT NULL COMMENT '通知标题',
    content VARCHAR(1000) NOT NULL COMMENT '通知内容',
    business_type VARCHAR(32) NOT NULL COMMENT '关联业务类型，例如 TICKET_ORDER',
    business_id BIGINT NOT NULL COMMENT '关联业务 ID，例如订单 ID',
    event_id BIGINT NOT NULL COMMENT '产生本通知的 Outbox 消息 ID',
    read_status TINYINT NOT NULL DEFAULT 0 COMMENT '阅读状态：0-未读，1-已读',
    read_at DATETIME DEFAULT NULL COMMENT '阅读时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_notification_event (user_id, event_id),
    KEY idx_user_notification_read_created (user_id, read_status, created_at),
    KEY idx_user_notification_business (business_type, business_id),
    CONSTRAINT ck_user_notification_read_status CHECK (read_status IN (0, 1))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='用户站内通知表';
