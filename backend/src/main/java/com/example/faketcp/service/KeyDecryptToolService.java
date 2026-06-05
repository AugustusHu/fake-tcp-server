package com.example.faketcp.service;

import com.example.faketcp.config.ChannelProperties;
import com.example.faketcp.dto.KeyDecryptRequestDto;
import com.example.faketcp.dto.KeyDecryptResponseDto;
import com.example.faketcp.iso.HexUtils;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class KeyDecryptToolService {
    private final ChannelService channelService;

    public KeyDecryptToolService(ChannelService channelService) {
        this.channelService = channelService;
    }

    public KeyDecryptResponseDto decrypt(String channelId, KeyDecryptRequestDto request) {
        ChannelProperties channel = channelService.getRequired(channelId);
        KeyDecryptRequestDto input = request == null ? new KeyDecryptRequestDto() : request;
        String ctmk1 = firstPresent(input.getCtmk1(), channel.getCtmk1());
        String ctmk2 = firstPresent(input.getCtmk2(), channel.getCtmk2());
        String source = isBlank(input.getCtmk1()) && isBlank(input.getCtmk2()) ? "CHANNEL" : "MANUAL";

        byte[] ctmkPlain = xorKey(cleanHex("CTMK1", ctmk1), cleanHex("CTMK2", ctmk2));
        KeyDecryptResponseDto response = new KeyDecryptResponseDto();
        response.setCtmkPlain(HexUtils.toHex(ctmkPlain));
        response.setCtmkSource(source);
        response.setAlgorithm("CTMK=CTMK1 XOR CTMK2; key decrypt=3DES/ECB/NoPadding");

        byte[] tmkPlain = null;
        if (!isBlank(input.getEncryptedTmk())) {
            tmkPlain = decryptKey("TMK", input.getEncryptedTmk(), ctmkPlain);
            response.setTmkPlain(HexUtils.toHex(tmkPlain));
        }
        if (!isBlank(input.getEncryptedTpk())) {
            tmkPlain = requireTmk(tmkPlain);
            response.setTpkPlain(HexUtils.toHex(decryptKey("TPK", input.getEncryptedTpk(), tmkPlain)));
        }
        if (!isBlank(input.getEncryptedTsk())) {
            tmkPlain = requireTmk(tmkPlain);
            response.setTskPlain(HexUtils.toHex(decryptKey("TSK", input.getEncryptedTsk(), tmkPlain)));
        }
        return response;
    }

    private byte[] requireTmk(byte[] tmkPlain) {
        if (tmkPlain == null || tmkPlain.length == 0) {
            throw new IllegalStateException("解密 TPK/TSK 前必须先填写密文 TMK");
        }
        return tmkPlain;
    }

    private byte[] decryptKey(String name, String encryptedKey, byte[] kek) {
        try {
            byte[] data = cleanHex(name + " 密文", encryptedKey);
            if (data.length == 0 || data.length % 8 != 0) {
                throw new IllegalStateException(name + " 密文必须是 8 字节倍数的 HEX");
            }
            return decrypt(data, normalizeDesKey(kek));
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(name + " 解密失败: " + e.getMessage(), e);
        }
    }

    private byte[] decrypt(byte[] data, byte[] key) throws Exception {
        if (key.length == 8) {
            Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "DES"));
            return cipher.doFinal(data);
        }
        Cipher cipher = Cipher.getInstance("DESede/ECB/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "DESede"));
        return cipher.doFinal(data);
    }

    private byte[] normalizeDesKey(byte[] key) {
        if (key.length == 8 || key.length == 24) {
            return key;
        }
        if (key.length == 16) {
            byte[] expanded = Arrays.copyOf(key, 24);
            System.arraycopy(key, 0, expanded, 16, 8);
            return expanded;
        }
        throw new IllegalStateException("密钥明文必须是 16、32 或 48 位 HEX");
    }

    private byte[] xorKey(byte[] left, byte[] right) {
        if (left.length != right.length) {
            throw new IllegalStateException("CTMK1 和 CTMK2 长度必须一致");
        }
        byte[] result = new byte[left.length];
        for (int i = 0; i < left.length; i++) {
            result[i] = (byte) (left[i] ^ right[i]);
        }
        normalizeDesKey(result);
        return result;
    }

    private byte[] cleanHex(String name, String value) {
        if (isBlank(value)) {
            throw new IllegalStateException(name + " 不能为空");
        }
        String clean = value.replaceAll("\\s+", "").toUpperCase();
        if (!clean.matches("[0-9A-F]+") || clean.length() % 2 != 0) {
            throw new IllegalStateException(name + " 必须是偶数长度 HEX");
        }
        return HexUtils.fromHex(clean);
    }

    private String firstPresent(String preferred, String fallback) {
        return isBlank(preferred) ? fallback : preferred;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
