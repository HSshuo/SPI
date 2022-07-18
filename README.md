# java SPI
## 介绍
- 破坏了双亲委派机制，参考：[双亲委派机制](https://blog.nowcoder.net/n/73bff2394e0c4179aa67a745458f17d7)
- Server Provider Interface，服务发现机制。将接口实现类的全限定名配置在文件中，并由服务加载读取配置文件，加载实现类。
- java通过**ServiceLoader**类实现，定义在目录 META-INF/services 文件夹，在里面定义具体的实现类。通过反射获取实例。

## 原理
- 获取定义在 META-INF/services 目录下的全限定名，通过反射方法Class.forName()加载类，并用newInstance方法将类实例化，并将实例化的类缓存到List对象，然后返回。

## 缺陷
- 不能按需加载，需要遍历所有的实现，并实例化，然后在循环中才能找到我们需要的实现。如果不想用某些实现类，或者某些类实例化很耗时，它也被载入并实例化了，这就造成了浪费。
- **配置文件中只是简单的列出了所有的拓展实现，而没有给他们命名，导致获取某个实现类的方式不够灵活，只能通过循环的形式获取，不能根据某个参数来获取对应的实现类**。
- 扩展之间彼此存在依赖，做不到自动注入和装配，不提供上下文的IOC和AOP功能。
- 扩展很难和其他的容器框架集成，比如扩展依赖一个外部Spring容器中的bean，原生的JDK SPI并不支持。
- 多个并发多线程使用ServiceLoader类的实例是不安全的。

## 参考
[SPI机制详解](https://pdai.tech/md/java/advanced/java-advanced-spi.html)

<br>
<br>

# Spring SPI
感觉大致与java相同，只不过换了方法（SpringFactoriesLoader.loadFactories）

<br>
<br>

# Dubbo SPI

## Dubbo中 SPI 的基础流程与使用
- 可以获取指定的实现类；
- 支持 AOP、IOC (URL、@SPI、@Adaptive)

<br>

##### 关键源码类
- ExtensionLoader：扩展点加载器接口

       type: 表示Class<?> 接口
       ObjectFactory: 表示ExtensionFactory 扩展类实例工厂
具体的实现ExtensionLoader接口：
  1. AdaptiveExtensionFactory
  2. SpiExtensionFactory
  3. SpringExtensionFactory

<br>

- getExtension(name)获取具体扩展点实现类，首先会从cachedInstances缓存中获取;
- 如果没有会通过单例模式创建 createExtension(name)：
  1. 通过getExtensionClasses()，加载目录下所有的扩展点实现类，通过ClassLoader加载并放入缓存中，根据name获取到对应的扩展点实现类；普通实现类放入：extensionClasses，AOP类放入：cachedWrapperClasses
  2. injectExtension(instance)，IOC，依赖注入只能通过set方法注入，并获取依赖类的类型、名字，通过objectFactory来创建扩展点实例；
  3. cachedWrapperClasses，AOP，通过getExtensionClasses()读取到的AOP代理类，存放在cachedWrapperClasses中。

<br>
<br>

##### 源码解析
1. 通过ExtensionLoader.getExtensionLoader(UserService.class); 获取所有的拓展点加载器，放到一个ConcurrentHashMap中;
```java
    public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) {
    
//        判断传入的类是否为空、接口、被@SPI注解修饰
        if (type == null)
            throw new IllegalArgumentException("Extension type == null");
        if (!type.isInterface()) {
            throw new IllegalArgumentException("Extension type(" + type + ") is not interface!");
        }
        if (!withExtensionAnnotation(type)) {
            throw new IllegalArgumentException("Extension type(" + type +
                    ") is not extension, because WITHOUT @" + SPI.class.getSimpleName() + " Annotation!");
        }

//        从缓存中获取扩展点加载器、如果没有创建对应的加载器并返回
        ExtensionLoader<T> loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
        if (loader == null) {
            EXTENSION_LOADERS.putIfAbsent(type, new ExtensionLoader<T>(type));
            loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
        }
        return loader;
    }
```

2. 通过构造函数创建扩展点加载器
```java
    private ExtensionLoader(Class<?> type) {
        this.type = type;
        objectFactory = (type == ExtensionFactory.class ? null : ExtensionLoader.getExtensionLoader(ExtensionFactory.class).getAdaptiveExtension());
    }
```

3. 通过 extensionLoader.getExtension()获取具体的实现类
- [final域指令重排](https://blog.nowcoder.net/n/e9a87d630a764f948c56303d120f5b1e)
- [单例模式](https://blog.nowcoder.net/n/713ea320f7e8491182d939340197e557)

```java

private final ConcurrentMap<String, Holder<Object>> cachedInstances = new ConcurrentHashMap<String, Holder<Object>>();

    public T getExtension(String name) {
        if (name == null || name.length() == 0)
            throw new IllegalArgumentException("Extension name == null");
        
//        获取默认的实体类名字，@SPI注解里面的value属性就是默认
        if ("true".equals(name)) {
            return getDefaultExtension();
        }
        
        Holder<Object> holder = cachedInstances.get(name);
        if (holder == null) {
            cachedInstances.putIfAbsent(name, new Holder<Object>());
            holder = cachedInstances.get(name);
        }
        
//        单例模式、DCL双重检测
        Object instance = holder.get();
        if (instance == null) {
            synchronized (holder) {
                instance = holder.get();
                if (instance == null) {
                    instance = createExtension(name);
                    holder.set(instance);
                }
            }
        }
        return (T) instance;
    }
```

4. 如果cachedInstances不存在实例，需要通过createExtension创建扩展点实例对象
```java
    private T createExtension(String name) {
//        获取扩展点对应的类(通过读取文件里面所有的实现类名字，用ClassLoader加载具体的实现类)
        Class<?> clazz = getExtensionClasses().get(name);
        if (clazz == null) {
            throw findException(name);
        }
        
        
        try {
//            获取实例对象，并存入缓存中
            T instance = (T) EXTENSION_INSTANCES.get(clazz);
            if (instance == null) {
                EXTENSION_INSTANCES.putIfAbsent(clazz, clazz.newInstance());
                instance = (T) EXTENSION_INSTANCES.get(clazz);
            }
            
//            依赖注入 IOC
            injectExtension(instance);
            
//            AOP
            Set<Class<?>> wrapperClasses = cachedWrapperClasses;
            if (wrapperClasses != null && !wrapperClasses.isEmpty()) {
                for (Class<?> wrapperClass : wrapperClasses) {
                    instance = injectExtension((T) wrapperClass.getConstructor(type).newInstance(instance));
                }
            }
            return instance;
        } catch (Throwable t) {
            throw new IllegalStateException("Extension instance(name: " + name + ", class: " +
                    type + ")  could not be instantiated: " + t.getMessage(), t);
        }
    }
```

5. injectExtension(), 依赖注入只能通过set方法注入，并获取依赖类的类型、名字，通过objectFactory来创建扩展点实例；
```java
    private T injectExtension(T instance) {
        try {
            if (objectFactory != null) {
                for (Method method : instance.getClass().getMethods()) {
                    
                    
//                    说明只能是set注入
                    if (method.getName().startsWith("set")
                            && method.getParameterTypes().length == 1
                            && Modifier.isPublic(method.getModifiers())) {
                        Class<?> pt = method.getParameterTypes()[0];
                        try {
                            String property = method.getName().length() > 3 ? method.getName().substring(3, 4).toLowerCase() + method.getName().substring(4) : "";
                            
                            
//                            将获取到的依赖的类，重新执行一遍流程getExtensionLoader()、getAdaptiveExtension()
                            Object object = objectFactory.getExtension(pt, property);
                            if (object != null) {
                                method.invoke(instance, object);
                            }
                        } catch (Exception e) {
                            logger.error("fail to inject via method " + method.getName()
                                    + " of interface " + type.getName() + ": " + e.getMessage(), e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return instance;
    }
```

6. AOP， 通过getExtensionClasses()读取到的AOP代理类，存放在cachedWrapperClasses中
```java
//   cachedWrapperClasses是getExtensionClasses读取到的AOP代理类，存放在cachedWrapperClasses中
   Set<Class<?>> wrapperClasses = cachedWrapperClasses;


    if (wrapperClasses != null && !wrapperClasses.isEmpty()) {
        for (Class<?> wrapperClass : wrapperClasses) {
//            获取代理类的实例，并进行依赖注入
            instance = injectExtension((T) wrapperClass.getConstructor(type).newInstance(instance));
        }
    }
    return instance;
    
```
