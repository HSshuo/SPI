package com.example.spi.dubbo.impl;

import com.alibaba.dubbo.common.URL;
import com.example.spi.dubbo.EatService;
import lombok.extern.slf4j.Slf4j;

/**
 * @author SHshuo
 * @data 2022/7/17--9:53
 */
@Slf4j
public class EatServiceImpl implements EatService {
    @Override
    public void message(URL url) {
        log.info("user正在吃饭");
    }
}
