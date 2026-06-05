package com.example.faketcp.tcp;

import com.example.faketcp.config.ChannelProperties;
import com.example.faketcp.config.FakeTcpProperties;
import com.example.faketcp.dto.IsoMessageDto;
import com.example.faketcp.dto.KeySettingsDto;
import com.example.faketcp.dto.RequestLogDto;
import com.example.faketcp.dto.RuleDto;
import com.example.faketcp.iso.HexUtils;
import com.example.faketcp.iso.IsoCodec;
import com.example.faketcp.iso.IsoMacService;
import com.example.faketcp.iso.PackagerFactory;
import com.example.faketcp.repository.RequestLogRepository;
import com.example.faketcp.repository.RuleRepository;
import com.example.faketcp.rule.RuleEngine;
import com.example.faketcp.service.ChannelService;
import com.example.faketcp.service.KeySettingsService;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.net.ssl.SSLSocket;
import org.jpos.iso.ISOBasePackager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TcpServerManager {
    private static final Logger log = LoggerFactory.getLogger(TcpServerManager.class);
    private static final String LISTEN_HOST = "0.0.0.0";

    private final ChannelService channelService;
    private final PackagerFactory packagerFactory;
    private final IsoCodec isoCodec;
    private final RuleRepository ruleRepository;
    private final RuleEngine ruleEngine;
    private final RequestLogRepository requestLogRepository;
    private final KeySettingsService keySettingsService;
    private final IsoMacService isoMacService;
    private final FakeTcpProperties properties;
    private final MockTlsServerSocketFactory mockTlsServerSocketFactory;
    private final ExecutorService acceptExecutor = Executors.newCachedThreadPool();
    private final ExecutorService clientExecutor = Executors.newCachedThreadPool();

    public TcpServerManager(
            ChannelService channelService,
            PackagerFactory packagerFactory,
            IsoCodec isoCodec,
            RuleRepository ruleRepository,
            RuleEngine ruleEngine,
            RequestLogRepository requestLogRepository,
            KeySettingsService keySettingsService,
            IsoMacService isoMacService,
            FakeTcpProperties properties,
            MockTlsServerSocketFactory mockTlsServerSocketFactory) {
        this.channelService = channelService;
        this.packagerFactory = packagerFactory;
        this.isoCodec = isoCodec;
        this.ruleRepository = ruleRepository;
        this.ruleEngine = ruleEngine;
        this.requestLogRepository = requestLogRepository;
        this.keySettingsService = keySettingsService;
        this.isoMacService = isoMacService;
        this.properties = properties;
        this.mockTlsServerSocketFactory = mockTlsServerSocketFactory;
    }

    @PostConstruct
    public void start() {
        List<ChannelProperties> channels = channelService.enabledChannels();
        log.info("Starting {} enabled Fake TCP channel listener(s)", channels.size());
        for (ChannelProperties channel : channels) {
            acceptExecutor.submit(() -> startChannel(channel));
        }
    }

    @PreDestroy
    public void stop() {
        acceptExecutor.shutdownNow();
        clientExecutor.shutdownNow();
    }

    private void startChannel(ChannelProperties channel) {
        try {
            ISOBasePackager packager = packagerFactory.create(channel.getIso8583().getPackager());
            ChannelRuntime runtime = new ChannelRuntime(
                    channel,
                    packager,
                    channelService.fieldValueTypes(packager));
            acceptLoop(runtime);
        } catch (Exception e) {
            log.error("TCP channel {} failed to initialize runtime", channel.getId(), e);
        }
    }

    private void acceptLoop(ChannelRuntime runtime) {
        ChannelProperties channel = runtime.getChannel();
        try (ServerSocket serverSocket = createServerSocket(channel)) {
            log.info("Fake TCP channel {} listening on {}:{} tls={}", channel.getId(), LISTEN_HOST, channel.getTcp().getPort(), channel.getTcp().isTlsEnabled());
            while (!Thread.currentThread().isInterrupted()) {
                Socket socket = serverSocket.accept();
                socket.setSoTimeout(channel.getTcp().getReadTimeoutMs());
                clientExecutor.submit(() -> handleClient(runtime, socket));
            }
        } catch (IOException e) {
            log.error("TCP channel {} stopped", channel.getId(), e);
        } catch (Exception e) {
            log.error("TCP channel {} failed to start", channel.getId(), e);
        }
    }

    private ServerSocket createServerSocket(ChannelProperties channel) throws Exception {
        if (channel.getTcp().isTlsEnabled()) {
            return mockTlsServerSocketFactory.create(
                    properties.getMockTls(),
                    LISTEN_HOST,
                    channel.getTcp().getPort(),
                    50);
        }
        return new ServerSocket(
                channel.getTcp().getPort(),
                50,
                InetAddress.getByName(LISTEN_HOST));
    }

    private void handleClient(ChannelRuntime runtime, Socket socket) {
        try (Socket ignored = socket) {
            if (socket instanceof SSLSocket) {
                ((SSLSocket) socket).startHandshake();
            }
            while (!socket.isClosed()) {
                handleFrame(runtime, socket);
            }
        } catch (IOException e) {
            log.debug("TCP client closed for channel {}", runtime.getChannel().getId(), e);
        } catch (Exception e) {
            log.warn("TCP client failed for channel {}", runtime.getChannel().getId(), e);
        }
    }

    private void handleFrame(ChannelRuntime runtime, Socket socket) throws Exception {
        Instant startedAt = Instant.now();
        RequestLogDto requestLog = new RequestLogDto();
        requestLog.setChannelId(runtime.getChannel().getId());
        requestLog.setRemoteAddress(socket.getRemoteSocketAddress().toString());

        byte[] requestPayload = null;
        byte[] responsePayload = null;
        IsoMessageDto request = null;
        IsoMessageDto response = null;
        RuleDto matchedRule = null;
        try {
            requestPayload = runtime.getFramer().readFrame(socket.getInputStream());
            PayloadParts requestParts = runtime.getHeaderHandler().split(requestPayload);
            request = isoCodec.unpack(requestParts.getIsoBody(), runtime.getPackager());
            requestLog.setMti(request.getMti());
            requestLog.setProcessingCode(request.getFields().get("3"));
            requestLog.setRequestFields(request.getFields());
            KeySettingsDto keySettings = keySettingsService.get(runtime.getChannel().getId());

            if (!isoMacService.verifyRequestMac(request, runtime.getPackager(), keySettings)) {
                response = ruleEngine.defaultResponse(request, runtime.getChannel().getNoMatch().getResponseCode());
                requestLog.setActionType("MAC_INVALID_DEFAULT");
            } else {
                Optional<com.example.faketcp.rule.RuleMatchResult> match =
                        ruleEngine.match(ruleRepository.findEnabledByChannel(runtime.getChannel().getId()), request, runtime.getFieldValueTypes());
                if (match.isPresent()) {
                    matchedRule = match.get().getRule();
                    response = match.get().getResponse();
                } else {
                    response = ruleEngine.defaultResponse(request, runtime.getChannel().getNoMatch().getResponseCode());
                }

                if (matchedRule != null) {
                    requestLog.setMatchedRuleId(matchedRule.getId());
                    requestLog.setMatchedRuleName(matchedRule.getName());
                    requestLog.setActionType(matchedRule.getAction().getType().name());
                } else {
                    requestLog.setActionType("NO_MATCH_DEFAULT");
                }
            }

            if (matchedRule != null && matchedRule.getAction().getType() == com.example.faketcp.dto.ActionDto.ActionType.TIMEOUT) {
                sleep(matchedRule.getAction().getDelayMs());
                return;
            }
            if (matchedRule != null && matchedRule.getAction().getType() == com.example.faketcp.dto.ActionDto.ActionType.DISCONNECT) {
                socket.close();
                return;
            }
            if (matchedRule != null && matchedRule.getAction().getType() == com.example.faketcp.dto.ActionDto.ActionType.DELAY_RESPOND) {
                sleep(matchedRule.getAction().getDelayMs());
            }

            isoMacService.sign(response, runtime.getPackager(), keySettings);
            byte[] isoBody = isoCodec.pack(response, runtime.getPackager());
            responsePayload = runtime.getHeaderHandler().combine(requestParts.getHeader(), isoBody);
            socket.getOutputStream().write(runtime.getFramer().writeFrame(responsePayload));
            socket.getOutputStream().flush();
            requestLog.setResponseFields(response.getFields());
            requestLog.setResponseCode(response.getFields().get("39"));
        } catch (Exception e) {
            requestLog.setErrorMessage(e.getMessage());
            throw e;
        } finally {
            requestLog.setRequestHex(HexUtils.toHex(requestPayload));
            requestLog.setResponseHex(HexUtils.toHex(responsePayload));
            requestLog.setDurationMs(Duration.between(startedAt, Instant.now()).toMillis());
            requestLogRepository.save(requestLog);
        }
    }

    private void sleep(long delayMs) throws InterruptedException {
        if (delayMs > 0) {
            Thread.sleep(delayMs);
        }
    }
}
