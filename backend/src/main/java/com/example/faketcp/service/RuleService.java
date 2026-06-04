package com.example.faketcp.service;

import com.example.faketcp.dto.ConditionDto;
import com.example.faketcp.dto.IsoMessageDto;
import com.example.faketcp.dto.KeySettingsDto;
import com.example.faketcp.dto.ResponseDto;
import com.example.faketcp.dto.ResponseFieldDto;
import com.example.faketcp.dto.RuleCapability;
import com.example.faketcp.dto.RuleDto;
import com.example.faketcp.dto.TestMatchResponse;
import com.example.faketcp.dto.UserDto;
import com.example.faketcp.iso.IsoFieldReferences;
import com.example.faketcp.iso.IsoMacService;
import com.example.faketcp.iso.IsoXmlMapper;
import com.example.faketcp.iso.PackagerFactory;
import com.example.faketcp.repository.RuleRepository;
import com.example.faketcp.rule.RuleEngine;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RuleService {
    private final RuleRepository ruleRepository;
    private final RuleEngine ruleEngine;
    private final ChannelService channelService;
    private final IsoXmlMapper isoXmlMapper;
    private final KeySettingsService keySettingsService;
    private final IsoMacService isoMacService;
    private final PackagerFactory packagerFactory;

    public RuleService(
            RuleRepository ruleRepository,
            RuleEngine ruleEngine,
            ChannelService channelService,
            IsoXmlMapper isoXmlMapper,
            KeySettingsService keySettingsService,
            IsoMacService isoMacService,
            PackagerFactory packagerFactory) {
        this.ruleRepository = ruleRepository;
        this.ruleEngine = ruleEngine;
        this.channelService = channelService;
        this.isoXmlMapper = isoXmlMapper;
        this.keySettingsService = keySettingsService;
        this.isoMacService = isoMacService;
        this.packagerFactory = packagerFactory;
    }

    public List<RuleDto> list(String channelId, RuleCapability capability, UserDto user) {
        channelService.getRequired(channelId);
        long ownerUserId = currentUserId(user);
        if (capability == null) {
            return ruleRepository.findByChannel(channelId, ownerUserId);
        }
        return ruleRepository.findByChannel(channelId, capability, ownerUserId);
    }

    public RuleDto create(String channelId, RuleDto rule, UserDto user) {
        channelService.getRequired(channelId);
        rule.setOwnerUserId(currentUserId(user));
        rule.setOwnerUsername(user.getUsername());
        normalizeRule(rule);
        return ruleRepository.create(channelId, rule);
    }

    public RuleDto update(String channelId, long id, RuleDto rule, UserDto user) {
        channelService.getRequired(channelId);
        rule.setOwnerUserId(currentUserId(user));
        rule.setOwnerUsername(user.getUsername());
        normalizeRule(rule);
        return ruleRepository.update(channelId, id, currentUserId(user), rule);
    }

    public void setEnabled(String channelId, long id, boolean enabled, UserDto user) {
        channelService.getRequired(channelId);
        ruleRepository.setEnabled(channelId, id, currentUserId(user), enabled);
    }

    public void setPublicRule(String channelId, long id, boolean publicRule, UserDto user) {
        channelService.getRequired(channelId);
        ruleRepository.setPublicRule(channelId, id, currentUserId(user), publicRule);
    }

    public List<RuleDto> publicRules() {
        return ruleRepository.findPublicRules();
    }

    public RuleDto copyPublicRule(String channelId, long id, UserDto user) {
        channelService.getRequired(channelId);
        RuleDto source = ruleRepository.findPublicRules().stream()
                .filter(rule -> rule.getId() != null && rule.getId() == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Public rule not found: " + id));
        source.setId(null);
        source.setChannelId(channelId);
        source.setOwnerUserId(currentUserId(user));
        source.setOwnerUsername(user.getUsername());
        source.setPublicRule(false);
        source.setName(source.getName() + " copy");
        normalizeRule(source);
        return ruleRepository.create(channelId, source);
    }

    public void delete(String channelId, long id, UserDto user) {
        channelService.getRequired(channelId);
        ruleRepository.delete(channelId, id, currentUserId(user));
    }

    public TestMatchResponse test(String channelId, IsoMessageDto request, UserDto user) {
        com.example.faketcp.config.ChannelProperties channel = channelService.getRequired(channelId);
        return test(channelId, request, channel, ruleRepository.findEnabledByChannel(channelId, currentUserId(user)));
    }

    private TestMatchResponse test(
            String channelId,
            IsoMessageDto request,
            com.example.faketcp.config.ChannelProperties channel,
            List<RuleDto> candidateRules) {
        TestMatchResponse response = new TestMatchResponse();
        java.util.Optional<com.example.faketcp.rule.RuleMatchResult> result =
                ruleEngine.match(candidateRules, request, channelService.fieldValueTypes(channelId));
        if (result.isPresent()) {
            response.setMatched(true);
            response.setRule(result.get().getRule());
            response.setAction(result.get().getRule().getAction());
            response.setResponse(result.get().getResponse());
        } else {
            response.setMatched(false);
            response.setResponse(ruleEngine.defaultResponse(
                    request,
                    channelService.getRequired(channelId).getNoMatch().getResponseCode()));
        }
        try {
            KeySettingsDto keySettings = keySettingsService.get(channelId);
            isoMacService.sign(response.getResponse(), packagerFactory.create(channel.getIso8583().getPackager()), keySettings);
        } catch (Exception e) {
            throw new IllegalStateException("生成响应 MAC 失败: " + e.getMessage(), e);
        }
        return response;
    }

    public String testXml(String channelId, String requestXml, UserDto user) {
        return isoXmlMapper.render(test(channelId, isoXmlMapper.parse(requestXml), user).getResponse());
    }

    private void normalizeRule(RuleDto rule) {
        if (rule == null) {
            return;
        }
        if (rule.getCapability() == null) {
            rule.setCapability(RuleCapability.DEBIT);
        }
        normalizeSystemConditions(rule);
        if (rule.getResponse() == null) {
            rule.setResponse(new ResponseDto());
        }
        if (isBlank(rule.getResponse().getMti())) {
            rule.getResponse().setMti(rule.getCapability().getResponseMti());
        }
        if (rule.getConditions() != null) {
            for (ConditionDto condition : rule.getConditions()) {
                if (condition != null) {
                    condition.setField(IsoFieldReferences.canonical(condition.getField()));
                }
            }
        }
        ResponseDto response = rule.getResponse();
        if (response.getFields() == null) {
            return;
        }
        Map<String, ResponseFieldDto> normalizedFields = new LinkedHashMap<>();
        for (Map.Entry<String, ResponseFieldDto> entry : response.getFields().entrySet()) {
            String field = IsoFieldReferences.canonical(entry.getKey());
            if (field == null || field.trim().isEmpty()) {
                continue;
            }
            ResponseFieldDto responseField = entry.getValue();
            if (responseField != null) {
                responseField.setSourceField(IsoFieldReferences.canonical(responseField.getSourceField()));
            }
            normalizedFields.put(field, responseField);
        }
        response.setFields(normalizedFields);
    }

    private void normalizeSystemConditions(RuleDto rule) {
        RuleCapability capability = rule.getCapability();
        List<ConditionDto> existing = rule.getSystemConditions() == null
                ? new ArrayList<>()
                : rule.getSystemConditions();
        List<ConditionDto> normalized = new ArrayList<>();
        normalized.add(systemCondition("0", systemConditionValue(existing, "0", capability.getRequestMti())));
        normalized.add(systemCondition("3", systemConditionValue(existing, "3", capability.getProcessCode())));
        rule.setSystemConditions(normalized);
    }

    private ConditionDto systemCondition(String field, String value) {
        ConditionDto condition = new ConditionDto();
        condition.setField(field);
        condition.setOperator(ConditionDto.Operator.EQ);
        condition.setValue(value);
        return condition;
    }

    private String systemConditionValue(List<ConditionDto> conditions, String field, String defaultValue) {
        String canonicalField = IsoFieldReferences.canonical(field);
        for (ConditionDto condition : conditions) {
            if (condition == null) {
                continue;
            }
            if (canonicalField.equals(IsoFieldReferences.canonical(condition.getField())) && !isBlank(condition.getValue())) {
                return condition.getValue().trim();
            }
        }
        return defaultValue;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private long currentUserId(UserDto user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("Current user is required");
        }
        return user.getId();
    }
}
