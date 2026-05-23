package com.hmdp.mq;

import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.IVoucherOrderService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
public class OrderConsumer {

    @Resource
    private IVoucherOrderService voucherOrderService;

    @RabbitListener(queues = "order.queue")
    public void handleOrder(Map<String, Object> msg, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        Long userId = Long.valueOf(msg.get("userId").toString());
        Long voucherId = Long.valueOf(msg.get("voucherId").toString());
        Long orderId = Long.valueOf(msg.get("orderId").toString());

        log.info("收到秒杀订单消息,正在处理异步写库... 订单ID: {}, 用户ID: {}", orderId, userId);

        try {
            // 1. 消费端幂等性防线
            VoucherOrder existOrder = voucherOrderService.getById(orderId);
            if (existOrder != null) {
                log.warn("MQ幂等触发,该订单 {} 已经写库成功，直接放行签收！", orderId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 2. 组装订单实体
            VoucherOrder voucherOrder = new VoucherOrder();
            voucherOrder.setId(orderId);
            voucherOrder.setUserId(userId);
            voucherOrder.setVoucherId(voucherId);

            // 3. 最终异步落盘写库
            voucherOrderService.handleVoucherOrder(voucherOrder);

            // 4. 手动确认无误，签收消息
            channel.basicAck(deliveryTag, false);
            log.info("MySQL写库成功,订单 {} 成功落盘，完成手动 ACK 签收！", orderId);

        } catch (Exception e) {
            log.error("异步写库失败,订单 {} 遭遇异常！正在执行消息拒绝签收并退回队列...", orderId, e);
            // 5. 失败兜底，拒绝签收并让消息重回队列
            channel.basicNack(deliveryTag, false, true);
        }
    }
}