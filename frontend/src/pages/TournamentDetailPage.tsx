import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { useStandings, useTournament, useTournamentMatches } from '../api/queries'
import type { StandingResponse } from '../api/types'
import { FollowTeamButton } from '../components/FollowTeamButton'
import { MatchCard } from '../components/MatchCard'
import { Pagination } from '../components/Pagination'
import { StatusBadge } from '../components/StatusBadge'
import { EmptyState, ErrorMessage, Loading } from '../components/QueryState'

function StandingsTable({ standings }: { standings: StandingResponse[] }) {
  // Standings arrive ordered by group then rank; group them for display.
  const groups = new Map<string, StandingResponse[]>()
  for (const s of standings) {
    const list = groups.get(s.groupName) ?? []
    list.push(s)
    groups.set(s.groupName, list)
  }

  return (
    <div className="space-y-6">
      {[...groups.entries()].map(([groupName, rows]) => (
        <div key={groupName}>
          <h3 className="mb-2 text-sm font-semibold text-zinc-400">{groupName}</h3>
          <div className="overflow-x-auto rounded-lg border border-zinc-800">
            <table className="w-full text-sm">
              <thead className="bg-zinc-900 text-left text-xs uppercase text-zinc-500">
                <tr>
                  <th className="px-4 py-2 font-medium">#</th>
                  <th className="px-4 py-2 font-medium">Team</th>
                  <th className="px-4 py-2 text-right font-medium">W</th>
                  <th className="px-4 py-2 text-right font-medium">L</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-800/70">
                {rows.map((s) => (
                  <tr key={s.team.id} className="bg-zinc-900/40">
                    <td className="px-4 py-2 tabular-nums text-zinc-500">{s.rank}</td>
                    <td className="px-4 py-2">
                      <span className="flex items-center gap-2">
                        {s.team.logoUrl && (
                          <img src={s.team.logoUrl} alt="" className="h-5 w-5 object-contain" />
                        )}
                        {s.team.name}
                        <FollowTeamButton team={s.team} />
                      </span>
                    </td>
                    <td className="px-4 py-2 text-right tabular-nums text-emerald-400">{s.wins}</td>
                    <td className="px-4 py-2 text-right tabular-nums text-red-400">{s.losses}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      ))}
    </div>
  )
}

export function TournamentDetailPage() {
  const { id } = useParams<{ id: string }>()
  const [page, setPage] = useState(0)

  const tournament = useTournament(id!)
  const standings = useStandings(id!)
  const matches = useTournamentMatches(id!, page)

  if (tournament.isPending) return <Loading />
  if (tournament.isError) return <ErrorMessage error={tournament.error} />

  const t = tournament.data

  return (
    <div className="space-y-10">
      <div>
        <div className="flex items-center gap-3">
          <h1 className="text-2xl font-bold">{t.name}</h1>
          <StatusBadge status={t.status} />
        </div>
        <p className="mt-1 text-sm text-zinc-500">
          {t.league?.name}
          {t.league?.region ? ` · ${t.league.region}` : ''}
          {t.startDate ? ` · ${t.startDate} → ${t.endDate ?? '?'}` : ''}
        </p>
      </div>

      <section>
        <h2 className="mb-4 text-lg font-semibold text-zinc-300">Standings</h2>
        {standings.isPending ? (
          <Loading />
        ) : standings.isError ? (
          <ErrorMessage error={standings.error} />
        ) : standings.data.length === 0 ? (
          <EmptyState message="No standings for this tournament (bracket stages have no win-loss table)." />
        ) : (
          <StandingsTable standings={standings.data} />
        )}
      </section>

      <section>
        <h2 className="mb-4 text-lg font-semibold text-zinc-300">Matches</h2>
        {matches.isPending ? (
          <Loading />
        ) : matches.isError ? (
          <ErrorMessage error={matches.error} />
        ) : matches.data.content.length === 0 ? (
          <EmptyState message="No matches synced for this tournament yet." />
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
      </section>
    </div>
  )
}
