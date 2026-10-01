import { useState } from 'react'
import { api } from '../api'
import ErrorMessage from '../components/ErrorMessage'

// A shared link like https://host/?code=K7QX opens straight on the join form.
const codeFromUrl = new URLSearchParams(window.location.search).get('code') ?? ''

export default function Home({ onEnter }) {
  const [mode, setMode] = useState(codeFromUrl ? 'join' : 'create')
  const [groupName, setGroupName] = useState('')
  const [code, setCode] = useState(codeFromUrl.toUpperCase())
  const [nickname, setNickname] = useState('')
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  async function submit(event) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      const membership =
        mode === 'create' ? await api.createGroup(groupName, nickname) : await api.joinGroup(code.trim(), nickname)
      onEnter(membership)
    } catch (err) {
      setError(err.message)
      setBusy(false)
    }
  }

  return (
    <main>
      <div className="tabs" role="tablist">
        <button role="tab" aria-selected={mode === 'create'} onClick={() => setMode('create')}>
          Create a group
        </button>
        <button role="tab" aria-selected={mode === 'join'} onClick={() => setMode('join')}>
          Join a group
        </button>
      </div>

      <form className="card stack" onSubmit={submit}>
        {mode === 'create' ? (
          <label>
            Group name
            <input
              value={groupName}
              onChange={(event) => setGroupName(event.target.value)}
              maxLength={80}
              placeholder="Friday Frights"
              required
            />
          </label>
        ) : (
          <label>
            Join code
            <input
              className="code-input"
              value={code}
              onChange={(event) => setCode(event.target.value.toUpperCase())}
              maxLength={8}
              placeholder="K7QX"
              autoCapitalize="characters"
              autoComplete="off"
              required
            />
          </label>
        )}
        <label>
          Your nickname
          <input
            value={nickname}
            onChange={(event) => setNickname(event.target.value)}
            maxLength={30}
            autoComplete="nickname"
            required
          />
        </label>
        <ErrorMessage>{error}</ErrorMessage>
        <button className="primary" disabled={busy}>
          {mode === 'create' ? 'Create group' : 'Join group'}
        </button>
        <p className="muted small-text center">
          Use your normal browser, not a private tab. Closing a private tab logs you out for good.
        </p>
      </form>
      <p className="muted center">
        Everyone secretly picks one horror film nobody has seen. You watch them in a random order, rate each one, and
        the app ranks them.
      </p>
    </main>
  )
}
