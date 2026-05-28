package com.codejudgex.problem.service;

import com.codejudgex.common.dto.PageResponse;
import com.codejudgex.common.exception.ResourceNotFoundException;
import com.codejudgex.problem.dto.AddTestCaseRequest;
import com.codejudgex.problem.dto.CreateProblemRequest;
import com.codejudgex.problem.dto.ProblemResponse;
import com.codejudgex.problem.dto.ProblemSummaryResponse;
import com.codejudgex.problem.dto.TestCaseResponse;
import com.codejudgex.problem.entity.Problem;
import com.codejudgex.problem.entity.ProblemTag;
import com.codejudgex.problem.entity.TestCase;
import com.codejudgex.problem.repository.ProblemRepository;
import com.codejudgex.problem.repository.ProblemTagRepository;
import com.codejudgex.problem.repository.TestCaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProblemService {

    private final ProblemRepository problemRepository;
    private final TestCaseRepository testCaseRepository;
    private final ProblemTagRepository problemTagRepository;

    @Transactional
    public ProblemResponse createProblem(CreateProblemRequest request, UUID createdBy) {
        Problem problem = new Problem();
        problem.setTitle(request.getTitle());
        problem.setDescription(request.getDescription());
        problem.setInputFormat(request.getInputFormat());
        problem.setOutputFormat(request.getOutputFormat());
        problem.setConstraintsText(request.getConstraintsText());
        problem.setDifficulty(request.getDifficulty());
        problem.setTimeLimitMs(request.getTimeLimitMs());
        problem.setMemoryLimitMb(request.getMemoryLimitMb());
        problem.setCreatedBy(createdBy);

        if (request.getTags() != null) {
            Set<ProblemTag> tags = request.getTags().stream()
                    .map(name -> problemTagRepository.findByName(name)
                            .orElseGet(() -> problemTagRepository.save(new ProblemTag(name))))
                    .collect(Collectors.toSet());
            problem.setTags(tags);
        }

        problemRepository.save(problem);
        return toFullResponse(problem, List.of());
    }

    @Transactional(readOnly = true)
    public PageResponse<ProblemSummaryResponse> listProblems(Pageable pageable) {
        Page<Problem> page = problemRepository.findAll(pageable);
        return toPageResponse(page);
    }

    @Transactional(readOnly = true)
    public ProblemResponse getProblemForStudent(UUID problemId) {
        Problem problem = problemRepository.findByIdWithTags(problemId)
                .orElseThrow(() -> new ResourceNotFoundException("Problem", problemId));

        // SECURITY: students only receive sample test cases
        List<TestCase> sampleCases = testCaseRepository.findByProblemIdAndIsSample(problemId, true);
        List<TestCaseResponse> safeCases = sampleCases.stream()
                .map(this::toTestCaseResponse)
                .collect(Collectors.toList());

        return toFullResponse(problem, safeCases);
    }

    @Transactional(readOnly = true)
    public ProblemResponse getProblemForFaculty(UUID problemId) {
        Problem problem = problemRepository.findByIdWithTags(problemId)
                .orElseThrow(() -> new ResourceNotFoundException("Problem", problemId));

        // Faculty see all test cases including hidden ones
        List<TestCase> allCases = testCaseRepository.findByProblemId(problemId);
        List<TestCaseResponse> allResponses = allCases.stream()
                .map(this::toTestCaseResponse)
                .collect(Collectors.toList());

        return toFullResponse(problem, allResponses);
    }

    @Transactional
    public TestCaseResponse addTestCase(UUID problemId, AddTestCaseRequest request, UUID createdBy) {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ResourceNotFoundException("Problem", problemId));

        TestCase tc = new TestCase();
        tc.setProblem(problem);
        tc.setInputData(request.getInputData());
        tc.setExpectedOutput(request.getExpectedOutput());
        tc.setSample(request.isSample());
        tc.setWeight(request.getWeight());

        testCaseRepository.save(tc);
        return toTestCaseResponse(tc);
    }

    private ProblemResponse toFullResponse(Problem problem, List<TestCaseResponse> testCases) {
        Set<String> tagNames = problem.getTags().stream()
                .map(ProblemTag::getName)
                .collect(Collectors.toSet());

        return ProblemResponse.builder()
                .id(problem.getId())
                .title(problem.getTitle())
                .description(problem.getDescription())
                .inputFormat(problem.getInputFormat())
                .outputFormat(problem.getOutputFormat())
                .constraintsText(problem.getConstraintsText())
                .difficulty(problem.getDifficulty())
                .timeLimitMs(problem.getTimeLimitMs())
                .memoryLimitMb(problem.getMemoryLimitMb())
                .tags(tagNames)
                .sampleTestCases(testCases)
                .createdBy(problem.getCreatedBy())
                .createdAt(problem.getCreatedAt())
                .build();
    }

    private PageResponse<ProblemSummaryResponse> toPageResponse(Page<Problem> page) {
        List<ProblemSummaryResponse> content = page.getContent().stream()
                .map(p -> ProblemSummaryResponse.builder()
                        .id(p.getId())
                        .title(p.getTitle())
                        .difficulty(p.getDifficulty())
                        .timeLimitMs(p.getTimeLimitMs())
                        .memoryLimitMb(p.getMemoryLimitMb())
                        .tags(p.getTags().stream().map(ProblemTag::getName).collect(Collectors.toSet()))
                        .build())
                .collect(Collectors.toList());

        return PageResponse.<ProblemSummaryResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    private TestCaseResponse toTestCaseResponse(TestCase tc) {
        return TestCaseResponse.builder()
                .id(tc.getId())
                .inputData(tc.getInputData())
                .expectedOutput(tc.getExpectedOutput())
                .isSample(tc.isSample())
                .weight(tc.getWeight())
                .build();
    }
}
