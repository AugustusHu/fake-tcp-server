package com.example.faketcp.iso;

import com.example.faketcp.dto.IsoMessageDto;
import com.example.faketcp.dto.KeySettingsDto;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.jpos.iso.ISOPackager;
import org.springframework.stereotype.Component;

@Component
public class IsoMacService {
    private static final byte[] ZERO_IV = new byte[8];
    private static final String ANSI_X9_19 = "ANSI_X9_19";
    private static final String SHA256_FIELD128_TRIM64 = "SHA256_FIELD128_TRIM64";
    private static final String ZERO_ANSI_MAC = "0000000000000000";
    private static final String ZERO_SHA256_FIELD128_MAC =
            "0000000000000000000000000000000000000000000000000000000000000000";
    private final IsoCodec isoCodec;

    public IsoMacService(IsoCodec isoCodec) {
        this.isoCodec = isoCodec;
    }

    public boolean macRequired(KeySettingsDto settings, IsoMessageDto message) {
        return settings != null
                && !isBlank(settings.getTskPlain())
                && !isTidInitialization(message);
    }

    public boolean verifyRequestMac(IsoMessageDto request, ISOPackager packager, KeySettingsDto settings) throws Exception {
        if (!macRequired(settings, request)) {
            return true;
        }
        String macField = macField(request, settings);
        String actualMac = fieldValue(request, macField);
        if (isBlank(actualMac)) {
            return false;
        }
        String expectedMac = computeMac(request, packager, settings, macField);
        return cleanHex(actualMac).equals(expectedMac);
    }

    public String sign(IsoMessageDto message, ISOPackager packager, KeySettingsDto settings) throws Exception {
        if (!macRequired(settings, message)) {
            return null;
        }
        String macField = macField(message, settings);
        String mac = computeMac(message, packager, settings, macField);
        putCanonicalMac(message, macField, mac);
        return mac;
    }

    public String macField(IsoMessageDto message) {
        return macField(message, null);
    }

    public String macField(IsoMessageDto message, KeySettingsDto settings) {
        if (isSha256Field128Trim64(settings)) {
            return "128";
        }
        String detected = detectedMacField(message);
        if (!isBlank(detected)) {
            return detected;
        }
        String hinted = normalizeMacField(message == null ? null : message.getBitmapMacFieldHint());
        if (!isBlank(hinted)) {
            return hinted;
        }
        return defaultMacField(message);
    }

    public String defaultMacField(IsoMessageDto message) {
        String processCode = message == null || message.getFields() == null ? null : fieldValue(message, "3");
        if ("9C0000".equalsIgnoreCase(processCode) || "9D0000".equalsIgnoreCase(processCode)) {
            return "64";
        }
        return "128";
    }

    public boolean isTidInitialization(IsoMessageDto message) {
        if (message == null || message.getFields() == null) {
            return false;
        }
        String processCode = message.getFields().get("3");
        return "9A0000".equalsIgnoreCase(processCode)
                || "9B0000".equalsIgnoreCase(processCode)
                || "9G0000".equalsIgnoreCase(processCode);
    }

    private String computeMac(IsoMessageDto message, ISOPackager packager, KeySettingsDto settings, String macField) throws Exception {
        if (isSha256Field128Trim64(settings)) {
            return computeSha256Field128Trim64(message, packager, settings);
        }
        return computeAnsiX919(message, packager, settings, macField);
    }

    private String computeAnsiX919(IsoMessageDto message, ISOPackager packager, KeySettingsDto settings, String macField) throws Exception {
        IsoMessageDto copy = copyOf(message);
        putCanonicalMac(copy, macField, ZERO_ANSI_MAC);
        byte[] data = isoCodec.pack(copy, packager);
        byte[] key = HexUtils.fromHex(settings.getTskPlain());
        return HexUtils.toHex(retailMac(data, key));
    }

    private String computeSha256Field128Trim64(IsoMessageDto message, ISOPackager packager, KeySettingsDto settings) throws Exception {
        IsoMessageDto copy = copyOf(message);
        putCanonicalMac(copy, "128", ZERO_SHA256_FIELD128_MAC);
        byte[] packedData = isoCodec.pack(copy, packager);
        if (packedData.length < 64) {
            throw new IllegalStateException("SHA-256 Field128 MAC requires packed ISO length to be at least 64 bytes");
        }
        byte[] trimmedPackedData = Arrays.copyOf(packedData, packedData.length - 64);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(HexUtils.fromHex(settings.getTskPlain()));
        digest.update(trimmedPackedData);
        return HexUtils.toHex(digest.digest());
    }

    private IsoMessageDto copyOf(IsoMessageDto source) {
        IsoMessageDto copy = new IsoMessageDto();
        copy.setMti(source.getMti());
        copy.setFields(new LinkedHashMap<>(source.getFields()));
        copy.setBitmapMacFieldHint(source.getBitmapMacFieldHint());
        return copy;
    }

    private String detectedMacField(IsoMessageDto message) {
        if (message == null || message.getFields() == null) {
            return null;
        }
        for (String field : message.getFields().keySet()) {
            if ("128".equals(IsoFieldReferences.canonical(field))) {
                return "128";
            }
        }
        for (String field : message.getFields().keySet()) {
            if ("64".equals(IsoFieldReferences.canonical(field))) {
                return "64";
            }
        }
        return null;
    }

    private String fieldValue(IsoMessageDto message, String field) {
        if (message == null || message.getFields() == null) {
            return null;
        }
        String canonicalField = IsoFieldReferences.canonical(field);
        for (Map.Entry<String, String> entry : message.getFields().entrySet()) {
            if (canonicalField.equals(IsoFieldReferences.canonical(entry.getKey()))) {
                return entry.getValue();
            }
        }
        return null;
    }

    private void putCanonicalMac(IsoMessageDto message, String macField, String mac) {
        String canonicalMacField = normalizeMacField(macField);
        if (message.getFields() == null) {
            message.setFields(new LinkedHashMap<>());
        }
        message.getFields().entrySet().removeIf(entry -> canonicalMacField.equals(IsoFieldReferences.canonical(entry.getKey())));
        message.getFields().put(canonicalMacField, mac);
    }

    private String normalizeMacField(String field) {
        String canonical = IsoFieldReferences.canonical(field);
        if ("64".equals(canonical) || "128".equals(canonical)) {
            return canonical;
        }
        return null;
    }

    private byte[] retailMac(byte[] data, byte[] key) throws Exception {
        byte[] padded = zeroPad(data);
        byte[] leftKey = Arrays.copyOfRange(key, 0, 8);
        byte[] block = Arrays.copyOf(ZERO_IV, ZERO_IV.length);
        for (int offset = 0; offset < padded.length; offset += 8) {
            block = desEncrypt(xor(block, Arrays.copyOfRange(padded, offset, offset + 8)), leftKey);
        }
        if (key.length >= 16) {
            byte[] rightKey = Arrays.copyOfRange(key, 8, 16);
            block = desEncrypt(desDecrypt(block, rightKey), leftKey);
        }
        return block;
    }

    private byte[] zeroPad(byte[] data) {
        int length = ((data.length + 7) / 8) * 8;
        if (length == 0) {
            length = 8;
        }
        return Arrays.copyOf(data, length);
    }

    private byte[] xor(byte[] left, byte[] right) {
        byte[] result = new byte[8];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) (left[i] ^ right[i]);
        }
        return result;
    }

    private byte[] desEncrypt(byte[] data, byte[] key) throws Exception {
        return des(Cipher.ENCRYPT_MODE, data, key);
    }

    private byte[] desDecrypt(byte[] data, byte[] key) throws Exception {
        return des(Cipher.DECRYPT_MODE, data, key);
    }

    private byte[] des(int mode, byte[] data, byte[] key) throws Exception {
        Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding");
        cipher.init(mode, new SecretKeySpec(key, "DES"));
        return cipher.doFinal(data);
    }

    private String cleanHex(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    private boolean isSha256Field128Trim64(KeySettingsDto settings) {
        return settings != null && SHA256_FIELD128_TRIM64.equalsIgnoreCase(settings.getMacAlgorithm());
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
