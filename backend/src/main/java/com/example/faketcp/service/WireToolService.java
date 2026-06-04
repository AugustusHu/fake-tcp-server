package com.example.faketcp.service;

import com.example.faketcp.config.ChannelProperties;
import com.example.faketcp.dto.IsoMessageDto;
import com.example.faketcp.dto.KeySettingsDto;
import com.example.faketcp.dto.WireParseResponseDto;
import com.example.faketcp.dto.WireRequestPreviewDto;
import com.example.faketcp.iso.HexUtils;
import com.example.faketcp.iso.IsoCodec;
import com.example.faketcp.iso.IsoMacService;
import com.example.faketcp.iso.IsoXmlMapper;
import com.example.faketcp.iso.PackagerFactory;
import com.example.faketcp.tcp.Framer;
import com.example.faketcp.tcp.PayloadParts;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import org.jpos.iso.ISOBasePackager;
import org.springframework.stereotype.Service;

@Service
public class WireToolService {
    private final ChannelService channelService;
    private final KeySettingsService keySettingsService;
    private final PackagerFactory packagerFactory;
    private final IsoCodec isoCodec;
    private final IsoXmlMapper isoXmlMapper;
    private final IsoMacService isoMacService;

    public WireToolService(
            ChannelService channelService,
            KeySettingsService keySettingsService,
            PackagerFactory packagerFactory,
            IsoCodec isoCodec,
            IsoXmlMapper isoXmlMapper,
            IsoMacService isoMacService) {
        this.channelService = channelService;
        this.keySettingsService = keySettingsService;
        this.packagerFactory = packagerFactory;
        this.isoCodec = isoCodec;
        this.isoXmlMapper = isoXmlMapper;
        this.isoMacService = isoMacService;
    }

    public WireRequestPreviewDto serializeRequest(String channelId, String requestXml) {
        try {
            ChannelProperties channel = channelService.getRequired(channelId);
            ISOBasePackager packager = packagerFactory.create(channel.getIso8583().getPackager());
            KeySettingsDto keySettings = keySettingsService.get(channelId);
            IsoMessageDto request = isoXmlMapper.parse(requestXml);
            boolean macRequired = isoMacService.macRequired(keySettings, request);
            String macValue = isoMacService.sign(request, packager, keySettings);
            byte[] isoBody = isoCodec.pack(request, packager);
            byte[] payload = combine(requestHeader(channel), isoBody);
            byte[] frame = new Framer(channel.getFraming()).writeFrame(payload);

            WireRequestPreviewDto preview = new WireRequestPreviewDto();
            preview.setRequestXml(isoXmlMapper.render(request));
            preview.setIsoHex(HexUtils.toHex(isoBody));
            preview.setPayloadHex(HexUtils.toHex(payload));
            preview.setFrameHex(HexUtils.toHex(frame));
            preview.setMacRequired(macRequired);
            preview.setMacField(isoMacService.macField(request));
            preview.setMacValue(macValue);
            preview.setTargetHost(sendHost(channel));
            preview.setTargetPort(channel.getTcp().getPort());
            preview.setSendHint(sendHint(channel));
            return preview;
        } catch (Exception e) {
            throw new IllegalStateException("序列化 TCP 请求失败: " + e.getMessage(), e);
        }
    }

    public WireRequestPreviewDto buildRequest(String channelId, String requestXml) {
        return serializeRequest(channelId, requestXml);
    }

    public WireParseResponseDto deserializeResponse(String channelId, String responseHex) {
        try {
            ChannelProperties channel = channelService.getRequired(channelId);
            ISOBasePackager packager = packagerFactory.create(channel.getIso8583().getPackager());
            byte[] raw = HexUtils.fromHex(responseHex);
            byte[] payload = readFrameOrPayload(channel, raw);
            PayloadParts parts = new com.example.faketcp.tcp.HeaderHandler(channel.getHeader()).split(payload);
            IsoMessageDto message = isoCodec.unpack(parts.getIsoBody(), packager);

            WireParseResponseDto response = new WireParseResponseDto();
            response.setPayloadHex(HexUtils.toHex(payload));
            response.setIsoHex(HexUtils.toHex(parts.getIsoBody()));
            response.setResponseXml(isoXmlMapper.render(message));
            return response;
        } catch (Exception e) {
            throw new IllegalStateException("反序列化响应失败: " + e.getMessage(), e);
        }
    }

    public WireParseResponseDto parseResponse(String channelId, String responseHex) {
        return deserializeResponse(channelId, responseHex);
    }

    private byte[] readFrameOrPayload(ChannelProperties channel, byte[] raw) {
        try {
            return new Framer(channel.getFraming()).readFrame(new ByteArrayInputStream(raw));
        } catch (Exception e) {
            return raw;
        }
    }

    private byte[] requestHeader(ChannelProperties channel) {
        if (!channel.getHeader().isEnabled() || channel.getHeader().getLength() <= 0) {
            return new byte[0];
        }
        byte[] configured = HexUtils.fromHex(channel.getHeader().getFixedValueHex());
        if (configured.length == 0) {
            return new byte[channel.getHeader().getLength()];
        }
        if (configured.length != channel.getHeader().getLength()) {
            throw new IllegalStateException("Header 固定 HEX 长度与渠道 Header 长度不一致");
        }
        return configured;
    }

    private byte[] combine(byte[] header, byte[] isoBody) {
        byte[] payload = Arrays.copyOf(header, header.length + isoBody.length);
        System.arraycopy(isoBody, 0, payload, header.length, isoBody.length);
        return payload;
    }

    private String sendHost(ChannelProperties channel) {
        String host = channel.getTcp().getHost();
        if (host == null || host.trim().isEmpty() || "0.0.0.0".equals(host.trim())) {
            return "127.0.0.1";
        }
        return host.trim();
    }

    private String sendHint(ChannelProperties channel) {
        return "将完整 Frame HEX 转为二进制字节后，通过任意 TCP 客户端发送到 "
                + sendHost(channel) + ":" + channel.getTcp().getPort()
                + "；收到响应后再转为 HEX，粘贴到反序列化工具。";
    }
}
