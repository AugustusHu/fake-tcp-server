package com.example.faketcp.service;

import com.example.faketcp.dto.IsoMessageDto;
import com.example.faketcp.dto.RuleDto;
import com.example.faketcp.dto.TestHistoryDto;
import com.example.faketcp.dto.TestHistoryRunRequestDto;
import com.example.faketcp.dto.TestMatchResponse;
import com.example.faketcp.dto.UserDto;
import com.example.faketcp.iso.IsoXmlMapper;
import com.example.faketcp.repository.TestHistoryRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TestHistoryService {
    private final TestHistoryRepository repository;
    private final RuleService ruleService;
    private final ChannelService channelService;
    private final IsoXmlMapper isoXmlMapper;

    public TestHistoryService(
            TestHistoryRepository repository,
            RuleService ruleService,
            ChannelService channelService,
            IsoXmlMapper isoXmlMapper) {
        this.repository = repository;
        this.ruleService = ruleService;
        this.channelService = channelService;
        this.isoXmlMapper = isoXmlMapper;
    }

    public List<TestHistoryDto> list(long userId, String channelId) {
        channelService.getRequired(channelId);
        return repository.findRecent(userId, channelId);
    }

    public List<TestHistoryDto> list(UserDto user, String channelId) {
        return list(currentUserId(user), channelId);
    }

    public TestHistoryDto run(long userId, String channelId, TestHistoryRunRequestDto request) {
        channelService.getRequired(channelId);
        UserDto user = new UserDto();
        user.setId(userId);
        return run(user, channelId, request);
    }

    public TestHistoryDto run(UserDto user, String channelId, TestHistoryRunRequestDto request) {
        channelService.getRequired(channelId);
        TestHistoryDto history = new TestHistoryDto();
        history.setUserId(currentUserId(user));
        history.setChannelId(channelId);
        history.setRequestXml(request == null || request.getRequestXml() == null ? "" : request.getRequestXml());
        try {
            IsoMessageDto message = isoXmlMapper.parse(history.getRequestXml());
            TestMatchResponse response = ruleService.test(channelId, message, user);
            history.setSuccess(true);
            history.setMatched(response.isMatched());
            RuleDto rule = response.getRule();
            if (rule != null) {
                history.setRuleId(rule.getId());
                history.setRuleName(rule.getName());
            }
            history.setResponseXml(isoXmlMapper.render(response.getResponse()));
        } catch (Exception e) {
            history.setSuccess(false);
            history.setMatched(false);
            history.setErrorMessage(e.getMessage());
        }
        return repository.create(history);
    }

    public void delete(long userId, String channelId, long historyId) {
        channelService.getRequired(channelId);
        repository.delete(userId, channelId, historyId);
    }

    public void clear(long userId, String channelId) {
        channelService.getRequired(channelId);
        repository.clear(userId, channelId);
    }

    private long currentUserId(UserDto user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("Current user is required");
        }
        return user.getId();
    }
}
