package com.chanacode.common.constant;

public class RegistryConstants {

    public static final String DEFAULT_NAMESPACE = "public";
    public static final String DEFAULT_GROUP = "default";
    public static final int DEFAULT_PORT = 9999;
    public static final int DEFAULT_WEIGHT = 100;
    public static final long DEFAULT_LEASE_TTL = 30_000;
    public static final long HEARTBEAT_INTERVAL = 5_000;
    public static final int MAX_INSTANCES_PER_SERVICE = 10000;

    private RegistryConstants() {
    }
}
