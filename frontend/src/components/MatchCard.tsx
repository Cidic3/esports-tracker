import { useNavigate } from 'react-router-dom'
import type { MatchResponse, TeamResponse } from '../api/types'
import { GameIcon } from './GameIcon'
import { LeagueChip } from './LeagueChip'
import { StatusBadge } from './StatusBadge'
import { FollowTeamButton } from './FollowTeamButton'
import { useCountdown } from './useCountdown'

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
      className="flex h-full cursor-pointer flex-col rounded-lg border border-zinc-800 bg-zinc-900/60 p-4 transition-colors hover:border-violet-700/60">
      <div className="relative mb-3 flex items-center justify-between gap-2 text-xs text-zinc-500">
        {/* Just the league chip (e.g. "LEC") — the tournament name was competing for the same
            row as the centered countdown and, on longer names, visually overlapping it. The
            full tournament name is still shown on the match detail page. */}
        <LeagueChip gameSlug={match.gameSlug} label={match.leagueSlug} />
        {upcoming && (
          <span className="absolute left-1/2 -translate-x-1/2 whitespace-nowrap font-medium tabular-nums text-violet-400">
            {countdown}
          </span>
        )}
        <StatusBadge status={match.status} />
      </div>
      {/* flex-1: in a grid row next to a shorter ApexMatchDayCard, the grid stretches this card
          to match — letting the team row absorb that extra height keeps it vertically centered
          and the icon pinned to the true bottom edge, same reasoning as ApexMatchDayCard. */}
      <div className="flex flex-1 items-center gap-4">
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
      <div className="mt-3 flex justify-center">
        <GameIcon gameSlug={match.gameSlug} />
      </div>
    </div>
  )
}
