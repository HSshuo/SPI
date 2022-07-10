package com.example.spi.dubbo.impl;

import com.example.spi.dubbo.UserService;
import lombok.extern.slf4j.Slf4j;

/**
 * @author SHshuo
 * @data 2022/7/1--10:13
 */

@Slf4j
public class UserServiceImpl1 implements UserService {
    @Override
    public void message() {
        log.info("dubbo user1");
    }
}
