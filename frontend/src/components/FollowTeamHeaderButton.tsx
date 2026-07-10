import { useState } from 'react'
import { useAuth } from '../auth/AuthContext'
import { useFollowTeams } from '../api/queries'
import type { TeamResponse } from '../api/types'

/**
 * Text-button follow toggle for a team's own page header (the star toggle on cards - see
 * FollowTeamButton - is too subtle for a primary page action). While already followed, hovering
 * swaps the label to "Unfollow" so the click's effect is obvious before it happens.
 */
export function FollowTeamHeaderButton({ team }: { team: TeamResponse }) {
  const { isAuthenticated, user } = useAuth()
  const followTeams = useFollowTeams()
  const [hovered, setHovered] = useState(false)

  if (!isAuthenticated || !user) return null

  const followedIds = user.followedTeams.map((t) => t.id)
  const isFollowed = followedIds.includes(team.id)

  const toggle = () => {
    const next = isFollowed
      ? followedIds.filter((id) => id !== team.id)
      : [...followedIds, team.id]
    followTeams.mutate(next)
  }

  const label = isFollowed ? (hovered ? 'Unfollow' : 'Followed') : 'Follow'

  return (
    <button
      onClick={toggle}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      disabled={followTeams.isPending}
      className={`rounded-md border px-4 py-1.5 text-sm font-medium transition-colors disabled:opacity-50 ${
        isFollowed
          ? hovered
            ? 'border-red-700 bg-red-950/30 text-red-300'
            : 'border-amber-600 bg-amber-950/20 text-amber-300'
          : 'border-violet-600 bg-violet-600 text-white hover:bg-violet-500'
      }`}
    >
      {label}
    </button>
  )
}
