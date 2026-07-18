# 兔咕噜网站 - 核心接口API文档

> **基础路径**：`/api`  
> **认证方式**：除登录接口外，所有接口请求头需携带 `Authorization: Bearer {JWT_Token}`  
> **统一响应结构**：
> {
>   "code": 200,
>   "msg": "操作成功",
>   "data": { ... }
> }

---

## 1. 用户认证模块 (`/auth`)

### 1.1 员工登录
- **URL**：`POST /auth/login`
- **入参**：
{
  "username": "admin",
  "password": "123456"
}
- **出参**：
{
  "code": 200,
  "msg": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "userInfo": {
      "id": 1,
      "realName": "张三",
      "role": "admin"
    }
  }
}

---

## 2. 快递入库查询模块 (`/inbound`) —— *前台英文页面调用*

### 2.1 单号/批量/模糊查询
- **URL**：`POST /inbound/query`
- **业务逻辑**：`trackingNos` 和 `keyword` 二选一。若传`keyword`，必须 >= 6位，执行前缀模糊匹配。
- **入参**：
{
  "trackingNos": ["SF1234567890", "YT9876543210"],
  "keyword": "SF123456" 
}
- **出参（已入库）**：
{
  "code": 200,
  "msg": "查询成功",
  "data": [
    {
      "trackingNo": "SF1234567890",
      "inboundTime": "2026-07-17 14:30:00",
      "shelfNo": "A-12-03",
      "imageUrl": "https://oss.example.com/images/abc.jpg",
      "status": 1
    }
  ]
}
- **出参（未入库）**：
{
  "code": 200,
  "msg": "查询成功",
  "data": [
    {
      "trackingNo": "SF9999999999",
      "status": 0,
      "message": "该单号未入库"
    }
  ]
}

### 2.2 获取双时钟时间（前台显示）
- **URL**：`GET /common/current-time`
- **出参**：
{
  "code": 200,
  "data": {
    "beijingTime": "2026-07-17 22:30:00",
    "localTime": "2026-07-17 10:30:00",
    "utcTime": "2026-07-17 14:30:00Z"
  }
}

---

## 3. 后台CMS内容管理 (`/cms`)

### 3.1 获取指定类型内容（前台展示）
- **URL**：`GET /cms/{type}` （type: company / service / product）
- **出参**：
{
  "code": 200,
  "data": {
    "id": 1,
    "titleEn": "About Us",
    "contentEn": "<p>We are a leading logistics company...</p>",
    "images": ["https://oss.../img1.jpg"]
  }
}

### 3.2 更新内容（后台编辑保存）
- **URL**：`PUT /cms/{type}`
- **入参**：
{
  "titleEn": "About Us",
  "contentEn": "<p>Updated content...</p>",
  "contentZh": "<p>更新的中文内容...</p>",
  "images": ["https://oss.../new_img.jpg"]
}
- **出参**：{ "code": 200, "msg": "保存成功" }

---

## 4. 快递入库操作 (`/inbound`) —— *手机端/扫码枪*

### 4.1 扫描录入（支持批量提交）
- **URL**：`POST /inbound/scan`
- **入参**：
{
  "list": [
    { "trackingNo": "SF1234567890", "shelfNo": "B-05-12" },
    { "trackingNo": "YT9876543210" }
  ],
  "imageUrl": "https://oss.../batch_photo.jpg"
}
- **出参**：
{
  "code": 200,
  "msg": "全部入库成功，共 2 件",
  "data": {
    "successCount": 2,
    "failList": []
  }
}
> **注意**：接口需做幂等性校验（若单号已存在，返回提示而非报错）。

---

## 5. 工单管理模块 (`/work-order`) —— *核心逻辑*

### 5.1 分状态查询工单列表（后台Tab切换）
- **URL**：`GET /work-order/list?status=PENDING_PACK&page=1&size=20`
- **出参**：
{
  "code": 200,
  "data": {
    "total": 100,
    "records": [
      {
        "id": 1001,
        "customerId": "CUS-001",
        "batchNo": "BATCH-2026-07-01",
        "status": "PENDING_PACK",
        "totalQuantity": 0,
        "packerName": null,
        "createdAt": "2026-07-17 10:00:00"
      }
    ]
  }
}

### 5.2 创建工单（-> 待备货）
- **URL**：`POST /work-order/create`
- **入参（必填：客户编号 + 批次号）**：
{
  "customerId": "CUS-001",
  "batchNo": "BATCH-2026-07-17-A"
}
- **出参**：{ "code": 200, "msg": "工单创建成功", "data": { "orderId": 1002 } }

### 5.3 待备货 -> 待打包 (确认备货)
- **URL**：`PUT /work-order/{orderId}/start-pack`
- **出参**：{ "code": 200, "msg": "已转为待打包" }

### 5.4 待打包 -> 已打包 (确认打包，需输入件数)
- **URL**：`PUT /work-order/{orderId}/confirm-pack`
- **入参**：{ "quantity": 5 }
- **业务触发**：后端自动记录当前登录人为 `packer_id`。
- **出参**：{ "code": 200, "msg": "打包成功，当前总件数：5" }

### 5.5 已打包 -> 补货 (仍为已打包，数量累加)
- **URL**：`PUT /work-order/{orderId}/replenish`
- **入参**：{ "additionalQuantity": 3 }
- **出参**：{ "code": 200, "msg": "补货成功，当前总件数累加至：8" }

### 5.6 已打包 -> 待封箱 (封箱)
- **URL**：`PUT /work-order/{orderId}/seal`
- **出参**：{ "code": 200, "msg": "已转为待封箱" }

### 5.7 待封箱 -> 待发货 (填写重量体积)
- **URL**：`PUT /work-order/{orderId}/confirm-ship`
- **入参（重量、体积必填）**：
{
  "quantity": 8,
  "weight": 15.50,
  "volume": 0.80
}
- **业务触发**：将 `weight` 写入统计表。
- **出参**：{ "code": 200, "msg": "发货确认成功" }

### 5.8 待发货 -> 已完成
- **URL**：`PUT /work-order/{orderId}/complete`
- **出参**：{ "code": 200, "msg": "工单已完成" }

### 5.9 查询工单历史流转记录
- **URL**：`GET /work-order/{orderId}/logs`
- **出参**：
{
  "code": 200,
  "data": [
    {
      "operatorName": "张三",
      "fromStatus": "待备货",
      "toStatus": "待打包",
      "remark": "",
      "operateTime": "2026-07-17 10:05:00"
    }
  ]
}

---

## 6. 统计分析模块 (`/stat`)

### 6.1 每日入库数量统计
- **URL**：`GET /stat/inbound?startDate=2026-07-01&endDate=2026-07-17`
- **出参**：
{
  "code": 200,
  "data": [
    { "statDate": "2026-07-17", "totalCount": 256 }
  ]
}

### 6.2 每日打包重量统计（按员工+客户）
- **URL**：`GET /stat/packing?statDate=2026-07-17`
- **出参**：
{
  "code": 200,
  "data": [
    {
      "packerName": "张三",
      "customerId": "CUS-001",
      "totalWeight": 120.50
    }
  ]
}