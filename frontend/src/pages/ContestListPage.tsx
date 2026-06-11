import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { contestService } from '@/services/contest.service'
import type { ContestStatus } from '@/types/contest.types'

const STATUS_BADGE: Record<ContestStatus, string> = {
  DRAFT: 'bg-gray-100 text-gray-700',
  UPCOMING: 'bg-blue-100 text-blue-700',
  LIVE: 'bg-green-100 text-green-700',
  ENDED: 'bg-red-100 text-red-700',
}

export default function ContestListPage() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['contests'],
    queryFn: () => contestService.list(),
  })

  if (isLoading) return <div className="p-8 text-gray-500">Loading contests…</div>
  if (isError) return <div className="p-8 text-red-600">Failed to load contests.</div>

  const contests = data?.content ?? []

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Contests</h1>

      {contests.length === 0 ? (
        <p className="text-gray-500">No contests available yet.</p>
      ) : (
        <div className="space-y-3">
          {contests.map((contest) => (
            <Link
              key={contest.id}
              to={`/contests/${contest.id}`}
              className="block bg-white rounded-xl border border-gray-200 p-5 hover:shadow-md transition-shadow"
            >
              <div className="flex items-start justify-between gap-4">
                <div>
                  <h2 className="font-semibold text-gray-900">{contest.title}</h2>
                  <p className="text-sm text-gray-500 mt-1">
                    {new Date(contest.startTime).toLocaleString()} →{' '}
                    {new Date(contest.endTime).toLocaleString()}
                  </p>
                </div>
                <span
                  className={`shrink-0 inline-block text-xs font-medium px-2.5 py-0.5 rounded-full ${STATUS_BADGE[contest.status]}`}
                >
                  {contest.status}
                </span>
              </div>
              <p className="text-xs text-gray-400 mt-2">{contest.participantCount} participants</p>
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}
