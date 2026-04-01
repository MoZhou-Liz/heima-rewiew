"# heima-rewiew" 
# 黑马点评 - 高并发秒杀系统

## 项目简介
基于 Spring Boot + Redis + RabbitMQ 的优惠券秒杀系统，实现高并发场景下的库存扣减、一人一单、异步下单等核心功能。

## 技术栈
- Spring Boot 2.7.x
- Redis（缓存、分布式锁、预减库存）
- Redisson（分布式锁）
- RabbitMQ（异步下单）
- MyBatis-Plus
- MySQL
- Swagger（接口文档）

## 核心功能
- [x] 优惠券查询（Redis 缓存，防穿透）
- [x] 优惠券秒杀（Redis 预减库存 + Redisson 分布式锁）
- [x] 一人一单（分布式锁）
- [x] 异步下单（RabbitMQ 削峰填谷）
- [x] 幂等性（MQ 消费者防重复消费）

## 快速启动
1. 克隆项目：`git clone https://github.com/SuMu-Liz/heima-review.git`
2. 导入 SQL 文件（`db/` 目录下）
3. 修改 `application.yml` 中的 Redis、MySQL、RabbitMQ 地址
4. 启动 Spring Boot 应用
5. 访问 Swagger 文档：`http://localhost:8081/swagger-ui.html`

## 项目亮点
- 使用 Redis 预减库存 + Lua 脚本保证原子性
- Redisson 分布式锁实现一人一单
- RabbitMQ 异步下单，提升吞吐量
- 接口幂等设计，支持安全重试
- Swagger 自动生成接口文档，方便调用
