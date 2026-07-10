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

// Team browser search result - unlike TeamResponse (embedded in Match/Standing, where the game is
// already contextual), a global team search can span games, so the game is included here.
export interface TeamSummaryResponse {
  id: string
  name: string
  slug: string
  logoUrl: string | null
  gameSlug: string
}

export type PlayerRole = 'TOP' | 'JUNGLE' | 'MID' | 'BOTTOM' | 'SUPPORT' | 'NONE'

// active is a heuristic (recent-match participation), not an authoritative starter/sub flag —
// see the backend's PlayerResponse javadoc.
export interface PlayerResponse {
  id: string
  summonerName: string
  firstName: string | null
  lastName: string | null
  imageUrl: string | null
  role: PlayerRole
  active: boolean
}

export interface OrganizationResponse {
  id: string
  name: string
  slug: string
  logoUrl: string | null
}

export interface TeamDetailResponse {
  id: string
  name: string
  slug: string
  logoUrl: string | null
  gameSlug: string
  organization: OrganizationResponse | null
  roster: PlayerResponse[]
  standings: StandingResponse[]
  liveMatches: MatchResponse[]
  recentMatches: MatchResponse[]
  upcomingMatches: MatchResponse[]
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
  leagueSlug: string
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
  tournamentName: string
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
  liveMatches: MatchResponse[]
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
