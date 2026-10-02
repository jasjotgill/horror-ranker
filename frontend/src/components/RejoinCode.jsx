import CopyButton from './CopyButton'

// The caller's personal way back in, shown only to them.
export default function RejoinCode({ group }) {
  return (
    <section className="card stack">
      <h2>Your rejoin code</h2>
      <div className="code-line">
        <span className="rejoin-code">{group.you.rejoinCode}</span>
        <CopyButton text={group.you.rejoinCode} label="Copy rejoin code" />
      </div>
      <p className="muted small-text">
        Save this. If you lose your place or switch phone or browser, choose Rejoin on the home screen and enter
        group code {group.code} with this code. Rejoining logs out wherever you were before. Keep it to yourself.
      </p>
    </section>
  )
}
