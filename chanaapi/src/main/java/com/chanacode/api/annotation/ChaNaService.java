package com.chanacode.api.annotation;

import java.lang.annotation.*;

/**
 * ChaNa服务注解
 *
 * <p>用于标注需要注册到服务中心的微服务类。
 * 添加此注解后，服务将在启动时自动注册到ChaNa注册中心。
 *
 * <p>注解属性:
 * <ul>
 *   <li>name - 服务名称（必填）</li>
 *   <li>version - 服务版本，默认1.0.0</li>
 *   <li>group - 分组名称，默认DEFAULT_GROUP</li>
 *   <li>weight - 权重，默认100</li>
 *   <li>enabled - 是否启用，默认true</li>
 *   <li>initMethod - 初始化方法名</li>
 *   <li>destroyMethod - 销毁方法名</li>
 * </ul>
 *
 * <p>使用示例:
 * <pre>
 * {@code @}ChaNaService(name = "order-service", version = "1.0.0", group = "production")
 * public class OrderServiceImpl implements OrderService {
 *     // ...
 * }
 * </pre>
 *
 * <p>完整示例:
 * <pre>
 * {@code @}ChaNaService(
 *     name = "order-service",
 *     version = "2.0.0",
 *     group = "production",
 *     weight = 100,
 *     enabled = true,
 *     initMethod = "init",
 *     destroyMethod = "destroy"
 * )
 * public class OrderServiceImpl implements OrderService {
 *
 *     public void init() {
 *         // 服务初始化逻辑
 *     }
 *
 *     public void destroy() {
 *         // 服务销毁逻辑
 *     }
 * }
 * </pre>
 *
 * @author ChaNa Team
 * @version 3.0.0
 * @since 3.0.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ChaNaService {
    
    /**
     * 服务名称
     *
     * <p>服务在注册中心中的唯一标识
     *
     * @return 服务名称
     */
    String name();

    /**
     * 服务版本
     *
     * <p>用于服务多版本管理，支持灰度发布和版本兼容
     *
     * @return 服务版本，默认1.0.0
     */
    String version() default "1.0.0";

    /**
     * 分组名称
     *
     * <p>用于服务分组，支持逻辑隔离
     *
     * @return 分组名称，默认DEFAULT_GROUP
     */
    String group() default "DEFAULT_GROUP";

    /**
     * 服务权重
     *
     * <p>用于负载均衡，权重越高的实例被选中的概率越大
     *
     * @return 权重，默认100
     */
    int weight() default 100;

    /**
     * 是否启用服务注册
     *
     * <p>设为false时，不会注册到注册中心
     *
     * @return 是否启用，默认true
     */
    boolean enabled() default true;

    /**
     * 初始化方法
     *
     * <p>服务注册成功后调用的初始化方法
     *
     * @return 初始化方法名，默认空字符串
     */
    String initMethod() default "";

    /**
     * 销毁方法
     *
     * <p>服务注销时调用的销毁方法
     *
     * @return 销毁方法名，默认空字符串
     */
    String destroyMethod() default "";

    /**
     * 元数据
     *
     * <p>额外的服务元数据信息
     *
     * @return 元数据键值对
     */
    String[] metadata() default {};
}
