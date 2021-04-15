/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package com.alibaba.mos.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.mos.api.SkuReadService;
import com.alibaba.mos.data.ChannelInventoryDO;
import com.alibaba.mos.data.SkuDO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO: 实现
 * @author superchao
 * @version $Id: SkuReadServiceImpl.java, v 0.1 2019年10月28日 10:49 AM superchao Exp $
 */
@Slf4j
@Service
public class SkuReadServiceImpl implements SkuReadService {

    /**
     * 数据问题路径
     */
    private final static String DATA_PATH = "src/main/resources/data/skus.txt";

    /**
     * 实体字段数组
     */
    private final static String[] FIELD_ARRAY = {"id", "name", "artNo", "spuId", "skuType", "price", "inventoryList"};

    /**
     * 将字符串转为实体
     * @param str
     * @return
     * @throws Exception
     */
    private static SkuDO toSkuDO(String str) {
        //id","name|artNo|spuId|skuType|price|inventoryList
        //1|货品1|A001|1001|ORIGIN|1|[{"channelCode":"MIAO","inventory":10},{"channelCode":"TMALL","inventory":5},{"channelCode":"INTIME","inventory":3}]
        if (StringUtils.isBlank(str)) {
            throw new RuntimeException("当前行数据为空");
        }
        String[] strings = str.split("\\|");
        if (strings.length != FIELD_ARRAY.length) {
            throw new RuntimeException("当前数据结构异常");
        }
        Map<String, Object> map = new HashMap<>(FIELD_ARRAY.length);
        for (int i = 0; i < FIELD_ARRAY.length - 1; i++) {
            map.put(FIELD_ARRAY[i], strings[i]);
        }
        map.put(FIELD_ARRAY[FIELD_ARRAY.length - 1], JSON.parseArray(strings[FIELD_ARRAY.length - 1], ChannelInventoryDO.class));
        //转换为实体
        return JSON.parseObject(JSONObject.toJSONString(map), SkuDO.class);
    }

    /**
     * 假设excel数据量很大无法一次性加载到内存中
     * @param handler
     */
    @Override
    public void loadSkus(SkuHandler handler) {
        String name = DATA_PATH;
        //一行一行加载
        InputStreamReader inputReader = null;
        BufferedReader bf = null;
        try {
            File file = new File(name);
            inputReader = new InputStreamReader(new FileInputStream(file));
            bf = new BufferedReader(inputReader);
            String str;
            // 按行读取字符串
            SkuDO skuDO;
            int row = 0;
            while ((str = bf.readLine()) != null) {
                row++;
                if (row == 1) {
                    continue;
                }
                //解析每行文本
                try {
                    skuDO = toSkuDO(str);
                } catch (Exception e) {
                    log.warn("[SkuReadServiceImpl::loadSkus] 第{}行[{}]解析失败！", row, str, e);
                    continue;
                }
                if (null == skuDO) {
                    log.warn("[SkuReadServiceImpl::loadSkus] 第{}行[{}]解析实体为空", row, str);
                    continue;
                }
                handler.handleSku(skuDO);
            }

        } catch (IOException e) {
            log.error("[SkuReadServiceImpl::loadSkus] 加载sku异常", e);
        } finally {
            if (null != bf) {
                try {
                    bf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != inputReader) {
                try {
                    inputReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}