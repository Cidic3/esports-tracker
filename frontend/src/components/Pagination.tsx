interface PaginationProps {
  page: number
  totalPages: number
  onPageChange: (page: number) => void
}

export function Pagination({ page, totalPages, onPageChange }: PaginationProps) {
  if (totalPages <= 1) return null
  const btn =
    'rounded-md border border-zinc-700 px-3 py-1.5 text-sm text-zinc-300 transition-colors hover:border-zinc-500 hover:text-white disabled:opacity-40 disabled:hover:border-zinc-700'
  return (
    <div className="mt-6 flex items-center justify-center gap-4">
      <button className={btn} disabled={page === 0} onClick={() => onPageChange(page - 1)}>
        ← Prev
      </button>
      <span className="text-sm text-zinc-500">
        Page {page + 1} of {totalPages}
      </span>
      <button
        className={btn}
        disabled={page >= totalPages - 1}
        onClick={() => onPageChange(page + 1)}
      >
        Next →
      </button>
    </div>
  )
}
