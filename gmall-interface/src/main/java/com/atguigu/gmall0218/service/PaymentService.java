package com.atguigu.gmall0218.service;

import com.atguigu.gmall0218.bean.PaymentInfo;

import java.util.Map;

/**
 * @Author ShawnYang
 * @Date 2020-02-27 9:24
 * @Description TODO
 * 修改人：
 * 修改时间：
 * 修改备注：
 */
public interface PaymentService {


    void savePaymentInfo(PaymentInfo paymentInfo);

    PaymentInfo getPaymentInfo(PaymentInfo paymentInfoQuery);

    void updatePaymentInfo(String out_trade_no, PaymentInfo paymentInfoUPD);

    void sendPaymentResult(PaymentInfo paymentInfo, String success);

    boolean refund(String orderId);

    Map createNative(String orderId, String s);

    //每隔15s主动向支付宝询问订单是否执行成功
    public void sendDelayPaymentResult(String outTradeNo,int delaySec ,int checkCount);

    boolean checkPayment(PaymentInfo paymentInfoQuery);

    void closePayment(String id);
}
