import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import {
  championIconUrl,
  itemIconUrl,
  useDdragonVersion,
  useMatch,
  useMatchDetails,
} from '../api/queries'
import type { GameDetails, PlayerGameDetails, TeamGameDetails } from '../api/types'
import { StatusBadge } from '../components/StatusBadge'
import { EmptyState, ErrorMessage, Loading } from '../components/QueryState'

const ROLE_ORDER = ['top', 'jungle', 'mid', 'bottom', 'support']

function sortByRole(players: PlayerGameDetails[]): PlayerGameDetails[] {
  return [...players].sort((a, b) => ROLE_ORDER.indexOf(a.role) - ROLE_ORDER.indexOf(b.role))
}

function ItemIcons({ items, version }: { items: number[]; version?: string }) {
  if (!version) return null
  // 0 = empty slot; trinkets/duplicates render fine, broken ids just hide themselves
  return (
    <div className="flex gap-0.5">
      {items
        .filter((id) => id > 0)
        .map((id, i) => (
          <img
            key={`${id}-${i}`}
            src={itemIconUrl(version, id)}
            alt={`Item ${id}`}
            className="h-5 w-5 rounded-sm"
            onError={(e) => ((e.target as HTMLImageElement).style.display = 'none')}
          />
        ))}
    </div>
  )
}

function TeamPanel({
  team,
  side,
  version,
}: {
  team: TeamGameDetails
  side: 'blue' | 'red'
  version?: string
}) {
  const sideColor = side === 'blue' ? 'text-sky-400' : 'text-red-400'
  return (
    <div className="rounded-lg border border-zinc-800 bg-zinc-900/60">
      <div className="flex flex-wrap items-center justify-between gap-2 border-b border-zinc-800 px-4 py-3">
        <span className="font-semibold">
          <span className={`mr-2 text-xs font-medium uppercase ${sideColor}`}>{side}</span>
          {team.name ?? 'Unknown team'}
        </span>
        <span className="flex gap-3 text-xs text-zinc-400">
          <span title="Kills">⚔ {team.totalKills}</span>
          <span title="Gold">🪙 {(team.totalGold / 1000).toFixed(1)}k</span>
          <span title="Towers">🏰 {team.towers}</span>
          <span title="Barons">🐲 {team.barons}</span>
          <span title="Dragons">{team.dragons.length} drakes</span>
        </span>
      </div>
      <table className="w-full text-sm">
        <tbody className="divide-y divide-zinc-800/70">
          {sortByRole(team.players).map((p) => (
            <tr key={p.summonerName}>
              <td className="py-2 pl-4">
                <span className="flex items-center gap-2">
                  {version && (
                    <img
                      src={championIconUrl(version, p.champion)}
                      alt={p.champion}
                      title={p.champion}
                      className="h-8 w-8 rounded"
                      onError={(e) => ((e.target as HTMLImageElement).style.display = 'none')}
                    />
                  )}
                  <span className="flex flex-col">
                    <span className="font-medium leading-tight">{p.summonerName}</span>
                    <span className="text-xs leading-tight text-zinc-500">
                      {p.champion} · {p.role}
                    </span>
                  </span>
                </span>
              </td>
              <td className="px-2 text-center tabular-nums">
                <span className="text-zinc-300">
                  {p.kills}/{p.deaths}/{p.assists}
                </span>
                <div className="text-xs text-zinc-500">KDA</div>
              </td>
              <td className="px-2 text-center tabular-nums">
                <span className="text-zinc-300">{p.creepScore}</span>
                <div className="text-xs text-zinc-500">CS</div>
              </td>
              <td className="px-2 text-center tabular-nums">
                <span className="text-zinc-300">{(p.gold / 1000).toFixed(1)}k</span>
                <div className="text-xs text-zinc-500">gold</div>
              </td>
              <td className="py-2 pr-4">
                <ItemIcons items={p.items} version={version} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function GamePanel({ game, version }: { game: GameDetails; version?: string }) {
  if (game.state === 'unstarted') {
    return <EmptyState message={`Game ${game.number} hasn't started yet.`} />
  }
  if (!game.blueTeam || !game.redTeam) {
    return <EmptyState message={`No stats coverage available for game ${game.number}.`} />
  }
  return (
    <div className="grid gap-4 lg:grid-cols-2">
      <TeamPanel team={game.blueTeam} side="blue" version={version} />
      <TeamPanel team={game.redTeam} side="red" version={version} />
    </div>
  )
}

export function MatchDetailPage() {
  const { id } = useParams<{ id: string }>()
  const match = useMatch(id!)
  const details = useMatchDetails(id!)
  const ddragon = useDdragonVersion()
  const [selectedGame, setSelectedGame] = useState(0)

  if (match.isPending) return <Loading />
  if (match.isError) return <ErrorMessage error={match.error} />

  const m = match.data
  const finishedOrLive = m.status !== 'UPCOMING'
  const games = details.data?.games ?? []
  const current = games[selectedGame]

  return (
    <div className="space-y-8">
      <div>
        <Link
          to={`/tournaments/${m.tournamentId}`}
          className="text-xs text-zinc-500 hover:text-violet-400"
        >
          {m.tournamentName}
        </Link>
        <div className="mt-1 flex flex-wrap items-center gap-4">
          <h1 className="text-2xl font-bold">
            {m.teamA?.name ?? 'TBD'}
            <span className="mx-3 text-zinc-500">
              {finishedOrLive ? `${m.scoreA ?? 0} : ${m.scoreB ?? 0}` : 'vs'}
            </span>
            {m.teamB?.name ?? 'TBD'}
          </h1>
          <StatusBadge status={m.status} />
        </div>
        <p className="mt-1 text-sm text-zinc-500">
          {new Date(m.scheduledAt).toLocaleString(undefined, {
            weekday: 'long',
            month: 'long',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
          })}
        </p>
      </div>

      {!finishedOrLive ? (
        <EmptyState message="Game stats will appear here once the match starts." />
      ) : details.isPending ? (
        <Loading />
      ) : details.isError ? (
        <ErrorMessage error={details.error} />
      ) : games.length === 0 ? (
        <EmptyState message="No per-game data available for this match." />
      ) : (
        <div>
          <div className="mb-4 flex gap-2">
            {games.map((g, i) => (
              <button
                key={g.number}
                onClick={() => setSelectedGame(i)}
                className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
                  i === selectedGame
                    ? 'bg-violet-600 text-white'
                    : 'border border-zinc-700 text-zinc-400 hover:border-zinc-500 hover:text-zinc-200'
                }`}
              >
                Game {g.number}
              </button>
            ))}
          </div>
          {current && <GamePanel game={current} version={ddragon.data} />}
        </div>
      )}
    </div>
  )
}
