package com.example.faketcp.service;

import com.example.faketcp.config.ChannelProperties;
import com.example.faketcp.dto.ChannelDto;
import com.example.faketcp.dto.DebugEnvironmentDto;
import com.example.faketcp.dto.IsoMessageDto;
import com.example.faketcp.dto.KeySettingsDto;
import com.example.faketcp.dto.PosDebugBuildXmlRequestDto;
import com.example.faketcp.dto.PosDebugBuildXmlResponseDto;
import com.example.faketcp.dto.PosDebugResponseDto;
import com.example.faketcp.dto.PosDebugSendRequestDto;
import com.example.faketcp.dto.PosDebugStepDto;
import com.example.faketcp.dto.PosDebugTidInitRequestDto;
import com.example.faketcp.dto.RuleCapability;
import com.example.faketcp.iso.HexUtils;
import com.example.faketcp.iso.IsoCodec;
import com.example.faketcp.iso.IsoFieldReferences;
import com.example.faketcp.iso.IsoMacService;
import com.example.faketcp.iso.IsoXmlMapper;
import com.example.faketcp.iso.PackagerFactory;
import com.example.faketcp.repository.DebugEnvironmentRepository;
import com.example.faketcp.tcp.Framer;
import com.example.faketcp.tcp.HeaderHandler;
import com.example.faketcp.tcp.PayloadParts;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.jpos.iso.ISOBasePackager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PosDebugService {
    private static final Logger log = LoggerFactory.getLogger(PosDebugService.class);
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final DateTimeFormatter DE7 = DateTimeFormatter.ofPattern("MMddHHmmss");
    private static final DateTimeFormatter DE12 = DateTimeFormatter.ofPattern("HHmmss");
    private static final DateTimeFormatter DE13 = DateTimeFormatter.ofPattern("MMdd");
    private static final DateTimeFormatter DE37 = DateTimeFormatter.ofPattern("yyMMddHHmmss");
    private static final SSLSocketFactory DEBUG_TLS_SOCKET_FACTORY = createDebugTlsSocketFactory();
    private static final List<String> AUTHORIZATION_REQUIRED_FIELDS = Arrays.asList(
            "2", "3", "4", "7", "11", "12", "13", "14", "18", "22", "25", "28", "32", "37",
            "41", "42", "43", "49", "123", "128");
    private static final List<String> FINANCIAL_REQUIRED_FIELDS = Arrays.asList(
            "2", "3", "4", "7", "11", "12", "13", "14", "18", "22", "25", "28", "32", "37",
            "41", "42", "43", "49", "123", "128");
    private static final List<String> FINANCIAL_ADVICE_REQUIRED_FIELDS = Arrays.asList(
            "3", "4", "7", "11", "12", "13", "22", "25", "41", "42", "43", "49", "123", "128");
    private static final List<String> REVERSAL_REQUIRED_FIELDS = Arrays.asList(
            "2", "3", "4", "7", "11", "12", "13", "14", "18", "22", "25", "32", "37",
            "41", "42", "43", "49", "56", "90", "95", "123", "128");
    private static final List<String> NETWORK_MANAGEMENT_REQUIRED_FIELDS = Arrays.asList(
            "3", "7", "11", "12", "13", "41");
    private static final List<String> STANDARD_AUTO_FIELDS = Arrays.asList(
            "2", "14", "18", "41", "42", "43", "49", "52");
    private static final List<String> NETWORK_AUTO_FIELDS = Arrays.asList("41", "62");
    private final Random random = new Random();

    private final ChannelService channelService;
    private final KeySettingsService keySettingsService;
    private final PackagerFactory packagerFactory;
    private final IsoCodec isoCodec;
    private final IsoXmlMapper isoXmlMapper;
    private final IsoMacService isoMacService;
    private final PinCryptoService pinCryptoService;
    private final DebugEnvironmentRepository debugEnvironmentRepository;

    public PosDebugService(
            ChannelService channelService,
            KeySettingsService keySettingsService,
            PackagerFactory packagerFactory,
            IsoCodec isoCodec,
            IsoXmlMapper isoXmlMapper,
            IsoMacService isoMacService,
            PinCryptoService pinCryptoService,
            DebugEnvironmentRepository debugEnvironmentRepository) {
        this.channelService = channelService;
        this.keySettingsService = keySettingsService;
        this.packagerFactory = packagerFactory;
        this.isoCodec = isoCodec;
        this.isoXmlMapper = isoXmlMapper;
        this.isoMacService = isoMacService;
        this.pinCryptoService = pinCryptoService;
        this.debugEnvironmentRepository = debugEnvironmentRepository;
    }

    public PosDebugBuildXmlResponseDto buildXml(String channelId, PosDebugBuildXmlRequestDto request) {
        return buildXml(null, channelId, request);
    }

    public PosDebugBuildXmlResponseDto buildXml(Long userId, String channelId, PosDebugBuildXmlRequestDto request) {
        channelService.getRequired(channelId);
        DebugEnvironmentDto environment = environment(userId, request == null ? null : request.getEnvironmentId());
        PosDebugBuildXmlRequestDto effectiveRequest = mergeBuildXmlRequest(environment, request);
        RuleCapability capability = capability(effectiveRequest == null ? null : effectiveRequest.getCapability());
        IsoMessageDto message = buildXmlMessage(capability, effectiveRequest);
        PosDebugBuildXmlResponseDto response = new PosDebugBuildXmlResponseDto();
        response.setCapability(capability.name());
        response.setRequestXml(isoXmlMapper.render(message));
        return response;
    }

    public PosDebugResponseDto send(String channelId, PosDebugSendRequestDto request) {
        return send(null, channelId, request);
    }

    public PosDebugResponseDto send(Long userId, String channelId, PosDebugSendRequestDto request) {
        log.info("POS debug send requested channel={} capability={} target={}:{}",
                channelId,
                request == null ? null : request.getCapability(),
                request == null ? null : request.getTargetIp(),
                request == null ? null : request.getTargetPort());
        DebugEnvironmentDto environment = environment(userId, request == null ? null : request.getEnvironmentId());
        DebugTarget target = target(channelId, environment, request == null ? null : request.getTargetIp(), request == null ? null : request.getTargetPort());
        EffectiveDebugInputs inputs = effectiveInputs(channelId, environment, request);
        RuleCapability capability = capability(request == null ? null : request.getCapability());
        String requestXml = request == null ? null : request.getRequestXml();
        PosDebugStepDto step = runStep(
                target,
                capability,
                capability.getLabel(),
                requestXml,
                inputs.macAlgorithm.value,
                inputs.pinAlgorithm.value,
                request == null ? null : request.getPin(),
                inputs.pan.value,
                inputs.de52.value,
                inputs.tpkPlain.value,
                inputs.tskPlain.value,
                inputs.macField.value,
                inputs.logs,
                false);
        PosDebugResponseDto response = baseResponse(target);
        response.getSteps().add(step);
        response.setSuccess(step.isSuccess());
        if (step.isSuccess() && request != null && request.isSaveTidConfig() && capability == RuleCapability.PARAMETER_DOWNLOAD) {
            response.setReport(saveTidConfigReport(channelId, requestXml, step.getResponseXml()));
        } else {
            response.setReport(report(response.getSteps(), null));
        }
        return response;
    }

    public PosDebugResponseDto tidInit(String channelId, PosDebugTidInitRequestDto request) {
        return tidInit(null, channelId, request);
    }

    public PosDebugResponseDto tidInit(Long userId, String channelId, PosDebugTidInitRequestDto request) {
        log.info("POS TID init requested channel={} target={}:{} tid={} snPresent={}",
                channelId,
                request == null ? null : request.getTargetIp(),
                request == null ? null : request.getTargetPort(),
                request == null ? null : request.getTid(),
                request != null && !isBlank(request.getSn()));
        DebugEnvironmentDto environment = environment(userId, request == null ? null : request.getEnvironmentId());
        DebugTarget target = target(channelId, environment, request == null ? null : request.getTargetIp(), request == null ? null : request.getTargetPort());
        ResolvedString tidValue = resolve("TID", environment == null ? null : environment.getTestTid(), "Environment.testTid", request == null ? null : request.getTid(), "request.tid", null, "none");
        ResolvedString snValue = resolve("SN", environment == null ? null : environment.getTestSn(), "Environment.testSn", request == null ? null : request.getSn(), "request.sn", null, "none");
        String tid = tidValue.value;
        String sn = snValue.value;
        if (isBlank(tid)) {
            throw new IllegalArgumentException("TID 初始化需要填写 TID");
        }
        if (isBlank(sn)) {
            throw new IllegalArgumentException("TID 初始化需要填写 SN");
        }
        List<RuleCapability> steps = Arrays.asList(
                RuleCapability.TID_INIT_9A,
                RuleCapability.TID_INIT_9G,
                RuleCapability.TID_INIT_9B);
        PosDebugResponseDto response = baseResponse(target);
        for (RuleCapability capability : steps) {
            log.info("POS TID init step start channel={} capability={} target={}:{}",
                    channelId, capability.name(), target.ip, target.port);
            IsoMessageDto message = baseMessage(capability, tid, sn);
            PosDebugStepDto step = runStep(
                    target,
                    capability,
                    capability.getLabel(),
                    isoXmlMapper.render(message),
                    null,
                    "NONE",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    Arrays.asList(
                            "Environment: " + environmentSummary(environment),
                            variableLog(tidValue, false),
                            variableLog(snValue, false),
                            "TID 初始化强制 PIN 算法 NONE，跳过请求/响应 MAC。"),
                    true);
            response.getSteps().add(step);
            log.info("POS TID init step result channel={} capability={} success={} de39={} durationMs={} error={}",
                    channelId,
                    capability.name(),
                    step.isSuccess(),
                    step.getResponseCode(),
                    step.getDurationMs(),
                    step.getErrorMessage());
            if (!step.isSuccess()) {
                step.getLogs().add("当前步骤失败，TID 初始化停止，后续步骤不再执行。");
                break;
            }
        }
        response.setSuccess(response.getSteps().stream().allMatch(PosDebugStepDto::isSuccess));
        log.info("POS TID init completed channel={} success={} steps={}", channelId, response.isSuccess(), response.getSteps().size());
        String note = null;
        if (request != null && request.isSaveKey()) {
            note = "已执行 TID 初始化三步，但未自动保存 TMK/TPK/TSK：规范说明 DE53 携带加密 key 和 KCV，仍需确认 CTMK/TMK 解密、KCV 校验和落库策略后才能安全保存。";
        }
        response.setReport(report(response.getSteps(), note));
        return response;
    }

    private PosDebugStepDto runStep(
            DebugTarget target,
            RuleCapability capability,
            String label,
            String requestXml,
            String macAlgorithm,
            String pinAlgorithm,
            String pin,
            String pan,
            String de52,
            String tpkPlain,
            String tskPlain,
            String macField,
            List<String> inputLogs,
            boolean requireResponseCode00) {
        long started = System.currentTimeMillis();
        PosDebugStepDto step = new PosDebugStepDto();
        step.setCapability(capability.name());
        step.setLabel(label);
        step.setTargetIp(target.ip);
        step.setTargetPort(target.port);
        step.setPackager(target.channelDto.getPackager());
        step.setRequestXml(requestXml);
        logStep(step, "开始 " + label + "，目标 " + target.ip + ":" + target.port + "，协议 " + (target.tlsEnabled ? "TLS" : "TCP"));
        logStep(step, "Packager: " + target.channelDto.getPackager());
        for (String targetLog : target.logs) {
            logStep(step, targetLog);
        }
        if (inputLogs != null) {
            for (String inputLog : inputLogs) {
                logStep(step, inputLog);
            }
        }
        try {
            if (isBlank(requestXml)) {
                throw new IllegalArgumentException("请求 XML 不能为空");
            }
            logStep(step, "准备请求 XML。");
            ISOBasePackager packager = packagerFactory.create(target.channel.getIso8583().getPackager());
            logStep(step, "Packager 已创建。");
            KeySettingsDto keySettings = keySettings(target.channelId, macAlgorithm, tpkPlain, tskPlain, macField);
            logStep(step, "Key/MAC 策略已加载：MAC " + nullSafe(keySettings.getMacAlgorithm()) + "，MAC Field " + nullSafe(keySettings.getMacField()));
            IsoMessageDto requestMessage = isoXmlMapper.parse(requestXml);
            logStep(step, "XML 已解析：MTI " + nullSafe(requestMessage.getMti()) + "，字段数 " + (requestMessage.getFields() == null ? 0 : requestMessage.getFields().size()));
            if (!isBlank(de52)) {
                applyField(requestMessage, "52", de52.trim());
                logStep(step, "使用外部 DE52 覆盖请求 PIN Block。");
            }
            pinCryptoService.applyPin(requestMessage, keySettings, isBlank(pin) && !isBlank(de52) ? "NONE" : pinAlgorithm, pin, pan);
            logStep(step, "PIN 处理完成：算法 " + nullSafe(isBlank(pin) && !isBlank(de52) ? "NONE" : pinAlgorithm));
            boolean skipMac = skipMacForCapability(capability);
            step.setMacRequired(!skipMac && isoMacService.macRequired(keySettings, requestMessage));
            logStep(step, "请求 MAC 判定：" + (step.isMacRequired() ? "需要" : "不需要"));
            if (skipMac) {
                logStep(step, "TID 初始化能力跳过请求 MAC。");
            } else {
                step.setMacValue(isoMacService.sign(requestMessage, packager, keySettings));
                step.setMacField(isoMacService.macField(requestMessage, keySettings));
                logStep(step, "请求 MAC 处理完成：字段 " + nullSafe(step.getMacField()) + "，值 " + nullSafe(step.getMacValue()));
            }
            step.setSignedRequestXml(isoXmlMapper.render(requestMessage));

            byte[] isoBody = isoCodec.pack(requestMessage, packager);
            byte[] payload = combine(requestHeader(target.channel), isoBody);
            Framer framer = new Framer(target.channel.getFraming());
            byte[] frame = framer.writeFrame(payload);
            step.setRequestHex(HexUtils.toHex(frame));
            logStep(step, "组包完成：ISO Body " + isoBody.length + " bytes，payload " + payload.length + " bytes，frame " + frame.length + " bytes。");

            logStep(step, "开始连接并发送 frame。");
            byte[] responsePayload = sendFrame(target, framer, frame);
            logStep(step, "收到响应 payload：" + responsePayload.length + " bytes。");
            step.setResponseHex(HexUtils.toHex(framer.writeFrame(responsePayload)));
            PayloadParts parts = new HeaderHandler(target.channel.getHeader()).split(responsePayload);
            logStep(step, "响应 Header 拆分完成：ISO Body " + parts.getIsoBody().length + " bytes。");
            IsoMessageDto responseMessage = isoCodec.unpack(parts.getIsoBody(), packager);
            step.setResponseXml(isoXmlMapper.render(responseMessage));
            step.setResponseCode(fieldValue(responseMessage, "39"));
            logStep(step, "响应解包完成：MTI " + nullSafe(responseMessage.getMti()) + "，DE39 " + nullSafe(step.getResponseCode()));
            boolean responseMacRequired = !skipMac && isoMacService.macRequired(keySettings, responseMessage);
            logStep(step, "响应 MAC 判定：" + (responseMacRequired ? "需要校验" : "不需要校验"));
            step.setResponseMacValid(responseMacRequired
                    ? Boolean.valueOf(isoMacService.verifyRequestMac(responseMessage, packager, keySettings))
                    : null);
            if (responseMacRequired) {
                logStep(step, "响应 MAC 校验：" + (Boolean.TRUE.equals(step.getResponseMacValid()) ? "通过" : "失败"));
            }
            boolean success = !Boolean.FALSE.equals(step.getResponseMacValid());
            if (requireResponseCode00) {
                if (isBlank(step.getResponseCode())) {
                    success = false;
                    step.setErrorMessage("响应缺少 DE39，TID 初始化步骤失败");
                    logStep(step, step.getErrorMessage());
                } else if (!"00".equals(step.getResponseCode())) {
                    success = false;
                    step.setErrorMessage("响应 DE39=" + step.getResponseCode() + "，TID 初始化步骤失败");
                    logStep(step, step.getErrorMessage());
                }
            }
            step.setSuccess(success);
            logStep(step, "步骤结果：" + (step.isSuccess() ? "SUCCESS" : "FAILED"));
        } catch (Exception e) {
            step.setSuccess(false);
            step.setErrorMessage(e.getMessage());
            logStep(step, "异常：" + nullSafe(e.getMessage()));
            log.error("POS debug step failed channel={} capability={} label={} target={}:{}",
                    target.channelId,
                    capability.name(),
                    label,
                    target.ip,
                    target.port,
                    e);
        } finally {
            step.setDurationMs(System.currentTimeMillis() - started);
            logStep(step, "耗时 " + step.getDurationMs() + " ms。");
        }
        return step;
    }

    private void logStep(PosDebugStepDto step, String message) {
        step.getLogs().add(message);
        log.info("POS debug step [{}] {}", step.getLabel(), message);
    }

    private String nullSafe(String value) {
        return isBlank(value) ? "-" : value;
    }

    private byte[] sendFrame(DebugTarget target, Framer framer, byte[] frame) throws Exception {
        Socket socket = createSocket(target);
        try {
            socket.connect(new InetSocketAddress(target.ip, target.port), CONNECT_TIMEOUT_MS);
            if (socket instanceof SSLSocket) {
                ((SSLSocket) socket).startHandshake();
            }
            socket.setSoTimeout(Math.max(1000, target.channel.getTcp().getReadTimeoutMs()));
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(frame);
            outputStream.flush();
            InputStream inputStream = socket.getInputStream();
            return framer.readFrame(inputStream);
        } finally {
            socket.close();
        }
    }

    private Socket createSocket(DebugTarget target) throws Exception {
        if (target.tlsEnabled) {
            return DEBUG_TLS_SOCKET_FACTORY.createSocket();
        }
        return new Socket();
    }

    static SSLSocketFactory debugTlsSocketFactory() {
        return DEBUG_TLS_SOCKET_FACTORY;
    }

    private static SSLSocketFactory createDebugTlsSocketFactory() {
        try {
            TrustManager[] trustManagers = new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
            };
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, trustManagers, new SecureRandom());
            return context.getSocketFactory();
        } catch (Exception e) {
            throw new IllegalStateException("Debug TLS socket factory 初始化失败", e);
        }
    }

    private IsoMessageDto buildXmlMessage(RuleCapability capability, PosDebugBuildXmlRequestDto request) {
        PosDebugBuildXmlRequestDto safeRequest = request == null ? new PosDebugBuildXmlRequestDto() : request;
        LocalDateTime now = LocalDateTime.now();
        IsoMessageDto message = new IsoMessageDto();
        message.setMti(capability.getRequestMti());
        message.setFields(new LinkedHashMap<>());

        Map<String, String> autoValues = new LinkedHashMap<>();
        mergeAllowed(autoValues, safeRequest.getEnvironmentFields(), autoFieldScope(capability));
        mergeAllowed(autoValues, safeRequest.getDynamicFields(), autoFieldScope(capability));
        mergeGeneratedValues(autoValues, capability, safeRequest, now);

        for (String field : requiredFields(capability)) {
            message.getFields().put(field, autoValues.containsKey(field) ? autoValues.get(field) : "");
        }

        for (Map.Entry<String, String> entry : autoValues.entrySet()) {
            if (!message.getFields().containsKey(entry.getKey()) && !isBlank(entry.getValue())) {
                message.getFields().put(entry.getKey(), entry.getValue());
            }
        }

        Map<String, String> userFields = safeRequest.getFields();
        if (userFields != null) {
            for (Map.Entry<String, String> entry : userFields.entrySet()) {
                applyField(message, entry.getKey(), entry.getValue() == null ? "" : entry.getValue());
            }
        }
        return message;
    }

    private void mergeAllowed(Map<String, String> target, Map<String, String> source, List<String> allowedFields) {
        if (source == null) {
            return;
        }
        for (Map.Entry<String, String> entry : source.entrySet()) {
            String canonical = IsoFieldReferences.canonical(entry.getKey());
            if (isBlank(canonical)) {
                continue;
            }
            if (containsField(allowedFields, canonical)) {
                target.put(canonical, entry.getValue() == null ? "" : entry.getValue());
            }
        }
    }

    private void mergeGeneratedValues(
            Map<String, String> target,
            RuleCapability capability,
            PosDebugBuildXmlRequestDto request,
            LocalDateTime now) {
        String tid = firstNonBlank(request.getTid(), fieldValue(request.getEnvironmentFields(), "41", "DE41", "testTid", "tid"));
        String sn = firstNonBlank(request.getSn(), fieldValue(request.getEnvironmentFields(), "SN", "testSn", "sn"));
        target.put("3", capability.getProcessCode());
        target.put("7", DE7.format(now));
        target.put("11", String.format("%06d", random.nextInt(1000000)));
        target.put("12", DE12.format(now));
        target.put("13", DE13.format(now));
        if (containsField(autoFieldScope(capability), "37")) {
            target.put("37", DE37.format(now));
        }
        if (!isBlank(tid)) {
            target.put("41", tid.trim());
        }
        if (requiresTidInitSn(capability)) {
            target.put("62", isBlank(sn) ? "" : tag("01", sn.trim()));
        } else if (isNetworkManagement(capability) && !isBlank(sn)) {
            target.put("62", tag("01", sn.trim()));
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private String fieldValue(Map<String, String> fields, String... keys) {
        if (fields == null) {
            return null;
        }
        for (String key : keys) {
            String canonical = IsoFieldReferences.canonical(key);
            for (Map.Entry<String, String> entry : fields.entrySet()) {
                if (canonical.equals(IsoFieldReferences.canonical(entry.getKey()))) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private List<String> requiredFields(RuleCapability capability) {
        switch (capability) {
            case BALANCE_QUERY:
            case PRE_AUTH:
                return AUTHORIZATION_REQUIRED_FIELDS;
            case DEBIT:
            case REFUND:
                return FINANCIAL_REQUIRED_FIELDS;
            case PRE_AUTH_COMPLETION:
                return FINANCIAL_ADVICE_REQUIRED_FIELDS;
            case REVERSAL:
                return REVERSAL_REQUIRED_FIELDS;
            case TID_INIT_9A:
            case TID_INIT_9G:
            case TID_INIT_9B:
                return appendIfMissing(NETWORK_MANAGEMENT_REQUIRED_FIELDS, "62");
            case PARAMETER_DOWNLOAD:
            case CALLHOME:
            default:
                return NETWORK_MANAGEMENT_REQUIRED_FIELDS;
        }
    }

    private List<String> autoFieldScope(RuleCapability capability) {
        if (isNetworkManagement(capability)) {
            return NETWORK_AUTO_FIELDS;
        }
        List<String> fields = requiredFields(capability);
        for (String optional : STANDARD_AUTO_FIELDS) {
            fields = appendIfMissing(fields, optional);
        }
        return fields;
    }

    private List<String> appendIfMissing(List<String> source, String field) {
        if (containsField(source, field)) {
            return source;
        }
        String[] copy = Arrays.copyOf(source.toArray(new String[0]), source.size() + 1);
        copy[source.size()] = field;
        return Arrays.asList(copy);
    }

    private boolean containsField(List<String> fields, String field) {
        String canonical = IsoFieldReferences.canonical(field);
        if (isBlank(canonical)) {
            return false;
        }
        for (String candidate : fields) {
            if (canonical.equals(IsoFieldReferences.canonical(candidate))) {
                return true;
            }
        }
        return false;
    }

    private boolean requiresTidInitSn(RuleCapability capability) {
        return capability == RuleCapability.TID_INIT_9A
                || capability == RuleCapability.TID_INIT_9G
                || capability == RuleCapability.TID_INIT_9B;
    }

    static boolean skipMacForCapability(RuleCapability capability) {
        return capability == RuleCapability.TID_INIT_9A
                || capability == RuleCapability.TID_INIT_9G
                || capability == RuleCapability.TID_INIT_9B;
    }

    private IsoMessageDto baseMessage(RuleCapability capability, String tid, String sn) {
        IsoMessageDto message = new IsoMessageDto();
        message.setMti(capability.getRequestMti());
        message.setFields(new LinkedHashMap<>());
        LocalDateTime now = LocalDateTime.now();
        message.getFields().put("3", capability.getProcessCode());
        message.getFields().put("7", DE7.format(now));
        message.getFields().put("11", String.format("%06d", random.nextInt(1000000)));
        message.getFields().put("12", DE12.format(now));
        message.getFields().put("13", DE13.format(now));
        if (!isBlank(tid)) {
            message.getFields().put("41", tid.trim());
        }
        if (isNetworkManagement(capability) && !isBlank(sn)) {
            message.getFields().put("62", tag("01", sn.trim()));
        }
        return message;
    }

    private boolean isNetworkManagement(RuleCapability capability) {
        return capability == RuleCapability.TID_INIT_9A
                || capability == RuleCapability.TID_INIT_9G
                || capability == RuleCapability.TID_INIT_9B
                || capability == RuleCapability.PARAMETER_DOWNLOAD
                || capability == RuleCapability.CALLHOME;
    }

    private String tag(String tag, String value) {
        return tag + String.format("%03d", value.length()) + value;
    }

    private void applyField(IsoMessageDto message, String field, String value) {
        if (field == null || field.trim().isEmpty() || value == null) {
            return;
        }
        String canonical = IsoFieldReferences.canonical(field);
        if (IsoFieldReferences.isMti(canonical)) {
            message.setMti(value);
            return;
        }
        message.getFields().put(canonical, value);
    }

    private String saveTidConfigReport(String channelId, String requestXml, String responseXml) {
        try {
            IsoMessageDto requestMessage = isoXmlMapper.parse(requestXml);
            IsoMessageDto responseMessage = isoXmlMapper.parse(responseXml);
            Map<String, String> tags = parseTags(fieldValue(responseMessage, "62"));
            KeySettingsDto settings = keySettingsService.get(channelId);
            String tid = fieldValue(requestMessage, "41");
            if (!isBlank(tid)) {
                settings.setTestTid(tid);
            }
            if (tags.containsKey("03")) {
                settings.setTestDe42(tags.get("03"));
            }
            if (tags.containsKey("08")) {
                settings.setTestDe18(tags.get("08"));
            }
            if (tags.containsKey("52")) {
                settings.setTestDe43(tags.get("52"));
            }
            if (tags.containsKey("05")) {
                settings.setTestDe49(tags.get("05"));
            }
            keySettingsService.save(channelId, settings);
            return "参数下载响应已解析 DE62，并尝试保存 TID、DE42、DE18、DE43、DE49 到 Key 设置；未出现在响应中的字段保持原值。";
        } catch (Exception e) {
            return "参数下载已完成，但保存 TID 配置失败: " + e.getMessage();
        }
    }

    private Map<String, String> parseTags(String value) {
        Map<String, String> tags = new LinkedHashMap<>();
        if (isBlank(value)) {
            return tags;
        }
        int offset = 0;
        while (offset + 5 <= value.length()) {
            String tag = value.substring(offset, offset + 2);
            String lengthText = value.substring(offset + 2, offset + 5);
            if (!lengthText.matches("[0-9]{3}")) {
                break;
            }
            int length = Integer.parseInt(lengthText);
            int valueStart = offset + 5;
            int valueEnd = valueStart + length;
            if (valueEnd > value.length()) {
                break;
            }
            tags.put(tag, value.substring(valueStart, valueEnd));
            offset = valueEnd;
        }
        return tags;
    }

    private String report(List<PosDebugStepDto> steps, String note) {
        StringBuilder builder = new StringBuilder();
        builder.append("POS Debug 报告\n");
        for (PosDebugStepDto step : steps) {
            builder.append("- ")
                    .append(step.getLabel())
                    .append(": ")
                    .append(step.isSuccess() ? "成功" : "失败")
                    .append(", 耗时 ")
                    .append(step.getDurationMs())
                    .append("ms");
            if (!isBlank(step.getResponseCode())) {
                builder.append(", DE39=").append(step.getResponseCode());
            }
            if (step.getResponseMacValid() != null) {
                builder.append(", 响应MAC=").append(step.getResponseMacValid() ? "通过" : "失败");
            }
            if (!isBlank(step.getErrorMessage())) {
                builder.append(", 错误=").append(step.getErrorMessage());
            }
            builder.append('\n');
        }
        if (!isBlank(note)) {
            builder.append(note).append('\n');
        }
        return builder.toString().trim();
    }

    private PosDebugResponseDto baseResponse(DebugTarget target) {
        PosDebugResponseDto response = new PosDebugResponseDto();
        response.setChannelId(target.channelId);
        response.setChannelCode(target.channelDto.getChannelCode());
        response.setTargetIp(target.ip);
        response.setTargetPort(target.port);
        response.setPackager(target.channelDto.getPackager());
        return response;
    }

    private DebugEnvironmentDto environment(Long userId, String environmentId) {
        if (userId == null || isBlank(environmentId) || debugEnvironmentRepository == null) {
            return null;
        }
        return debugEnvironmentRepository.findOne(userId, environmentId);
    }

    private PosDebugBuildXmlRequestDto mergeBuildXmlRequest(DebugEnvironmentDto environment, PosDebugBuildXmlRequestDto request) {
        PosDebugBuildXmlRequestDto source = request == null ? new PosDebugBuildXmlRequestDto() : request;
        if (environment == null) {
            return source;
        }
        PosDebugBuildXmlRequestDto merged = new PosDebugBuildXmlRequestDto();
        merged.setEnvironmentId(source.getEnvironmentId());
        merged.setCapability(source.getCapability());
        merged.setTid(firstNonBlank(environment.getTestTid(), source.getTid()));
        merged.setSn(firstNonBlank(environment.getTestSn(), source.getSn()));
        Map<String, String> environmentFields = new LinkedHashMap<>();
        putIfPresent(environmentFields, "DE41", environment.getTestTid());
        putIfPresent(environmentFields, "SN", environment.getTestSn());
        putIfPresent(environmentFields, "DE2", environment.getTestPan());
        putIfPresent(environmentFields, "DE14", environment.getTestDe14());
        putIfPresent(environmentFields, "DE52", environment.getTestDe52());
        mergeMissing(environmentFields, source.getEnvironmentFields());
        Map<String, String> dynamicFields = new LinkedHashMap<>();
        putIfPresent(dynamicFields, "DE18", environment.getTestDe18());
        putIfPresent(dynamicFields, "DE42", environment.getTestDe42());
        putIfPresent(dynamicFields, "DE43", environment.getTestDe43());
        putIfPresent(dynamicFields, "DE49", environment.getTestDe49());
        mergeMissing(dynamicFields, source.getDynamicFields());
        merged.setEnvironmentFields(environmentFields);
        merged.setDynamicFields(dynamicFields);
        merged.setFields(source.getFields());
        return merged;
    }

    private void putIfPresent(Map<String, String> target, String key, String value) {
        if (!isBlank(value)) {
            target.put(key, value.trim());
        }
    }

    private void mergeMissing(Map<String, String> target, Map<String, String> source) {
        if (source == null) {
            return;
        }
        for (Map.Entry<String, String> entry : source.entrySet()) {
            String key = entry.getKey();
            if (isBlank(key) || target.containsKey(key)) {
                continue;
            }
            target.put(key, entry.getValue());
        }
    }

    private DebugTarget target(String channelId, DebugEnvironmentDto environment, String overrideIp, Integer overridePort) {
        ChannelProperties channel = channelService.getRequired(channelId);
        ChannelDto channelDto = channelService.list().stream()
                .filter(candidate -> channelId.equals(candidate.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown channel: " + channelId));
        ResolvedString targetIpValue = resolve("target.ip", environment == null ? null : environment.getTargetIp(), "Environment.targetIp", overrideIp, "request.targetIp", channelDto.getThirdPartyTestIp(), "channel.thirdPartyTestIp");
        ResolvedInteger targetPortValue = resolve("target.port", environment == null ? null : environment.getTargetPort(), "Environment.targetPort", overridePort, "request.targetPort", channelDto.getThirdPartyTestPort(), "channel.thirdPartyTestPort");
        String targetIp = targetIpValue.value;
        Integer targetPort = targetPortValue.value;
        if (isBlank(targetIp) || targetPort == null) {
            throw new IllegalStateException("当前环境未配置第三方测试环境 IP 和端口，无法执行 Debug 请求");
        }
        if (targetPort < 1 || targetPort > 65535) {
            throw new IllegalArgumentException("当前环境目标端口必须在 1 到 65535 之间");
        }
        List<String> logs = new ArrayList<>();
        logs.add("Environment: " + environmentSummary(environment));
        logs.add(variableLog(targetIpValue, false));
        logs.add(variableLog(targetPortValue));
        logs.add("变量 tls = " + channelDto.isThirdPartyTlsEnabled() + "，来源 channel.thirdPartyTlsEnabled");
        logs.add("协议隧道 Packager/Framing/Header 来源渠道配置，不从 Environment 覆盖。");
        return new DebugTarget(channelId, channel, channelDto, targetIp, targetPort, channelDto.isThirdPartyTlsEnabled(), logs);
    }

    private EffectiveDebugInputs effectiveInputs(String channelId, DebugEnvironmentDto environment, PosDebugSendRequestDto request) {
        KeySettingsDto source = keySettingsService.get(channelId);
        EffectiveDebugInputs inputs = new EffectiveDebugInputs();
        inputs.environment = environment;
        inputs.macAlgorithm = resolve("mac.algorithm", environment == null ? null : environment.getMacAlgorithm(), "Environment.macAlgorithm", request == null ? null : request.getMacAlgorithm(), "request.macAlgorithm", source.getMacAlgorithm(), "keySettings.macAlgorithm");
        inputs.pinAlgorithm = resolve("pin.algorithm", environment == null ? null : environment.getPinAlgorithm(), "Environment.pinAlgorithm", request == null ? null : request.getPinAlgorithm(), "request.pinAlgorithm", "NONE", "default");
        inputs.pan = resolve("DE2/PAN", environment == null ? null : environment.getTestPan(), "Environment.testPan", request == null ? null : request.getPan(), "request.pan", null, "none");
        inputs.de52 = resolve("DE52", environment == null ? null : environment.getTestDe52(), "Environment.testDe52", request == null ? null : request.getDe52(), "request.de52", null, "none");
        inputs.tpkPlain = resolve("TPK", environment == null ? null : environment.getTpkPlain(), "Environment.tpkPlain", request == null ? null : request.getTpkPlain(), "request.tpkPlain", source.getTpkPlain(), "keySettings.tpkPlain");
        inputs.tskPlain = resolve("TSK", environment == null ? null : environment.getTskPlain(), "Environment.tskPlain", request == null ? null : request.getTskPlain(), "request.tskPlain", source.getTskPlain(), "keySettings.tskPlain");
        inputs.macField = resolve("mac.field", environment == null ? null : environment.getMacField(), "Environment.macField", request == null ? null : request.getMacField(), "request.macField", source.getMacField(), "keySettings.macField");
        inputs.logs.add("Environment: " + environmentSummary(environment));
        inputs.logs.add(variableLog(inputs.macAlgorithm, false));
        inputs.logs.add(variableLog(inputs.pinAlgorithm, false));
        inputs.logs.add(variableLog(inputs.macField, false));
        inputs.logs.add(variableLog(inputs.pan, true));
        inputs.logs.add(variableLog(inputs.de52, true));
        inputs.logs.add(variableLog(inputs.tpkPlain, true));
        inputs.logs.add(variableLog(inputs.tskPlain, true));
        return inputs;
    }

    private KeySettingsDto keySettings(String channelId, String macAlgorithm, String tpkPlain, String tskPlain, String macField) {
        KeySettingsDto source = keySettingsService.get(channelId);
        KeySettingsDto copy = new KeySettingsDto();
        copy.setChannelId(source.getChannelId());
        copy.setTpkPlain(isBlank(tpkPlain) ? source.getTpkPlain() : tpkPlain.trim());
        copy.setTskPlain(isBlank(tskPlain) ? source.getTskPlain() : tskPlain.trim());
        copy.setMacField(isBlank(macField) ? source.getMacField() : macField.trim());
        copy.setMacAlgorithm(isBlank(macAlgorithm) ? source.getMacAlgorithm() : macAlgorithm.trim().toUpperCase());
        copy.setTestTid(source.getTestTid());
        copy.setTestPan(source.getTestPan());
        copy.setTestPin(source.getTestPin());
        copy.setTestDe14(source.getTestDe14());
        copy.setTestDe42(source.getTestDe42());
        copy.setTestDe18(source.getTestDe18());
        copy.setTestDe43(source.getTestDe43());
        copy.setTestDe49(source.getTestDe49());
        return copy;
    }

    private byte[] requestHeader(ChannelProperties channel) {
        if (!channel.getHeader().isEnabled() || channel.getHeader().getLength() <= 0) {
            return new byte[0];
        }
        byte[] configured = HexUtils.fromHex(channel.getHeader().getFixedValueHex());
        if (configured.length == 0) {
            return new byte[channel.getHeader().getLength()];
        }
        if (configured.length != channel.getHeader().getLength()) {
            throw new IllegalStateException("Header 固定 HEX 长度与渠道 Header 长度不一致");
        }
        return configured;
    }

    private byte[] combine(byte[] header, byte[] isoBody) {
        byte[] payload = Arrays.copyOf(header, header.length + isoBody.length);
        System.arraycopy(isoBody, 0, payload, header.length, isoBody.length);
        return payload;
    }

    private RuleCapability capability(String value) {
        if (isBlank(value)) {
            return RuleCapability.DEBIT;
        }
        String normalized = value.trim();
        if ("TID_INIT".equals(normalized)) {
            return RuleCapability.TID_INIT_9A;
        }
        if ("TID_INIT_9C".equals(normalized)) {
            return RuleCapability.TID_INIT_9B;
        }
        return RuleCapability.valueOf(normalized);
    }

    private String fieldValue(IsoMessageDto message, String field) {
        if (message == null || message.getFields() == null) {
            return null;
        }
        String canonical = IsoFieldReferences.canonical(field);
        for (Map.Entry<String, String> entry : message.getFields().entrySet()) {
            if (canonical.equals(IsoFieldReferences.canonical(entry.getKey()))) {
                return entry.getValue();
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private ResolvedString resolve(
            String name,
            String environmentValue,
            String environmentSource,
            String requestValue,
            String requestSource,
            String fallbackValue,
            String fallbackSource) {
        if (!isBlank(environmentValue)) {
            return new ResolvedString(name, environmentValue.trim(), environmentSource);
        }
        if (!isBlank(requestValue)) {
            return new ResolvedString(name, requestValue.trim(), requestSource);
        }
        if (!isBlank(fallbackValue)) {
            return new ResolvedString(name, fallbackValue.trim(), fallbackSource);
        }
        return new ResolvedString(name, null, "none");
    }

    private ResolvedInteger resolve(
            String name,
            Integer environmentValue,
            String environmentSource,
            Integer requestValue,
            String requestSource,
            Integer fallbackValue,
            String fallbackSource) {
        if (environmentValue != null) {
            return new ResolvedInteger(name, environmentValue, environmentSource);
        }
        if (requestValue != null) {
            return new ResolvedInteger(name, requestValue, requestSource);
        }
        if (fallbackValue != null) {
            return new ResolvedInteger(name, fallbackValue, fallbackSource);
        }
        return new ResolvedInteger(name, null, "none");
    }

    private String variableLog(ResolvedString value, boolean sensitive) {
        return "变量 " + value.name + " = " + formatValue(value.value, sensitive) + "，来源 " + value.source;
    }

    private String variableLog(ResolvedInteger value) {
        return "变量 " + value.name + " = " + (value.value == null ? "-" : value.value) + "，来源 " + value.source;
    }

    private String formatValue(String value, boolean sensitive) {
        if (isBlank(value)) {
            return "-";
        }
        if (!sensitive) {
            return value;
        }
        return "***(" + value.trim().length() + " chars)";
    }

    private String environmentSummary(DebugEnvironmentDto environment) {
        if (environment == null) {
            return "未指定或未加载，允许使用 request/channel/keySettings 回落值";
        }
        return environment.getId() + " / " + environment.getName();
    }

    private static final class EffectiveDebugInputs {
        private DebugEnvironmentDto environment;
        private ResolvedString macAlgorithm;
        private ResolvedString pinAlgorithm;
        private ResolvedString pan;
        private ResolvedString de52;
        private ResolvedString tpkPlain;
        private ResolvedString tskPlain;
        private ResolvedString macField;
        private final List<String> logs = new ArrayList<>();
    }

    private static final class ResolvedString {
        private final String name;
        private final String value;
        private final String source;

        private ResolvedString(String name, String value, String source) {
            this.name = name;
            this.value = value;
            this.source = source;
        }
    }

    private static final class ResolvedInteger {
        private final String name;
        private final Integer value;
        private final String source;

        private ResolvedInteger(String name, Integer value, String source) {
            this.name = name;
            this.value = value;
            this.source = source;
        }
    }

    private static final class DebugTarget {
        private final String channelId;
        private final ChannelProperties channel;
        private final ChannelDto channelDto;
        private final String ip;
        private final int port;
        private final boolean tlsEnabled;
        private final List<String> logs;

        private DebugTarget(String channelId, ChannelProperties channel, ChannelDto channelDto, String ip, int port, boolean tlsEnabled, List<String> logs) {
            this.channelId = channelId;
            this.channel = channel;
            this.channelDto = channelDto;
            this.ip = ip;
            this.port = port;
            this.tlsEnabled = tlsEnabled;
            this.logs = logs;
        }
    }
}
