package com.chanacode.spring.boot.starter;

import com.chanacode.api.annotation.ChaNaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ChaNa服务注解后置处理器
 *
 * <p>用于处理带有@ChaNaService注解的Bean，实现自动注册服务。
 * 在Bean初始化完成后自动将服务注册到注册中心。
 *
 * <p>设计模式:
 * <ul>
 *   <li>后置处理器模式 - 在Bean初始化后进行处理</li>
 *   <li>代理模式 - 包装原始Bean</li>
 * </ul>
 *
 * <p>功能特性:
 * <ul>
 *   <li>自动扫描@ChaNaService注解</li>
 *   <li>支持Spring Boot 3.x的@Async等异步方法</li>
 *   <li>支持自定义注册逻辑</li>
 * </ul>
 *
 * @author ChaNa Team
 * @version 3.0.0
 * @since 3.0.0
 */
@Slf4j
public class ChaNaServiceAnnotationBeanPostProcessor implements BeanPostProcessor {

    /**
     * 回调接口 - 用于自定义注册逻辑
     */
    @FunctionalInterface
    public interface RegisterCallback {
        /**
         * 注册回调
         *
         * @param bean        bean对象
         * @param beanName    bean名称
         * @param annotation  注解信息
         */
        void onRegister(Object bean, String beanName, ChaNaService annotation);
    }

    private final ChaNaServiceRegistry registry;
    private final Map<String, Class<?>> registeredClasses = new ConcurrentHashMap<>();
    private final Map<String, ChaNaService> registeredAnnotations = new ConcurrentHashMap<>();
    private RegisterCallback registerCallback;

    /**
     * 构造函数
     *
     * @param registry 服务注册表
     * @throws IllegalArgumentException 如果registry为null
     */
    public ChaNaServiceAnnotationBeanPostProcessor(ChaNaServiceRegistry registry) {
        Assert.notNull(registry, "ChaNaServiceRegistry cannot be null");
        this.registry = registry;
    }

    /**
     * 设置注册回调
     *
     * @param callback 回调接口
     */
    public void setRegisterCallback(RegisterCallback callback) {
        this.registerCallback = callback;
    }

    /**
     * Bean初始化后处理
     *
     * <p>检查Bean是否带有@ChaNaService注解，如果有则注册服务
     *
     * @param bean     bean对象
     * @param beanName bean名称
     * @return 处理后的bean对象
     * @throws BeansException 如果处理过程中发生异常
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();
        
        ChaNaService annotation = AnnotationUtils.findAnnotation(beanClass, ChaNaService.class);
        if (annotation != null) {
            processAnnotation(bean, beanName, annotation);
        }
        
        return bean;
    }

    /**
     * 处理注解
     *
     * @param bean       bean对象
     * @param beanName   bean名称
     * @param annotation 注解
     */
    private void processAnnotation(Object bean, String beanName, ChaNaService annotation) {
        registeredClasses.put(beanName, bean.getClass());
        registeredAnnotations.put(beanName, annotation);

        String serviceName = resolveServiceName(bean.getClass(), annotation);
        
        log.info("ChaNa: Processing @ChaNaService annotation - bean: {}, service: {}, version: {}, group: {}",
                beanName, serviceName, annotation.version(), annotation.group());

        try {
            registry.registerService(bean.getClass(), annotation);
            
            if (registerCallback != null) {
                registerCallback.onRegister(bean, beanName, annotation);
            }
            
            invokeInitMethod(bean, annotation);
            
        } catch (Exception e) {
            log.error("ChaNa: Failed to register service for bean - {}", beanName, e);
        }
    }

    /**
     * 解析服务名称
     *
     * <p>优先级:
     * <ol>
     *   <li>注解中指定的name</li>
     *   <li>从配置中获取</li>
     *   <li>Bean类的简单名称</li>
     * </ol>
     *
     * @param beanClass  bean类
     * @param annotation 注解
     * @return 服务名称
     */
    private String resolveServiceName(Class<?> beanClass, ChaNaService annotation) {
        if (StringUtils.hasText(annotation.name())) {
            return annotation.name();
        }
        return beanClass.getSimpleName();
    }

    /**
     * 调用初始化方法
     *
     * <p>如果注解指定了initMethod，则调用该方法
     *
     * @param bean       bean对象
     * @param annotation 注解
     */
    private void invokeInitMethod(Object bean, ChaNaService annotation) {
        if (!annotation.initMethod().isEmpty()) {
            try {
                Method method = bean.getClass().getMethod(annotation.initMethod());
                method.invoke(bean);
                log.debug("ChaNa: Invoked init method - {}", annotation.initMethod());
            } catch (NoSuchMethodException e) {
                log.warn("ChaNa: Init method not found - {}", annotation.initMethod());
            } catch (InvocationTargetException | IllegalAccessException e) {
                log.error("ChaNa: Failed to invoke init method - {}", annotation.initMethod(), e);
            }
        }
    }

    /**
     * 获取已注册的类映射
     *
     * @return bean名称到类的映射
     */
    public Map<String, Class<?>> getRegisteredClasses() {
        return registeredClasses;
    }

    /**
     * 获取已注册的注解映射
     *
     * @return bean名称到注解的映射
     */
    public Map<String, ChaNaService> getRegisteredAnnotations() {
        return registeredAnnotations;
    }

    /**
     * 获取指定Bean的注册信息
     *
     * @param beanName bean名称
     * @return 注解信息，如果不存在返回null
     */
    public ChaNaService getRegisteredAnnotation(String beanName) {
        return registeredAnnotations.get(beanName);
    }

    /**
     * 注销指定Bean的服务
     *
     * @param beanName bean名称
     * @return 注销成功返回true，否则返回false
     */
    public boolean deregisterService(String beanName) {
        ChaNaService annotation = registeredAnnotations.get(beanName);
        if (annotation == null) {
            log.warn("ChaNa: No registration found for bean - {}", beanName);
            return false;
        }

        String serviceName = resolveServiceName(registeredClasses.get(beanName), annotation);
        String instanceId = generateInstanceId(serviceName);
        
        return registry.deregisterService(serviceName, instanceId);
    }

    /**
     * 生成实例ID
     *
     * @param serviceName 服务名称
     * @return 实例ID
     */
    private String generateInstanceId(String serviceName) {
        String host = System.getProperty("host.address", "127.0.0.1");
        String port = System.getProperty("server.port", "8080");
        return String.format("%s-%s-%s", serviceName, host, port);
    }
}
