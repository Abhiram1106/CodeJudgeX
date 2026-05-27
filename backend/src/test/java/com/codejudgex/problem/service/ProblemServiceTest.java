package com.codejudgex.problem.service;

import com.codejudgex.common.exception.ResourceNotFoundException;
import com.codejudgex.problem.dto.AddTestCaseRequest;
import com.codejudgex.problem.dto.CreateProblemRequest;
import com.codejudgex.problem.dto.ProblemResponse;
import com.codejudgex.problem.dto.TestCaseResponse;
import com.codejudgex.problem.entity.Problem;
import com.codejudgex.problem.entity.TestCase;
import com.codejudgex.problem.repository.ProblemRepository;
import com.codejudgex.problem.repository.ProblemTagRepository;
import com.codejudgex.problem.repository.TestCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProblemServiceTest {

    @Mock ProblemRepository problemRepository;
    @Mock TestCaseRepository testCaseRepository;
    @Mock ProblemTagRepository problemTagRepository;

    @InjectMocks ProblemService problemService;

    private Problem problem;
    private UUID problemId;
    private UUID facultyId;

    @BeforeEach
    void setUp() {
        problemId = UUID.randomUUID();
        facultyId = UUID.randomUUID();

        problem = new Problem();
        problem.setTitle("Two Sum");
        problem.setDescription("Given an array...");
        problem.setDifficulty("EASY");
        problem.setTimeLimitMs(2000);
        problem.setMemoryLimitMb(256);
        problem.setCreatedBy(facultyId);
        problem.setTags(new HashSet<>());
    }

    @Test
    void createProblem_noTags_success() {
        CreateProblemRequest request = new CreateProblemRequest();
        request.setTitle("Two Sum");
        request.setDescription("Given an array...");
        request.setDifficulty("EASY");
        request.setTimeLimitMs(2000);
        request.setMemoryLimitMb(256);

        when(problemRepository.save(any(Problem.class))).thenReturn(problem);

        ProblemResponse response = problemService.createProblem(request, facultyId);

        assertThat(response.getTitle()).isEqualTo("Two Sum");
        assertThat(response.getDifficulty()).isEqualTo("EASY");
    }

    @Test
    void getProblemForStudent_returnsOnlySampleTestCases() {
        TestCase hidden = new TestCase();
        hidden.setSample(false);
        hidden.setInputData("hidden input");
        hidden.setExpectedOutput("hidden output");

        TestCase sample = new TestCase();
        sample.setSample(true);
        sample.setInputData("1 2");
        sample.setExpectedOutput("3");
        sample.setWeight(1);

        when(problemRepository.findByIdWithTags(problemId)).thenReturn(Optional.of(problem));
        // Service calls findByProblemIdAndIsSample(id, true) for students
        when(testCaseRepository.findByProblemIdAndIsSample(problemId, true)).thenReturn(List.of(sample));

        ProblemResponse response = problemService.getProblemForStudent(problemId);

        assertThat(response.getSampleTestCases()).hasSize(1);
        assertThat(response.getSampleTestCases().get(0).getInputData()).isEqualTo("1 2");
        // hidden test case must not appear
        assertThat(response.getSampleTestCases())
                .noneMatch(tc -> "hidden input".equals(tc.getInputData()));
    }

    @Test
    void getProblemForStudent_notFound_throwsResourceNotFoundException() {
        when(problemRepository.findByIdWithTags(problemId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> problemService.getProblemForStudent(problemId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addTestCase_success() {
        AddTestCaseRequest request = new AddTestCaseRequest();
        request.setInputData("1 2");
        request.setExpectedOutput("3");
        request.setSample(true);
        request.setWeight(1);

        TestCase tc = new TestCase();
        tc.setProblem(problem);
        tc.setInputData("1 2");
        tc.setExpectedOutput("3");
        tc.setSample(true);
        tc.setWeight(1);

        when(problemRepository.findById(problemId)).thenReturn(Optional.of(problem));
        when(testCaseRepository.save(any(TestCase.class))).thenReturn(tc);

        TestCaseResponse response = problemService.addTestCase(problemId, request, facultyId);

        assertThat(response.getInputData()).isEqualTo("1 2");
        assertThat(response.isSample()).isTrue();
    }
}
