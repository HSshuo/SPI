package com.example.spi.java.impl;

import com.example.spi.java.UserService;
import lombok.extern.slf4j.Slf4j;

/**
 * @author SHshuo
 * @data 2022/7/1--10:14
 */

@Slf4j
public class UserServiceImpl2 implements UserService {
    @Override
    public void message() {
        log.info("java user2");
    }
}
