export type SubmissionStatus =
  | 'QUEUED'
  | 'RUNNING'
  | 'ACCEPTED'
  | 'WRONG_ANSWER'
  | 'TIME_LIMIT_EXCEEDED'
  | 'MEMORY_LIMIT_EXCEEDED'
  | 'RUNTIME_ERROR'
  | 'COMPILATION_ERROR'
  | 'INTERNAL_ERROR'

export const TERMINAL_STATUSES = new Set<SubmissionStatus>([
  'ACCEPTED',
  'WRONG_ANSWER',
  'TIME_LIMIT_EXCEEDED',
  'MEMORY_LIMIT_EXCEEDED',
  'RUNTIME_ERROR',
  'COMPILATION_ERROR',
  'INTERNAL_ERROR',
])

export interface SubmissionResponse {
  id: string
  status: SubmissionStatus
  score: number
  languageId: number
  submittedAt: string
  evaluatedAt?: string
  executionTimeMs?: number
  memoryUsedMb?: number
}

export interface SubmissionStatusResponse {
  id: string
  status: SubmissionStatus
  score: number
}

export interface CreateSubmissionRequest {
  contestId: string
  problemId: string
  languageId: number
  sourceCode: string
}

export const LANGUAGE_OPTIONS = [
  { id: 62, label: 'Java (JDK 17)' },
  { id: 54, label: 'C++ (GCC 9.2)' },
  { id: 71, label: 'Python 3.8' },
  { id: 63, label: 'JavaScript (Node 12)' },
  { id: 50, label: 'C (GCC 9.2)' },
] as const

export type LanguageId = (typeof LANGUAGE_OPTIONS)[number]['id']
