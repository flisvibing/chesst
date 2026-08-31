import { mountNav, renderFooter, api, authStore, toast, requireAuth, escapeHtml, CLS_META } from './common.js';

mountNav('analysis');
document.getElementById('footer-mount').innerHTML = renderFooter();

const Chess = window.Chess;
const Chessboard = window.Chessboard;
const START_FEN = 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1';

let game = new Chess();
let moves = [];
let currentPly = 0;
let orientation = 'white';
let board = null;
let classifications = {};

function initBoard() {
  const cfg = {
    draggable: true,
    position: START_FEN,
    orientation,
    pieceTheme: 'https://unpkg.com/@chrisoakman/chessboardjs@1.0.0/dist/img/chesspieces/wikipedia/{piece}.png',
    onDrop,
    onSnapEnd,
  };
  board = Chessboard('board', cfg);
  if (window.jQuery) window.jQuery(window).on('resize', () => board && board.resize());
}

function onDrop(source, target) {
  const move = tryMove(source, target);
  if (!move) return 'snapback';
}

function tryMove(source, target, promotion = 'q') {
  const candidate = { from: source, to: target, promotion };
  let move;
  try { move = game.move(candidate); } catch (e) { return null; }
  if (!move) return null;
  moves = moves.slice(0, currentPly);
  moves.push({ san: move.san, fen: game.fen(), color: move.color, from: move.from, to: move.to });
  currentPly = moves.length;
  classifications = {};
  afterChange();
  return move;
}

function onSnapEnd() {
  board.position(game.fen(), false);
  updateEvalBar(null);
  analyzeCurrent();
}

function gotoPly(ply) {
  currentPly = Math.max(0, Math.min(moves.length, ply));
  const rebuild = new Chess();
  for (let i = 0; i < currentPly; i++) {
    try { rebuild.move(moves[i].san); } catch { break; }
  }
  game = rebuild;
  board.position(game.fen());
  renderMoves();
  updateCounter();
  analyzeCurrent();
}

function navFirst() { gotoPly(0); }
function navPrev()  { gotoPly(currentPly - 1); }
function navNext()  { gotoPly(currentPly + 1); }
function navLast()  { gotoPly(moves.length); }

function updateCounter() {
  const el = document.getElementById('move-counter');
  if (currentPly === 0) el.textContent = 'Start';
  else el.textContent = `Move ${Math.ceil(currentPly / 2)} · ply ${currentPly}`;
}

function renderMoves() {
  const host = document.getElementById('move-list');
  if (!moves.length) {
    host.innerHTML = '<div class="empty-state"><div class="icon">♟</div>No moves yet. Play on the board or load a PGN/FEN.</div>';
    return;
  }
  let html = '';
  for (let i = 0; i < moves.length; i += 2) {
    const num = i / 2 + 1;
    const w = moves[i];
    const b = moves[i + 1];
    html += `<div class="move-row"><div class="num">${num}.</div>`;
    html += renderMoveCell(w, i + 1);
    html += renderMoveCell(b, i + 2);
    html += '</div>';
  }
  host.innerHTML = html;
  host.querySelectorAll('.move-cell').forEach(c => {
    c.addEventListener('click', () => gotoPly(Number(c.dataset.ply)));
  });
}

function renderMoveCell(m, ply) {
  if (!m) return '<div class="move-cell"></div>';
  const cls = classifications[ply];
  const clsMeta = cls ? CLS_META[cls] : null;
  const current = currentPly === ply ? ' current' : '';
  const clsHtml = clsMeta ? `<span class="move-cls" style="background:${clsMeta.bg};color:${clsMeta.color}">${clsMeta.symbol}</span>` : '';
  return `<div class="move-cell${current}" data-ply="${ply}">${escapeHtml(m.san)}${clsHtml}</div>`;
}

function renderEnginePanel() {
  const host = document.getElementById('panel-engine');
  host.innerHTML = `
    <div style="display:flex; align-items:center; gap:8px; margin-bottom:12px;">
      <span style="font-weight:600;">Chesst Engine</span>
      <span class="badge" id="engine-status">idle</span>
    </div>
    <div class="stat-grid">
      <div class="stat"><div class="label">Eval</div><div class="val" id="stat-eval">—</div></div>
      <div class="stat"><div class="label">Depth</div><div class="val" id="stat-depth">—</div></div>
      <div class="stat"><div class="label">Best move</div><div class="val" id="stat-best">—</div></div>
    </div>
    <div id="engine-pv" style="font-family:var(--mono); font-size:12px; color:var(--fg-soft); min-height:30px;"></div>
    <hr style="border:none; border-top:1px solid var(--border); margin:14px 0;">
    <h4 style="font-size:14px; margin-bottom:8px;">Game report</h4>
    <button class="btn btn-primary btn-sm" id="run-full" style="width:100%;">Run full analysis</button>
    <div id="full-progress" style="margin-top:10px; display:none;"></div>
    <div id="full-summary" style="margin-top:12px;"></div>
  `;
  document.getElementById('run-full').addEventListener('click', runFullAnalysis);
}

async function analyzeCurrent() {
  if (!authStore.isLoggedIn()) return;
  const statusEl = document.getElementById('engine-status');
  if (statusEl) statusEl.textContent = 'thinking…';
  try {
    const res = await api.post('/api/analyses/position', { fen: game.fen(), depth: 14, movetimeMs: 2000 });
    updateEval(res);
    if (statusEl) statusEl.textContent = 'idle';
  } catch (e) {
    if (statusEl) statusEl.textContent = 'error';
  }
}

function updateEval(res) {
  if (!res) { updateEvalBar(null); return; }
  const turn = game.turn();
  const whiteCp = turn === 'w' ? res.cp : -res.cp;
  updateEvalBar({ cp: whiteCp, mate: res.mate ? (turn === 'w' ? res.mate : -res.mate) : null });
  const statEval = document.getElementById('stat-eval');
  const statDepth = document.getElementById('stat-depth');
  const statBest = document.getElementById('stat-best');
  const pvEl = document.getElementById('engine-pv');
  if (statEval) {
    let txt = res.mate != null ? `M${Math.abs(res.mate)}` : `${(res.cp/100).toFixed(2)}`;
    statEval.textContent = txt;
    statEval.className = 'val ' + (whiteCp >= 0 ? 'eval-positive' : 'eval-negative');
  }
  if (statDepth) statDepth.textContent = res.depth || '—';
  if (statBest) {
    const uci = res.bestMoveUci || '';
    let san = uci;
    try {
      const tmp = new Chess(game.fen());
      const m = tmp.move({ from: uci.slice(0,2), to: uci.slice(2,4), promotion: uci.slice(4) || 'q' });
      if (m) san = m.san;
    } catch {}
    statBest.textContent = san;
  }
  if (pvEl) pvEl.textContent = res.pv ? 'PV: ' + res.pv : '';
}

function updateEvalBar(data) {
  const fill = document.querySelector('.white-fill');
  const label = document.getElementById('eval-label');
  let whiteShare = 0.5;
  let text = '0.0';
  if (data) {
    if (data.mate != null) { whiteShare = data.mate > 0 ? 1 : 0; text = `M${Math.abs(data.mate)}`; }
    else if (data.cp != null) {
      const pawns = data.cp / 100;
      whiteShare = 1 / (1 + Math.exp(-pawns * 0.4));
      text = (pawns >= 0 ? '+' : '') + pawns.toFixed(1);
    }
  }
  fill.style.height = (whiteShare * 100) + '%';
  label.textContent = text;
  label.style.color = whiteShare > 0.5 ? '#000' : '#fff';
  label.style.bottom = whiteShare > 0.5 ? '4px' : 'auto';
  label.style.top = whiteShare > 0.5 ? 'auto' : '4px';
}

async function runFullAnalysis() {
  if (!moves.length) { toast('No moves to analyze.', 'error'); return; }
  if (!requireAuth()) return;
  const btn = document.getElementById('run-full');
  const prog = document.getElementById('full-progress');
  btn.disabled = true;
  prog.style.display = 'block';
  prog.innerHTML = '<span class="spinner"></span> Analyzing positions…';
  const replay = new Chess();
  const plies = [];
  for (const m of moves) {
    plies.push({ ply: plies.length + 1, color: m.color, san: m.san, fenBefore: replay.fen() });
    try { replay.move(m.san); } catch { break; }
  }
  let gameId = null;
  try {
    const saved = await api.post('/api/games', { pgn: exportPgn(), white: 'White', black: 'Black', result: '*' });
    gameId = saved.id;
  } catch (e) {
    toast('Could not save game for analysis: ' + e.message, 'error');
    btn.disabled = false; prog.style.display = 'none'; return;
  }
  try {
    const res = await api.post(`/api/analyses/games/${gameId}`, { startFen: START_FEN, plies });
    classifications = {};
    for (const m of res.moves) classifications[m.ply] = m.classification;
    renderMoves();
    prog.innerHTML = '';
    const sum = document.getElementById('full-summary');
    sum.innerHTML = `
      <div class="stat-grid" style="grid-template-columns: repeat(2, 1fr);">
        <div class="stat"><div class="label">White accuracy</div><div class="val">${res.accuracyW != null ? res.accuracyW.toFixed(1) + '%' : '—'}</div></div>
        <div class="stat"><div class="label">Black accuracy</div><div class="val">${res.accuracyB != null ? res.accuracyB.toFixed(1) + '%' : '—'}</div></div>
      </div>
      <div class="classification-row">${renderClassChips(res)}</div>
    `;
    toast('Analysis complete.', 'success');
  } catch (e) {
    prog.innerHTML = `<div class="alert alert-error">${escapeHtml(e.message)}</div>`;
  } finally { btn.disabled = false; }
}

function renderClassChips(res) {
  const counts = { blunder: res.blundersW + res.blundersB, mistake: res.mistakesW + res.mistakesB };
  let html = '';
  for (const [k, n] of Object.entries(counts)) {
    if (!n) continue;
    const m = CLS_META[k];
    html += `<span class="cls-chip" style="border-color:${m.color}"><span class="cls-symbol" style="color:${m.color}">${m.symbol}</span> ${m.label}: ${n}</span>`;
  }
  return html;
}

function exportPgn() {
  const replay = new Chess();
  for (const m of moves) { try { replay.move(m.san); } catch { break; } }
  return replay.pgn();
}

function loadPgn(pgnText) {
  const c = new Chess();
  try { c.loadPgn(pgnText); } catch (e) { toast('Invalid PGN: ' + e.message, 'error'); return false; }
  game = c;
  const hist = c.history({ verbose: true });
  moves = hist.map(m => ({ san: m.san, fen: '', color: m.color, from: m.from, to: m.to }));
  const replay = new Chess();
  for (let i = 0; i < moves.length; i++) { try { replay.move(moves[i].san); } catch { break; } moves[i].fen = replay.fen(); }
  currentPly = moves.length;
  classifications = {};
  board.position(c.fen());
  afterChange();
  toast('PGN loaded (' + moves.length + ' plies).', 'success');
  return true;
}

function loadFen(fenText) {
  let c;
  try { c = new Chess(fenText); } catch (e) { toast('Invalid FEN.', 'error'); return false; }
  game = c; moves = []; currentPly = 0; classifications = {};
  board.position(fenText);
  afterChange();
  toast('Position loaded.', 'success');
  return true;
}

function afterChange() {
  renderMoves();
  updateCounter();
  updateEvalBar(null);
  analyzeCurrent();
}

function wireLoadModal() {
  const modal = document.getElementById('load-modal');
  document.getElementById('btn-load').addEventListener('click', () => modal.style.display = 'flex');
  document.getElementById('load-close').addEventListener('click', () => modal.style.display = 'none');
  modal.addEventListener('click', (e) => { if (e.target === modal) modal.style.display = 'none'; });
  document.getElementById('load-pgn').addEventListener('click', () => {
    const txt = document.getElementById('pgn-input').value;
    if (loadPgn(txt)) { modal.style.display = 'none'; document.getElementById('pgn-input').value = ''; }
  });
  document.getElementById('insert-sample').addEventListener('click', () => {
    document.getElementById('pgn-input').value = `[Event "Casual"]\n[White "Anand"]\n[Black "Karpov"]\n[Result "1-0"]\n\n1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 4. Ba4 Nf6 5. O-O Be7 6. Re1 b5 7. Bb3 d6 8. c3 O-O 1-0`;
  });
  document.getElementById('load-fen').addEventListener('click', () => {
    const txt = document.getElementById('fen-input').value.trim();
    if (loadFen(txt)) { modal.style.display = 'none'; document.getElementById('fen-input').value = ''; }
  });
}

function wireSaveModal() {
  const modal = document.getElementById('save-modal');
  document.getElementById('btn-save').addEventListener('click', openSaveModal);
  document.getElementById('save-close').addEventListener('click', () => modal.style.display = 'none');
  modal.addEventListener('click', (e) => { if (e.target === modal) modal.style.display = 'none'; });
}

async function openSaveModal() {
  if (!requireAuth()) return;
  if (!moves.length) { toast('Nothing to save.', 'error'); return; }
  const modal = document.getElementById('save-modal');
  const body = document.getElementById('save-body');
  body.innerHTML = `
    <div class="form-group"><label>White</label><input class="input" id="sv-white" value="White"></div>
    <div class="form-group"><label>Black</label><input class="input" id="sv-black" value="Black"></div>
    <div style="display:grid; grid-template-columns:1fr 1fr; gap:8px;">
      <div class="form-group"><label>Result</label>
        <select class="select" id="sv-result"><option value="*">*</option><option value="1-0">1-0</option><option value="0-1">0-1</option><option value="1/2-1/2">½-½</option></select>
      </div>
    </div>
    <button class="btn btn-primary" id="sv-submit" style="width:100%;">Save game</button>
  `;
  modal.style.display = 'flex';
  document.getElementById('sv-submit').addEventListener('click', async () => {
    const payload = {
      pgn: exportPgn(),
      white: document.getElementById('sv-white').value || 'White',
      black: document.getElementById('sv-black').value || 'Black',
      result: document.getElementById('sv-result').value,
    };
    try {
      await api.post('/api/games', payload);
      toast('Game saved to archive.', 'success');
      modal.style.display = 'none';
    } catch (e) { toast(e.message, 'error'); }
  });
}

function wireTabs() {
  document.querySelectorAll('.panel-tab').forEach(t => {
    t.addEventListener('click', () => {
      document.querySelectorAll('.panel-tab').forEach(x => x.classList.remove('active'));
      t.classList.add('active');
      ['moves','engine','info'].forEach(k => {
        document.getElementById('panel-' + k).style.display = k === t.dataset.tab ? 'block' : 'none';
      });
    });
  });
}

document.addEventListener('DOMContentLoaded', () => {
  if (!Chess || !Chessboard) {
    document.getElementById('board').innerHTML = '<div class="empty-state">Failed to load chess libraries. Check your internet connection.</div>';
    return;
  }
  initBoard();
  renderEnginePanel();
  wireTabs();
  wireLoadModal();
  wireSaveModal();
  document.getElementById('nav-first').addEventListener('click', navFirst);
  document.getElementById('nav-prev').addEventListener('click', navPrev);
  document.getElementById('nav-next').addEventListener('click', navNext);
  document.getElementById('nav-last').addEventListener('click', navLast);
  document.getElementById('btn-flip').addEventListener('click', () => {
    orientation = orientation === 'white' ? 'black' : 'white';
    board.orientation(orientation);
  });
  document.getElementById('btn-reset').addEventListener('click', () => {
    game = new Chess(); moves = []; currentPly = 0; classifications = {};
    board.position(START_FEN);
    afterChange();
  });
  const stashed = sessionStorage.getItem('chesst:loadPgn');
  if (stashed) { sessionStorage.removeItem('chesst:loadPgn'); loadPgn(stashed); }
  else if (authStore.isLoggedIn()) analyzeCurrent();
});
