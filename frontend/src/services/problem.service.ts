import { apiClient } from '@/lib/axios'
import type { PageResponse } from '@/types/api.types'
import type {
  AddTestCaseRequest,
  CreateProblemRequest,
  ProblemResponse,
  ProblemSummaryResponse,
  TestCaseResponse,
} from '@/types/problem.types'

export const problemService = {
  list: (page = 0, size = 20): Promise<PageResponse<ProblemSummaryResponse>> =>
    apiClient.get('/problems', { params: { page, size } }).then((r) => r.data.data),

  get: (id: string): Promise<ProblemResponse> =>
    apiClient.get(`/problems/${id}`).then((r) => r.data.data),

  getFull: (id: string): Promise<ProblemResponse> =>
    apiClient.get(`/problems/${id}/full`).then((r) => r.data.data),

  create: (data: CreateProblemRequest): Promise<ProblemResponse> =>
    apiClient.post('/problems', data).then((r) => r.data.data),

  addTestCase: (problemId: string, data: AddTestCaseRequest): Promise<TestCaseResponse> =>
    apiClient.post(`/problems/${problemId}/test-cases`, data).then((r) => r.data.data),

  listTestCases: (problemId: string): Promise<TestCaseResponse[]> =>
    apiClient.get(`/problems/${problemId}/test-cases`).then((r) => r.data.data),
}
