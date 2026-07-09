import type { EventStatus } from '../api/types'

const styles: Record<EventStatus, string> = {
  UPCOMING: 'bg-sky-500/15 text-sky-300 ring-sky-500/30',
  RUNNING: 'bg-emerald-500/15 text-emerald-300 ring-emerald-500/30',
  FINISHED: 'bg-zinc-500/15 text-zinc-400 ring-zinc-500/30',
}

export function StatusBadge({ status }: { status: EventStatus }) {
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium ring-1 ${styles[status]}`}
    >
      {status === 'RUNNING' && (
        <span className="relative flex h-1.5 w-1.5">
          <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-75" />
          <span className="relative inline-flex h-1.5 w-1.5 rounded-full bg-emerald-400" />
        </span>
      )}
      {status.toLowerCase()}
    </span>
  )
}
