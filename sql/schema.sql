-- 兔咕噜网站数据库初始化脚本
-- Database: tugulu_warehouse

CREATE DATABASE IF NOT EXISTS tugulu_warehouse
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE tugulu_warehouse;

-- 1. 员工/管理员表
CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  username VARCHAR(50) NOT NULL COMMENT '登录账号（唯一）',
  password VARCHAR(255) NOT NULL COMMENT 'BCrypt加密密码',
  real_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
  role VARCHAR(20) NOT NULL DEFAULT 'employee' COMMENT '角色：admin / employee',
  status TINYINT(1) NOT NULL DEFAULT 1 COMMENT '账号状态：1启用 0禁用',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工/管理员表';

-- 2. 前台CMS内容表
CREATE TABLE IF NOT EXISTS cms_content (
  id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  type VARCHAR(20) NOT NULL COMMENT '类型：company / service / product',
  title_en VARCHAR(200) DEFAULT NULL COMMENT '英文标题',
  content_en LONGTEXT DEFAULT NULL COMMENT '英文正文',
  content_zh LONGTEXT DEFAULT NULL COMMENT '中文正文',
  images TEXT DEFAULT NULL COMMENT '图片URL列表（JSON数组）',
  sort_order INT(11) DEFAULT 0 COMMENT '排序权重',
  update_by VARCHAR(50) DEFAULT NULL COMMENT '最后编辑人',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='前台CMS内容表';

-- 3. 快递入库记录表
CREATE TABLE IF NOT EXISTS inbound_order (
  id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  tracking_no VARCHAR(100) NOT NULL COMMENT '快递单号',
  shelf_no VARCHAR(50) DEFAULT NULL COMMENT '货架号',
  image_url VARCHAR(500) DEFAULT NULL COMMENT '入库图片URL',
  inbound_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
  create_by VARCHAR(50) DEFAULT NULL COMMENT '入库操作人',
  status TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态：1已入库 0已删除',
  PRIMARY KEY (id),
  KEY idx_tracking_no (tracking_no),
  KEY idx_inbound_time (inbound_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='快递入库记录表';

-- 4. 工单主表
CREATE TABLE IF NOT EXISTS work_order (
  id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID（工单号）',
  customer_id VARCHAR(50) NOT NULL COMMENT '客户编号',
  batch_no VARCHAR(100) NOT NULL COMMENT '批次号',
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING_PREP' COMMENT '工单状态',
  total_quantity INT(11) NOT NULL DEFAULT 0 COMMENT '当前总件数',
  weight DECIMAL(10,2) DEFAULT NULL COMMENT '总重量（KG）',
  volume DECIMAL(10,2) DEFAULT NULL COMMENT '总体积（m3）',
  packer_id BIGINT(20) DEFAULT NULL COMMENT '打包人ID',
  create_by BIGINT(20) NOT NULL COMMENT '工单创建人ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  version INT(11) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (id),
  KEY idx_customer_id (customer_id),
  KEY idx_packer_id (packer_id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单主表';

-- 5. 工单操作日志表
CREATE TABLE IF NOT EXISTS work_order_log (
  id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  order_id BIGINT(20) NOT NULL COMMENT '关联工单ID',
  operator_id BIGINT(20) NOT NULL COMMENT '操作人ID',
  from_status VARCHAR(20) DEFAULT NULL COMMENT '操作前状态',
  to_status VARCHAR(20) NOT NULL COMMENT '操作后状态',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  operate_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (id),
  KEY idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单操作日志表';

-- 6. 每日入库统计表
CREATE TABLE IF NOT EXISTS stat_inbound (
  id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  stat_date DATE NOT NULL COMMENT '统计日期',
  total_count INT(11) NOT NULL DEFAULT 0 COMMENT '当日入库总件数',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_stat_date (stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日入库统计表';

-- 7. 每日打包重量统计表
CREATE TABLE IF NOT EXISTS stat_packing (
  id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  stat_date DATE NOT NULL COMMENT '统计日期',
  packer_id BIGINT(20) NOT NULL COMMENT '打包员工ID',
  customer_id VARCHAR(50) NOT NULL COMMENT '客户编号',
  total_weight DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '打包总重量',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_date_packer_customer (stat_date, packer_id, customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日打包重量统计表';

-- Seed users: admin/admin123, employee/admin123 (BCrypt)
-- Hash will be ensured by backend DataInitializer if missing
INSERT INTO sys_user (username, password, real_name, role, status)
VALUES ('admin', '$2a$10$EqKcp1WFKVQISheBxmXNYeJYvFzqKxFzqKxFzqKxFzqKxFzqKxFzqK', 'Admin', 'admin', 1)
ON DUPLICATE KEY UPDATE username = username;

INSERT INTO sys_user (username, password, real_name, role, status)
VALUES ('employee', '$2a$10$EqKcp1WFKVQISheBxmXNYeJYvFzqKxFzqKxFzqKxFzqKxFzqKxFzqK', 'Employee', 'employee', 1)
ON DUPLICATE KEY UPDATE username = username;

INSERT INTO cms_content (type, title_en, content_en, content_zh, images, sort_order)
SELECT 'company', 'About TuGuLu',
       '<p>We are a professional warehouse and logistics service provider.</p>',
       '<p>Zhongwen company intro</p>', '[]', 100
WHERE NOT EXISTS (SELECT 1 FROM cms_content WHERE type = 'company');

INSERT INTO cms_content (type, title_en, content_en, content_zh, images, sort_order)
SELECT 'service', 'Our Services',
       '<p>Inbound scanning, packing, sealing and shipping services.</p>',
       '<p>Zhongwen service intro</p>', '[]', 100
WHERE NOT EXISTS (SELECT 1 FROM cms_content WHERE type = 'service');

INSERT INTO cms_content (type, title_en, content_en, content_zh, images, sort_order)
SELECT 'product', 'Our Products',
       '<p>Warehouse management and tracking solutions.</p>',
       '<p>Zhongwen product intro</p>', '[]', 100
WHERE NOT EXISTS (SELECT 1 FROM cms_content WHERE type = 'product');
