import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useGames, useTeams } from '../api/queries'
import { TeamCard } from '../components/TeamCard'
import { Pagination } from '../components/Pagination'
import { EmptyState, ErrorMessage, Loading } from '../components/QueryState'

const select =
  'rounded-md border border-zinc-700 bg-zinc-900 px-3 py-1.5 text-sm text-zinc-300 focus:border-violet-500 focus:outline-none'

export function TeamsPage() {
  const [searchParams] = useSearchParams()
  const urlSearch = searchParams.get('search') ?? ''
  const [query, setQuery] = useState(urlSearch)
  const [game, setGame] = useState('')
  const [page, setPage] = useState(0)

  // Re-searching from the nav bar while already on this page navigates to the same route with a
  // new query string, which React Router doesn't remount — a plain useState initializer alone
  // would never see the change, so keep local state synced to the URL explicitly.
  useEffect(() => {
    setQuery(urlSearch)
    setPage(0)
  }, [urlSearch])

  const games = useGames()
  // `search: query` (not `query || undefined`) so an empty query disables the fetch entirely —
  // useTeams treats an explicit empty string as "don't query yet", matching the empty state below.
  const teams = useTeams({
    search: query,
    game: game || undefined,
    page,
  })

  return (
    <div>
      <div className="mx-auto mb-8 flex max-w-xl flex-col items-center gap-4 text-center">
        <h1 className="text-2xl font-bold">Search Teams</h1>
        <input
          type="search"
          autoFocus
          value={query}
          onChange={(e) => {
            setQuery(e.target.value)
            setPage(0)
          }}
          placeholder="Search teams…"
          className="w-full rounded-lg border border-zinc-700 bg-zinc-900 px-4 py-3 text-lg text-zinc-100 placeholder-zinc-500 focus:border-violet-500 focus:outline-none"
        />
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
      </div>

      {!query ? (
        <EmptyState message="Type a team name to search." />
      ) : teams.isPending ? (
        <Loading />
      ) : teams.isError ? (
        <ErrorMessage error={teams.error} />
      ) : teams.data.content.length === 0 ? (
        <EmptyState message={`No teams found for "${query}".`} />
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {teams.data.content.map((t) => (
              <TeamCard key={t.id} team={t} />
            ))}
          </div>
          <Pagination page={teams.data.page} totalPages={teams.data.totalPages} onPageChange={setPage} />
        </>
      )}
    </div>
  )
}
