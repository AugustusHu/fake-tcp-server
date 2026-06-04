package com.example.faketcp.iso;

import com.example.faketcp.dto.IsoMessageDto;
import com.example.faketcp.dto.KeySettingsDto;
import java.security.MessageDigest;
import java.util.concurrent.atomic.AtomicReference;
import org.jpos.iso.ISOPackager;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IsoMacServiceTest {
    private static final String ZERO_SHA256_FIELD128_MAC =
            "0000000000000000000000000000000000000000000000000000000000000000";
    private final IsoMacService isoMacService = new IsoMacService(null);

    @Test
    void actualBitmapFieldsTakePrecedenceOverDefaults() {
        IsoMessageDto message = message("000000");
        message.getFields().put("64", "AAAAAAAAAAAAAAAA");

        assertThat(isoMacService.macField(message)).isEqualTo("64");
    }

    @Test
    void secondaryMacFieldWinsWhenBothMacFieldsExist() {
        IsoMessageDto message = message("9G0000");
        message.getFields().put("64", "AAAAAAAAAAAAAAAA");
        message.getFields().put("128", "BBBBBBBBBBBBBBBB");

        assertThat(isoMacService.macField(message)).isEqualTo("128");
    }

    @Test
    void xmlBitmapHintSelectsMacFieldWhenValueIsNotPresentYet() {
        IsoMessageDto message = message("000000");
        message.setBitmapMacFieldHint("64");

        assertThat(isoMacService.macField(message)).isEqualTo("64");
    }

    @Test
    void parameterDownloadAndCallhomeDefaultToField64() {
        assertThat(isoMacService.defaultMacField(message("9C0000"))).isEqualTo("64");
        assertThat(isoMacService.defaultMacField(message("9D0000"))).isEqualTo("64");
    }

    @Test
    void otherTransactionsDefaultToField128() {
        assertThat(isoMacService.defaultMacField(message("000000"))).isEqualTo("128");
    }

    @Test
    void tidInitializationDoesNotRequireMac() {
        KeySettingsDto settings = new KeySettingsDto();
        settings.setTskPlain("0123456789ABCDEF");

        assertThat(isoMacService.macRequired(settings, message("9A0000"))).isFalse();
        assertThat(isoMacService.macRequired(settings, message("9B0000"))).isFalse();
        assertThat(isoMacService.macRequired(settings, message("9G0000"))).isFalse();
        assertThat(isoMacService.macRequired(settings, message("9C0000"))).isTrue();
    }

    @Test
    void sha256Field128AlwaysUsesField128() {
        KeySettingsDto settings = sha256Field128Settings();
        IsoMessageDto message = message("9G0000");
        message.getFields().put("64", "AAAAAAAAAAAAAAAA");

        assertThat(isoMacService.macField(message, settings)).isEqualTo("128");
    }

    @Test
    void sha256Field128SignsTrimmedPackedDataWithZeroField128() throws Exception {
        AtomicReference<IsoMessageDto> packedMessage = new AtomicReference<>();
        IsoMacService service = new IsoMacService(new IsoCodec() {
            @Override
            public byte[] pack(IsoMessageDto dto, ISOPackager packager) {
                packedMessage.set(dto);
                return packedWithMacTail("0200AABBCCDD");
            }
        });
        KeySettingsDto settings = sha256Field128Settings();
        IsoMessageDto message = message("000000");

        String mac = service.sign(message, null, settings);

        assertThat(packedMessage.get().getFields().get("128")).isEqualTo(ZERO_SHA256_FIELD128_MAC);
        assertThat(mac).isEqualTo(sha256Hex("0123456789ABCDEF", HexUtils.fromHex("0200AABBCCDD")));
        assertThat(message.getFields().get("128")).isEqualTo(mac);
        assertThat(mac).hasSize(64);
    }

    @Test
    void sha256Field128VerifiesAgainstField128() throws Exception {
        IsoMacService service = new IsoMacService(new IsoCodec() {
            @Override
            public byte[] pack(IsoMessageDto dto, ISOPackager packager) {
                return packedWithMacTail("0200AABBCCDD");
            }
        });
        KeySettingsDto settings = sha256Field128Settings();
        IsoMessageDto message = message("000000");
        message.getFields().put("128", sha256Hex("0123456789ABCDEF", HexUtils.fromHex("0200AABBCCDD")));

        assertThat(service.verifyRequestMac(message, null, settings)).isTrue();
    }

    private IsoMessageDto message(String processCode) {
        IsoMessageDto message = new IsoMessageDto();
        message.setMti("0200");
        message.getFields().put("3", processCode);
        return message;
    }

    private KeySettingsDto sha256Field128Settings() {
        KeySettingsDto settings = new KeySettingsDto();
        settings.setTskPlain("0123456789ABCDEF");
        settings.setMacAlgorithm("SHA256_FIELD128_TRIM64");
        return settings;
    }

    private String sha256Hex(String tsk, byte[] packedData) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(HexUtils.fromHex(tsk));
        digest.update(packedData);
        return HexUtils.toHex(digest.digest());
    }

    private byte[] packedWithMacTail(String prefixHex) {
        byte[] prefix = HexUtils.fromHex(prefixHex);
        byte[] data = new byte[prefix.length + 64];
        System.arraycopy(prefix, 0, data, 0, prefix.length);
        return data;
    }
}
