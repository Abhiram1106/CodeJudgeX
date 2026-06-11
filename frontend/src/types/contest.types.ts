export type ContestStatus = 'DRAFT' | 'UPCOMING' | 'LIVE' | 'ENDED'

export interface ContestSummaryResponse {
  id: string
  title: string
  status: ContestStatus
  startTime: string
  endTime: string
  participantCount: number
}

export interface ContestResponse extends ContestSummaryResponse {
  description: string
  problems: ContestProblemSummary[]
}

export interface ContestProblemSummary {
  problemId: string
  title: string
  difficulty: string
}

export interface CreateContestRequest {
  title: string
  description?: string
  startTime: string
  endTime: string
}
