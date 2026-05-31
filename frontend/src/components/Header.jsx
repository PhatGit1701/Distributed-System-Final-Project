export default function Header() {
  return (
    <header>
      <div className="logo-section">
        <h1>ROWA-A Distributed Database Visualizer</h1>
        <p>
          ROWA-A · eventual consistency · hinted handoff — không linearizable.
          Read One / Latest / Quorum + read repair.
        </p>
      </div>
      <div className="badge-mode">Active Status Running</div>
    </header>
  );
}
