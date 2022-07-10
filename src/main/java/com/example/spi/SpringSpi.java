package com.example.spi;

import com.example.spi.spring.UserService;
import org.springframework.core.io.support.SpringFactoriesLoader;

import java.util.List;

/**
 * @author SHshuo
 * @data 2022/7/1--15:12
 */
public class SpringSpi {
    public static void main(String[] args) {
        List<UserService> userServices = SpringFactoriesLoader.loadFactories(UserService.class, Thread.currentThread().getContextClassLoader());
        for(UserService userService : userServices){
            userService.message();
        }
    }
}
