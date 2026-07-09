import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useLogin, useRegister } from '../api/queries'
import { useAuth } from '../auth/AuthContext'
import { ErrorMessage } from '../components/QueryState'

export function RegisterPage() {
  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const register = useRegister()
  const login = useLogin()
  const auth = useAuth()
  const navigate = useNavigate()

  // Register doesn't return a token, so we chain a login right after —
  // one click, straight into the app.
  const handleSubmit = (e: FormEvent) => {
    e.preventDefault()
    register.mutate(
      { username, email, password },
      {
        onSuccess: () =>
          login.mutate(
            { username, password },
            {
              onSuccess: ({ token }) => {
                auth.login(token)
                navigate('/', { replace: true })
              },
            },
          ),
      },
    )
  }

  const pending = register.isPending || login.isPending
  const error = register.isError ? register.error : login.isError ? login.error : null

  const input =
    'w-full rounded-md border border-zinc-700 bg-zinc-900 px-3 py-2 text-sm placeholder-zinc-500 focus:border-violet-500 focus:outline-none'

  return (
    <div className="mx-auto max-w-sm pt-12">
      <h1 className="mb-6 text-2xl font-bold">Create account</h1>
      <form onSubmit={handleSubmit} className="space-y-4">
        <input
          className={input}
          placeholder="Username (3–30 characters)"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          minLength={3}
          maxLength={30}
          autoFocus
          required
        />
        <input
          className={input}
          type="email"
          placeholder="Email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
        <input
          className={input}
          type="password"
          placeholder="Password (min. 8 characters)"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          minLength={8}
          required
        />
        {error && <ErrorMessage error={error} />}
        <button
          type="submit"
          disabled={pending}
          className="w-full rounded-md bg-violet-600 py-2 text-sm font-medium text-white transition-colors hover:bg-violet-500 disabled:opacity-50"
        >
          {pending ? 'Creating…' : 'Sign up'}
        </button>
      </form>
      <p className="mt-4 text-sm text-zinc-500">
        Already registered?{' '}
        <Link to="/login" className="text-violet-400 hover:text-violet-300">
          Log in
        </Link>
      </p>
    </div>
  )
}
