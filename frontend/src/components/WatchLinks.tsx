import { getWatchChannels } from '../api/leagueChannels'

function TwitchIcon() {
  return (
    <svg viewBox="0 0 24 24" className="h-5 w-5" fill="currentColor">
      <path d="M4.5 2 3 5.5V19h5v3l3-3h3.5L20 14V2H4.5zm14 11-3 3h-3.5l-2.5 2.5V16H6V4h12.5v9z" />
      <path d="M15 7h1.8v4.5H15V7zM10.5 7h1.8v4.5h-1.8V7z" />
    </svg>
  )
}

function YoutubeIcon() {
  return (
    <svg viewBox="0 0 24 24" className="h-5 w-5">
      <path
        fill="currentColor"
        d="M22 12s0-3.2-.4-4.7a3 3 0 0 0-2.1-2.1C17.9 4.8 12 4.8 12 4.8s-5.9 0-7.5.4A3 3 0 0 0 2.4 7.3C2 8.8 2 12 2 12s0 3.2.4 4.7a3 3 0 0 0 2.1 2.1c1.6.4 7.5.4 7.5.4s5.9 0 7.5-.4a3 3 0 0 0 2.1-2.1C22 15.2 22 12 22 12z"
      />
      <path fill="#18181b" d="M10 15.5V8.5L16 12z" />
    </svg>
  )
}

function TrophyIcon() {
  return (
    <svg viewBox="0 0 24 24" className="h-5 w-5" fill="currentColor">
      <path d="M6 3h12v2h2.5a.5.5 0 0 1 .5.5V7a4 4 0 0 1-4 4h-.35A6 6 0 0 1 13 15.9V18h3v2H8v-2h3v-2.1A6 6 0 0 1 7.85 11H7.5a4 4 0 0 1-4-4V5.5a.5.5 0 0 1 .5-.5H6V3zm0 4H5.5v.5A2 2 0 0 0 7 9.35 5.98 5.98 0 0 1 6 6.28V7zm12 0v-.72a5.98 5.98 0 0 1-1 3.07A2 2 0 0 0 18.5 7.5V7H18z" />
    </svg>
  )
}

/**
 * Riot doesn't publish a per-match stream URL until the broadcast is essentially live (see
 * leagueChannels.ts), so this points to the league's known channel instead. Twitch only shows up
 * when we have a verified handle for that league (a wrong guess is worse than no link); YouTube
 * and watch.lolesports.com always show, since both are real, stable, league-agnostic channels.
 * Shown for UPCOMING/RUNNING matches only; a finished match has no live broadcast to link to.
 */
export function WatchLinks({ leagueSlug }: { leagueSlug: string | null | undefined }) {
  const { twitch, youtube, website } = getWatchChannels(leagueSlug)
  return (
    <div className="flex flex-wrap gap-3">
      {twitch && (
        <a
          href={twitch}
          target="_blank"
          rel="noreferrer"
          className="flex items-center gap-2 rounded-lg bg-[#9146FF] px-4 py-2.5 text-sm font-semibold text-white transition-opacity hover:opacity-90"
        >
          <TwitchIcon />
          Watch live on Twitch
        </a>
      )}
      <a
        href={youtube}
        target="_blank"
        rel="noreferrer"
        className="flex items-center gap-2 rounded-lg bg-[#FF0000] px-4 py-2.5 text-sm font-semibold text-white transition-opacity hover:opacity-90"
      >
        <YoutubeIcon />
        Watch on YouTube
      </a>
      <a
        href={website}
        target="_blank"
        rel="noreferrer"
        className="flex items-center gap-2 rounded-lg bg-[#C89B3C] px-4 py-2.5 text-sm font-semibold text-zinc-950 transition-opacity hover:opacity-90"
      >
        <TrophyIcon />
        Watch on lolesports to earn rewards
      </a>
    </div>
  )
}
