package com.atguigu.gmall0218.payment;


import com.atguigu.gmall0218.config.ActiveMQUtil;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.jms.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallPaymentApplicationTests {

    @Autowired
    private ActiveMQUtil activeMQUtil;
    @Test
    public void contextLoads() {
    }

    @Test
    public void testM() throws JMSException {
        Connection connection = activeMQUtil.getConnection();
//		ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory("tcp://192.168.67.219:61616");
//		Connection connection = activeMQConnectionFactory.createConnection();
        connection.start();
        // 第一个参数：是否开启事务
        // 第二个参数：表示开启/关闭事务的相应配置参数，
//        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Session session = connection.createSession(true, Session.SESSION_TRANSACTED); // 必须提交

        Queue atguigu = session.createQueue("atguigu-tools");

        MessageProducer producer = session.createProducer(atguigu);

        ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();
        activeMQTextMessage.setText("睡醒了，要吃饭了！");
        producer.send(activeMQTextMessage);

        session.commit();

        producer.close();
        session.close();
        connection.close();
    }

}
