import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useLogin } from '../api/queries'
import { useAuth } from '../auth/AuthContext'
import { ErrorMessage } from '../components/QueryState'

export function LoginPage() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const login = useLogin()
  const auth = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault()
    login.mutate(
      { username, password },
      {
        onSuccess: ({ token }) => {
          auth.login(token)
          navigate((location.state as { from?: string } | null)?.from ?? '/', { replace: true })
        },
      },
    )
  }

  const input =
    'w-full rounded-md border border-zinc-700 bg-zinc-900 px-3 py-2 text-sm placeholder-zinc-500 focus:border-violet-500 focus:outline-none'

  return (
    <div className="mx-auto max-w-sm pt-12">
      <h1 className="mb-6 text-2xl font-bold">Log in</h1>
      <form onSubmit={handleSubmit} className="space-y-4">
        <input
          className={input}
          placeholder="Username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          autoFocus
          required
        />
        <input
          className={input}
          type="password"
          placeholder="Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />
        {login.isError && <ErrorMessage error={login.error} />}
        <button
          type="submit"
          disabled={login.isPending}
          className="w-full rounded-md bg-violet-600 py-2 text-sm font-medium text-white transition-colors hover:bg-violet-500 disabled:opacity-50"
        >
          {login.isPending ? 'Logging in…' : 'Log in'}
        </button>
      </form>
      <p className="mt-4 text-sm text-zinc-500">
        No account?{' '}
        <Link to="/register" className="text-violet-400 hover:text-violet-300">
          Sign up
        </Link>
      </p>
    </div>
  )
}
