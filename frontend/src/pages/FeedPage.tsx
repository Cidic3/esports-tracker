import { Link } from 'react-router-dom'
import { useFeed } from '../api/queries'
import { useAuth } from '../auth/AuthContext'
import { ApexMatchDayCard } from '../components/ApexMatchDayCard'
import { MatchCard } from '../components/MatchCard'
import { TournamentCard } from '../components/TournamentCard'
import { EmptyState, ErrorMessage, Loading } from '../components/QueryState'

export function FeedPage() {
  const { user } = useAuth()
  const feed = useFeed()

  if (feed.isPending) return <Loading />
  if (feed.isError) return <ErrorMessage error={feed.error} />

  const { liveMatches, upcomingMatches, runningTournaments, apexMatchDays } = feed.data

  // One combined "upcoming" list across all followed games, ordered purely by start time —
  // the LeagueChip on each card is what identifies the game, not separate sections.
  const upcoming = [
    ...upcomingMatches.map((m) => ({ kind: 'match' as const, time: m.scheduledAt, match: m })),
    ...apexMatchDays.map((d) => ({ kind: 'apex' as const, time: d.startsAt, day: d })),
  ].sort((a, b) => new Date(a.time).getTime() - new Date(b.time).getTime())
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

      {liveMatches.length > 0 && (
        <section>
          <h2 className="mb-4 text-lg font-semibold text-zinc-300">Live now</h2>
          <div className="grid gap-4 sm:grid-cols-2">
            {liveMatches.map((m) => (
              <MatchCard key={m.id} match={m} />
            ))}
          </div>
        </section>
      )}

      <section>
        <h2 className="mb-4 text-lg font-semibold text-zinc-300">Ongoing tournaments</h2>
        {runningTournaments.length === 0 ? (
          <EmptyState message="No tournaments ongoing for what you follow." />
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
        {upcoming.length === 0 ? (
          <EmptyState message="No upcoming matches for what you follow." />
        ) : (
          <div className="grid gap-4 sm:grid-cols-2">
            {upcoming.map((item) =>
              item.kind === 'match' ? (
                <MatchCard key={item.match.id} match={item.match} />
              ) : (
                <ApexMatchDayCard key={item.day.id} day={item.day} />
              ),
            )}
          </div>
        )}
      </section>
    </div>
  )
}
