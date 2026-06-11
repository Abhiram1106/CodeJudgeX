import { apiClient } from '@/lib/axios'
import type { PageResponse } from '@/types/api.types'
import type {
  ContestResponse,
  ContestSummaryResponse,
  CreateContestRequest,
} from '@/types/contest.types'

export const contestService = {
  list: (status?: string, page = 0, size = 20): Promise<PageResponse<ContestSummaryResponse>> =>
    apiClient
      .get('/contests', { params: { status, page, size } })
      .then((r) => r.data.data),

  get: (id: string): Promise<ContestResponse> =>
    apiClient.get(`/contests/${id}`).then((r) => r.data.data),

  create: (data: CreateContestRequest): Promise<ContestResponse> =>
    apiClient.post('/contests', data).then((r) => r.data.data),

  addProblem: (contestId: string, problemId: string): Promise<ContestResponse> =>
    apiClient
      .post(`/contests/${contestId}/problems`, null, { params: { problemId } })
      .then((r) => r.data.data),

  register: (contestId: string): Promise<void> =>
    apiClient.post(`/contests/${contestId}/register`).then(() => undefined),
}
