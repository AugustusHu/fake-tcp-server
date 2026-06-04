package com.example.faketcp.iso;

import com.example.faketcp.dto.IsoMessageDto;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jpos.iso.ISOBasePackager;
import org.jpos.iso.ISOFieldPackager;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOPackager;
import org.springframework.stereotype.Component;

@Component
public class IsoCodec {
    public IsoMessageDto unpack(byte[] payload, ISOPackager packager) throws Exception {
        ISOMsg message = new ISOMsg();
        message.setPackager(packager);
        message.unpack(payload);

        IsoMessageDto dto = new IsoMessageDto();
        dto.setMti(message.getMTI());
        Map<String, String> fields = new LinkedHashMap<>();
        for (int i = 1; i <= message.getMaxField(); i++) {
            if (message.hasField(i)) {
                if (isBinaryField(packager, i)) {
                    fields.put(String.valueOf(i), HexUtils.toHex(message.getBytes(i)));
                } else {
                    fields.put(String.valueOf(i), message.getString(i));
                }
            }
        }
        dto.setFields(fields);
        return dto;
    }

    public byte[] pack(IsoMessageDto dto, ISOPackager packager) throws Exception {
        ISOMsg message = new ISOMsg();
        message.setPackager(packager);
        message.setMTI(dto.getMti());
        for (Map.Entry<String, String> entry : dto.getFields().entrySet()) {
            if (entry.getValue() != null && !entry.getValue().trim().isEmpty()) {
                int field = Integer.parseInt(entry.getKey());
                if (isBinaryField(packager, field)) {
                    message.set(field, HexUtils.fromHex(entry.getValue()));
                } else {
                    message.set(field, entry.getValue());
                }
            }
        }
        return message.pack();
    }

    private boolean isBinaryField(ISOPackager packager, int field) {
        if (!(packager instanceof ISOBasePackager)) {
            return false;
        }
        try {
            ISOFieldPackager fieldPackager = ((ISOBasePackager) packager).getFieldPackager(field);
            return IsoFieldValueType.from(fieldPackager) == IsoFieldValueType.BINARY;
        } catch (Exception e) {
            return false;
        }
    }
}
