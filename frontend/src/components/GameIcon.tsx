import { useState } from 'react'

/**
 * Small per-game logo shown under the "vs"/day-name on match cards, so the game is identifiable
 * at a glance even faster than reading the LeagueChip text.
 *
 * Logo files are supplied locally rather than hotlinked: no game's publisher hosts a hotlinkable
 * "game logo" asset on any official CDN (confirmed by direct probing 2026-07-11 — Data Dragon,
 * Riot's own CDN already used elsewhere in this app for champion icons, only serves
 * champion/item/profile-icon assets, not a game logo; EA/Respawn has no public asset CDN at all
 * for Apex; Valve's Steam CDN does have a real Dota 2 image, but only as a wide promotional
 * banner, not a square icon). Expects small square PNG/SVG files at:
 *   frontend/public/logos/league-of-legends.png
 *   frontend/public/logos/dota-2.png
 *   frontend/public/logos/apex-legends.png
 * Falls back to an emoji placeholder if a file is missing (e.g. not added yet), so a card never
 * renders a broken-image icon while logos are still being sourced.
 */
const FALLBACK_ICONS: Record<string, string> = {
  'league-of-legends': '⚔️',
  'dota-2': '🐲',
  'apex-legends': '🪂',
}

const GAME_LABELS: Record<string, string> = {
  'league-of-legends': 'League of Legends',
  'dota-2': 'Dota 2',
  'apex-legends': 'Apex Legends',
}

export function GameIcon({ gameSlug }: { gameSlug: string }) {
  const [errored, setErrored] = useState(false)
  const fallback = FALLBACK_ICONS[gameSlug]

  if (errored || !fallback) {
    return fallback ? (
      <span className="text-2xl leading-none" aria-hidden>
        {fallback}
      </span>
    ) : null
  }

  return (
    <img
      src={`/logos/${gameSlug}.png`}
      alt={GAME_LABELS[gameSlug] ?? gameSlug}
      className="h-8 w-8 object-contain"
      onError={() => setErrored(true)}
    />
  )
}
