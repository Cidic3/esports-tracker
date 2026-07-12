import { useEffect } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, ApiError, qs } from './client'
import type {
  ApexMatchDayDetailResponse,
  ApexMatchDayResponse,
  AuthResponse,
  EventStatus,
  FeedResponse,
  GameResponse,
  LeagueResponse,
  MatchDetailsResponse,
  MatchResponse,
  PagedResponse,
  StandingResponse,
  TeamDetailResponse,
  TeamSummaryResponse,
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
    // Without this, a match card's countdown can hit "Starting now" and just sit there — the
    // backend only flips UPCOMING -> RUNNING on its own 15-min sync tick, and TanStack Query
    // otherwise only refetches on refocus/remount, so an open tab would show a stale status
    // indefinitely. Only polls while the tab has focus (default refetchIntervalInBackground: false).
    refetchInterval: 60_000,
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
      // Unlike the pure-"upcoming" match views (soonest-first makes sense there since every
      // result is in the future), this list mixes finished and upcoming matches across the whole
      // tournament — descending puts the next match and most recent results at the top instead of
      // burying them behind the tournament's opening-day matches.
      api.get<PagedResponse<MatchResponse>>(
        `/api/tournaments/${id}/matches` + qs({ page, sort: 'scheduledAt,desc' }),
      ),
  })
}

export function useStandings(id: string) {
  return useQuery({
    queryKey: ['tournaments', id, 'standings'],
    queryFn: () => api.get<StandingResponse[]>(`/api/tournaments/${id}/standings`),
  })
}

export interface TeamFilters {
  game?: string
  search?: string
  page?: number
  size?: number
}

export function useTeams(filters: TeamFilters) {
  return useQuery({
    queryKey: ['teams', filters],
    queryFn: () => api.get<PagedResponse<TeamSummaryResponse>>('/api/teams' + qs({ ...filters })),
    // The search dropdown re-fires this on every keystroke (debounced upstream); an empty
    // search string means "don't query yet" rather than "browse all teams".
    enabled: filters.search === undefined || filters.search.length > 0,
  })
}

/**
 * Visiting a team page kicks off a background sync of that team's league on the backend (fire-
 * and-forget, doesn't block the response - see TeamSyncTrigger). This renders instantly from
 * whatever's already cached/in the DB, then refetches once a few seconds later to pick up
 * anything the background sync found; `isFetching` while `!isPending` is that second, quiet
 * refetch, not the loading spinner.
 */
export function useTeamDetail(id: string) {
  const queryClient = useQueryClient()
  const query = useQuery({
    queryKey: ['teams', id],
    queryFn: () => api.get<TeamDetailResponse>(`/api/teams/${id}`),
  })

  useEffect(() => {
    const timeout = setTimeout(() => {
      queryClient.invalidateQueries({ queryKey: ['teams', id] })
    }, 4000)
    return () => clearTimeout(timeout)
  }, [id, queryClient])

  return query
}

export interface ApexMatchDayFilters {
  league?: string
  status?: EventStatus
  page?: number
  size?: number
}

export function useApexMatchDays(filters: ApexMatchDayFilters) {
  // Upcoming reads soonest-first; mixed/finished views read newest-first (same as useMatches).
  const sort = filters.status === 'UPCOMING' ? 'startsAt,asc' : 'startsAt,desc'
  return useQuery({
    queryKey: ['apex', 'matchdays', filters],
    queryFn: () =>
      api.get<PagedResponse<ApexMatchDayResponse>>('/api/apex/matchdays' + qs({ sort, ...filters })),
    refetchInterval: 60_000, // see useMatches — same "don't sit on a stale status" reasoning
  })
}

export function useApexMatchDay(id: string) {
  return useQuery({
    queryKey: ['apex', 'matchdays', id],
    queryFn: () => api.get<ApexMatchDayDetailResponse>(`/api/apex/matchdays/${id}`),
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
    refetchInterval: 60_000, // see useMatches — same "don't sit on a stale status" reasoning
  })
}

export function useUpcoming(page: number) {
  return useQuery({
    queryKey: ['matches', 'upcoming', page],
    queryFn: () => api.get<PagedResponse<MatchResponse>>('/api/matches/upcoming' + qs({ page })),
    refetchInterval: 60_000,
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

/**
 * The backend rejects a follow-update PUT with 409 if the submitted `version` doesn't match the
 * user's current one - i.e. this write was computed from a profile that's since changed elsewhere
 * (another tab, or a second toggle fired before an earlier one's response updated the cache). On a
 * 409 there's no good way to auto-merge the two intents, so we just refetch the real current state
 * (rather than leaving the UI showing what it optimistically assumed) and let the error surface so
 * the user knows to retry with fresh data.
 */
function useFollowMutationOptions() {
  const invalidate = useInvalidateFollows()
  const queryClient = useQueryClient()
  return {
    onSuccess: invalidate,
    onError: (error: unknown) => {
      if (error instanceof ApiError && error.status === 409) {
        queryClient.invalidateQueries({ queryKey: ['me'] })
      }
    },
  }
}

function currentUserVersion(queryClient: ReturnType<typeof useQueryClient>): number {
  return queryClient.getQueryData<UserResponse>(['me'])?.version ?? 0
}

export function useFollowGames() {
  const options = useFollowMutationOptions()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (slugs: string[]) =>
      api.put<UserResponse>('/api/users/me/games', { slugs, version: currentUserVersion(queryClient) }),
    ...options,
  })
}

export function useFollowTeams() {
  const options = useFollowMutationOptions()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (teamIds: string[]) =>
      api.put<UserResponse>('/api/users/me/teams', { teamIds, version: currentUserVersion(queryClient) }),
    ...options,
  })
}

export function useFollowLeagues() {
  const options = useFollowMutationOptions()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (leagueIds: string[]) =>
      api.put<UserResponse>('/api/users/me/leagues', {
        leagueIds,
        version: currentUserVersion(queryClient),
      }),
    ...options,
  })
}
