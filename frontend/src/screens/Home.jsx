import { useState } from 'react'
import { api } from '../api'
import ErrorMessage from '../components/ErrorMessage'

// A shared link like https://host/?code=K7QX opens straight on the join form.
const codeFromUrl = new URLSearchParams(window.location.search).get('code') ?? ''

const BUTTON_LABEL = { create: 'Create group', join: 'Join group', rejoin: 'Rejoin group' }

export default function Home({ onEnter, notice }) {
  const [mode, setMode] = useState(codeFromUrl ? 'join' : 'create')
  const [groupName, setGroupName] = useState('')
  const [code, setCode] = useState(codeFromUrl.toUpperCase())
  const [nickname, setNickname] = useState('')
  const [rejoinCode, setRejoinCode] = useState('')
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  function switchTo(newMode) {
    setMode(newMode)
    setError(null)
  }

  async function submit(event) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      let membership
      if (mode === 'create') membership = await api.createGroup(groupName, nickname)
      else if (mode === 'join') membership = await api.joinGroup(code.trim(), nickname)
      else membership = await api.rejoinGroup(code.trim(), rejoinCode.trim())
      onEnter(membership)
    } catch (err) {
      setError(err.message)
      setBusy(false)
    }
  }

  return (
    <main>
      {notice && <p className="banner warning">{notice}</p>}

      <div className="tabs" role="tablist">
        <button role="tab" aria-selected={mode === 'create'} onClick={() => switchTo('create')}>
          Create
        </button>
        <button role="tab" aria-selected={mode === 'join'} onClick={() => switchTo('join')}>
          Join
        </button>
        <button role="tab" aria-selected={mode === 'rejoin'} onClick={() => switchTo('rejoin')}>
          Rejoin
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
              placeholder="e.g. Friday Frights"
              required
            />
          </label>
        ) : (
          <label>
            Group code
            <input
              className="code-input"
              value={code}
              onChange={(event) => setCode(event.target.value.toUpperCase())}
              maxLength={8}
              placeholder="e.g. K7QX"
              autoCapitalize="characters"
              autoComplete="off"
              required
            />
          </label>
        )}
        {mode === 'rejoin' ? (
          <label>
            Your rejoin code
            <input
              className="code-input"
              value={rejoinCode}
              onChange={(event) => setRejoinCode(event.target.value.toUpperCase())}
              maxLength={8}
              autoCapitalize="characters"
              autoComplete="off"
              required
            />
          </label>
        ) : (
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
        )}
        <ErrorMessage>{error}</ErrorMessage>
        <button className="primary" disabled={busy}>
          {BUTTON_LABEL[mode]}
        </button>
        <p className="muted small-text center">
          {mode === 'rejoin'
            ? 'Already in a group but lost your place, or on a new phone or browser? Your rejoin code brings you back as yourself and logs out the old one.'
            : 'Once you are in, save the rejoin code shown at the bottom of the screen. It gets you back in from any phone or browser.'}
        </p>
      </form>
      <p className="muted center">
        Everyone secretly picks one horror film nobody has seen. You watch them in a random order, rate each one, and
        the app ranks them.
      </p>
    </main>
  )
}
