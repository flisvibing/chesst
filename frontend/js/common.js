// Chesst frontend — shared utilities, API client, auth.
const API_BASE = (function () {
  const stored = localStorage.getItem('chesst:apiBase');
  if (stored) return stored.replace(/\/$/, '');
  return 'https://chesst.onrender.com';
})();

const TOKEN_KEY = 'chesst:token';
const USER_KEY = 'chesst:user';

export const api = {
  base: API_BASE,
  async request(path, { method = 'GET', body, auth = true } = {}) {
    const headers = { 'Content-Type': 'application/json' };
    if (auth) {
      const token = authStore.token();
      if (token) headers['Authorization'] = 'Bearer ' + token;
    }
    const opts = { method, headers };
    if (body !== undefined) opts.body = JSON.stringify(body);
    let res;
    try { res = await fetch(API_BASE + path, opts); }
    catch (e) { throw new ApiError('Network error: cannot reach server.', 0); }
    let data = null;
    const text = await res.text();
    if (text) { try { data = JSON.parse(text); } catch { data = text; } }
    if (!res.ok) {
      const msg = (data && data.message) ? data.message : ('Request failed (' + res.status + ')');
      throw new ApiError(msg, res.status, data);
    }
    return data;
  },
  get(path, opts) { return this.request(path, { ...opts, method: 'GET' }); },
  post(path, body, opts) { return this.request(path, { ...opts, method: 'POST', body }); },
  patch(path, body, opts) { return this.request(path, { ...opts, method: 'PATCH', body }); },
  del(path, opts) { return this.request(path, { ...opts, method: 'DELETE' }); },
};

export class ApiError extends Error {
  constructor(message, status, data) { super(message); this.status = status; this.data = data; }
}

export const authStore = {
  token() { return localStorage.getItem(TOKEN_KEY); },
  user() { try { return JSON.parse(localStorage.getItem(USER_KEY) || 'null'); } catch { return null; } },
  setSession(token, user) {
    if (token) localStorage.setItem(TOKEN_KEY, token); else localStorage.removeItem(TOKEN_KEY);
    if (user) localStorage.setItem(USER_KEY, JSON.stringify(user)); else localStorage.removeItem(USER_KEY);
  },
  clear() { localStorage.removeItem(TOKEN_KEY); localStorage.removeItem(USER_KEY); },
  isLoggedIn() { return !!this.token(); },
};

export function toast(message, type = 'info', duration = 3500) {
  let host = document.getElementById('toast');
  if (!host) { host = document.createElement('div'); host.id = 'toast'; document.body.appendChild(host); }
  const el = document.createElement('div');
  el.className = 'toast ' + type;
  el.textContent = message;
  host.appendChild(el);
  setTimeout(() => { el.style.opacity = '0'; setTimeout(() => el.remove(), 200); }, duration);
}

export function requireAuth(redirect = 'login.html') {
  if (!authStore.isLoggedIn()) { window.location.href = redirect; return false; }
  return true;
}

export function formatDate(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  return d.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
}

export function escapeHtml(s) {
  if (s == null) return '';
  return String(s).replace(/[&<>"']/g, c => ({ '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;' }[c]));
}

export function renderNav(active) {
  const user = authStore.user();
  const links = [
    { href: 'analysis.html', label: 'Analysis', key: 'analysis' },
    { href: 'openings.html', label: 'Openings', key: 'openings' },
    { href: 'archive.html', label: 'Archive', key: 'archive' },
    { href: 'profile.html', label: 'Profile', key: 'profile' },
    { href: 'settings.html', label: 'Settings', key: 'settings' },
  ];
  const linksHtml = links.map(l =>
    `<a href="${l.href}" class="${active === l.key ? 'active' : ''}">${l.label}</a>`
  ).join('');
  const right = user
    ? `<span class="badge" title="${user.email}">${user.username}</span>
       <button class="btn btn-sm btn-ghost" id="logout-btn">Sign out</button>`
    : `<a href="login.html" class="btn btn-sm">Sign in</a>
       <a href="register.html" class="btn btn-sm btn-primary">Register</a>`;
  return `
    <header class="header">
      <a href="index.html" class="brand">
        <span class="brand-logo">♞</span>
        <span class="brand-text">Chesst</span>
      </a>
      <nav class="nav">${linksHtml}</nav>
      <div class="header-spacer"></div>
      <div class="header-actions">${right}</div>
      <button class="btn btn-icon btn-ghost" id="theme-toggle" title="Toggle theme">◐</button>
    </header>
  `;
}

export function mountNav(active) {
  const navHost = document.getElementById('nav');
  if (!navHost) return;
  navHost.innerHTML = renderNav(active);
  const logoutBtn = document.getElementById('logout-btn');
  if (logoutBtn) logoutBtn.addEventListener('click', () => {
    api.post('/api/auth/logout').catch(() => {});
    authStore.clear();
    window.location.href = 'index.html';
  });
  const themeBtn = document.getElementById('theme-toggle');
  if (themeBtn) themeBtn.addEventListener('click', toggleTheme);
  applyStoredTheme();
}

export function applyStoredTheme() {
  const theme = localStorage.getItem('chesst:theme') || 'light';
  document.documentElement.setAttribute('data-theme', theme);
}

export function toggleTheme() {
  const current = localStorage.getItem('chesst:theme') || 'light';
  const next = current === 'light' ? 'dark' : 'light';
  localStorage.setItem('chesst:theme', next);
  document.documentElement.setAttribute('data-theme', next);
}

export function renderFooter() {
  return `<footer class="footer">Chesst — analyze, study, improve. &middot; Real Stockfish analysis &middot; 220 openings indexed</footer>`;
}

export const CLS_META = {
  best:       { label: 'Best',       symbol: '!!', color: '#1f9d55', bg: '#dcfce7' },
  great:      { label: 'Great',      symbol: '!',  color: '#0e7a3d', bg: '#bbf7d0' },
  good:       { label: 'Good',       symbol: '✓',  color: '#2563eb', bg: '#dbeafe' },
  book:       { label: 'Book',       symbol: '≡',  color: '#6b7280', bg: '#f3f4f6' },
  inaccuracy: { label: 'Inaccuracy', symbol: '?!', color: '#b45309', bg: '#fef3c7' },
  mistake:    { label: 'Mistake',    symbol: '?',  color: '#c2410c', bg: '#ffedd5' },
  blunder:    { label: 'Blunder',    symbol: '??', color: '#b91c1c', bg: '#fee2e2' },
};
