package com.example.faketcp.service;

import com.example.faketcp.config.ChannelProperties;
import com.example.faketcp.config.MockTlsProperties;
import com.example.faketcp.dto.IsoMessageDto;
import com.example.faketcp.dto.PosDebugBuildXmlRequestDto;
import com.example.faketcp.dto.RuleCapability;
import com.example.faketcp.iso.IsoXmlMapper;
import com.example.faketcp.tcp.MockTlsServerSocketFactory;
import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PosDebugServiceTest {
    private static final String CHANNEL_ID = "channel-a";
    private final IsoXmlMapper isoXmlMapper = new IsoXmlMapper();
    private final ChannelService channelService = mock(ChannelService.class);
    private final PosDebugService service = new PosDebugService(
            channelService,
            null,
            null,
            null,
            isoXmlMapper,
            null,
            null,
            null);

    @Test
    void buildXmlGeneratesFinancialSkeletonWithEnvironmentAndDynamicFields() {
        when(channelService.getRequired(eq(CHANNEL_ID))).thenReturn(new ChannelProperties());
        PosDebugBuildXmlRequestDto request = new PosDebugBuildXmlRequestDto();
        request.setCapability("DEBIT");
        request.setEnvironmentFields(fields(
                "2", "5061231451417639567",
                "14", "3002",
                "41", "TERM0001",
                "52", "1122334455667788"));
        request.setDynamicFields(fields(
                "18", "6012",
                "42", "2044LA310924601",
                "43", "PALMPAY LIMITED        LA           LANG",
                "49", "566"));
        request.setFields(fields("4", "000000010000"));

        IsoMessageDto message = isoXmlMapper.parse(service.buildXml(CHANNEL_ID, request).getRequestXml());

        assertThat(message.getMti()).isEqualTo("0200");
        assertThat(message.getFields().get("3")).isEqualTo("000000");
        assertThat(message.getFields().get("2")).isEqualTo("5061231451417639567");
        assertThat(message.getFields().get("4")).isEqualTo("000000010000");
        assertThat(message.getFields().get("18")).isEqualTo("6012");
        assertThat(message.getFields().get("41")).isEqualTo("TERM0001");
        assertThat(message.getFields().get("42")).isEqualTo("2044LA310924601");
        assertThat(message.getFields().get("49")).isEqualTo("566");
        assertThat(message.getFields().get("11")).matches("\\d{6}");
        assertThat(message.getFields().get("37")).matches("\\d{12}");
        assertThat(message.getFields().get("22")).isEqualTo("");
        assertThat(message.getFields().get("128")).isEqualTo("");
    }

    @Test
    void buildXmlForTidInitializationUsesTidAndSnWithoutCardFields() {
        when(channelService.getRequired(eq(CHANNEL_ID))).thenReturn(new ChannelProperties());
        PosDebugBuildXmlRequestDto request = new PosDebugBuildXmlRequestDto();
        request.setCapability("TID_INIT_9A");
        request.setEnvironmentFields(fields("2", "5061231451417639567", "41", "TERM0001", "SN", "SN123"));
        request.setDynamicFields(fields("18", "6012"));

        IsoMessageDto message = isoXmlMapper.parse(service.buildXml(CHANNEL_ID, request).getRequestXml());

        assertThat(message.getMti()).isEqualTo("0800");
        assertThat(message.getFields().get("3")).isEqualTo("9A0000");
        assertThat(message.getFields().get("41")).isEqualTo("TERM0001");
        assertThat(message.getFields().get("62")).isEqualTo("01005SN123");
        assertThat(message.getFields()).doesNotContainKeys("2", "18", "37", "128");
    }

    @Test
    void debugTlsSocketFactoryAcceptsSelfSignedExternalCertificate() throws Exception {
        MockTlsServerSocketFactory serverFactory = new MockTlsServerSocketFactory();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try (SSLServerSocket serverSocket = serverFactory.create(new MockTlsProperties(), "127.0.0.1", 0, 1)) {
            serverSocket.setSoTimeout(5000);
            Future<?> server = executor.submit(() -> {
                try (SSLSocket accepted = (SSLSocket) serverSocket.accept()) {
                    accepted.startHandshake();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            try (SSLSocket client = (SSLSocket) PosDebugService.debugTlsSocketFactory().createSocket()) {
                client.connect(new InetSocketAddress("127.0.0.1", serverSocket.getLocalPort()), 5000);
                client.startHandshake();
            }

            server.get(5, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void tidInitializationCapabilitiesSkipRequestAndResponseMac() {
        assertThat(PosDebugService.skipMacForCapability(RuleCapability.TID_INIT_9A)).isTrue();
        assertThat(PosDebugService.skipMacForCapability(RuleCapability.TID_INIT_9G)).isTrue();
        assertThat(PosDebugService.skipMacForCapability(RuleCapability.TID_INIT_9B)).isTrue();
        assertThat(PosDebugService.skipMacForCapability(RuleCapability.PARAMETER_DOWNLOAD)).isFalse();
    }

    private Map<String, String> fields(String... entries) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (int index = 0; index + 1 < entries.length; index += 2) {
            fields.put(entries[index], entries[index + 1]);
        }
        return fields;
    }
}
