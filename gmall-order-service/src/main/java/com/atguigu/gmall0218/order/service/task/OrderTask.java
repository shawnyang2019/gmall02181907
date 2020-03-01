package com.atguigu.gmall0218.order.service.task;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0218.bean.OrderInfo;
import com.atguigu.gmall0218.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @Author ShawnYang
 * @Date 2020-02-28 14:47
 * @Description TODO
 * 修改人：
 * 修改时间：
 * 修改备注：
 */
@Component
@EnableScheduling
public class OrderTask {

    @Reference
    private OrderService orderService;

    @Scheduled(cron = "5 * * * * ?")
    public void test01(){
        System.out.println(Thread.currentThread().getName()+"------------------1");
    }

    @Scheduled(cron = "0/5 * * * * ?")
    public void test02(){
        System.out.println(Thread.currentThread().getName()+"------------------2");
    }

    @Scheduled(cron ="0/20 * * * * ?")
    public void checkOrder(){
       /*
       1.	查询有多少订单是过期：
            什么样的订单算是过期了？
            当前系统时间>过期时间 and 当前状态是未支付！

        2.	循环过期订单列表，进行处理！
            orderInfo
            paymentInfo
        */

        List<OrderInfo> orderInfoList =  orderService.getExpiredOrderList();

        for (OrderInfo orderInfo : orderInfoList) {
            // 关闭过期订单
            orderService.execExpiredOrder(orderInfo);
        }

    }
}
