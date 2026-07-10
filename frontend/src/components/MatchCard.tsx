import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import type { MatchResponse, TeamResponse } from '../api/types'
import { StatusBadge } from './StatusBadge'
import { FollowTeamButton } from './FollowTeamButton'

function formatCountdown(targetIso: string): string {
  const diffMs = new Date(targetIso).getTime() - Date.now()
  if (diffMs <= 0) return 'Starting now'
  const totalSeconds = Math.floor(diffMs / 1000)
  const days = Math.floor(totalSeconds / 86400)
  const hours = Math.floor((totalSeconds % 86400) / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60
  if (days > 0) return `in ${days}d ${hours}h`
  if (hours > 0) return `in ${hours}h ${minutes}m`
  if (minutes > 0) return `in ${minutes}m ${seconds}s`
  return `in ${seconds}s`
}

/**
 * Self-adjusting tick rate rather than a flat setInterval: far-future matches only need
 * minute-level granularity (cheap, one re-render/min), imminent ones tick every second for a
 * genuinely live countdown. A recursive setTimeout (not setInterval) is what makes the rate able
 * to change between ticks. `enabled` skips scheduling entirely for non-upcoming matches, which
 * would otherwise tick forever just to keep re-rendering "Starting now".
 */
function useCountdown(targetIso: string, enabled: boolean): string {
  const [label, setLabel] = useState(() => formatCountdown(targetIso))

  useEffect(() => {
    if (!enabled) return
    let timeoutId: ReturnType<typeof setTimeout>
    const tick = () => {
      setLabel(formatCountdown(targetIso))
      const diffMs = new Date(targetIso).getTime() - Date.now()
      const delay = diffMs > 3_600_000 ? 60_000 : 1_000
      timeoutId = setTimeout(tick, delay)
    }
    tick()
    return () => clearTimeout(timeoutId)
  }, [targetIso, enabled])

  return label
}

function formatSchedule(iso: string): string {
  const date = new Date(iso)
  const today = new Date()
  const isToday = date.toDateString() === today.toDateString()
  const time = date.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })
  if (isToday) return `Today ${time}`
  return `${date.toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric' })} ${time}`
}

function TeamSide({ team, align }: { team: TeamResponse | null; align: 'left' | 'right' }) {
  const row = align === 'right' ? 'flex-row-reverse' : 'flex-row'
  return (
    <div className={`flex flex-1 items-center gap-2 ${row}`}>
      {team?.logoUrl ? (
        <img src={team.logoUrl} alt="" className="h-8 w-8 rounded object-contain" />
      ) : (
        <div className="h-8 w-8 rounded bg-zinc-800" />
      )}
      <span className="truncate font-medium">{team?.name ?? 'TBD'}</span>
      {team && <FollowTeamButton team={team} />}
    </div>
  )
}

export function MatchCard({ match }: { match: MatchResponse }) {
  const finished = match.status === 'FINISHED'
  const upcoming = match.status === 'UPCOMING'
  const navigate = useNavigate()
  const countdown = useCountdown(match.scheduledAt, upcoming)
  // A div with navigate, not a <Link>: the card contains its own anchors
  // (stream link) and buttons, and nested <a> elements are invalid HTML.
  return (
    <div
      onClick={() => navigate(`/matches/${match.id}`)}
      className="cursor-pointer rounded-lg border border-zinc-800 bg-zinc-900/60 p-4 transition-colors hover:border-violet-700/60">
      <div className="relative mb-3 flex items-center justify-between gap-2 text-xs text-zinc-500">
        <span className="truncate">{match.tournamentName}</span>
        {upcoming && (
          <span className="absolute left-1/2 -translate-x-1/2 whitespace-nowrap font-medium tabular-nums text-violet-400">
            {countdown}
          </span>
        )}
        <StatusBadge status={match.status} />
      </div>
      <div className="flex items-center gap-4">
        <TeamSide team={match.teamA} align="left" />
        <div className="shrink-0 text-center">
          {finished || match.status === 'RUNNING' ? (
            <span className="text-lg font-bold tabular-nums">
              {match.scoreA ?? 0} : {match.scoreB ?? 0}
            </span>
          ) : (
            <span className="text-sm text-zinc-500">vs</span>
          )}
        </div>
        <TeamSide team={match.teamB} align="right" />
      </div>
      <div className="mt-3 flex items-center justify-between text-xs text-zinc-500">
        <span>{formatSchedule(match.scheduledAt)}</span>
        {match.streamUrl && match.status !== 'FINISHED' && (
          <a
            href={match.streamUrl}
            target="_blank"
            rel="noreferrer"
            onClick={(e) => e.stopPropagation()}
            className="text-violet-400 hover:text-violet-300"
          >
            Watch ↗
          </a>
        )}
      </div>
    </div>
  )
}
