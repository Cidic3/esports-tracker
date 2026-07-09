import { useState } from 'react'
import { useGames, useTournaments } from '../api/queries'
import type { EventStatus, TournamentTier } from '../api/types'
import { Pagination } from '../components/Pagination'
import { TournamentCard } from '../components/TournamentCard'
import { EmptyState, ErrorMessage, Loading } from '../components/QueryState'

const select =
  'rounded-md border border-zinc-700 bg-zinc-900 px-3 py-1.5 text-sm text-zinc-300 focus:border-violet-500 focus:outline-none'

export function TournamentsPage() {
  const [game, setGame] = useState('')
  const [status, setStatus] = useState<EventStatus | ''>('RUNNING')
  const [tier, setTier] = useState<TournamentTier | ''>('')
  const [page, setPage] = useState(0)

  const games = useGames()
  const tournaments = useTournaments({
    game: game || undefined,
    status: status || undefined,
    tier: tier || undefined,
    page,
  })

  return (
    <div>
      <div className="mb-6 flex flex-wrap items-center gap-3">
        <h1 className="mr-auto text-2xl font-bold">Tournaments</h1>
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
        <select
          className={select}
          value={tier}
          onChange={(e) => {
            setTier(e.target.value as TournamentTier | '')
            setPage(0)
          }}
        >
          <option value="">Any tier</option>
          <option value="INTERNATIONAL">International</option>
          <option value="PRIMARY">Primary</option>
          <option value="SECONDARY">Secondary</option>
        </select>
      </div>

      {tournaments.isPending ? (
        <Loading />
      ) : tournaments.isError ? (
        <ErrorMessage error={tournaments.error} />
      ) : tournaments.data.content.length === 0 ? (
        <EmptyState message="No tournaments found for these filters." />
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {tournaments.data.content.map((t) => (
              <TournamentCard key={t.id} tournament={t} />
            ))}
          </div>
          <Pagination
            page={tournaments.data.page}
            totalPages={tournaments.data.totalPages}
            onPageChange={setPage}
          />
        </>
      )}
    </div>
  )
}
