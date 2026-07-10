import { Link } from 'react-router-dom'
import type { TeamSummaryResponse } from '../api/types'

const GAME_LABELS: Record<string, string> = {
  'league-of-legends': 'League of Legends',
  'dota-2': 'Dota 2',
}

export function TeamCard({ team }: { team: TeamSummaryResponse }) {
  return (
    <Link
      to={`/teams/${team.id}`}
      className="flex items-center gap-3 rounded-lg border border-zinc-800 bg-zinc-900/60 p-4 transition-colors hover:border-violet-700/60"
    >
      {team.logoUrl ? (
        <img src={team.logoUrl} alt="" className="h-12 w-12 shrink-0 object-contain" />
      ) : (
        <div className="h-12 w-12 shrink-0 rounded bg-zinc-800" />
      )}
      <div className="min-w-0">
        <p className="truncate font-semibold">{team.name}</p>
        <p className="truncate text-xs text-zinc-500">
          {GAME_LABELS[team.gameSlug] ?? team.gameSlug}
        </p>
      </div>
    </Link>
  )
}
