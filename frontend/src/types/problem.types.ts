export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD'

export interface TestCaseResponse {
  id: string
  inputData: string
  expectedOutput: string
  isSample: boolean
}

export interface ProblemSummaryResponse {
  id: string
  title: string
  difficulty: Difficulty
  tags: string[]
  timeLimitMs: number
  memoryLimitMb: number
}

export interface ProblemResponse extends ProblemSummaryResponse {
  description: string
  inputFormat: string
  outputFormat: string
  constraintsText: string
  sampleTestCases: TestCaseResponse[]
  createdBy: string
  createdAt: string
}

export interface CreateProblemRequest {
  title: string
  description: string
  inputFormat?: string
  outputFormat?: string
  constraintsText?: string
  difficulty: Difficulty
  timeLimitMs: number
  memoryLimitMb: number
  tags?: string[]
}

export interface AddTestCaseRequest {
  inputData: string
  expectedOutput: string
  isSample: boolean
  weight?: number
}
