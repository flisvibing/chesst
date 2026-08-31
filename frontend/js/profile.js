import { mountNav, renderFooter, api, authStore, requireAuth, toast, escapeHtml, formatDate } from './common.js';
mountNav('profile');
document.getElementById('footer-mount').innerHTML = renderFooter();
if (!requireAuth()) throw new Error('auth required');

async function load() {
  const root = document.getElementById('profile-root');
  try {
    const p = await api.get('/api/profile');
    const u = authStore.user();
    const initials = (u?.username || '?').slice(0, 2).toUpperCase();
    root.innerHTML = `
      <div class="card" style="margin-bottom:20px;">
        <div class="profile-header">
          <div class="avatar">${escapeHtml(initials)}</div>
          <div style="flex:1;">
            <h1 style="font-size:22px;">${escapeHtml(p.username)}</h1>
            ${p.displayName ? `<p style="color:var(--fg-soft);">${escapeHtml(p.displayName)}</p>` : ''}
            <p style="color:var(--fg-soft); font-size:13px;">
              Rating <strong>${p.rating}</strong> · Joined ${formatDate(u?.createdAt)}
              ${p.lichessUsername ? ` · Lichess: <a href="https://lichess.org/@/${escapeHtml(p.lichessUsername)}" target="_blank">${escapeHtml(p.lichessUsername)}</a>` : ''}
              ${p.chesscomUsername ? ` · Chess.com: <a href="https://chess.com/member/${escapeHtml(p.chesscomUsername)}" target="_blank">${escapeHtml(p.chesscomUsername)}</a>` : ''}
            </p>
            ${p.bio ? `<p style="margin-top:10px;">${escapeHtml(p.bio)}</p>` : ''}
          </div>
          <button class="btn btn-sm" id="edit-btn">Edit</button>
        </div>
        <div class="stats-row">
          <div class="stat-box"><div class="n">${p.gameCount}</div><div class="l">Games</div></div>
          <div class="stat-box"><div class="n">${p.rating}</div><div class="l">Rating</div></div>
          <div class="stat-box"><div class="n">${p.emailVerified ? '✓' : '✗'}</div><div class="l">Verified</div></div>
          <div class="stat-box"><div class="n">${p.lichessUsername || p.chesscomUsername ? '🔗' : '—'}</div><div class="l">Linked</div></div>
        </div>
      </div>
      <div class="card">
        <h3 class="card-title">Recent games</h3>
        <div id="recent-games"><div class="center-load"><span class="spinner"></span></div></div>
      </div>
    `;
    document.getElementById('edit-btn').addEventListener('click', openEdit);
    fetchRecent();
  } catch (e) {
    root.innerHTML = `<div class="empty-state">Failed to load profile: ${escapeHtml(e.message)}</div>`;
  }
}

async function fetchRecent() {
  try {
    const games = await api.get('/api/games');
    const host = document.getElementById('recent-games');
    if (!games.length) { host.innerHTML = '<div class="empty-state">No games yet. <a href="analysis.html">Analyze a game</a> to get started.</div>'; return; }
    host.innerHTML = games.slice(0, 10).map(g => `
      <div class="game-row">
        <div class="game-players">
          <div class="names">${escapeHtml(g.white)} ${escapeHtml(g.result)} ${escapeHtml(g.black)}</div>
          <div class="meta">${g.eco ? `<span class="badge">${escapeHtml(g.eco)}</span>` : ''} <span>${escapeHtml(g.openingName || '—')}</span> <span>${formatDate(g.createdAt)}</span></div>
        </div>
        <button class="btn btn-sm" data-pgn="${g.id}">Open</button>
      </div>
    `).join('');
    host.querySelectorAll('[data-pgn]').forEach(b => {
      b.addEventListener('click', () => {
        const id = Number(b.dataset.pgn);
        const g = games.find(x => x.id === id);
        if (g) { sessionStorage.setItem('chesst:loadPgn', g.pgn); window.location.href = 'analysis.html'; }
      });
    });
  } catch (e) { document.getElementById('recent-games').innerHTML = `<div class="empty-state">${escapeHtml(e.message)}</div>`; }
}

function openEdit() {
  const modal = document.createElement('div');
  modal.className = 'modal-backdrop';
  modal.style.display = 'flex';
  modal.innerHTML = `
    <div class="modal">
      <div class="modal-head"><h3>Edit profile</h3><button class="close-btn">×</button></div>
      <div class="modal-body">
        <div class="form-group"><label>Display name</label><input class="input" id="ed-name"></div>
        <div class="form-group"><label>Bio</label><textarea class="textarea" id="ed-bio" rows="3" maxlength="500"></textarea></div>
        <div class="form-group"><label>Avatar URL</label><input class="input" id="ed-avatar"></div>
      </div>
      <div class="modal-foot">
        <button class="btn" id="ed-cancel">Cancel</button>
        <button class="btn btn-primary" id="ed-save">Save</button>
      </div>
    </div>
  `;
  document.body.appendChild(modal);
  modal.querySelector('.close-btn').addEventListener('click', () => modal.remove());
  modal.querySelector('#ed-cancel').addEventListener('click', () => modal.remove());
  modal.addEventListener('click', (e) => { if (e.target === modal) modal.remove(); });
  api.get('/api/profile').then(p => {
    modal.querySelector('#ed-name').value = p.displayName || '';
    modal.querySelector('#ed-bio').value = p.bio || '';
    modal.querySelector('#ed-avatar').value = p.avatarUrl || '';
  });
  modal.querySelector('#ed-save').addEventListener('click', async () => {
    try {
      await api.patch('/api/profile', {
        displayName: modal.querySelector('#ed-name').value,
        bio: modal.querySelector('#ed-bio').value,
        avatarUrl: modal.querySelector('#ed-avatar').value,
      });
      toast('Profile updated.', 'success');
      modal.remove();
      load();
    } catch (e) { toast(e.message, 'error'); }
  });
}

load();
