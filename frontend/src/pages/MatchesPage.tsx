import { useState } from 'react'
import { useGames, useMatches } from '../api/queries'
import type { EventStatus } from '../api/types'
import { MatchCard } from '../components/MatchCard'
import { Pagination } from '../components/Pagination'
import { EmptyState, ErrorMessage, Loading } from '../components/QueryState'

const select =
  'rounded-md border border-zinc-700 bg-zinc-900 px-3 py-1.5 text-sm text-zinc-300 focus:border-violet-500 focus:outline-none'

export function MatchesPage() {
  const [game, setGame] = useState('')
  const [status, setStatus] = useState<EventStatus | ''>('UPCOMING')
  const [page, setPage] = useState(0)

  const games = useGames()
  const matches = useMatches({
    game: game || undefined,
    status: status || undefined,
    page,
  })

  return (
    <div>
      <div className="mb-6 flex flex-wrap items-center gap-3">
        <h1 className="mr-auto text-2xl font-bold">Matches</h1>
        <select
          className={select}
          value={game}
          onChange={(e) => {
            setGame(e.target.value)
            setPage(0)
          }}
        >
          <option value="">All games</option>
          {games.data?.map((g) => (
            <option key={g.slug} value={g.slug}>
              {g.name}
            </option>
          ))}
        </select>
        <select
          className={select}
          value={status}
          onChange={(e) => {
            setStatus(e.target.value as EventStatus | '')
            setPage(0)
          }}
        >
          <option value="">Any status</option>
          <option value="UPCOMING">Upcoming</option>
          <option value="RUNNING">Running</option>
          <option value="FINISHED">Finished</option>
        </select>
      </div>

      {matches.isPending ? (
        <Loading />
      ) : matches.isError ? (
        <ErrorMessage error={matches.error} />
      ) : matches.data.content.length === 0 ? (
        <EmptyState message="No matches found for these filters." />
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-2">
            {matches.data.content.map((m) => (
              <MatchCard key={m.id} match={m} />
            ))}
          </div>
          <Pagination
            page={matches.data.page}
            totalPages={matches.data.totalPages}
            onPageChange={setPage}
          />
        </>
      )}
    </div>
  )
}
