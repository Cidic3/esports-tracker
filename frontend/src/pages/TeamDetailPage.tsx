import { Link, useParams } from 'react-router-dom'
import { useTeamDetail } from '../api/queries'
import type { PlayerResponse } from '../api/types'
import { FollowTeamHeaderButton } from '../components/FollowTeamHeaderButton'
import { MatchCard } from '../components/MatchCard'
import { EmptyState, ErrorMessage, Loading } from '../components/QueryState'

const GAME_LABELS: Record<string, string> = {
  'league-of-legends': 'League of Legends',
  'dota-2': 'Dota 2',
  'apex-legends': 'Apex Legends',
}

const PLACE_EMOJI: Record<number, string> = {
  1: '🏆',
  2: '🥈',
  3: '🥉',
}

function ordinal(n: number): string {
  const mod100 = n % 100
  if (mod100 >= 11 && mod100 <= 13) return `${n}th`
  switch (n % 10) {
    case 1:
      return `${n}st`
    case 2:
      return `${n}nd`
    case 3:
      return `${n}rd`
    default:
      return `${n}th`
  }
}

const ROLE_LABELS: Record<PlayerResponse['role'], string> = {
  TOP: 'Top',
  JUNGLE: 'Jungle',
  MID: 'Mid',
  BOTTOM: 'Bottom',
  SUPPORT: 'Support',
  NONE: 'Roster',
}

function PlayerCard({ player }: { player: PlayerResponse }) {
  return (
    <div className="flex items-center gap-3 rounded-lg border border-zinc-800 bg-zinc-900/60 p-3">
      {player.imageUrl ? (
        <img src={player.imageUrl} alt="" className="h-10 w-10 rounded-full object-cover" />
      ) : (
        <div className="h-10 w-10 rounded-full bg-zinc-800" />
      )}
      <div className="min-w-0">
        <p className="truncate font-medium">{player.summonerName}</p>
        <p className="truncate text-xs text-zinc-500">
          {ROLE_LABELS[player.role]}
          {player.firstName || player.lastName
            ? ` · ${[player.firstName, player.lastName].filter(Boolean).join(' ')}`
            : ''}
        </p>
      </div>
    </div>
  )
}

/**
 * "active" is a heuristic (played in a recent match), not an authoritative starter/sub flag — see
 * PlayerResponse. Split into two sections rather than an inline badge so the starting five reads
 * clearly, with everyone else still visible underneath.
 */
function RosterList({ roster }: { roster: PlayerResponse[] }) {
  if (roster.length === 0) {
    return <EmptyState message="No roster synced for this team yet." />
  }
  const active = roster.filter((p) => p.active)
  const bench = roster.filter((p) => !p.active)

  // No one matched the heuristic (e.g. no recent finished matches synced yet) — showing an
  // all-"Bench" roster would be misleading, so fall back to one flat, unlabeled list.
  if (active.length === 0) {
    return (
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {roster.map((p) => (
          <PlayerCard key={p.id} player={p} />
        ))}
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div>
        <h3 className="mb-2 text-sm font-semibold text-zinc-400">Starting lineup</h3>
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {active.map((p) => (
            <PlayerCard key={p.id} player={p} />
          ))}
        </div>
      </div>
      {bench.length > 0 && (
        <div>
          <h3 className="mb-2 text-sm font-semibold text-zinc-400">Bench</h3>
          <div className="grid gap-3 opacity-60 sm:grid-cols-2 lg:grid-cols-3">
            {bench.map((p) => (
              <PlayerCard key={p.id} player={p} />
            ))}
          </div>
        </div>
      )}
    </div>
  )
}

export function TeamDetailPage() {
  const { id } = useParams<{ id: string }>()
  const team = useTeamDetail(id!)

  if (team.isPending) return <Loading />
  if (team.isError) return <ErrorMessage error={team.error} />

  const t = team.data

  return (
    <div className="space-y-10">
      <div className="flex items-center gap-4">
        {t.logoUrl ? (
          <img src={t.logoUrl} alt="" className="h-16 w-16 object-contain" />
        ) : (
          <div className="h-16 w-16 rounded bg-zinc-800" />
        )}
        <div>
          <h1 className="text-2xl font-bold">{t.name}</h1>
          <p className="text-sm text-zinc-500">{GAME_LABELS[t.gameSlug] ?? t.gameSlug}</p>
        </div>
        <FollowTeamHeaderButton team={t} />
        {team.isFetching && (
          <span className="ml-auto flex items-center gap-2 text-xs text-zinc-500">
            <span className="h-3 w-3 animate-spin rounded-full border-2 border-zinc-700 border-t-violet-500" />
            Updating matches…
          </span>
        )}
      </div>

      {/* Apex teams have no roster/head-to-head-match/standings data (Cito exposes team names and
          results only) — showing those sections as permanently empty would read as broken, so
          Apex teams get just their recent ALGS match day placements instead. */}
      {t.gameSlug === 'apex-legends' ? (
        <section>
          <h2 className="mb-4 text-lg font-semibold text-zinc-300">Recent ALGS results</h2>
          {t.apexResults.length === 0 ? (
            <EmptyState message="No ALGS results synced for this team yet." />
          ) : (
            <div className="space-y-2">
              {t.apexResults.map((r) => (
                <Link
                  key={`${r.matchDayId}`}
                  to={`/apex/${r.matchDayId}`}
                  className="flex items-center justify-between rounded-lg border border-zinc-800 bg-zinc-900/60 px-4 py-3 text-sm transition-colors hover:border-zinc-600"
                >
                  <span className="min-w-0">
                    <span className="block truncate font-medium text-zinc-100">{r.matchDayName}</span>
                    <span className="block truncate text-xs text-zinc-500">{r.tournamentName}</span>
                  </span>
                  <span className="shrink-0 text-right">
                    <span className="font-semibold text-zinc-100">
                      {PLACE_EMOJI[r.rank] && <span className="mr-1">{PLACE_EMOJI[r.rank]}</span>}
                      {ordinal(r.rank)} Place
                    </span>
                    <span className="ml-3 tabular-nums text-zinc-400">{r.totalPoints} pts</span>
                  </span>
                </Link>
              ))}
            </div>
          )}
        </section>
      ) : (
        <>
      {t.liveMatches.length > 0 && (
        <section>
          <h2 className="mb-4 text-lg font-semibold text-zinc-300">Live now</h2>
          <div className="grid gap-4 sm:grid-cols-2">
            {t.liveMatches.map((m) => (
              <MatchCard key={m.id} match={m} />
            ))}
          </div>
        </section>
      )}

      <section>
        <h2 className="mb-4 text-lg font-semibold text-zinc-300">Upcoming matches</h2>
        {t.upcomingMatches.length === 0 ? (
          <EmptyState message="No upcoming matches scheduled." />
        ) : (
          <div className="grid gap-4 sm:grid-cols-2">
            {t.upcomingMatches.map((m) => (
              <MatchCard key={m.id} match={m} />
            ))}
          </div>
        )}
      </section>

      <section>
        <h2 className="mb-4 text-lg font-semibold text-zinc-300">Roster</h2>
        <RosterList roster={t.roster} />
      </section>

      {t.standings.length > 0 && (
        <section>
          <h2 className="mb-4 text-lg font-semibold text-zinc-300">Results</h2>
          <div className="space-y-2">
            {t.standings.map((s, i) => (
              <div
                key={`${s.team.id}-${s.groupName}-${i}`}
                className="flex items-center justify-between rounded-lg border border-zinc-800 bg-zinc-900/60 px-4 py-3 text-sm"
              >
                <span className="text-zinc-400">{s.tournamentName}</span>
                <span>
                  <span className="font-semibold text-zinc-100">
                    {PLACE_EMOJI[s.rank] && <span className="mr-1">{PLACE_EMOJI[s.rank]}</span>}
                    {ordinal(s.rank)} Place
                  </span>
                  <span className="ml-3 tabular-nums">
                    <span className="text-emerald-400">{s.wins}W</span>
                    {' – '}
                    <span className="text-red-400">{s.losses}L</span>
                  </span>
                </span>
              </div>
            ))}
          </div>
        </section>
      )}

      <section>
        <h2 className="mb-4 text-lg font-semibold text-zinc-300">Recent matches</h2>
        {t.recentMatches.length === 0 ? (
          <EmptyState message="No finished matches synced yet." />
        ) : (
          <div className="grid gap-4 sm:grid-cols-2">
            {t.recentMatches.map((m) => (
              <MatchCard key={m.id} match={m} />
            ))}
          </div>
        )}
      </section>
        </>
      )}
    </div>
  )
}
