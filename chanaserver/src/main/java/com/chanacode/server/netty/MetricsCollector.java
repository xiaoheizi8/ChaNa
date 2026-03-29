package com.chanacode.server.netty;

import com.chanacode.core.metrics.HighPrecisionMetricsCollector;

/**
 * 指标采集器
 *
 * <p>委托给HighPrecisionMetricsCollector实现，提供统一的指标采集接口。
 * 主要用于Netty Handler中的指标记录。
 *
 * @author 一朝风月
 * @version 1.0.0
 * @since 2026-03-27
 */
public class MetricsCollector {

    private final HighPrecisionMetricsCollector delegate;

    public MetricsCollector() {
        this.delegate = new HighPrecisionMetricsCollector();
    }

    public void recordLatency(long latencyNanos) {
        delegate.recordLatency(latencyNanos);
    }

    public void incrementRegister() {
        delegate.incrementRegister();
    }

    public void incrementDeregister() {
        delegate.incrementDeregister();
    }

    public void incrementDiscover() {
        delegate.incrementDiscover();
    }

    public void recordHeartbeat() {
        delegate.incrementHeartbeat();
    }

    public void incrementConnection() {
        delegate.incrementConnection();
    }

    public void decrementConnection() {
        delegate.decrementConnection();
    }

    public HighPrecisionMetricsCollector.MetricsSnapshot getSnapshot() {
        return delegate.getSnapshot();
    }
}
