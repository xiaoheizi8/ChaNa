package com.chanacode.common.constant;

public final class MessageType {

    private MessageType() {}

    public static final byte REGISTER = 0x01;
    public static final byte DEREGISTER = 0x02;
    public static final byte HEARTBEAT = 0x03;
    public static final byte DISCOVER = 0x04;
    public static final byte SUBSCRIBE = 0x05;
    public static final byte UNSUBSCRIBE = 0x06;
    public static final byte SYNC = 0x07;
    public static final byte ACK = 0x08;
    public static final byte PUSH = 0x09;
    public static final byte BATCH_REGISTER = 0x0A;
    public static final byte BATCH_DISCOVER = 0x0B;
    public static final byte METADATA_UPDATE = 0x0C;
    public static final byte LEASE_RENEW = 0x0D;
    public static final byte CLUSTER_SYNC = 0x0E;
}
