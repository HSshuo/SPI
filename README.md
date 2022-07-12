# java SPI
## 介绍
- 破坏了双亲委派机制。
- Server Provider Interface，服务发现机制。将接口实现类的全限定名配置在文件中，并由服务加载读取配置文件，加载实现类。
- java通过ServiceLoader类实现，定义在目录META-INF/services文件夹，在里面定义具体的实现类。通过反射获取实例。
## 原理
- 获取定义在META-INF/services目录下的全限定名，通过反射方法Class.forName()加载类，并用newInstance方法将类实例化，并将实例化的类缓存到List对象，然后返回。
## 缺陷
- 不能按需加载，需要遍历所有的实现，并实例化，然后在循环中才能找到我们需要的实现。如果不想用某些实现类，或者某些类实例化很耗时，它也被载入并实例化了，这就造成了浪费。
- 获取某个实现类的方式不够灵活，只能通过Iterator形式获取，不能根据某个参数来获取对应的实现类。
- 多个并发多线程使用ServiceLoader类的实例是不安全的。
## 参考
[SPI机制详解](https://pdai.tech/md/java/advanced/java-advanced-spi.html)


# Spring SPI
感觉大致与java相同，只不过换了方法（SpringFactoriesLoader.loadFactories）

# Dubbo SPI
