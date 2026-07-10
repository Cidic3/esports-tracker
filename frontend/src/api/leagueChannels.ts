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

// Twitch only, for leagues with a verified/well-known stable handle.
const LEAGUE_TWITCH: Record<string, string> = {
  lec: 'https://twitch.tv/lec',
  lck: 'https://twitch.tv/lck',
  lcs: 'https://twitch.tv/lcs',
  // International events stream on Riot's own channel, not a regional one.
  msi: 'https://twitch.tv/riotgames',
  worlds: 'https://twitch.tv/riotgames',
}

export function getWatchChannels(leagueSlug: string | null | undefined): WatchChannels {
  const twitch = leagueSlug ? LEAGUE_TWITCH[leagueSlug] : undefined
  return { twitch, youtube: OFFICIAL_YOUTUBE, website: OFFICIAL_SITE }
}
