package com.example.spi;

import com.alibaba.dubbo.common.URL;
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

//        获取扩展点加载器
        ExtensionLoader<UserService> extensionLoader = ExtensionLoader.getExtensionLoader(UserService.class);
//        @SPI声明之后就是默认的
        extensionLoader.getDefaultExtension().message(null);




//        进行依赖注入，需要添加拓展点
        URL url = new URL("http", "localhost", 8080);
//        url.addParameter("eat.service", "EatServiceImpl");

//        根据名字，从缓存中获取对应的类；如果不存在需要创建对应的扩展点实例对象；
//        首先或通过getExtensionClasses方法加载所有的扩展点，通过ClassLoader加载并将其缓存，拿到对应的扩展点实现类
        extensionLoader.getExtension("UserServiceImpl1").message(url);
    }
}
