package com.example.faketcp.service;

import com.example.faketcp.dto.TestCaseDto;
import com.example.faketcp.dto.TestGroupDto;
import com.example.faketcp.dto.TestRunResultDto;
import com.example.faketcp.dto.UserDto;
import com.example.faketcp.repository.TestGroupRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TestGroupService {
    private final TestGroupRepository repository;
    private final RuleService ruleService;
    private final ChannelService channelService;

    public TestGroupService(TestGroupRepository repository, RuleService ruleService, ChannelService channelService) {
        this.repository = repository;
        this.ruleService = ruleService;
        this.channelService = channelService;
    }

    public List<TestGroupDto> list(long userId, String channelId) {
        channelService.getRequired(channelId);
        return repository.findGroups(userId, channelId);
    }

    public TestGroupDto createGroup(long userId, String channelId, TestGroupDto group) {
        channelService.getRequired(channelId);
        return repository.createGroup(userId, channelId, group);
    }

    public TestCaseDto createCase(long userId, String channelId, long groupId, TestCaseDto testCase) {
        channelService.getRequired(channelId);
        repository.findGroup(groupId, userId, channelId);
        return repository.createCase(groupId, testCase);
    }

    public void deleteGroup(long userId, String channelId, long groupId) {
        channelService.getRequired(channelId);
        repository.deleteGroup(groupId, userId, channelId);
    }

    public void deleteCase(long userId, String channelId, long groupId, long caseId) {
        channelService.getRequired(channelId);
        repository.deleteCase(caseId, groupId, userId, channelId);
    }

    public List<TestRunResultDto> runGroup(long userId, String channelId, long groupId, UserDto user) {
        channelService.getRequired(channelId);
        List<TestRunResultDto> results = new ArrayList<>();
        for (TestCaseDto testCase : repository.findCases(groupId, userId, channelId)) {
            if (!testCase.isEnabled()) {
                continue;
            }
            TestRunResultDto result = new TestRunResultDto();
            result.setCaseId(testCase.getId());
            result.setName(testCase.getName());
            try {
                result.setResponseXml(ruleService.testXml(channelId, testCase.getRequestXml(), user));
                result.setSuccess(true);
            } catch (Exception e) {
                result.setSuccess(false);
                result.setError(e.getMessage());
            }
            results.add(result);
        }
        return results;
    }
}
