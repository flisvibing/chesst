import { mountNav, renderFooter, api, authStore, toast, requireAuth, escapeHtml, formatDate } from './common.js';
mountNav('archive');
document.getElementById('footer-mount').innerHTML = renderFooter();
if (!requireAuth()) throw new Error('auth required');

let allGames = [];

async function fetchGames() {
  const list = document.getElementById('game-list');
  list.innerHTML = '<div class="center-load"><span class="spinner"></span> Loading…</div>';
  try {
    allGames = await api.get('/api/games');
    renderList();
  } catch (e) {
    list.innerHTML = `<div class="empty-state">Failed to load: ${escapeHtml(e.message)}</div>`;
  }
}

function renderList() {
  const list = document.getElementById('game-list');
  const q = document.getElementById('search').value.toLowerCase();
  const fc = document.getElementById('filter-color').value;
  const fr = document.getElementById('filter-result').value;
  let items = allGames;
  if (q) items = items.filter(g =>
    (g.white + ' ' + g.black + ' ' + (g.event || '') + ' ' + (g.openingName || '')).toLowerCase().includes(q)
  );
  const uname = (authStore.user()?.username || '').toLowerCase();
  if (fc === 'white') items = items.filter(g => (g.white || '').toLowerCase().includes(uname));
  if (fc === 'black') items = items.filter(g => (g.black || '').toLowerCase().includes(uname));
  if (fr) items = items.filter(g => g.result === fr);
  document.getElementById('archive-sub').textContent = `${items.length} saved game${items.length !== 1 ? 's' : ''}.`;
  if (!items.length) {
    list.innerHTML = '<div class="empty-state"><div class="icon">📁</div>No games yet. <a href="analysis.html">Analyze a game</a> and save it.</div>';
    return;
  }
  list.innerHTML = items.map(g => {
    const isWhite = (g.white || '').toLowerCase().includes(uname);
    const isBlack = (g.black || '').toLowerCase().includes(uname);
    let outcome = '—', cls = 'draw';
    if (g.result === '1-0') { outcome = isWhite ? 'W' : isBlack ? 'L' : '1'; cls = isWhite ? 'win' : isBlack ? 'loss' : 'draw'; }
    else if (g.result === '0-1') { outcome = isBlack ? 'W' : isWhite ? 'L' : '0'; cls = isBlack ? 'win' : isWhite ? 'loss' : 'draw'; }
    else if (g.result === '1/2-1/2') { outcome = 'D'; cls = 'draw'; }
    return `
      <div class="game-row" data-id="${g.id}">
        <div class="game-result ${cls}">${outcome}</div>
        <div class="game-players">
          <div class="names">${escapeHtml(g.white)} vs ${escapeHtml(g.black)}</div>
          <div class="meta">
            ${g.eco ? `<span class="badge">${escapeHtml(g.eco)}</span>` : ''}
            <span>${escapeHtml(g.openingName || '—')}</span>
            <span>${g.moveCount} plies</span>
            <span>${formatDate(g.createdAt)}</span>
            <span class="badge">${escapeHtml(g.source)}</span>
          </div>
        </div>
        <button class="btn btn-sm btn-ghost" data-act="analyze">Analyze</button>
        <button class="btn btn-sm btn-ghost btn-danger" data-act="delete">Delete</button>
      </div>
    `;
  }).join('');
  list.querySelectorAll('.game-row').forEach(row => {
    const id = Number(row.dataset.id);
    row.querySelector('[data-act="analyze"]').addEventListener('click', (e) => {
      e.stopPropagation();
      const g = allGames.find(x => x.id === id);
      if (g) { sessionStorage.setItem('chesst:loadPgn', g.pgn); window.location.href = 'analysis.html'; }
    });
    row.querySelector('[data-act="delete"]').addEventListener('click', async (e) => {
      e.stopPropagation();
      if (!confirm('Delete this game?')) return;
      try {
        await api.del('/api/games/' + id);
        allGames = allGames.filter(x => x.id !== id);
        renderList();
        toast('Game deleted.', 'success');
      } catch (err) { toast(err.message, 'error'); }
    });
  });
}

let timer = null;
document.getElementById('search').addEventListener('input', () => { clearTimeout(timer); timer = setTimeout(renderList, 200); });
document.getElementById('filter-color').addEventListener('change', renderList);
document.getElementById('filter-result').addEventListener('change', renderList);
fetchGames();
