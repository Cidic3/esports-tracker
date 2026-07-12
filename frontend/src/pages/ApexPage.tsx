import { useState } from 'react'
import { useApexMatchDays, useLeagues } from '../api/queries'
import type { EventStatus } from '../api/types'
import { ApexMatchDayCard } from '../components/ApexMatchDayCard'
import { Pagination } from '../components/Pagination'
import { EmptyState, ErrorMessage, Loading } from '../components/QueryState'

const select =
  'rounded-md border border-zinc-700 bg-zinc-900 px-3 py-1.5 text-sm text-zinc-300 focus:border-violet-500 focus:outline-none'

export function ApexPage() {
  const [league, setLeague] = useState('')
  const [status, setStatus] = useState<EventStatus | ''>('UPCOMING')
  const [page, setPage] = useState(0)

  const leagues = useLeagues('apex-legends')
  const days = useApexMatchDays({
    league: league || undefined,
    status: status || undefined,
    page,
  })

  return (
    <div>
      <div className="mb-6 flex flex-wrap items-center gap-3">
        <h1 className="mr-auto text-2xl font-bold">ALGS match days</h1>
        <select
          className={select}
          value={league}
          onChange={(e) => {
            setLeague(e.target.value)
            setPage(0)
          }}
        >
          <option value="">All regions</option>
          {leagues.data?.map((l) => (
            <option key={l.slug} value={l.slug}>
              {l.name}
            </option>
          ))}
        </select>
        <select
          className={select}
          value={status}
          onChange={(e) => {
            setStatus(e.target.value as EventStatus | '')
            setPage(0)
          }}
        >
          <option value="">Any status</option>
          <option value="UPCOMING">Upcoming</option>
          <option value="FINISHED">Finished</option>
        </select>
      </div>

      {days.isPending ? (
        <Loading />
      ) : days.isError ? (
        <ErrorMessage error={days.error} />
      ) : days.data.content.length === 0 ? (
        <EmptyState message="No match days found for these filters." />
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-2">
            {days.data.content.map((d) => (
              <ApexMatchDayCard key={d.id} day={d} />
            ))}
          </div>
          <Pagination page={days.data.page} totalPages={days.data.totalPages} onPageChange={setPage} />
        </>
      )}
    </div>
  )
}
