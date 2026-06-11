import { apiClient } from '@/lib/axios'
import type { LeaderboardEntryResponse } from '@/types/leaderboard.types'

export const leaderboardService = {
  getTopN: (contestId: string, limit = 50): Promise<LeaderboardEntryResponse[]> =>
    apiClient
      .get(`/leaderboards/contests/${contestId}`, { params: { limit } })
      .then((r) => r.data.data),

  getMyRank: (contestId: string): Promise<LeaderboardEntryResponse> =>
    apiClient.get(`/leaderboards/contests/${contestId}/me`).then((r) => r.data.data),
}
