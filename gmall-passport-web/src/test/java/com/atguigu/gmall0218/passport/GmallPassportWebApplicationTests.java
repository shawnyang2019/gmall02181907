package com.atguigu.gmall0218.passport;

import com.atguigu.gmall0218.passport.config.JwtUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallPassportWebApplicationTests {

	@Test
	public void contextLoads() {
	}

	@Test
	public void testJWT(){
		String key = "atguigu";
		HashMap<String , Object> map = new HashMap<>();
		map.put("userId",1001);
		map.put("nickName","admin");
		String salt = "192.168.67.123";
		String token = JwtUtil.encode(key, map, salt);

		System.out.println("token:"+token);
		// token=eyJhbGciOiJIUzI1NiJ9.eyJuaWNrTmFtZSI6ImFkbWluIiwidXNlcklkIjoxMDAxfQ.CZr6TCs7v3AOl1GxMCMgvhQIpOrz-CJlYv16cRq_a3M

		// 解密token
		Map<String, Object> maps = JwtUtil.decode(token, key, "192.168.67.223");
		System.out.println(maps);

	}

}
