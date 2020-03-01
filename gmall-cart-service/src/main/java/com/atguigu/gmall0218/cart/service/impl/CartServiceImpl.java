package com.atguigu.gmall0218.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0218.bean.CartInfo;
import com.atguigu.gmall0218.bean.SkuInfo;
import com.atguigu.gmall0218.cart.constant.CartConst;
import com.atguigu.gmall0218.cart.mapper.CartInfoMapper;
import com.atguigu.gmall0218.config.RedisUtil;
import com.atguigu.gmall0218.service.CartService;
import com.atguigu.gmall0218.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

@Service
@Component
public class CartServiceImpl implements CartService {
    @Autowired
    private CartInfoMapper cartInfoMapper;

    @Reference
    private ManageService manageService;

    @Autowired
    private RedisUtil redisUtil;
    
    // 添加登录，还是未登录！在控制器去判断！\
    // 登录时添加购物车！
    //
    @Override
    public void addToCart(String skuId, String userId, Integer skuNum) {
        /*
            1.  先查询一下购物车中是否有相同的商品，如果有则数量相加
            2.  如果没有，直接添加到数据库！
            3.  更新缓存！
         */
        // 获取Jedis
        Jedis jedis = redisUtil.getJedis();

        // 定义购物车的key=user:userId:cart  用户key=user:userId:info
        String cartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;

        // 采用哪种数据类型来存储！hash
        // key = user:userId:cart field = skuId value=CartInfo的字符串

        // 先通过skuId，userId 查询一下 是否有该商品！
        // select * from cartInfo where userId = ? and skuId = ?
        CartInfo cartInfo = new CartInfo();
        cartInfo.setUserId(userId);
        cartInfo.setSkuId(skuId);
        // skuNum = 2
        CartInfo cartInfoExist  = cartInfoMapper.selectOne(cartInfo);
        // 有相同的商品
        if (cartInfoExist!=null){
            // 数量相加 skuNum = 1
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum()+skuNum);

            // 给skuPrice 初始化操作！ skuPrice = cartPrice
            cartInfoExist.setSkuPrice(cartInfoExist.getCartPrice());

            // 更新数据 skuNum = 3;
            cartInfoMapper.updateByPrimaryKeySelective(cartInfoExist);

            // 同步缓存！
            // jedis.hset(cartKey,skuId, JSON.toJSONString(cartInfoExist));

        }else {
            // 没有相同的商品！
            // cartInfo 数据来源于商品详情页面！ 也就是来源于skuInfo
            // 根据skuId 查询skuInfo
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);
            CartInfo cartInfo1 = new CartInfo();
            // 属性赋值
            cartInfo1.setSkuId(skuId);
            cartInfo1.setCartPrice(skuInfo.getPrice());
            cartInfo1.setSkuPrice(skuInfo.getPrice());
            cartInfo1.setSkuName(skuInfo.getSkuName());
            cartInfo1.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo1.setUserId(userId);
            cartInfo1.setSkuNum(skuNum);

            // 添加到数据库
            cartInfoMapper.insertSelective(cartInfo1);
            cartInfoExist = cartInfo1;
            // 同步缓存！
            // jedis.hset(cartKey,skuId, JSON.toJSONString(cartInfo1));
        }
        // 将数据放入缓存！
        jedis.hset(cartKey,skuId, JSON.toJSONString(cartInfoExist));

        // 缓存的过期时间！
        // 购物车需要过期时间么？ 不去设置失效时间
        // 设置失效时间？ 与用户的过期时间一致！
        // 获取一用户的过期时间
        // 得到用户的key key=user:userId:info
        String userKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USERINFOKEY_SUFFIX;

        // 如何获取userkey 的过期时间
        Long ttl = jedis.ttl(userKey);

        // 给购物车设置过期时间
        jedis.expire(cartKey,ttl.intValue());
        // 关闭redis
        jedis.close();

    }

    @Override
    public List<CartInfo> getCartList(String userId) {

        List<CartInfo> cartInfoList = new ArrayList<>();
        /*
            1.  如果购物车在缓存中存在 看购物车！
            2   如果缓存中不存在 看数据库，并将数据放入缓存！
         */
        // 获取jedis
        Jedis jedis = redisUtil.getJedis();
        // 定义购物车的key=user:userId:cart  用户key=user:userId:info
        String cartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        // 从key 中获取数据
        //  jedis.hgetAll(cartKey); 返回map key = field value = cartInfo 字符串
        List<String> stringList = jedis.hvals(cartKey);// 返回List集合 String = cartInfo 字符串
        // 从缓存中获取数据
        if (stringList!=null && stringList.size()>0){
            // 循环遍历
            for (String cartInfoStr : stringList) {
                // cartInfoStr 转换为对象CartInfo 并添加到集合
                cartInfoList.add(JSON.parseObject(cartInfoStr,CartInfo.class));
            }
            // 查看的时候应该做排序！真实项目按照更新时间 {模拟按照id 进行排序}
            cartInfoList.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    // 定义比较规则
                    // compareTo 比较规则： s1 = abc s2=abcd
                    return o1.getId().compareTo(o2.getId());
                }
            });
            return cartInfoList;
        }else {
            // 从数据库获取数据 order by ,并添加到缓存！
            cartInfoList = loadCartCache(userId);
            return cartInfoList;
        }

    }

    @Override
    public List<CartInfo> mergeToCartList(List<CartInfo> cartListCK, String userId) {
        /*
            未登录： 33 1 ， 34 2
            登录：34 1 ，36 1
            匹配之后：33 1 ，34 3
            合并 ：33 1 ，34 3，36 1
         */

        // 根据userId 获取购物车数据
        List<CartInfo> cartInfoListDB = cartInfoMapper.selectCartListWithCurPrice(userId);
        // 开始合并 合并条件：skuId 相同 匹配上 更新数量
        for (CartInfo cartInfoCK : cartListCK) {
            // 定义一个boolean 类型变量 默认值给false
            boolean isMatch =false;
            for (CartInfo cartInfoDB : cartInfoListDB) {
                if (cartInfoCK.getSkuId().equals(cartInfoDB.getSkuId())){
                    // 将数量进行相加
                    cartInfoDB.setSkuNum(cartInfoCK.getSkuNum()+cartInfoDB.getSkuNum());
                    // 修改数据库
                    cartInfoMapper.updateByPrimaryKeySelective(cartInfoDB);
                    // 给true
                    isMatch=true;
                }
            }
            // 没有匹配上！ cookie里面没有匹配上 插入数据库
            if (!isMatch){
                // 未登录的对象添加到数据库
                // 将用户Id 赋值给未登录对象
                cartInfoCK.setUserId(userId);
                cartInfoMapper.insertSelective(cartInfoCK);
            }
        }
        // 最终将合并之后的数据返回！
        List<CartInfo> cartInfoList = loadCartCache(userId);
        return cartInfoList;
    }

    @Override
    public void checkCart(String skuId, String isChecked, String userId) {
        /*
            1.  获取Jedis 客户端
            2.  获取购物车
            3.  直接修改skuId 商品的勾选状态 isChecked
            4.  写回购物车
-------------------------------------------------------------------------
            5.  新建一个购物车来存储勾选的商品！
         */

        // 获取jedis
        Jedis jedis = redisUtil.getJedis();
        // 定义购物车的key=user:userId:cart  用户key=user:userId:info
        String cartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;

        //cartKey redis数据库的key   skuId 属性名称，数据库中cartInfoJson数据值对应的key
        String cartInfoJson = jedis.hget(cartKey, skuId);
        // 将其转换为对象
        CartInfo cartInfo = JSON.parseObject(cartInfoJson, CartInfo.class);

        cartInfo.setIsChecked(isChecked);

        // 写回购物车
        jedis.hset(cartKey,skuId,JSON.toJSONString(cartInfo));

        // 新建一个购物车key user:userId:checked
        String cartKeyChecked = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CHECKED_KEY_SUFFIX;
        // isChecked=1 是勾选商品
        if ("1".equals(isChecked)){
            jedis.hset(cartKeyChecked,skuId,JSON.toJSONString(cartInfo));
        }else {
            // 删除被勾选的商品
            jedis.hdel(cartKeyChecked,skuId);
        }

        jedis.close();
    }

    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        List<CartInfo> cartInfoList = new ArrayList<>();

        // 获取被选中的购物车集合
        /*
            1. 获取Jedis
            2. 定义key
            3. 获取数据并返回！
         */
        // 获取jedis
        Jedis jedis = redisUtil.getJedis();
        // 被选中的购物车
        String cartKeyChecked = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CHECKED_KEY_SUFFIX;

        List<String> stringList = jedis.hvals(cartKeyChecked);
        // 循环判断
        if (stringList!=null && stringList.size()>0){
            for (String cartJson : stringList) {
                cartInfoList.add(JSON.parseObject(cartJson,CartInfo.class));
            }

        }

        jedis.close();

        return cartInfoList;
    }

    @Override
    // 根据userId 查询购物车 {skuPrice 实时价格}
    public List<CartInfo> loadCartCache(String userId) {
        // select * from cartInfo where userId = ? 不可取！查询不到实时价格！
        // cartInfo , skuInfo 从这两张表中查询！
        List<CartInfo> cartInfoList = cartInfoMapper.selectCartListWithCurPrice(userId);

        if (cartInfoList==null || cartInfoList.size()==0){
            return  null;
        }
        // 获取jedis
        Jedis jedis = redisUtil.getJedis();
        // 定义购物车的key=user:userId:cart  用户key=user:userId:info
        String cartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;

        // cartInfoList 从数据库查询到的数据放入 redis
//        for (CartInfo cartInfo : cartInfoList) {
//            jedis.hset(cartKey,cartInfo.getSkuId(),JSON.toJSONString(cartInfo));
//        }

        HashMap<String, String> map = new HashMap<>();
        for (CartInfo cartInfo : cartInfoList) {
            map.put(cartInfo.getSkuId(),JSON.toJSONString(cartInfo));
        }

        // 一次放入多条数据
        jedis.hmset(cartKey,map);

        jedis.close();
        return cartInfoList;
    }
}
