import { Component, type ErrorInfo, type ReactNode } from 'react'

interface Props {
  children: ReactNode
}

interface State {
  error: Error | null
}

/**
 * Catches render errors in whatever it wraps so one broken page doesn't blank the whole app -
 * only class components can be error boundaries (no hook equivalent exists in React yet).
 * Scoped around <Outlet/> in Layout rather than the whole tree, so the header/nav (and the
 * "back to safety" link) stay usable even when the active page's render throws.
 */
export class ErrorBoundary extends Component<Props, State> {
  state: State = { error: null }

  static getDerivedStateFromError(error: Error): State {
    return { error }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('Unhandled render error:', error, info.componentStack)
  }

  render() {
    if (this.state.error) {
      return (
        <div className="flex flex-col items-center gap-3 rounded-lg border border-zinc-800 bg-zinc-900/50 px-6 py-16 text-center">
          <p className="text-lg font-semibold text-zinc-100">Something went wrong</p>
          <p className="max-w-md text-sm text-zinc-400">
            This page hit an unexpected error. Try reloading, or head back to the feed.
          </p>
          <button
            onClick={() => this.setState({ error: null })}
            className="mt-2 rounded-md bg-violet-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-violet-500"
          >
            Try again
          </button>
        </div>
      )
    }
    return this.props.children
  }
}
