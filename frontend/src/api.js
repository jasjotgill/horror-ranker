// Every call to the backend goes through this file.

const SESSION_KEY = 'horror-ranker-session'

// The session is { code, nickname, token }. It lives in localStorage so a refresh,
// or closing and reopening the tab, does not log anyone out.
export function loadSession() {
  try {
    return JSON.parse(localStorage.getItem(SESSION_KEY))
  } catch {
    return null
  }
}

export function saveSession(session) {
  localStorage.setItem(SESSION_KEY, JSON.stringify(session))
}

export function clearSession() {
  localStorage.removeItem(SESSION_KEY)
}

export class ApiError extends Error {
  constructor(status, message, fields = {}) {
    super(message)
    this.status = status
    this.fields = fields
  }
}

async function request(method, path, { token, body, signal } = {}) {
  const headers = {}
  if (token) headers['X-Member-Token'] = token
  if (body) headers['Content-Type'] = 'application/json'

  let response
  try {
    response = await fetch(path, { method, headers, signal, body: body ? JSON.stringify(body) : undefined })
  } catch (error) {
    // A cancelled request is not a failure; let the caller recognise and ignore it.
    if (error.name === 'AbortError') throw error
    throw new ApiError(0, 'Could not reach the server. Check your connection.')
  }
  if (response.status === 204) return null

  // The backend sends errors as { status, message, fields }.
  const data = await response.json().catch(() => null)
  if (!response.ok) {
    const fieldMessages = Object.entries(data?.fields ?? {}).map(([field, problem]) => `${field} ${problem}`)
    const message = fieldMessages.length > 0 ? fieldMessages.join(', ') : (data?.message ?? 'Something went wrong')
    throw new ApiError(response.status, message, data?.fields)
  }
  return data
}

const group = (session) => `/api/groups/${encodeURIComponent(session.code)}`

export const api = {
  createGroup: (name, nickname) => request('POST', '/api/groups', { body: { name, nickname } }),
  joinGroup: (code, nickname) =>
    request('POST', `/api/groups/${encodeURIComponent(code)}/members`, { body: { nickname } }),
  rejoinGroup: (code, rejoinCode) =>
    request('POST', `/api/groups/${encodeURIComponent(code)}/rejoin`, { body: { rejoinCode } }),
  leave: (session) => request('POST', `${group(session)}/leave`, session),
  kick: (session, nickname) => request('POST', `${group(session)}/kick`, { token: session.token, body: { nickname } }),
  state: (session) => request('GET', group(session), session),
  search: (session, query, signal) =>
    request('GET', `/api/movies/search?q=${encodeURIComponent(query)}`, { token: session.token, signal }),
  pick: (session, pick) => request('POST', `${group(session)}/picks`, { token: session.token, body: pick }),
  seen: (session, pickId) => request('POST', `/api/picks/${pickId}/seen`, session),
  unseen: (session, pickId) => request('DELETE', `/api/picks/${pickId}/seen`, session),
  start: (session) => request('POST', `${group(session)}/start`, session),
  rate: (session, pickId, scores) =>
    request('PUT', `/api/picks/${pickId}/rating`, { token: session.token, body: scores }),
  giveTicket: (session, nickname) =>
    request('PUT', `${group(session)}/ticket`, { token: session.token, body: { nickname } }),
  finish: (session) => request('POST', `${group(session)}/finish`, session),
  results: (session) => request('GET', `${group(session)}/results`, session),
}
