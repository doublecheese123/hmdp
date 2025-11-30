package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    @Override
    public Result queryById(Long id) {
        //缓存穿透

        //互斥锁解决缓存击穿问题
//        Shop shop = queryWithMutex(id);

        //逻辑过期解决缓存击穿问题
        Shop shop = cacheClient.queryWithLogicalExpire(CACHE_SHOP_KEY, LOCK_SHOP_KEY, id, Shop.class,
                this::getById, 10L, TimeUnit.SECONDS);
        if (shop == null) {
            Result.fail("店铺不存在！");
        }
        return Result.ok(shop);
    }

//    private Shop queryWithPassThrough(Long id) {
//        String key = CACHE_SHOP_KEY + id;
//        //查询redis是否存储该数据
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        //有数据，直接返回
//        if (StrUtil.isNotBlank(shopJson)) {
//            return JSONUtil.toBean(shopJson, Shop.class);
//        }
//        //空数据也直接返回
//        if (shopJson != null) {
//            return null;
//        }
//        //无数据，查询数据库加入redis并返回数据
//        Shop shop = getById(id);
//        if (shop == null) {
//            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
//            return null;
//        }
//        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        return shop;
//    }
//
//    private Shop queryWithMutex(Long id) {
//        String key = CACHE_SHOP_KEY + id;
//        //查询redis是否存储该数据
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        if (StrUtil.isNotBlank(shopJson)) {
//            //有数据，直接返回
//            return JSONUtil.toBean(shopJson, Shop.class);
//        }
//        if ("".equals(shopJson)) {
//            return null;
//        }
//
//        String lockKey = LOCK_SHOP_KEY + id;
//        Shop shop = null;
//        try {
//            if (!tryLock(lockKey)) {
//                //获取锁不成功，休眠等待重新查询redis
//                Thread.sleep(50);
//                return queryWithMutex(id);
//            }
//            //获取成功
//            // 先再次查询redis看是否有数据，如果有则不需要重建缓存
//            shopJson = stringRedisTemplate.opsForValue().get(key);
//            if (StrUtil.isNotBlank(shopJson)) {
//                //有数据，直接返回
//                return JSONUtil.toBean(shopJson, Shop.class);
//            }
//            if ("".equals(shopJson)) {
//                return null;
//            }
//            //无数据，查询数据库
//            shop = getById(id);
//            if (shop == null) {
//                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
//                return null;
//            }
//            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        } finally {
//            //释放锁
//            unLock(lockKey);
//        }
//        return shop;
//    }
//
//    private Shop queryWithLogicalExpire(Long id) {
//        String key = CACHE_SHOP_KEY + id;
//        //查询redis是否存储该数据
//        String json = stringRedisTemplate.opsForValue().get(key);
//        //redis无数据，直接返回
//        if (StrUtil.isBlank(json)) {
//            return null;
//        }
//        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
//        LocalDateTime expireTime = redisData.getExpireTime();
//        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
//        //检查数据是否过期，没过期直接返回
//        if (expireTime.isAfter(LocalDateTime.now())) {
//            return shop;
//        }
//        //过期抢锁进行数据重建
//        String lockKey = LOCK_SHOP_KEY + id;
//        boolean isLock = tryLock(lockKey);
//        if (isLock) {
//            //抢到锁，开启新线程进行数据重建
//            CACHE_REBUILD_EXECUTOR.submit(() -> {
//                try {
//                    this.saveShop2Redis(id, 20L);
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                } finally {
//                    //释放锁
//                    unLock(lockKey);
//                }
//            });
//        }
//        //返回旧数据
//        return shop;
//    }
//
//    private boolean tryLock(String key) {
//        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_SHOP_TTL, TimeUnit.SECONDS);
//        return BooleanUtil.isTrue(flag);
//    }
//
//    private void unLock(String key) {
//        stringRedisTemplate.delete(key);
//    }
//
//    public void saveShop2Redis(Long id, Long expireSeconds) throws InterruptedException {
//        //查询店铺数据
//        Shop shop = getById(id);
//        Thread.sleep(200);
//        //封装逻辑过期时间
//        RedisData redisData = new RedisData();
//        redisData.setData(shop);
//        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
//        //写入redis
//        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
//    }

    //需保证数据库与缓存的操作同时成功或失败
    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("商铺id不能为空！");
        }
        //先更新数据库
        updateById(shop);
        //再删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
        return Result.ok();
    }

}
