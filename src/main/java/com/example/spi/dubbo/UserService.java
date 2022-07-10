package com.example.spi.dubbo;

import com.alibaba.dubbo.common.extension.SPI;

/**
 * @author SHshuo
 * @data 2022/7/1--16:17
 */
@SPI("user2")
public interface UserService {
    void message();
}
