import { apiClient } from '@/lib/axios'
import type {
  CreateSubmissionRequest,
  SubmissionResponse,
  SubmissionStatusResponse,
} from '@/types/submission.types'

export const submissionService = {
  submit: (data: CreateSubmissionRequest): Promise<SubmissionResponse> =>
    apiClient.post('/submissions', data).then((r) => r.data.data),

  getStatus: (id: string): Promise<SubmissionStatusResponse> =>
    apiClient.get(`/submissions/${id}/status`).then((r) => r.data.data),

  get: (id: string): Promise<SubmissionResponse> =>
    apiClient.get(`/submissions/${id}`).then((r) => r.data.data),
}
