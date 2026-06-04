package com.example.faketcp.tcp;

import com.example.faketcp.config.ChannelProperties;
import com.example.faketcp.iso.IsoFieldValueType;
import java.util.Map;
import org.jpos.iso.ISOPackager;

public class ChannelRuntime {
    private final ChannelProperties channel;
    private final ISOPackager packager;
    private final Map<String, IsoFieldValueType> fieldValueTypes;
    private final Framer framer;
    private final HeaderHandler headerHandler;

    public ChannelRuntime(ChannelProperties channel, ISOPackager packager, Map<String, IsoFieldValueType> fieldValueTypes) {
        this.channel = channel;
        this.packager = packager;
        this.fieldValueTypes = fieldValueTypes;
        this.framer = new Framer(channel.getFraming());
        this.headerHandler = new HeaderHandler(channel.getHeader());
    }

    public ChannelProperties getChannel() {
        return channel;
    }

    public ISOPackager getPackager() {
        return packager;
    }

    public Map<String, IsoFieldValueType> getFieldValueTypes() {
        return fieldValueTypes;
    }

    public Framer getFramer() {
        return framer;
    }

    public HeaderHandler getHeaderHandler() {
        return headerHandler;
    }
}
