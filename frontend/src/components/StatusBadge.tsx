import type { EventStatus } from '../api/types'

const styles: Record<EventStatus, string> = {
  UPCOMING: 'bg-sky-500/15 text-sky-300 ring-sky-500/30',
  RUNNING: 'bg-emerald-500/15 text-emerald-300 ring-emerald-500/30',
  FINISHED: 'bg-zinc-500/15 text-zinc-400 ring-zinc-500/30',
}

// A Match's RUNNING status is Riot's own "inProgress" state — a game is genuinely being played
// right now, so "LIVE" is accurate. A Tournament/League's RUNNING status just means today falls
// within its date range (see RiotSyncService.deriveStatus) — it says nothing about whether a game
// is on stream at this exact moment, so it gets a calmer "ongoing" label instead.
const matchLabels: Record<EventStatus, string> = {
  UPCOMING: 'upcoming',
  RUNNING: 'LIVE',
  FINISHED: 'finished',
}

const tournamentLabels: Record<EventStatus, string> = {
  UPCOMING: 'upcoming',
  RUNNING: 'ongoing',
  FINISHED: 'finished',
}

export function StatusBadge({
  status,
  kind = 'match',
}: {
  status: EventStatus
  kind?: 'match' | 'tournament'
}) {
  const labels = kind === 'tournament' ? tournamentLabels : matchLabels
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium ring-1 ${styles[status]}`}
    >
      {status === 'RUNNING' && kind === 'match' && (
        <span className="relative flex h-1.5 w-1.5">
          <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-75" />
          <span className="relative inline-flex h-1.5 w-1.5 rounded-full bg-emerald-400" />
        </span>
      )}
      {labels[status]}
    </span>
  )
}
