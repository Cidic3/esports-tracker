import { useEffect, useState } from 'react'

export function formatCountdown(targetIso: string): string {
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
 *
 * Shared by MatchCard and ApexMatchDayCard — "Starting now" only means the scheduled time has
 * passed, not that the backend has actually flipped status yet (that lags behind by up to a
 * sync-cycle); see the refetchInterval on useFeed/useMatches/useApexMatchDays for the other half
 * of keeping this from looking stuck once a match/day actually goes live or finishes.
 */
export function useCountdown(targetIso: string, enabled: boolean): string {
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
