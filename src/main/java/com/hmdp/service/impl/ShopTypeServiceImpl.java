package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public Result queryTypeList() {
        String key = "cache:shop-type:";
        String shopTypeJson = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(shopTypeJson)) {
            try {
                ShopType[] shopTypeArray = OBJECT_MAPPER.readValue(shopTypeJson, ShopType[].class);
                List<ShopType> resultList = Arrays.asList(shopTypeArray);
                return Result.ok(resultList);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        List<ShopType> typeList = query().orderByAsc("sort").list();
        if (typeList == null || typeList.size() == 0) {
            return Result.fail("商铺类型不存在！");
        }
        try {
            String typeListStr = OBJECT_MAPPER.writeValueAsString(typeList);
            stringRedisTemplate.opsForValue().set(key, typeListStr);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return Result.ok(typeList);
    }
}
