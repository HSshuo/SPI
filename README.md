# java SPI
## 介绍
- Server Provider Interface，服务发现机制。将接口实现类的全限定名配置在文件中，并由服务加载读取配置文件，加载实现类。
- java 通过 **ServiceLoader** 类实现，定义在目录 META-INF/services 文件夹，在里面定义具体的实现类。通过反射获取实例。
- 破坏了双亲委派机制，参考：[双亲委派机制](https://blog.nowcoder.net/n/73bff2394e0c4179aa67a745458f17d7)

<br>

## 源码解析

#### 总结
- 根据接口名调用 ServerLoader.load(), 根据约定找到 META-INF/services 目录，解析文件得到实现类的全限定名，然后循环 通过反射方法Class.forName()加载类，和通过newInstance() 将类实例化，并将实例化的类缓存到List对象，然后返回。
![alt](https://uploadfiles.nowcoder.com/images/20220731/630417200_1659235495688/D2B5CA33BD970F64A6301FA75AE2EB22)


<br>

##### ServerLoader.load()
- 先寻找当前线程绑定的ClassLoader，如果没有就是使用SystemClassLoader
- 然后清除缓存，创建一个LazyIterator
```java
	public static <S> ServiceLoader<S> load(Class<S> service) {
        // 获取当前线程的ClassLoader
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return ServiceLoader.load(service, cl);
    }


	private ServiceLoader(Class<S> svc, ClassLoader cl) {
        service = Objects.requireNonNull(svc, "Service interface cannot be null");
      
        //  如果没有则使用SystemClassLoader
        loader = (cl == null) ? ClassLoader.getSystemClassLoader() : cl;
        acc = (System.getSecurityManager() != null) ? AccessController.getContext() : null;
        reload();
    }


     
	public void reload() {
        // 清空缓存、再创建一个LazyIterator
        providers.clear();
        lookupIterator = new LazyIterator(service, loader);
    }
```

##### hasNext()、next()
- 调用hasNext()来做实例循环
- 调用next() 得到一个实例
```java
		public boolean hasNext() {
            if (acc == null) {
                return hasNextService();
            } else {
                PrivilegedAction<Boolean> action = new PrivilegedAction<Boolean>() {
                    public Boolean run() { return hasNextService(); }
                };
                return AccessController.doPrivileged(action, acc);
            }
        }

        public S next() {
            if (acc == null) {
                return nextService();
            } else {
                PrivilegedAction<S> action = new PrivilegedAction<S>() {
                    public S run() { return nextService(); }
                };
                return AccessController.doPrivileged(action, acc);
            }
        }
```

##### hasNextService()
- 获取文件的位置，加载配置文件
- 按行遍历文件内容，解析内容，赋给nextName集合
```java

		Iterator<String> pending = null;

		private boolean hasNextService() {
            if (nextName != null) {
                return true;
            }
            if (configs == null) {
                try {
                  
                    // 得到文件的位置
                    String fullName = PREFIX + service.getName();
                    if (loader == null)
                        
                        // 加载配置文件
                        configs = ClassLoader.getSystemResources(fullName);
                    else
                        configs = loader.getResources(fullName);
                } catch (IOException x) {
                    fail(service, "Error locating configuration files", x);
                }
            }
          
            // 按行遍历文件内容
            while ((pending == null) || !pending.hasNext()) {
                if (!configs.hasMoreElements()) {
                    return false;
                }
              
                // 解析内容
                pending = parse(service, configs.nextElement());
            }
            nextName = pending.next();
            return true;
        }
```

##### nextService()
- 通过解析到的全限定名加载类，并且创建实例放入缓存中，之后返回实例
```java

		private S nextService() {
            if (!hasNextService())
                throw new NoSuchElementException();
          
            // 获取当前解析到的实现类全限定名
            String cn = nextName;
            nextName = null;
            Class<?> c = null;
            try {
              
                // 加载指定的实现类
                c = Class.forName(cn, false, loader);
            } 
          
            ···················
             
            try {
                
                // 创建实现类
                S p = service.cast(c.newInstance());
                
                // 缓存实现类
                providers.put(cn, p);
                return p;
            } 
          
            ················
        }
```

<br>

#### 缺陷
- **不能按需加载，需要遍历所有的实现，并实例化，然后在循环中才能找到我们需要的实现。如果不想用某些实现类，或者某些类实例化很耗时，它也被载入并实例化了，这就造成了浪费**。
- **配置文件中只是简单的列出了所有的拓展实现，而没有给他们命名，导致获取某个实现类的方式不够灵活，只能通过循环的形式获取，不能根据某个参数来获取对应的实现类**。
- 扩展之间彼此存在依赖，做不到自动注入和装配，不提供上下文的IOC和AOP功能。
- 扩展很难和其他的容器框架集成，比如扩展依赖一个外部Spring容器中的bean，原生的JDK SPI并不支持。
- 多个并发多线程使用 ServiceLoader 类的实例是不安全的。

<br>
<br>

# Spring SPI
- 感觉大致与java相同，只不过换了方法（SpringFactoriesLoader.loadFactories）
- 可以拓展[SpringBoot的自动装配](https://blog.nowcoder.net/n/9343e7c4215547eb82fbe7fe8f0dc1a1)

<br>
<br>

# Dubbo SPI

- 可以按需加载，通过名字去文件里面找到对应的实现类全限定名然后加载实例化
- 支持 AOP、IOC (URL、@SPI、@Adaptive)
- 自适应扩展机制

<br>

## 总结
1. 调用 ExtensionLoader.getExtensionLoader() 获取接口对应的拓展点加载器，先从缓存中获取，如果没有就通过构造函数创建
2. 调用 extensionLoader.getExtension() 获取对应实现类的实例，先从缓存中获取，如果没有就通过 双重检测 的方式调用 createExtension() 创建实例
    - 通过getExtensionClasses()，先从缓存中获取实现类，如果没有则加载目录下所有的扩展点实现类，再通过ClassLoader加载并放入缓存中，根据name获取到对应的扩展点实现类；通过 loadClass() 实现缓存操作，分别有 Adptive 拓展点放入、extensionClasses 普通实现类放入、cachedWrapperClasses AOP类放入。
    - 通过 injectExtension(instance)，IOC，依赖注入只能通过set方法注入，并获取依赖类的类型、名字，通过objectFactory.getExtension() 来创建依赖对象，执行注入依赖；
    - 通过 WrapperClasses，AOP，通过 getExtensionClasses() 读取到的 AOP 代理类，存放在cachedWrapperClasses中，通过获取代理实例进行依赖注入。

<br>

## 源码解析
1. 通过 ExtensionLoader.getExtensionLoader() 获取接口对应的拓展点加载器
2. 通过 extensionLoader.getExtension() 获取对应实现类的实例

<br>

##### ExtensionLoader.getExtensionLoader()
- 从缓存中**获取接口对应的拓展点加载器**，如果没有就会通过构造函数创建，并且放到一个ConcurrentHashMap缓存中;
- 从缓存中获取：EXTENSION_LOADERS.get(type);
- 通过构造函数创建并存入缓存中：EXTENSION_LOADERS.putIfAbsent(type, new ExtensionLoader<T>(type));
  
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
  
##### new ExtensionLoader<T>(type)
- 通过构造函数创建扩展点加载器
```java
    private ExtensionLoader(Class<?> type) {
        this.type = type;
        objectFactory = (type == ExtensionFactory.class ? null : ExtensionLoader.getExtensionLoader(ExtensionFactory.class).getAdaptiveExtension());
    }
```

<br>

##### extensionLoader.getExtension()
- 通过缓存**获取目标实现类的实例**，如果没有，会通过双重检查的方式调用 createExtension() 方法创建实例
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
        
        //  从缓存中获取持有目标对象，没有则创建
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

                    // 实例对象
                    instance = createExtension(name);
                    holder.set(instance);
                }
            }
        }
        return (T) instance;
    }
```

##### createExtension()
- 通过 getExtensionClasses() 获取对应的实现类
- 从缓存中获取实例，如果没有通过反射创建实例
- 调用 injectExtension() 实现 setter 依赖注入
- wrapperClasses 包装类实现 AOP

```java
    private T createExtension(String name) {
//        获取扩展点对应的类(通过读取文件里面所有的实现类名字，用ClassLoader加载具体的实现类)
        Class<?> clazz = getExtensionClasses().get(name);
        if (clazz == null) {
            throw findException(name);
        }
        
        
        try {
//            从缓存中获取实例，如果没有通过反射创建实例
            T instance = (T) EXTENSION_INSTANCES.get(clazz);
            if (instance == null) {
                EXTENSION_INSTANCES.putIfAbsent(clazz, clazz.newInstance());
                instance = (T) EXTENSION_INSTANCES.get(clazz);
            }
            
//            setter 依赖注入 IOC
            injectExtension(instance);
            
//            AOP
            Set<Class<?>> wrapperClasses = cachedWrapperClasses;
            if (wrapperClasses != null && !wrapperClasses.isEmpty()) {
                for (Class<?> wrapperClass : wrapperClasses) {
                    instance = injectExtension((T) wrapperClass.getConstructor(type).newInstance(instance));
                }
            }
            return instance;
        } 

      ·················

```

##### getExtensionClasses()
- 先从缓存中获取实现类，如果没有调用 loadExtensionClasses() 创建并存放到缓存中
```java
     private Map<String, Class<?>> getExtensionClasses() {
        Map<String, Class<?>> classes = cachedClasses.get();
        if (classes == null) {
            synchronized (cachedClasses) {
                classes = cachedClasses.get();
                if (classes == null) {
                    classes = loadExtensionClasses();
                    cachedClasses.set(classes);
                }
            }
        }
        return classes;
    }
  
  
   
  
  private Map<String, Class<?>> loadExtensionClasses() {
        final SPI defaultAnnotation = type.getAnnotation(SPI.class);
  
        ·············

        Map<String, Class<?>> extensionClasses = new HashMap<String, Class<?>>();
  
        //  从三个目录里面查找 
        // META-INF/services、META-INF/dubbo、META-INF/dubbo/internal
  
        loadDirectory(extensionClasses, DUBBO_INTERNAL_DIRECTORY);
        loadDirectory(extensionClasses, DUBBO_DIRECTORY);
        loadDirectory(extensionClasses, SERVICES_DIRECTORY);
        return extensionClasses;
    }
```
  
###### loadDirectory()
- 根据类名和指定的目录，找到文件获取所有资源，然后一个一个去加载类
- 再通过 loadClass() 做三种缓存操作，缓存 Adaptive 、WrapperClass 和普通类这三种
```java
     private void loadDirectory(Map<String, Class<?>> extensionClasses, String dir) {
  
        // 获取目录
        String fileName = dir + type.getName();
        try {
  
            // 通过目录获取资源
            Enumeration<java.net.URL> urls;
            ClassLoader classLoader = findClassLoader();
            if (classLoader != null) {
                urls = classLoader.getResources(fileName);
            } else {
                urls = ClassLoader.getSystemResources(fileName);
            }
  
  
            // 遍历资源，调用loadResource一个一个加载
            if (urls != null) {
                while (urls.hasMoreElements()) {
                    java.net.URL resourceURL = urls.nextElement();
                    loadResource(extensionClasses, classLoader, resourceURL);
                }
            }
        } 
        
        ······················
    }

  
  
  
  
    private void loadClass(Map<String, Class<?>> extensionClasses, java.net.URL resourceURL, Class<?> clazz, String name) throws NoSuchMethodException {

        ·····························

        //  如果类标注了 Adptive注解，保存
        if (clazz.isAnnotationPresent(Adaptive.class)) {
            if (cachedAdaptiveClass == null) {
                cachedAdaptiveClass = clazz;
            } else if (!cachedAdaptiveClass.equals(clazz)) {
                throw ···············
            }


         //  如果是包装类注解， 保存
        } else if (isWrapperClass(clazz)) {
            Set<Class<?>> wrappers = cachedWrapperClasses;
            if (wrappers == null) {
                cachedWrapperClasses = new ConcurrentHashSet<Class<?>>();
                wrappers = cachedWrapperClasses;
            }
            wrappers.add(clazz);
        } else {
  
            //  普通类进入
            clazz.getConstructor();
  
            ······················
  
            String[] names = NAME_SEPARATOR.split(name);
            if (names != null && names.length > 0) {
                Activate activate = clazz.getAnnotation(Activate.class);
                if (activate != null) {
  
                    // 标记Adptive 注解，保存
                    cachedActivates.put(names[0], activate);
                }
                for (String n : names) {
                    if (!cachedNames.containsKey(clazz)) {
  
                        // 记录映射（类 --> 名字）
                        cachedNames.put(clazz, n);
                    }
                    Class<?> c = extensionClasses.get(n);
                    if (c == null) {

                        // 记录映射（名字 --> 类）
                        extensionClasses.put(n, clazz);
                    } 

                    ··························
                }
            }
        }
    }
```
 
##### injectExtension() IOC
- 依赖注入只能通过set方法注入，并获取依赖类的类型、名字
- 通过 objectFactory.getExtension() 获取依赖对象
- 通过 method.invoke() 执行set方法注入依赖

```java
    private T injectExtension(T instance) {
        try {
            if (objectFactory != null) {
                for (Method method : instance.getClass().getMethods()) {
                    
                    
//                    说明只能是set注入 并且方法仅有一个参数，且方法访问级别为 public
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
                        } 
  
        ······························
  
        return instance;
    }
```

##### wrapperClasses AOP
- 通过 getExtensionClasses() 读取到的 AOP 代理类，存放在cachedWrapperClasses中
- 获取代理实例进行依赖注入
  
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

<br>
<br>

# 参考
- [SPI机制详解](https://pdai.tech/md/java/advanced/java-advanced-spi.html)
- [Hshuo手写实现SPI](https://github.com/HSshuo/SPI)
- [敖丙关于SPI的文章](https://mp.weixin.qq.com/s/gwWOsdQGEN0t2GJVMQQexw)
