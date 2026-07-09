import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, qs } from './client'
import type {
  AuthResponse,
  EventStatus,
  FeedResponse,
  GameResponse,
  LeagueResponse,
  MatchDetailsResponse,
  MatchResponse,
  PagedResponse,
  StandingResponse,
  TournamentResponse,
  TournamentTier,
  UserResponse,
} from './types'

// ---- public data ----

export function useGames() {
  return useQuery({
    queryKey: ['games'],
    queryFn: () => api.get<GameResponse[]>('/api/games'),
    staleTime: Infinity, // two rows, seeded manually — never changes at runtime
  })
}

export function useLeagues(game?: string) {
  return useQuery({
    queryKey: ['leagues', game ?? 'all'],
    queryFn: () => api.get<LeagueResponse[]>('/api/leagues' + qs({ game })),
    staleTime: 6 * 60 * 60_000, // league catalog changes at sync cadence (6h), not per click
  })
}

export interface MatchFilters {
  game?: string
  team?: string
  status?: EventStatus
  from?: string
  to?: string
  page?: number
  size?: number
}

export function useMatches(filters: MatchFilters) {
  // Upcoming reads soonest-first; mixed/finished views read newest-first.
  const sort = filters.status === 'UPCOMING' ? 'scheduledAt,asc' : 'scheduledAt,desc'
  return useQuery({
    queryKey: ['matches', filters],
    queryFn: () =>
      api.get<PagedResponse<MatchResponse>>('/api/matches' + qs({ sort, ...filters })),
  })
}

export function useTodayMatches(page: number) {
  return useQuery({
    queryKey: ['matches', 'today', page],
    queryFn: () => api.get<PagedResponse<MatchResponse>>('/api/matches/today' + qs({ page })),
  })
}

export interface TournamentFilters {
  game?: string
  status?: EventStatus
  tier?: TournamentTier
  page?: number
  size?: number
}

export function useTournaments(filters: TournamentFilters) {
  return useQuery({
    queryKey: ['tournaments', filters],
    queryFn: () =>
      api.get<PagedResponse<TournamentResponse>>('/api/tournaments' + qs({ ...filters })),
  })
}

export function useTournament(id: string) {
  return useQuery({
    queryKey: ['tournaments', id],
    queryFn: () => api.get<TournamentResponse>(`/api/tournaments/${id}`),
  })
}

export function useTournamentMatches(id: string, page: number) {
  return useQuery({
    queryKey: ['tournaments', id, 'matches', page],
    queryFn: () =>
      api.get<PagedResponse<MatchResponse>>(
        `/api/tournaments/${id}/matches` + qs({ page, sort: 'scheduledAt,asc' }),
      ),
  })
}

export function useStandings(id: string) {
  return useQuery({
    queryKey: ['tournaments', id, 'standings'],
    queryFn: () => api.get<StandingResponse[]>(`/api/tournaments/${id}/standings`),
  })
}

export function useMatch(id: string) {
  return useQuery({
    queryKey: ['matches', id],
    queryFn: () => api.get<MatchResponse>(`/api/matches/${id}`),
  })
}

export function useMatchDetails(id: string) {
  return useQuery({
    queryKey: ['matches', id, 'details'],
    queryFn: () => api.get<MatchDetailsResponse>(`/api/matches/${id}/details`),
    // The backend walks Riot's frame feed for this (several upstream calls) — don't refire
    // on every focus; its own cache is 60s anyway.
    staleTime: 60_000,
  })
}

// Data Dragon: Riot's public static-asset CDN (champion/item images). Fetched directly
// from the browser — no reason to proxy static assets through our API.
export function useDdragonVersion() {
  return useQuery({
    queryKey: ['ddragon', 'version'],
    queryFn: async () => {
      const versions = await fetch('https://ddragon.leagueoflegends.com/api/versions.json').then(
        (r) => r.json() as Promise<string[]>,
      )
      return versions[0]
    },
    staleTime: Infinity,
  })
}

export function championIconUrl(version: string, champion: string): string {
  return `https://ddragon.leagueoflegends.com/cdn/${version}/img/champion/${champion}.png`
}

export function itemIconUrl(version: string, itemId: number): string {
  return `https://ddragon.leagueoflegends.com/cdn/${version}/img/item/${itemId}.png`
}

// ---- auth + personalized ----

export function useMe(enabled: boolean) {
  return useQuery({
    queryKey: ['me'],
    queryFn: () => api.get<UserResponse>('/api/users/me'),
    enabled,
    retry: false,
  })
}

export function useFeed() {
  return useQuery({
    queryKey: ['feed'],
    queryFn: () => api.get<FeedResponse>('/api/feed'),
  })
}

export function useUpcoming(page: number) {
  return useQuery({
    queryKey: ['matches', 'upcoming', page],
    queryFn: () => api.get<PagedResponse<MatchResponse>>('/api/matches/upcoming' + qs({ page })),
  })
}

export function useLogin() {
  return useMutation({
    mutationFn: (body: { username: string; password: string }) =>
      api.post<AuthResponse>('/api/auth/login', body),
  })
}

export function useRegister() {
  return useMutation({
    mutationFn: (body: { username: string; email: string; password: string }) =>
      api.post<UserResponse>('/api/auth/register', body),
  })
}

/**
 * Follow updates are full-replace on the backend (PUT the complete list),
 * so callers pass the entire desired state, not a delta. After a change,
 * everything derived from follows (profile, feed, upcoming) is stale.
 */
function useInvalidateFollows() {
  const queryClient = useQueryClient()
  return () => {
    queryClient.invalidateQueries({ queryKey: ['me'] })
    queryClient.invalidateQueries({ queryKey: ['feed'] })
    queryClient.invalidateQueries({ queryKey: ['matches', 'upcoming'] })
  }
}

export function useFollowGames() {
  const invalidate = useInvalidateFollows()
  return useMutation({
    mutationFn: (slugs: string[]) => api.put<UserResponse>('/api/users/me/games', { slugs }),
    onSuccess: invalidate,
  })
}

export function useFollowTeams() {
  const invalidate = useInvalidateFollows()
  return useMutation({
    mutationFn: (teamIds: string[]) => api.put<UserResponse>('/api/users/me/teams', { teamIds }),
    onSuccess: invalidate,
  })
}

export function useFollowLeagues() {
  const invalidate = useInvalidateFollows()
  return useMutation({
    mutationFn: (leagueIds: string[]) =>
      api.put<UserResponse>('/api/users/me/leagues', { leagueIds }),
    onSuccess: invalidate,
  })
}
