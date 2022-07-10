package com.example.spi;

import com.example.spi.java.UserService;

import java.util.ServiceLoader;

/**
 * @author SHshuo
 * @data 2022/7/1--10:12
 */
public class JavaSpi {
    public static void main(String[] args) {
        // 接口
        ServiceLoader<UserService> services = ServiceLoader.load(UserService.class);
        for(UserService service : services){
            service.message();
        }
    }
}
