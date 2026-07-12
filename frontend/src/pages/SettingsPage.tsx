import { useState } from 'react'
import { useFollowGames, useFollowLeagues, useFollowTeams, useGames, useLeagues } from '../api/queries'
import { useAuth } from '../auth/AuthContext'
import type { GameResponse, LeagueResponse, TournamentTier } from '../api/types'
import { ErrorMessage, Loading } from '../components/QueryState'

/**
 * Follow model: games are just a UI grouping (they decide which league sections
 * show here); the feed itself is driven only by followed leagues and teams.
 */
export function SettingsPage() {
  const { user } = useAuth()
  const games = useGames()
  const followGames = useFollowGames()
  const followLeagues = useFollowLeagues()
  const followTeams = useFollowTeams()

  if (!user || games.isPending) return <Loading />
  if (games.isError) return <ErrorMessage error={games.error} />

  const followedSlugs = user.followedGames.map((g) => g.slug)
  const followedLeagueIds = user.followedLeagues.map((l) => l.id)

  const toggleGame = (slug: string) => {
    const next = followedSlugs.includes(slug)
      ? followedSlugs.filter((s) => s !== slug)
      : [...followedSlugs, slug]
    followGames.mutate(next)
  }

  const toggleLeague = (leagueId: string) => {
    const next = followedLeagueIds.includes(leagueId)
      ? followedLeagueIds.filter((id) => id !== leagueId)
      : [...followedLeagueIds, leagueId]
    followLeagues.mutate(next)
  }

  const unfollowTeam = (teamId: string) => {
    followTeams.mutate(user.followedTeams.map((t) => t.id).filter((id) => id !== teamId))
  }

  return (
    <div className="mx-auto max-w-2xl space-y-10">
      <h1 className="text-2xl font-bold">Following</h1>

      <section>
        <h2 className="mb-1 text-lg font-semibold text-zinc-300">Games</h2>
        <p className="mb-4 text-sm text-zinc-500">
          Pick the games you care about, then choose leagues below. Your feed only shows
          leagues and teams you follow — never a whole game.
        </p>
        <div className="space-y-2">
          {games.data.map((game) => {
            const followed = followedSlugs.includes(game.slug)
            return (
              <label
                key={game.slug}
                className="flex cursor-pointer items-center justify-between rounded-lg border border-zinc-800 bg-zinc-900/60 px-4 py-3 transition-colors hover:border-zinc-700"
              >
                <span className="font-medium">{game.name}</span>
                <input
                  type="checkbox"
                  checked={followed}
                  disabled={followGames.isPending}
                  onChange={() => toggleGame(game.slug)}
                  className="h-4 w-4 accent-violet-500"
                />
              </label>
            )
          })}
        </div>
        {followGames.isError && (
          <div className="mt-3">
            <ErrorMessage error={followGames.error} />
          </div>
        )}
      </section>

      {user.followedGames.map((game) => (
        <LeagueSection
          key={game.slug}
          game={game}
          followedLeagueIds={followedLeagueIds}
          onToggle={toggleLeague}
          disabled={followLeagues.isPending}
        />
      ))}
      {followLeagues.isError && <ErrorMessage error={followLeagues.error} />}

      <section>
        <h2 className="mb-1 text-lg font-semibold text-zinc-300">Teams</h2>
        <p className="mb-4 text-sm text-zinc-500">
          Follow teams by starring them on any match card or standings table. Followed teams
          appear in your feed even when you don't follow their league.
        </p>
        {user.followedTeams.length === 0 ? (
          <p className="text-sm text-zinc-500">Not following any teams yet.</p>
        ) : (
          <ul className="space-y-2">
            {user.followedTeams.map((team) => (
              <li
                key={team.id}
                className="flex items-center justify-between rounded-lg border border-zinc-800 bg-zinc-900/60 px-4 py-3"
              >
                <span className="flex items-center gap-2">
                  {team.logoUrl && (
                    <img src={team.logoUrl} alt="" className="h-6 w-6 object-contain" />
                  )}
                  <span className="font-medium">{team.name}</span>
                </span>
                <button
                  onClick={() => unfollowTeam(team.id)}
                  disabled={followTeams.isPending}
                  className="text-sm text-zinc-500 transition-colors hover:text-red-400 disabled:opacity-50"
                >
                  Unfollow
                </button>
              </li>
            ))}
          </ul>
        )}
        {followTeams.isError && (
          <div className="mt-3">
            <ErrorMessage error={followTeams.error} />
          </div>
        )}
      </section>
    </div>
  )
}

// Display order and labels for the tier groups ("SECONDARY" reads as national leagues).
const TIER_GROUPS: { tier: TournamentTier; label: string; description: string }[] = [
  { tier: 'INTERNATIONAL', label: 'International', description: 'Cross-region events — Worlds, MSI, ALGS Playoffs' },
  { tier: 'PRIMARY', label: 'Primary leagues', description: 'The major regional leagues — LEC, LCK, ALGS regions' },
  { tier: 'SECONDARY', label: 'National leagues', description: 'Domestic and development leagues' },
]

function LeagueSection({
  game,
  followedLeagueIds,
  onToggle,
  disabled,
}: {
  game: GameResponse
  followedLeagueIds: string[]
  onToggle: (leagueId: string) => void
  disabled: boolean
}) {
  const leagues = useLeagues(game.slug)

  return (
    <section>
      <h2 className="mb-1 text-lg font-semibold text-zinc-300">{game.name} leagues</h2>
      <p className="mb-4 text-sm text-zinc-500">
        Follow a league to get all of its tournaments and matches in your feed.
      </p>
      {leagues.isPending ? (
        <Loading />
      ) : leagues.isError ? (
        <ErrorMessage error={leagues.error} />
      ) : leagues.data.length === 0 ? (
        <p className="text-sm text-zinc-500">No leagues synced for {game.name} yet.</p>
      ) : (
        <div className="space-y-3">
          {TIER_GROUPS.map(({ tier, label, description }) => (
            <TierGroup
              key={tier}
              label={label}
              description={description}
              leagues={leagues.data.filter((l) => l.tier === tier)}
              followedLeagueIds={followedLeagueIds}
              onToggle={onToggle}
              disabled={disabled}
            />
          ))}
        </div>
      )}
    </section>
  )
}

function TierGroup({
  label,
  description,
  leagues,
  followedLeagueIds,
  onToggle,
  disabled,
}: {
  label: string
  description: string
  leagues: LeagueResponse[]
  followedLeagueIds: string[]
  onToggle: (leagueId: string) => void
  disabled: boolean
}) {
  const [open, setOpen] = useState(false)
  if (leagues.length === 0) return null
  const followedCount = leagues.filter((l) => followedLeagueIds.includes(l.id)).length

  return (
    <div className="rounded-lg border border-zinc-800 bg-zinc-900/60">
      <button
        type="button"
        onClick={() => setOpen(!open)}
        aria-expanded={open}
        className="flex w-full items-center justify-between px-4 py-3 text-left transition-colors hover:bg-zinc-800/40"
      >
        <span>
          <span className="font-medium">{label}</span>
          <span className="ml-2 text-xs text-zinc-500">{description}</span>
        </span>
        <span className="flex shrink-0 items-center gap-3">
          <span className="text-xs text-zinc-500">
            {followedCount > 0 && (
              <span className="mr-2 rounded-full bg-violet-500/15 px-2 py-0.5 text-violet-300">
                {followedCount} followed
              </span>
            )}
            {leagues.length} {leagues.length === 1 ? 'league' : 'leagues'}
          </span>
          <span
            className={`text-zinc-500 transition-transform ${open ? 'rotate-90' : ''}`}
            aria-hidden
          >
            ▸
          </span>
        </span>
      </button>
      {open && (
        <div className="grid gap-2 border-t border-zinc-800 p-3 sm:grid-cols-2">
          {leagues.map((league) => {
            const followed = followedLeagueIds.includes(league.id)
            return (
              <label
                key={league.id}
                className={`flex cursor-pointer items-center justify-between rounded-lg border px-3 py-2 text-sm transition-colors ${
                  followed
                    ? 'border-violet-700/60 bg-violet-950/30'
                    : 'border-zinc-800 bg-zinc-900/60 hover:border-zinc-700'
                }`}
              >
                <span>
                  <span className="font-medium">{league.name}</span>
                  {league.region && (
                    <span className="ml-2 text-xs text-zinc-500">{league.region}</span>
                  )}
                </span>
                <input
                  type="checkbox"
                  checked={followed}
                  disabled={disabled}
                  onChange={() => onToggle(league.id)}
                  className="h-4 w-4 accent-violet-500"
                />
              </label>
            )
          })}
        </div>
      )}
    </div>
  )
}
