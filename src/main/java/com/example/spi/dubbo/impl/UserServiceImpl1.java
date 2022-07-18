package com.example.spi.dubbo.impl;

import com.alibaba.dubbo.common.URL;
import com.example.spi.dubbo.EatService;
import com.example.spi.dubbo.UserService;
import lombok.extern.slf4j.Slf4j;

/**
 * @author SHshuo
 * @data 2022/7/1--10:13
 */

@Slf4j
public class UserServiceImpl1 implements UserService {

//    依赖注入
    private EatService eatService;

    public void setEatService(EatService eatService) {
        this.eatService = eatService;
    }

    @Override
    public void message(URL url) {
        eatService.message(url);
        log.info("dubbo user1");
    }
}
