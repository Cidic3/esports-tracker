// Mirrors the backend DTOs in model/dto — keep in sync by hand.
// Java Instant/LocalDate serialize as ISO-8601 strings; UUID as string.

export type EventStatus = 'UPCOMING' | 'RUNNING' | 'FINISHED'
export type TournamentTier = 'INTERNATIONAL' | 'PRIMARY' | 'SECONDARY'

export interface GameResponse {
  id: string
  name: string
  slug: string
  iconUrl: string | null
}

export interface TeamResponse {
  id: string
  name: string
  slug: string
  logoUrl: string | null
}

export interface LeagueResponse {
  id: string
  name: string
  slug: string
  region: string | null
  tier: TournamentTier
}

export interface MatchResponse {
  id: string
  scheduledAt: string
  status: EventStatus
  scoreA: number | null
  scoreB: number | null
  streamUrl: string | null
  teamA: TeamResponse | null
  teamB: TeamResponse | null
  tournamentId: string
  tournamentName: string
  gameSlug: string
}

export interface TournamentResponse {
  id: string
  name: string
  slug: string
  tier: TournamentTier
  status: EventStatus
  startDate: string | null
  endDate: string | null
  prizePool: string | null
  gameSlug: string
  league: LeagueResponse | null
}

export interface StandingResponse {
  groupName: string
  rank: number
  wins: number
  losses: number
  team: TeamResponse
}

export interface UserResponse {
  id: string
  username: string
  email: string
  createdAt: string
  followedGames: GameResponse[]
  followedLeagues: LeagueResponse[]
  followedTeams: TeamResponse[]
}

export interface FeedResponse {
  upcomingMatches: MatchResponse[]
  runningTournaments: TournamentResponse[]
}

export interface AuthResponse {
  token: string
}

export interface PlayerGameDetails {
  summonerName: string
  champion: string
  role: string
  level: number
  kills: number
  deaths: number
  assists: number
  creepScore: number
  gold: number
  items: number[]
}

export interface TeamGameDetails {
  teamId: string | null
  name: string | null
  totalKills: number
  totalGold: number
  towers: number
  barons: number
  dragons: string[]
  players: PlayerGameDetails[]
}

export interface GameDetails {
  number: number
  state: string // Riot vocabulary: completed | inProgress | unstarted
  blueTeam: TeamGameDetails | null
  redTeam: TeamGameDetails | null
}

export interface MatchDetailsResponse {
  matchId: string
  games: GameDetails[]
}

export interface PagedResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}
