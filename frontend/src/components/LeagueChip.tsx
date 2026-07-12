/**
 * Small colored league tag whose color identifies the game at a glance (violet = LoL,
 * red = Apex/ALGS) — the "which game is this?" signal on mixed-game lists like the feed.
 * A text chip rather than a logo because league logo URLs aren't synced (Riot's getLeagues
 * exposes them but we don't store them yet; Cito has none at all).
 */
const GAME_STYLES: Record<string, string> = {
  'league-of-legends': 'bg-violet-950/50 text-violet-300',
  'apex-legends': 'bg-red-950/40 text-red-300',
  'dota-2': 'bg-sky-950/50 text-sky-300',
}

export function LeagueChip({ gameSlug, label }: { gameSlug: string; label: string }) {
  const style = GAME_STYLES[gameSlug] ?? 'bg-zinc-800 text-zinc-300'
  return (
    <span className={`shrink-0 rounded px-1.5 py-0.5 text-[10px] font-semibold uppercase tracking-wide ${style}`}>
      {label}
    </span>
  )
}
