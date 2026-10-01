// A film poster, or a plain placeholder card when there is none
// (manual picks without a poster URL, and the odd TMDB film).
export default function Poster({ url, title }) {
  if (!url) {
    return (
      <div className="poster placeholder" aria-hidden="true">
        {title.slice(0, 1).toUpperCase()}
      </div>
    )
  }
  return <img className="poster" src={url} alt="" loading="lazy" />
}
