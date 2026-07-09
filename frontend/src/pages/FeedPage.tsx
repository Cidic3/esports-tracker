import { Link } from 'react-router-dom'
import { useFeed } from '../api/queries'
import { useAuth } from '../auth/AuthContext'
import { MatchCard } from '../components/MatchCard'
import { TournamentCard } from '../components/TournamentCard'
import { EmptyState, ErrorMessage, Loading } from '../components/QueryState'

export function FeedPage() {
  const { user } = useAuth()
  const feed = useFeed()

  if (feed.isPending) return <Loading />
  if (feed.isError) return <ErrorMessage error={feed.error} />

  const { upcomingMatches, runningTournaments } = feed.data
  // Games alone don't drive the feed — only leagues and teams do.
  const followsNothing =
    user && user.followedLeagues.length === 0 && user.followedTeams.length === 0

  return (
    <div className="space-y-10">
      <h1 className="text-2xl font-bold">Your feed</h1>

      {followsNothing && (
        <div className="rounded-lg border border-violet-800/50 bg-violet-950/30 px-4 py-3 text-sm text-violet-200">
          Your feed is empty until you follow something.{' '}
          <Link to="/settings" className="font-medium underline hover:text-white">
            Pick your leagues
          </Link>{' '}
          (like LEC or MSI) or star teams on any match — only what you choose shows up here.
        </div>
      )}

      <section>
        <h2 className="mb-4 text-lg font-semibold text-zinc-300">Running tournaments</h2>
        {runningTournaments.length === 0 ? (
          <EmptyState message="No tournaments running for what you follow." />
        ) : (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {runningTournaments.map((t) => (
              <TournamentCard key={t.id} tournament={t} />
            ))}
          </div>
        )}
      </section>

      <section>
        <h2 className="mb-4 text-lg font-semibold text-zinc-300">Upcoming matches</h2>
        {upcomingMatches.length === 0 ? (
          <EmptyState message="No upcoming matches for what you follow." />
        ) : (
          <div className="grid gap-4 sm:grid-cols-2">
            {upcomingMatches.map((m) => (
              <MatchCard key={m.id} match={m} />
            ))}
          </div>
        )}
      </section>
    </div>
  )
}
