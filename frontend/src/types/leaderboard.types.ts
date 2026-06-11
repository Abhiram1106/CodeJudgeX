export interface LeaderboardEntryResponse {
  rank: number
  studentId: string
  studentName?: string
  totalScore: number
  solvedCount: number
  lastSubmissionAt?: string
}
