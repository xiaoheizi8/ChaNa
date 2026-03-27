package com.chanacode.server.netty;

import com.chanacode.common.dto.RegistryRequest;
import com.chanacode.common.dto.RegistryResponse;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.List;

/**
 * ChaNa协议编解码器
 *
 * <p>基于JSON的高性能二进制协议，兼容性和性能兼顾。
 *
 * <p>协议格式：
 * <pre>
 * +-------------+-------------+
 * | 4字节长度   | JSON数据     |
 * | length      | data        |
 * +-------------+-------------+
 * </pre>
 *
 * <p>编解码器：
 * <ul>
 *   <li>RegistryRequestDecoder - 请求解码</li>
 *   <li>RegistryRequestEncoder - 请求编码</li>
 *   <li>RegistryResponseEncoder - 响应编码</li>
 * </ul>
 *
 * @author 一朝风月
 * @version 1.0.0
 * @since 2026-03-27
 */
public class ProtocolCodec {

    /**
     * @className: RegistryRequestDecoder
     * @description: 请求解码器
     */
    public static class RegistryRequestDecoder extends ByteToMessageDecoder {

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

            RegistryRequest request = JSON.parseObject(data, RegistryRequest.class,
                JSONReader.Feature.SupportAutoType);
            out.add(request);
        }
    }

    /**
     * @className: RegistryResponseEncoder
     * @description: 响应编码器
     */
    public static class RegistryResponseEncoder extends MessageToByteEncoder<RegistryResponse> {

        @Override
        protected void encode(ChannelHandlerContext ctx, RegistryResponse msg, ByteBuf out) {
            byte[] data = JSON.toJSONBytes(msg,
                JSONWriter.Feature.WriteNulls,
                JSONWriter.Feature.FieldBased);

            out.writeInt(4 + data.length);
            out.writeBytes(data);
        }
    }

    /**
     * @className: RegistryRequestEncoder
     * @description: 请求编码器
     */
    public static class RegistryRequestEncoder extends MessageToByteEncoder<RegistryRequest> {

        @Override
        protected void encode(ChannelHandlerContext ctx, RegistryRequest msg, ByteBuf out) {
            byte[] data = JSON.toJSONBytes(msg,
                JSONWriter.Feature.WriteNulls,
                JSONWriter.Feature.FieldBased);

            out.writeInt(4 + data.length);
            out.writeBytes(data);
        }
    }
}
