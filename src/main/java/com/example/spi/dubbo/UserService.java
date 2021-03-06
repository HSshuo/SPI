package com.example.spi.dubbo;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.SPI;

/**
 * @author SHshuo
 * @data 2022/7/1--16:17
 */
@SPI("UserServiceImpl2")
public interface UserService {
    void message(URL url);
}
