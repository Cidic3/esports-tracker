import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTeams } from '../api/queries'

const GAME_LABELS: Record<string, string> = {
  'league-of-legends': 'LoL',
  'dota-2': 'Dota 2',
}

export function TeamSearch() {
  const [query, setQuery] = useState('')
  const [debounced, setDebounced] = useState('')
  const [open, setOpen] = useState(false)
  const containerRef = useRef<HTMLDivElement>(null)
  const navigate = useNavigate()

  useEffect(() => {
    const timeout = setTimeout(() => setDebounced(query.trim()), 250)
    return () => clearTimeout(timeout)
  }, [query])

  useEffect(() => {
    function onClickOutside(e: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', onClickOutside)
    return () => document.removeEventListener('mousedown', onClickOutside)
  }, [])

  const results = useTeams({ search: debounced, size: 8 })
  const showDropdown = open && debounced.length > 0

  function goToTeam(id: string) {
    navigate(`/teams/${id}`)
    setQuery('')
    setDebounced('')
    setOpen(false)
  }

  function goToSearchPage() {
    const trimmed = query.trim()
    if (!trimmed) return
    navigate(`/teams?search=${encodeURIComponent(trimmed)}`)
    setQuery('')
    setDebounced('')
    setOpen(false)
  }

  return (
    <div ref={containerRef} className="relative w-56">
      <input
        type="search"
        value={query}
        onChange={(e) => {
          setQuery(e.target.value)
          setOpen(true)
        }}
        onFocus={() => setOpen(true)}
        onKeyDown={(e) => {
          if (e.key === 'Enter') goToSearchPage()
        }}
        placeholder="Search teams…"
        className="w-full rounded-md border border-zinc-700 bg-zinc-900 px-3 py-1.5 text-sm text-zinc-100 placeholder-zinc-500 focus:border-violet-500 focus:outline-none"
      />
      {showDropdown && (
        <div className="absolute right-0 z-20 mt-1 w-72 rounded-md border border-zinc-800 bg-zinc-900 shadow-xl">
          {results.isPending ? (
            <p className="px-3 py-3 text-sm text-zinc-500">Searching…</p>
          ) : results.isError ? (
            <p className="px-3 py-3 text-sm text-red-400">Search failed.</p>
          ) : results.data.content.length === 0 ? (
            <p className="px-3 py-3 text-sm text-zinc-500">No teams found for "{debounced}".</p>
          ) : (
            <ul className="max-h-80 overflow-y-auto py-1">
              {results.data.content.map((team) => (
                <li key={team.id}>
                  <button
                    onClick={() => goToTeam(team.id)}
                    className="flex w-full items-center gap-2 px-3 py-2 text-left text-sm hover:bg-zinc-800"
                  >
                    {team.logoUrl ? (
                      <img src={team.logoUrl} alt="" className="h-6 w-6 object-contain" />
                    ) : (
                      <div className="h-6 w-6 rounded bg-zinc-800" />
                    )}
                    <span className="flex-1 truncate">{team.name}</span>
                    <span className="text-xs text-zinc-500">
                      {GAME_LABELS[team.gameSlug] ?? team.gameSlug}
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  )
}
