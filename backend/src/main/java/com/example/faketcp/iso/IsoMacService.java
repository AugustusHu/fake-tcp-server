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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class IsoMacService {
    private static final Logger log = LoggerFactory.getLogger(IsoMacService.class);
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
            log.info("ISO MAC verify skipped: reason={}, settings={}, request={}",
                    macSkipReason(settings, request),
                    settingsSummary(settings),
                    IsoLogUtils.messageSummary(request));
            return true;
        }
        String macField = macField(request, settings);
        String actualMac = fieldValue(request, macField);
        log.info("ISO MAC verify start: settings={}, resolvedMacField=DE{}, actualMac={}, request={}",
                settingsSummary(settings),
                macField,
                cleanHex(actualMac),
                IsoLogUtils.messageSummary(request));
        if (isBlank(actualMac)) {
            log.warn("ISO MAC verify failed: missing MAC field DE{}, settings={}, request={}",
                    macField,
                    settingsSummary(settings),
                    IsoLogUtils.messageSummary(request));
            return false;
        }
        String expectedMac = computeMac(request, packager, settings, macField);
        boolean valid = cleanHex(actualMac).equals(expectedMac);
        log.info("ISO MAC verify result: resolvedMacField=DE{}, actualMac={}, expectedMac={}, valid={}",
                macField,
                cleanHex(actualMac),
                expectedMac,
                valid);
        return valid;
    }

    public String sign(IsoMessageDto message, ISOPackager packager, KeySettingsDto settings) throws Exception {
        if (!macRequired(settings, message)) {
            log.info("ISO MAC sign skipped: reason={}, settings={}, message={}",
                    macSkipReason(settings, message),
                    settingsSummary(settings),
                    IsoLogUtils.messageSummary(message));
            return null;
        }
        String macField = macField(message, settings);
        log.info("ISO MAC sign start: settings={}, resolvedMacField=DE{}, message={}",
                settingsSummary(settings),
                macField,
                IsoLogUtils.messageSummary(message));
        String mac = computeMac(message, packager, settings, macField);
        putCanonicalMac(message, macField, mac);
        log.info("ISO MAC sign result: resolvedMacField=DE{}, mac={}", macField, mac);
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
        log.info("ISO MAC compute input: algorithm={}, macField=DE{}, zeroMac={}, fields={}",
                ANSI_X9_19,
                macField,
                ZERO_ANSI_MAC,
                IsoLogUtils.fieldsSummary(copy.getFields()));
        byte[] data;
        try {
            data = isoCodec.pack(copy, packager);
        } catch (Exception e) {
            log.warn("ISO MAC compute pack failed: algorithm={}, macField=DE{}, settings={}, fields={}",
                    ANSI_X9_19,
                    macField,
                    settingsSummary(settings),
                    IsoLogUtils.fieldsSummary(copy.getFields()),
                    e);
            throw e;
        }
        byte[] key = HexUtils.fromHex(settings.getTskPlain());
        String mac = HexUtils.toHex(retailMac(data, key));
        log.info("ISO MAC compute result: algorithm={}, macField=DE{}, packedBytes={}, tsk={}, resultMac={}",
                ANSI_X9_19,
                macField,
                data.length,
                IsoLogUtils.keyFingerprint(settings.getTskPlain()),
                mac);
        return mac;
    }

    private String computeSha256Field128Trim64(IsoMessageDto message, ISOPackager packager, KeySettingsDto settings) throws Exception {
        IsoMessageDto copy = copyOf(message);
        putCanonicalMac(copy, "128", ZERO_SHA256_FIELD128_MAC);
        log.info("ISO MAC compute input: algorithm={}, macField=DE128, zeroMacLen={}, fields={}",
                SHA256_FIELD128_TRIM64,
                ZERO_SHA256_FIELD128_MAC.length(),
                IsoLogUtils.fieldsSummary(copy.getFields()));
        byte[] packedData;
        try {
            packedData = isoCodec.pack(copy, packager);
        } catch (Exception e) {
            log.warn("ISO MAC compute pack failed: algorithm={}, macField=DE128, settings={}, fields={}",
                    SHA256_FIELD128_TRIM64,
                    settingsSummary(settings),
                    IsoLogUtils.fieldsSummary(copy.getFields()),
                    e);
            throw e;
        }
        if (packedData.length < 64) {
            throw new IllegalStateException("SHA-256 Field128 MAC requires packed ISO length to be at least 64 bytes");
        }
        byte[] trimmedPackedData = Arrays.copyOf(packedData, packedData.length - 64);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(HexUtils.fromHex(settings.getTskPlain()));
        digest.update(trimmedPackedData);
        String mac = HexUtils.toHex(digest.digest());
        log.info("ISO MAC compute result: algorithm={}, packedBytes={}, trimmedBytes={}, tsk={}, resultMac={}",
                SHA256_FIELD128_TRIM64,
                packedData.length,
                trimmedPackedData.length,
                IsoLogUtils.keyFingerprint(settings.getTskPlain()),
                mac);
        return mac;
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

    private String settingsSummary(KeySettingsDto settings) {
        if (settings == null) {
            return "<null>";
        }
        return "algorithm=" + settings.getMacAlgorithm()
                + ", configuredMacField=DE" + settings.getMacField()
                + ", tsk=" + IsoLogUtils.keyFingerprint(settings.getTskPlain());
    }

    private String macSkipReason(KeySettingsDto settings, IsoMessageDto message) {
        if (settings == null) {
            return "settings_missing";
        }
        if (isBlank(settings.getTskPlain())) {
            return "tsk_blank";
        }
        if (isTidInitialization(message)) {
            return "tid_initialization";
        }
        return "not_required";
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
