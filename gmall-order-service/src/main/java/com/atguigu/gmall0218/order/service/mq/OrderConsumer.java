package com.atguigu.gmall0218.order.service.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0218.bean.PaymentInfo;
import com.atguigu.gmall0218.bean.enums.ProcessStatus;
import com.atguigu.gmall0218.service.OrderService;
import com.atguigu.gmall0218.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;
//消费者必须在容器中才能被扫描到 然后进行消费消息
@Component
public class OrderConsumer {

    @Reference
    private OrderService orderService;
    // 获取消息队列中的数据
    /**
     * destination 表示监听的队列名称
     * containerFactory 配置文件中注入的消息监听器工厂
     */
    @JmsListener(destination = "PAYMENT_RESULT_QUEUE",containerFactory = "jmsQueueListener")
    public void consumerPaymentResult(MapMessage mapMessage) throws JMSException {
        // 通过mapMessage获取
        String orderId = mapMessage.getString("orderId");
        String result = mapMessage.getString("result");
        // 支付成功
        if ("success".equals(result)){
            // 更新订单状态 至已支付
            orderService.updateOrderStatus(orderId, ProcessStatus.PAID);
            // 发送消息给库存模块（库存模块里面有一个消费者）
            orderService.sendOrderStatus(orderId);
            // 更新订单状态 为已经通知仓库
            orderService.updateOrderStatus(orderId, ProcessStatus.NOTIFIED_WARE);
        }
    }

    @JmsListener(destination = "SKU_DEDUCT_QUEUE",containerFactory = "jmsQueueListener")
    public void consumeSkuDeduct(MapMessage mapMessage) throws JMSException {
        // 通过mapMessage获取
        String orderId = mapMessage.getString("orderId");
        String status = mapMessage.getString("status");
        if ("DEDUCTED".equals(status)){
            orderService.updateOrderStatus(orderId, ProcessStatus.DELEVERED);
        }
    }

}
