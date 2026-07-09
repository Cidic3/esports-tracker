import { ApiError } from '../api/client'

export function Loading() {
  return (
    <div className="flex justify-center py-16">
      <div className="h-8 w-8 animate-spin rounded-full border-2 border-zinc-700 border-t-violet-500" />
    </div>
  )
}

export function ErrorMessage({ error }: { error: unknown }) {
  const message =
    error instanceof ApiError ? error.message : 'Something went wrong. Is the backend running?'
  return (
    <div className="rounded-lg border border-red-900/60 bg-red-950/40 px-4 py-3 text-sm text-red-300">
      {message}
    </div>
  )
}

export function EmptyState({ message }: { message: string }) {
  return <p className="py-12 text-center text-sm text-zinc-500">{message}</p>
}
