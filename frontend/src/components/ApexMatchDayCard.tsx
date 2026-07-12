import { useNavigate } from 'react-router-dom'
import type { ApexMatchDayResponse } from '../api/types'
import { GameIcon } from './GameIcon'
import { LeagueChip } from './LeagueChip'
import { StatusBadge } from './StatusBadge'
import { useCountdown } from './useCountdown'

function formatSchedule(iso: string): string {
  const date = new Date(iso)
  const today = new Date()
  const isToday = date.toDateString() === today.toDateString()
  const time = date.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })
  if (isToday) return `Today ${time}`
  return `${date.toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric' })} ${time}`
}

/**
 * Card for an ALGS match day — no teamA/teamB row like MatchCard, since a BR day is ~20 teams
 * at once; the league + day name is the identity, results live on the detail page. Countdown and
 * game icon mirror MatchCard's layout (top-center countdown, bottom-center icon) so mixed-game
 * lists like the feed read consistently.
 */
export function ApexMatchDayCard({ day }: { day: ApexMatchDayResponse }) {
  const navigate = useNavigate()
  const upcoming = day.status === 'UPCOMING'
  const countdown = useCountdown(day.startsAt, upcoming)
  return (
    <div
      onClick={() => navigate(`/apex/${day.id}`)}
      className="flex h-full cursor-pointer flex-col rounded-lg border border-zinc-800 bg-zinc-900/60 p-4 transition-colors hover:border-zinc-600"
    >
      <div className="relative mb-2 flex items-center justify-between gap-2 text-xs text-zinc-500">
        {/* Just the league chip (e.g. "ALGS EMEA") — the full tournament name ("ALGS Year 6
            Pro League Qualifier EMEA") was long enough to overlap the centered countdown. */}
        <LeagueChip gameSlug={day.gameSlug} label={day.leagueName} />
        {upcoming && (
          <span className="absolute left-1/2 -translate-x-1/2 whitespace-nowrap font-medium tabular-nums text-violet-400">
            {countdown}
          </span>
        )}
        <StatusBadge status={day.status} kind="match" />
      </div>
      {/* flex-1 rather than a plain block: this card has no team-logos row like MatchCard, so
          it's naturally shorter — in a grid row next to taller MatchCards (e.g. the feed, which
          mixes both), the grid stretches this card to match, and without flex-1 here that extra
          height would just appear as dead space below the icon. Letting the day name absorb it
          instead centers the name vertically and keeps the icon pinned to the true bottom edge,
          matching MatchCard's layout regardless of which card ends up taller. */}
      <div className="flex flex-1 items-center justify-center text-center">
        <span className="font-medium">{day.name}</span>
      </div>
      <p className="mt-2 text-xs text-zinc-500">{formatSchedule(day.startsAt)}</p>
      <div className="mt-3 flex justify-center">
        <GameIcon gameSlug={day.gameSlug} />
      </div>
    </div>
  )
}
