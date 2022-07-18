package com.example.spi.dubbo;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.Adaptive;
import com.alibaba.dubbo.common.extension.SPI;

/**
 * @author SHshuo
 * @data 2022/7/17--9:53
 */

@SPI("EatServiceImpl")
public interface EatService {

    @Adaptive
    void message(URL url);
}
