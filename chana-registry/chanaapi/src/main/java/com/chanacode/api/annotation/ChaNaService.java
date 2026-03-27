package com.chanacode.api.annotation;

import java.lang.annotation.*;

/**
 * ChaNa服务注解
 *
 * <p>用于标注需要注册到服务中心的微服务类。
 *
 * <p>注解属性：
 * <ul>
 *   <li>name - 服务名称（必填）</li>
 *   <li>version - 服务版本，默认1.0.0</li>
 *   <li>group - 分组名称，默认DEFAULT_GROUP</li>
 *   <li>weight - 权重，默认100</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>
 * {@code @}ChaNaService(name = "order-service", version = "1.0.0", group = "production")
 * public class OrderServiceImpl implements OrderService {
 *     // ...
 * }
 * </pre>
 *
 * @author 一朝风月
 * @version 1.0.0
 * @since 2026-03-27
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ChaNaService {
    
    /**
     * @methodName: name
     * @description: 服务名称
     * @param: []
     * @return: java.lang.String
     */
    String name();
    
    /**
     * @methodName: version
     * @description: 服务版本
     * @param: []
     * @return: java.lang.String
     */
    String version() default "1.0.0";
    
    /**
     * @methodName: group
     * @description: 分组名称
     * @param: []
     * @return: java.lang.String
     */
    String group() default "DEFAULT_GROUP";
    
    /**
     * @methodName: weight
     * @description: 权重
     * @param: []
     * @return: int
     */
    int weight() default 100;
}
