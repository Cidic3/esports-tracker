import { useAuth } from '../auth/AuthContext'
import { useFollowTeams } from '../api/queries'
import type { TeamResponse } from '../api/types'

/**
 * Star toggle shown next to team names. The backend uses full-replace
 * semantics for follows, so toggling means sending the complete new list.
 */
export function FollowTeamButton({ team }: { team: TeamResponse }) {
  const { isAuthenticated, user } = useAuth()
  const followTeams = useFollowTeams()

  if (!isAuthenticated || !user) return null

  const followedIds = user.followedTeams.map((t) => t.id)
  const isFollowed = followedIds.includes(team.id)

  const toggle = () => {
    const next = isFollowed
      ? followedIds.filter((id) => id !== team.id)
      : [...followedIds, team.id]
    followTeams.mutate(next)
  }

  return (
    <button
      onClick={(e) => {
        e.preventDefault() // cards are often wrapped in links
        e.stopPropagation()
        toggle()
      }}
      disabled={followTeams.isPending}
      title={isFollowed ? `Unfollow ${team.name}` : `Follow ${team.name}`}
      className={`text-xl leading-none transition-colors disabled:opacity-50 ${
        isFollowed ? 'text-amber-400 hover:text-amber-300' : 'text-zinc-600 hover:text-zinc-300'
      }`}
    >
      {isFollowed ? '★' : '☆'}
    </button>
  )
}
