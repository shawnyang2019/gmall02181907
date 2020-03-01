package com.atguigu.gmall0218.order.service.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0218.bean.PaymentInfo;
import com.atguigu.gmall0218.bean.enums.ProcessStatus;
import com.atguigu.gmall0218.service.PaymentService;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

/**
 * @Author ShawnYang
 * @Date 2020-02-28 11:30
 * @Description TODO
 * 修改人：
 * 修改时间：
 * 修改备注：
 */
@Component
public class PaymentConsumer {

    @Reference
    private PaymentService paymentService;

    @JmsListener(destination = "PAYMENT_RESULT_CHECK_QUEUE",containerFactory = "jmsQueueListener")
    public void paymentResultCheck(MapMessage mapMessage) throws JMSException {
        // 通过mapMessage获取
        String outTradeNo = mapMessage.getString("outTradeNo");
        int delaySec = mapMessage.getInt("delaySec");
        int checkCount = mapMessage.getInt("checkCount");

        // 创建一个paymentInfo
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOutTradeNo(outTradeNo);
        PaymentInfo paymentInfoQuery = paymentService.getPaymentInfo(paymentInfo);
        // 调用 paymentService.checkPayment(paymentInfoQuery);
        boolean flag = paymentService.checkPayment(paymentInfoQuery);
        System.out.println("检查结果："+flag);
        if (!flag && checkCount!=0){
            // 还需要继续检查
            System.out.println("检查的次数："+checkCount);
            paymentService.sendDelayPaymentResult(outTradeNo,delaySec,checkCount-1);
        }
    }


}

