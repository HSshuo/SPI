package com.example.spi;

import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.example.spi.dubbo.UserService;

/**
 * @author SHshuo
 * @data 2022/7/1--16:18
 * 每一个具体的实现类需要取一个名字name
 * @SPI 与 @Adaptive
 */
public class DubboSpi {
    public static void main(String[] args) {
        ExtensionLoader<UserService> extensionLoader = ExtensionLoader.getExtensionLoader(UserService.class);
        extensionLoader.getDefaultExtension().message(); //@SPI声明之后就是默认的
        extensionLoader.getExtension("user1").message();
    }
}
