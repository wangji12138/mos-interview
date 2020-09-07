/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package com.alibaba.mos.service;

import com.alibaba.mos.api.ItemService;
import com.alibaba.mos.api.SkuReadService;
import com.alibaba.mos.data.ItemDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author superchao
 * @version $Id: ItemServiceImpl.java, v 0.1 2019年11月20日 3:06 PM superchao Exp $
 */
@Service
public class ItemServiceImpl implements ItemService<ItemDO> {
    @Autowired
    SkuReadService skuReadService;

    @Override
    public void aggregation() {
        // 聚合商品数据并通过com.alibaba.mos.dao.ItemDAO将其保存到虚拟数据库中

    }
}