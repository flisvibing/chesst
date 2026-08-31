import { mountNav, renderFooter, api, authStore, requireAuth, toast, toggleTheme, escapeHtml } from './common.js';
mountNav('settings');
document.getElementById('footer-mount').innerHTML = renderFooter();
if (!requireAuth()) throw new Error('auth required');

api.get('/api/profile').then(p => {
  document.getElementById('account-info').innerHTML = `
    <div style="display:grid; grid-template-columns: max-content 1fr; gap:8px 16px; font-size:14px;">
      <span style="color:var(--fg-soft);">Username</span><strong>${escapeHtml(p.username)}</strong>
      <span style="color:var(--fg-soft);">Email</span><span>${escapeHtml(p.email)}</span>
      <span style="color:var(--fg-soft);">Verified</span><span>${p.emailVerified ? '✓ yes' : '✗ no'}</span>
      <span style="color:var(--fg-soft);">Rating</span><span>${p.rating}</span>
      <span style="color:var(--fg-soft);">Games saved</span><span>${p.gameCount}</span>
    </div>
  `;
}).catch(e => { document.getElementById('account-info').innerHTML = `<div class="empty-state">${escapeHtml(e.message)}</div>`; });

const integ = document.getElementById('integrations');
integ.innerHTML = `
  <div class="form-group">
    <label>Lichess username</label>
    <div style="display:flex; gap:8px;">
      <input class="input" id="li-user" placeholder="e.g. drnykterstein">
      <button class="btn btn-sm" id="li-import">Import games</button>
    </div>
    <div id="li-result" style="margin-top:8px; font-size:13px; color:var(--fg-soft);"></div>
  </div>
  <div class="form-group">
    <label>Chess.com username</label>
    <div style="display:flex; gap:8px;">
      <input class="input" id="cc-user" placeholder="e.g. hikaru">
      <button class="btn btn-sm" id="cc-import">Import games</button>
    </div>
    <div id="cc-result" style="margin-top:8px; font-size:13px; color:var(--fg-soft);"></div>
  </div>
`;

document.getElementById('li-import').addEventListener('click', async () => {
  const u = document.getElementById('li-user').value.trim();
  if (!u) return;
  const res = document.getElementById('li-result');
  res.innerHTML = '<span class="spinner"></span> Importing…';
  try {
    const r = await api.post(`/api/integrations/lichess/${encodeURIComponent(u)}/import?max=10`);
    res.innerHTML = `Imported <strong>${r.imported}</strong> of ${r.total} games. <a href="archive.html">View archive →</a>`;
    toast(`Imported ${r.imported} games from Lichess.`, 'success');
  } catch (e) { res.innerHTML = escapeHtml(e.message); }
});

document.getElementById('cc-import').addEventListener('click', async () => {
  const u = document.getElementById('cc-user').value.trim();
  if (!u) return;
  const res = document.getElementById('cc-result');
  res.innerHTML = '<span class="spinner"></span> Importing…';
  try {
    const r = await api.post(`/api/integrations/chesscom/${encodeURIComponent(u)}/import?max=10`);
    res.innerHTML = `Imported <strong>${r.imported}</strong> of ${r.total} games. <a href="archive.html">View archive →</a>`;
    toast(`Imported ${r.imported} games from Chess.com.`, 'success');
  } catch (e) { res.innerHTML = escapeHtml(e.message); }
});

const baseInput = document.getElementById('api-base');
baseInput.value = localStorage.getItem('chesst:apiBase') || 'https://chesst.onrender.com';
document.getElementById('save-api').addEventListener('click', () => {
  localStorage.setItem('chesst:apiBase', baseInput.value.trim().replace(/\/$/, ''));
  toast('API endpoint saved. Reloading…', 'success');
  setTimeout(() => window.location.reload(), 600);
});

document.getElementById('toggle-theme').addEventListener('click', toggleTheme);
