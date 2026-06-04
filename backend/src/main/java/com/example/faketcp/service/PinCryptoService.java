package com.example.faketcp.service;

import com.example.faketcp.dto.IsoMessageDto;
import com.example.faketcp.dto.KeySettingsDto;
import com.example.faketcp.iso.HexUtils;
import com.example.faketcp.iso.IsoFieldReferences;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class PinCryptoService {
    private static final String NONE = "NONE";
    private static final String ISO0_3DES_ECB = "ISO0_3DES_ECB";

    public void applyPin(IsoMessageDto message, KeySettingsDto settings, String algorithm, String pin, String pan) {
        String normalizedAlgorithm = normalizeAlgorithm(algorithm);
        if (NONE.equals(normalizedAlgorithm)) {
            return;
        }
        if (!ISO0_3DES_ECB.equals(normalizedAlgorithm)) {
            throw new IllegalArgumentException("PIN 算法只支持 NONE 或 ISO0_3DES_ECB");
        }
        if (isBlank(pin)) {
            throw new IllegalArgumentException("启用 PIN 加密时 PIN 不能为空");
        }
        String effectivePan = isBlank(pan) ? fieldValue(message, "2") : pan;
        if (isBlank(effectivePan)) {
            throw new IllegalArgumentException("启用 PIN 加密时 PAN 不能为空，可填写参数或在 XML DE2 中提供");
        }
        if (settings == null || isBlank(settings.getTpkPlain())) {
            throw new IllegalArgumentException("启用 PIN 加密时需要先在 Key 设置中配置明文 TPK");
        }
        message.getFields().put("52", encryptIso0PinBlock(pin.trim(), effectivePan, settings.getTpkPlain()));
    }

    private String encryptIso0PinBlock(String pin, String pan, String tpkPlain) {
        if (!pin.matches("[0-9]{4,12}")) {
            throw new IllegalArgumentException("PIN 必须是 4 到 12 位数字");
        }
        String cleanPan = pan.replaceAll("[^0-9]", "");
        if (cleanPan.length() < 2) {
            throw new IllegalArgumentException("PAN 格式不正确");
        }
        String pinBlock = "0" + Integer.toHexString(pin.length()).toUpperCase() + pin;
        while (pinBlock.length() < 16) {
            pinBlock += "F";
        }
        String pan12 = panBlockDigits(cleanPan);
        byte[] clearBlock = xor(HexUtils.fromHex(pinBlock), HexUtils.fromHex("0000" + pan12));
        try {
            return HexUtils.toHex(encrypt(clearBlock, HexUtils.fromHex(tpkPlain)));
        } catch (Exception e) {
            throw new IllegalStateException("PIN 加密失败: " + e.getMessage(), e);
        }
    }

    private String panBlockDigits(String pan) {
        String withoutCheckDigit = pan.substring(0, pan.length() - 1);
        if (withoutCheckDigit.length() > 12) {
            return withoutCheckDigit.substring(withoutCheckDigit.length() - 12);
        }
        StringBuilder builder = new StringBuilder();
        for (int i = withoutCheckDigit.length(); i < 12; i++) {
            builder.append('0');
        }
        builder.append(withoutCheckDigit);
        return builder.toString();
    }

    private byte[] encrypt(byte[] clearBlock, byte[] key) throws Exception {
        if (key.length == 8) {
            Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "DES"));
            return cipher.doFinal(clearBlock);
        }
        if (key.length == 16) {
            byte[] expanded = Arrays.copyOf(key, 24);
            System.arraycopy(key, 0, expanded, 16, 8);
            return tripleDes(clearBlock, expanded);
        }
        if (key.length == 24) {
            return tripleDes(clearBlock, key);
        }
        throw new IllegalArgumentException("TPK 必须是 16、32 或 48 位 HEX");
    }

    private byte[] tripleDes(byte[] clearBlock, byte[] key) throws Exception {
        Cipher cipher = Cipher.getInstance("DESede/ECB/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "DESede"));
        return cipher.doFinal(clearBlock);
    }

    private byte[] xor(byte[] left, byte[] right) {
        byte[] result = new byte[left.length];
        for (int i = 0; i < left.length; i++) {
            result[i] = (byte) (left[i] ^ right[i]);
        }
        return result;
    }

    private String fieldValue(IsoMessageDto message, String field) {
        if (message == null || message.getFields() == null) {
            return null;
        }
        String canonical = IsoFieldReferences.canonical(field);
        for (java.util.Map.Entry<String, String> entry : message.getFields().entrySet()) {
            if (canonical.equals(IsoFieldReferences.canonical(entry.getKey()))) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String normalizeAlgorithm(String algorithm) {
        return isBlank(algorithm) ? NONE : algorithm.trim().toUpperCase();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
