/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package com.alibaba.mos.api;

/**
 *
 * @author superchao
 * @version $Id: ItemService.java, v 0.1 2019年10月28日 12:00 PM superchao Exp $
 */
public interface ItemService<T> {
    /**
     * 执行生产者消费
     * @param
     */
    void aggregation();
}