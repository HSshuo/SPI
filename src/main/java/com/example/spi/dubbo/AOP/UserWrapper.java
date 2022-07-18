package com.example.spi.dubbo.AOP;

import com.alibaba.dubbo.common.URL;
import com.example.spi.dubbo.UserService;
import lombok.extern.slf4j.Slf4j;

/**
 * @author SHshuo
 * @data 2022/7/18--10:56
 *
 * 整个实现UserService接口的切面
 */
@Slf4j
public class UserWrapper implements UserService {

    private UserService userService;

    public UserWrapper(UserService userService){
        this.userService = userService;
    }

    @Override
    public void message(URL url) {
        log.info("wrapper");
        userService.message(url);
    }
}
