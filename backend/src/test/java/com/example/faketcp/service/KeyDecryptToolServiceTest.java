package com.example.faketcp.service;

import com.example.faketcp.config.ChannelProperties;
import com.example.faketcp.dto.KeyDecryptRequestDto;
import com.example.faketcp.dto.KeyDecryptResponseDto;
import com.example.faketcp.iso.HexUtils;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KeyDecryptToolServiceTest {
    private static final String CHANNEL_ID = "channel-a";
    private final ChannelService channelService = mock(ChannelService.class);
    private final KeyDecryptToolService service = new KeyDecryptToolService(channelService);

    @Test
    void decryptsEncryptedWorkingKeysWithChannelCtmkComponents() throws Exception {
        String ctmkPlain = "00112233445566778899AABBCCDDEEFF";
        ChannelProperties channel = new ChannelProperties();
        channel.setCtmk1("11111111111111111111111111111111");
        channel.setCtmk2("11003322554477669988BBAADDCCFFEE");
        when(channelService.getRequired(eq(CHANNEL_ID))).thenReturn(channel);

        String tmkPlain = "0123456789ABCDEFFEDCBA9876543210";
        String tpkPlain = "89ABCDEF0123456776543210FEDCBA98";
        String tskPlain = "ABCDEF01234567899876543210FEDCBA";
        KeyDecryptRequestDto request = new KeyDecryptRequestDto();
        request.setEncryptedTmk(encrypt(tmkPlain, ctmkPlain));
        request.setEncryptedTpk(encrypt(tpkPlain, tmkPlain));
        request.setEncryptedTsk(encrypt(tskPlain, tmkPlain));

        KeyDecryptResponseDto response = service.decrypt(CHANNEL_ID, request);

        assertThat(response.getCtmkSource()).isEqualTo("CHANNEL");
        assertThat(response.getCtmkPlain()).isEqualTo(ctmkPlain);
        assertThat(response.getTmkPlain()).isEqualTo(tmkPlain);
        assertThat(response.getTpkPlain()).isEqualTo(tpkPlain);
        assertThat(response.getTskPlain()).isEqualTo(tskPlain);
    }

    private String encrypt(String clearHex, String keyHex) throws Exception {
        Cipher cipher = Cipher.getInstance("DESede/ECB/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(normalizeDesKey(HexUtils.fromHex(keyHex)), "DESede"));
        return HexUtils.toHex(cipher.doFinal(HexUtils.fromHex(clearHex)));
    }

    private byte[] normalizeDesKey(byte[] key) {
        if (key.length == 24) {
            return key;
        }
        if (key.length == 16) {
            byte[] expanded = Arrays.copyOf(key, 24);
            System.arraycopy(key, 0, expanded, 16, 8);
            return expanded;
        }
        throw new IllegalArgumentException("test key must be double or triple length");
    }
}
