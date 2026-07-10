import { Link } from 'react-router-dom'
import type { TournamentResponse } from '../api/types'
import { StatusBadge } from './StatusBadge'

const tierStyles: Record<TournamentResponse['tier'], string> = {
  INTERNATIONAL: 'bg-amber-500/15 text-amber-300 ring-amber-500/30',
  PRIMARY: 'bg-violet-500/15 text-violet-300 ring-violet-500/30',
  SECONDARY: 'bg-zinc-500/15 text-zinc-400 ring-zinc-500/30',
}

function formatRange(start: string | null, end: string | null): string {
  const fmt = (d: string) =>
    new Date(d + 'T00:00:00').toLocaleDateString(undefined, { month: 'short', day: 'numeric' })
  if (start && end) return `${fmt(start)} – ${fmt(end)}`
  if (start) return `from ${fmt(start)}`
  return ''
}

export function TournamentCard({ tournament }: { tournament: TournamentResponse }) {
  return (
    <Link
      to={`/tournaments/${tournament.id}`}
      className="block rounded-lg border border-zinc-800 bg-zinc-900/60 p-4 transition-colors hover:border-violet-700/60"
    >
      <div className="mb-2 flex items-start justify-between gap-2">
        <h3 className="font-semibold leading-snug">{tournament.name}</h3>
        <StatusBadge status={tournament.status} kind="tournament" />
      </div>
      <div className="flex flex-wrap items-center gap-2 text-xs text-zinc-500">
        <span
          className={`inline-flex rounded-full px-2 py-0.5 font-medium ring-1 ${tierStyles[tournament.tier]}`}
        >
          {tournament.tier.toLowerCase()}
        </span>
        {tournament.league && <span>{tournament.league.name}</span>}
        <span>{formatRange(tournament.startDate, tournament.endDate)}</span>
      </div>
    </Link>
  )
}
