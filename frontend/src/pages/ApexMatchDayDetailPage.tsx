import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { useApexMatchDay } from '../api/queries'
import type { ApexTeamResultResponse } from '../api/types'
import { FollowTeamButton } from '../components/FollowTeamButton'
import { StatusBadge } from '../components/StatusBadge'
import { EmptyState, ErrorMessage, Loading } from '../components/QueryState'

const PLACE_EMOJI: Record<number, string> = {
  1: '🏆',
  2: '🥈',
  3: '🥉',
}

function formatSchedule(iso: string): string {
  const date = new Date(iso)
  return `${date.toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric' })} ${date.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })}`
}

/**
 * One team's row: rank, name, total points, expandable to the per-game placement/kills/points
 * breakdown. Expansion is per-row local state — a 20-team table with every breakdown open at
 * once would be a wall of numbers. The row is a clickable div rather than a <button> because it
 * contains the follow star, which is itself a button — nested buttons are invalid HTML (same
 * reasoning as MatchCard's clickable div).
 */
function ResultRow({ result }: { result: ApexTeamResultResponse }) {
  const [open, setOpen] = useState(false)
  return (
    <div className="rounded-lg border border-zinc-800 bg-zinc-900/60">
      <div
        role="button"
        tabIndex={0}
        onClick={() => setOpen(!open)}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault()
            setOpen(!open)
          }
        }}
        aria-expanded={open}
        className="flex w-full cursor-pointer items-center gap-3 px-4 py-3 text-left text-sm transition-colors hover:bg-zinc-800/40"
      >
        <span className="w-10 shrink-0 font-semibold tabular-nums text-zinc-300">
          {PLACE_EMOJI[result.rank] ?? `#${result.rank}`}
        </span>
        <span className="flex-1 truncate font-medium">{result.team.name}</span>
        <FollowTeamButton team={result.team} />
        <span className="shrink-0 font-semibold tabular-nums text-zinc-100">
          {result.totalPoints} pts
        </span>
        <span className={`text-zinc-500 transition-transform ${open ? 'rotate-90' : ''}`} aria-hidden>
          ▸
        </span>
      </div>
      {open && result.games.length > 0 && (
        <div className="overflow-x-auto border-t border-zinc-800 px-4 py-3">
          <table className="w-full text-xs">
            <thead>
              <tr className="text-left text-zinc-500">
                <th className="pb-2 pr-4 font-medium">Game</th>
                <th className="pb-2 pr-4 font-medium">Placement</th>
                <th className="pb-2 pr-4 font-medium">Kills</th>
                <th className="pb-2 font-medium">Points</th>
              </tr>
            </thead>
            <tbody className="tabular-nums text-zinc-300">
              {result.games.map((g) => (
                <tr key={g.gameNumber}>
                  <td className="py-1 pr-4">#{g.gameNumber}</td>
                  <td className="py-1 pr-4">{g.placement === 1 ? '🏆 1' : g.placement}</td>
                  <td className="py-1 pr-4">{g.kills}</td>
                  <td className="py-1">{g.points}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

export function ApexMatchDayDetailPage() {
  const { id } = useParams<{ id: string }>()
  const day = useApexMatchDay(id!)

  if (day.isPending) return <Loading />
  if (day.isError) return <ErrorMessage error={day.error} />

  const d = day.data

  return (
    <div className="space-y-8">
      <div>
        <p className="text-sm text-zinc-500">{d.tournamentName}</p>
        <div className="mt-1 flex flex-wrap items-center gap-3">
          <h1 className="text-2xl font-bold">{d.name}</h1>
          <StatusBadge status={d.status} kind="match" />
          <span className="rounded bg-red-950/40 px-2 py-0.5 text-xs font-medium text-red-300">
            {d.leagueName}
          </span>
        </div>
        <p className="mt-2 text-sm text-zinc-500">{formatSchedule(d.startsAt)}</p>
      </div>

      <section>
        <h2 className="mb-4 text-lg font-semibold text-zinc-300">Results</h2>
        {d.results.length === 0 ? (
          <EmptyState
            message={
              d.status === 'UPCOMING'
                ? "This match day hasn't been played yet — results appear once it's over."
                : 'No results synced for this match day.'
            }
          />
        ) : (
          <div className="space-y-2">
            {d.results.map((r) => (
              <ResultRow key={r.team.id} result={r} />
            ))}
          </div>
        )}
      </section>
    </div>
  )
}
