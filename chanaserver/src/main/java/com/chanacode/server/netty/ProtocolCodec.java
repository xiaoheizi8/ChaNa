package com.chanacode.server.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import com.alibaba.fastjson.JSON;
import com.chanacode.common.dto.RegistryRequest;

import java.util.List;

public class ProtocolCodec extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < 4) {
            return;
        }
        in.markReaderIndex();
        int length = in.readInt();
        if (in.readableBytes() < length - 4) {
            in.resetReaderIndex();
            return;
        }
        byte[] data = new byte[length - 4];
        in.readBytes(data);
        try {
            RegistryRequest request = JSON.parseObject(data, RegistryRequest.class);
            out.add(request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode request", e);
        }
    }
}
