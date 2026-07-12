// Riot's schedule API only exposes a match's stream URL once the broadcast is essentially live
// (confirmed by direct probing — even a match ~21h out returns an empty `streams` array), and
// league catalog data (getLeagues) has no channel info at all. So Twitch links here are a small,
// deliberately conservative curated map — only leagues with a genuinely well-known, stable handle
// are listed (an earlier "twitch.tv/lolesports" fallback for unlisted leagues was wrong — that
// channel doesn't exist — so unlisted leagues get no Twitch link rather than a guess).
//
// YouTube is different: Riot's official "lolesports" channel (youtube.com/@lolesports) is a real,
// stable channel that simulcasts virtually every league, so it's used as a universal fallback
// rather than needing a per-league override.
//
// watch.lolesports.com is Riot's own site (and the only place that pays out watch rewards), so
// it's always offered as a third option regardless of league.

export interface WatchChannels {
  twitch?: string
  youtube: string
  website: string
}

const OFFICIAL_SITE = 'https://watch.lolesports.com'
const OFFICIAL_YOUTUBE = 'https://youtube.com/@lolesports'

// Twitch only, for leagues with a verified/well-known stable handle. Expanded 2026-07-10 via
// direct web research per league (not guessed) - each entry below is that league's own primary
// broadcast channel, confirmed independently rather than assumed from a naming pattern. Several
// leagues in our DB were deliberately left out because research didn't turn up a channel that's
// unambiguously *that* league's own (as opposed to a shared/regional Riot channel covering several
// leagues, or a successor league's channel that may have absorbed the old one) - see the list below
// the map. Guessing here has bitten us before (see file history), so "no confirmed channel" beats
// a plausible-looking one.
const LEAGUE_TWITCH: Record<string, string> = {
  lec: 'https://twitch.tv/lec',
  lck: 'https://twitch.tv/lck',
  lcs: 'https://twitch.tv/lcs',
  lpl: 'https://twitch.tv/lpl',
  'cblol-brazil': 'https://twitch.tv/cblol',
  'ljl-japan': 'https://twitch.tv/leagueoflegendsjp',
  lla: 'https://twitch.tv/lla',
  lco: 'https://twitch.tv/lco',
  primeleague: 'https://twitch.tv/primeleague',
  vcs: 'https://twitch.tv/vcsenglish',
  // LCP (region "PACIFIC") is the 2024+ tier-1 successor covering the Pacific broadly; PCS
  // (region "HONG KONG, MACAU, TAIWAN") is now a tier-2 feeder into it, and no PCS-specific
  // channel distinct from LCP's turned up in research - so only lcp gets a link, not pcs.
  lcp: 'https://twitch.tv/lolpacificen',
  // International events stream on Riot's own channel, not a regional one.
  msi: 'https://twitch.tv/riotgames',
  worlds: 'https://twitch.tv/riotgames',
}

// Researched but deliberately NOT added, since no channel found was confidently *that* league's
// own rather than a shared/ambiguous one:
// - pcs (see lcp comment above)
// - turkiye-sampiyonluk-ligi (TCL): only found a general "Riot Games Turkish" channel, not
//   confirmed as TCL-specific
// - lcl: league has been inactive since the 2022 Russian invasion of Ukraine disrupted it; no
//   current channel found

export function getWatchChannels(leagueSlug: string | null | undefined): WatchChannels {
  const twitch = leagueSlug ? LEAGUE_TWITCH[leagueSlug] : undefined
  return { twitch, youtube: OFFICIAL_YOUTUBE, website: OFFICIAL_SITE }
}
