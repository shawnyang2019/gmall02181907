package com.atguigu.gmall0218.user.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0218.bean.UserAddress;
import com.atguigu.gmall0218.bean.UserInfo;
import com.atguigu.gmall0218.config.RedisUtil;
import com.atguigu.gmall0218.service.UserService;
import com.atguigu.gmall0218.user.mapper.UserAddressMapper;
import com.atguigu.gmall0218.user.mapper.UserInfoMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import redis.clients.jedis.Jedis;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private UserAddressMapper userAddressMapper;

    @Autowired
    private RedisUtil redisUtil;

    public String userKey_prefix="user:";
    public String userinfoKey_suffix=":info";
    public int userKey_timeOut=60*60*24;


    @Override
    public List<UserInfo> findAll() {
        return userInfoMapper.selectAll();
    }

    @Override
    public List<UserAddress> getUserAddressList(String userId) {
        // 调用mapper
        // select * from userAddress where userId=?
        UserAddress userAddress = new UserAddress();
        userAddress.setUserId(userId);
        return    userAddressMapper.select(userAddress);
    }

    @Override
    public UserInfo login(UserInfo userInfo) {

        // select * from userInfo where loginName = ? and passwd=?
        /*
            1.  根据当前的sql 语句 查询是否有当前用户
            2.  将用户信息存储到缓存中！
         */
        // 202cb962ac59075b964b07152d234b70 密码需要进行加密
        String passwd = userInfo.getPasswd();
        // 对密码进行加密
        String newPwd = DigestUtils.md5DigestAsHex(passwd.getBytes());
        // 将加密后的密码赋值给当前对象
        userInfo.setPasswd(newPwd);
        //根据用户名和密码进行到用户表进行查询，如果能够查询到结果 说明存在该用户
        UserInfo info = userInfoMapper.selectOne(userInfo);

        if (info!=null){
            // 获取Jedis
            Jedis jedis = redisUtil.getJedis();
            // 放入redis ,必须起key=user:userId:info
            String userKey = userKey_prefix+info.getId()+userinfoKey_suffix;

            // 哪种数据类型 setex 表示带过期时间设置
            jedis.setex(userKey,userKey_timeOut, JSON.toJSONString(info));
            // 关闭jedis
            jedis.close();
            return info;
        }
        return null;
    }

    @Override
    public UserInfo verify(String userId) {
        Jedis jedis= null;
        try {
            // 获取jedis
            jedis = redisUtil.getJedis();
            // 定义key
            String userKey = userKey_prefix+userId+userinfoKey_suffix;

            String userJson = jedis.get(userKey);
            if (!StringUtils.isEmpty(userJson)){
                // userJson 转成对象
                UserInfo userInfo = JSON.parseObject(userJson, UserInfo.class);
                return userInfo;

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jedis!=null){
                jedis.close();
            }
        }
        return null;
    }
}
