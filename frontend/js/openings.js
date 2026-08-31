import { mountNav, renderFooter, api, escapeHtml } from './common.js';
mountNav('openings');
document.getElementById('footer-mount').innerHTML = renderFooter();

const VOLUMES = [
  { key: '', label: 'All' },
  { key: 'A', label: 'A · Flank' },
  { key: 'B', label: 'B · Semi-open' },
  { key: 'C', label: 'C · Open' },
  { key: 'D', label: 'D · Closed' },
  { key: 'E', label: 'E · Indian' },
];
let currentVol = '';
let currentQ = '';
let selected = null;

function renderVolumeTabs() {
  const host = document.getElementById('volume-tabs');
  host.innerHTML = VOLUMES.map(v =>
    `<button class="volume-tab ${currentVol === v.key ? 'active' : ''}" data-vol="${v.key}">${v.label}</button>`
  ).join('');
  host.querySelectorAll('.volume-tab').forEach(b => {
    b.addEventListener('click', () => { currentVol = b.dataset.vol; renderVolumeTabs(); fetchOpenings(); });
  });
}

let searchTimer = null;
document.getElementById('search').addEventListener('input', (e) => {
  currentQ = e.target.value.trim();
  clearTimeout(searchTimer);
  searchTimer = setTimeout(fetchOpenings, 250);
});

async function fetchOpenings() {
  const list = document.getElementById('opening-list');
  list.innerHTML = '<div class="center-load"><span class="spinner"></span> Loading…</div>';
  try {
    const params = new URLSearchParams();
    if (currentQ) params.set('q', currentQ);
    if (currentVol) params.set('eco', currentVol);
    params.set('size', '60');
    const res = await api.get('/api/openings?' + params.toString(), { auth: false });
    renderList(res.content || []);
  } catch (e) {
    list.innerHTML = `<div class="empty-state">Failed to load: ${escapeHtml(e.message)}</div>`;
  }
}

function renderList(items) {
  const list = document.getElementById('opening-list');
  if (!items.length) { list.innerHTML = '<div class="empty-state">No openings found.</div>'; return; }
  list.innerHTML = items.map(o => `
    <div class="opening-row ${selected && selected.id === o.id ? 'selected' : ''}" data-id="${o.id}">
      <span class="opening-eco">${escapeHtml(o.eco)}</span>
      <span class="opening-name">${escapeHtml(o.name)}</span>
      ${winbar(o)}
    </div>
  `).join('');
  list.querySelectorAll('.opening-row').forEach(r => {
    r.addEventListener('click', () => {
      const id = Number(r.dataset.id);
      selected = items.find(x => x.id === id);
      renderList(items);
      renderDetail(selected);
    });
  });
}

function winbar(o) {
  const t = o.whiteWins + o.draws + o.blackWins || 1;
  const wp = (o.whiteWins / t) * 100, dp = (o.draws / t) * 100, bp = (o.blackWins / t) * 100;
  return `<div class="winbar"><div class="w" style="width:${wp}%"></div><div class="d" style="width:${dp}%"></div><div class="b" style="width:${bp}%"></div></div>`;
}

function renderDetail(o) {
  const t = o.whiteWins + o.draws + o.blackWins || 1;
  const wp = (o.whiteWins / t) * 100, dp = (o.draws / t) * 100, bp = (o.blackWins / t) * 100;
  const host = document.getElementById('opening-detail');
  host.innerHTML = `
    <div class="card">
      <span class="opening-eco" style="display:inline-block; margin-bottom:6px;">${escapeHtml(o.eco)}</span>
      <h3 style="font-size:18px; margin-bottom:14px;">${escapeHtml(o.name)}</h3>
      <div style="margin-bottom:14px;">
        <div style="font-size:11px; text-transform:uppercase; color:var(--fg-soft); margin-bottom:4px;">Moves</div>
        <div style="font-family:var(--mono); font-size:14px;">${escapeHtml(o.pgn)}</div>
      </div>
      <div>
        <div style="font-size:11px; text-transform:uppercase; color:var(--fg-soft); margin-bottom:6px;">Community results</div>
        ${winbar(o)}
        <div style="display:flex; justify-content:space-between; margin-top:6px; font-size:12px; color:var(--fg-soft);">
          <span>${wp.toFixed(0)}% White</span><span>${dp.toFixed(0)}% Draw</span><span>${bp.toFixed(0)}% Black</span>
        </div>
      </div>
      <a href="analysis.html" class="btn btn-primary" id="study-btn" style="width:100%; margin-top:16px;">Study in analysis</a>
    </div>
  `;
  document.getElementById('study-btn').addEventListener('click', () => {
    sessionStorage.setItem('chesst:loadPgn', o.pgn);
  });
}

renderVolumeTabs();
fetchOpenings();
