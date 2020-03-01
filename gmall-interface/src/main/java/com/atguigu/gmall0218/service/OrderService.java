package com.atguigu.gmall0218.service;

import com.atguigu.gmall0218.bean.OrderInfo;
import com.atguigu.gmall0218.bean.enums.ProcessStatus;

import java.util.List;
import java.util.Map;

public interface OrderService {
    /**
     * 保存订单
     * @param orderInfo
     * @return
     */
    String saveOrder(OrderInfo orderInfo);


    /**
     * 生成流水号
     * @param userId
     * @return
     */
    String getTradeNo(String userId);

    /**
     *
     * @param userId 获取缓存的流水号
     * @param tradeCodeNo 页面的流水号
     * @return
     */
    boolean checkTradeCode(String userId, String tradeCodeNo);

    /**
     * 删除流水号
     * @param userId
     */
    void  delTradeCode(String userId);


    boolean checkStock(String skuId,Integer skuNum);

    OrderInfo getOrderInfo(String orderId);

    void updateOrderStatus(String orderId, ProcessStatus paid);

    void sendOrderStatus(String orderId);

    List<OrderInfo> getExpiredOrderList();


    void execExpiredOrder(OrderInfo orderInfo);

    Map initWareOrder(OrderInfo orderInfo);

    List<OrderInfo> splitOrder(String orderId, String wareSkuMap);
}
