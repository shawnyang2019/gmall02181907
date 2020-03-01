package com.atguigu.gmall0218.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0218.bean.UserInfo;
import com.atguigu.gmall0218.passport.config.JwtUtil;
import com.atguigu.gmall0218.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PassportController {

    @Value("${token.key}")
    private String key;

    // 调用服务层
    @Reference
    private UserService userService;

    @RequestMapping("index")
    public String index(HttpServletRequest request){
        // 需要登录的页面发送登录请求的时候，带上originUrl参数，下面的代码获取originUrl
        String originUrl = request.getParameter("originUrl");
        // 保存originUrl 到请求域中 并放入index的隐藏域中，当登录成功后就跳转到这个originUrl
        request.setAttribute("originUrl",originUrl);
        return "index";
    }

    // 控制器获取页面的数据
    @RequestMapping("login")
    @ResponseBody
    public String login(UserInfo userInfo,HttpServletRequest request){
        // salt 服务器的IP 地址
        String salt = request.getHeader("X-forwarded-for");
        // 调用登录方法，把用户信息放入redis中
       UserInfo info = userService.login(userInfo);
       if (info!=null){
           // 如果登录成之后,返回token!
           // 如何制作token!
           HashMap<String , Object> map = new HashMap<>();
           map.put("userId",info.getId());
           map.put("nickName",info.getNickName());
           // 生成token
           String token = JwtUtil.encode(key, map, salt);

           return token;
       }else {
           return "fail";
       }
    }

    // http://passport.atguigu.com/verify?token=xxx&salt=x
    @RequestMapping("verify")
    @ResponseBody
    public String verify(HttpServletRequest request){
//        1.	获取服务器的Ip，token
//        2.	key+ip ,解密token 得到用户的信息 userId,nickName
//        3.	判断用户是否登录：key=user:userId:info  value=userInfo
//        4.	userInfo!=null true success; false fail;
//        String salt = request.getHeader("X-forwarded-for");
        String token = request.getParameter("token");
        String salt = request.getParameter("salt");

        // 调用jwt工具类
        Map<String, Object> map = JwtUtil.decode(token, key, salt);

        if (map!=null && map.size()>0){
            // 获取userId
            String userId = (String) map.get("userId");
            // 调用服务层查询用户是否已经登录
            UserInfo userInfo = userService.verify(userId);
            if (userInfo!=null){
                return "success";
            }else {
                return "fail";
            }
        }
        return "fail";

    }

}
