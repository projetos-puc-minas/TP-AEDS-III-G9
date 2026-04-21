/* ============================================================
   BiblioSys — app.js
   SPA: Livros · Editoras · Autores · Usuários
   ============================================================ */

const API = '';
const PAGE_SIZE = 15;

/* ── State ──────────────────────────────────────────────── */
const state = {
  module:    'livros',
  records:   [],
  filtered:  [],
  page:      1,
  sort:      { key: 'id', dir: 'asc' },
  filter:    'all',
  search:    '',
  editId:    null,
  // Cache de relacionamentos
  editoras:      [],
  autores:       [],
  usuarios:      [],
  livros:        [],
  livrosAutores: [],
};

/* ── API helpers ─────────────────────────────────────────── */
async function apiFetch(path, opts = {}) {
  const res = await fetch(API + path, {
    headers: { 'Content-Type': 'application/json' },
    ...opts,
  });
  const json = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(json.erro || `HTTP ${res.status}`);
  return json;
}

const get  = (p)    => apiFetch(p);
const post = (p, b) => apiFetch(p, { method: 'POST',   body: JSON.stringify(b) });
const put  = (p, b) => apiFetch(p, { method: 'PUT',    body: JSON.stringify(b) });
const del  = (p)    => apiFetch(p, { method: 'DELETE' });

/* ── Module configs ──────────────────────────────────────── */
const MODULES = {
  livros: {
    label:    'Livros',
    subtitle: 'Gerencie o acervo',
    apiPath:  '/api/livros',
    columns: [
      { key: 'id',            label: 'ID',       render: r => `<span class="cell-id">#${r.id}</span>` },
      { key: 'titulo',        label: 'Título',   render: r => `<span class="cell-title">${esc(r.titulo)}</span>` },
      { key: 'editora',       label: 'Editora',  sort: false, render: r => `<span class="cell-muted">${editoraNome(r.idEditora)}</span>` },
      { key: 'isbn',          label: 'ISBN',     render: r => `<span class="cell-mono">${esc(r.isbn.trim())}</span>` },
      { key: 'anoPublicacao', label: 'Ano',      render: r => `<span class="cell-muted">${r.anoPublicacao}</span>` },
      { key: 'preco',         label: 'Preço',    render: r => `<span class="cell-price">R$ ${r.preco.toFixed(2)}</span>` },
      { key: 'generos',       label: 'Gêneros',  sort: false, render: r => renderGeneros(r.generos) },
      { key: 'autores',       label: 'Autores',  sort: false, render: r => renderAutoresLivro(r.id) },
      { key: '_actions',      label: '',         sort: false, render: r => actionBtns(r.id) },
    ],
    filters: [{ value: 'all', label: 'Todos' }],
    sortOptions: [
      { value: 'id-asc',             label: 'ID ↑' },
      { value: 'id-desc',            label: 'ID ↓' },
      { value: 'titulo-asc',         label: 'Título A→Z' },
      { value: 'titulo-desc',        label: 'Título Z→A' },
      { value: 'anoPublicacao-desc', label: 'Mais recentes' },
      { value: 'anoPublicacao-asc',  label: 'Mais antigos' },
      { value: 'preco-asc',          label: 'Preço ↑' },
      { value: 'preco-desc',         label: 'Preço ↓' },
    ],
  },

  editoras: {
    label:    'Editoras',
    subtitle: 'Cadastro de editoras',
    apiPath:  '/api/editoras',
    columns: [
      { key: 'id',          label: 'ID',      render: r => `<span class="cell-id">#${r.id}</span>` },
      { key: 'nome',        label: 'Nome',    render: r => `<span class="cell-title">${esc(r.nome)}</span>` },
      { key: 'cidade',      label: 'Cidade',  render: r => `<span class="cell-muted">${esc(r.cidade)}</span>` },
      { key: 'anoFundacao', label: 'Fundação',render: r => `<span class="cell-mono">${r.anoFundacao}</span>` },
      { key: '_actions',    label: '',        sort: false, render: r => actionBtns(r.id) },
    ],
    filters: [{ value: 'all', label: 'Todas' }],
    sortOptions: [
      { value: 'id-asc',          label: 'ID ↑' },
      { value: 'id-desc',         label: 'ID ↓' },
      { value: 'nome-asc',        label: 'Nome A→Z' },
      { value: 'nome-desc',       label: 'Nome Z→A' },
      { value: 'anoFundacao-asc', label: 'Mais antigas' },
      { value: 'anoFundacao-desc',label: 'Mais recentes' },
    ],
  },

  autores: {
    label:    'Autores',
    subtitle: 'Cadastro de autores',
    apiPath:  '/api/autores',
    columns: [
      { key: 'id',                     label: 'ID',        render: r => `<span class="cell-id">#${r.id}</span>` },
      { key: 'nome',                   label: 'Nome',      render: r => `<span class="cell-title">${esc(r.nome)}</span>` },
      { key: 'dataNascimentoFormatada',label: 'Nascimento',render: r => `<span class="cell-mono">${esc(r.dataNascimentoFormatada || '')}</span>` },
      { key: 'biografia',              label: 'Biografia', render: r => `<span class="cell-muted">${truncate(r.biografia, 55)}</span>` },
      { key: 'livros',                 label: 'Livros',    sort: false, render: r => renderLivrosAutor(r.id) },
      { key: '_actions',               label: '',          sort: false, render: r => actionBtns(r.id) },
    ],
    filters: [{ value: 'all', label: 'Todos' }],
    sortOptions: [
      { value: 'id-asc',   label: 'ID ↑' },
      { value: 'id-desc',  label: 'ID ↓' },
      { value: 'nome-asc', label: 'Nome A→Z' },
      { value: 'nome-desc',label: 'Nome Z→A' },
    ],
  },

  usuarios: {
    label:    'Usuários',
    subtitle: 'Cadastro de usuários',
    apiPath:  '/api/usuarios',
    columns: [
      { key: 'id',       label: 'ID',    render: r => `<span class="cell-id">#${r.id}</span>` },
      { key: 'nome',     label: 'Nome',  render: r => `<span class="cell-title">${esc(r.nome)}</span>` },
      { key: 'email',    label: 'Email', render: r => `<span class="cell-mono">${esc(r.email)}</span>` },
      { key: '_actions', label: '',      sort: false, render: r => actionBtns(r.id) },
    ],
    filters: [{ value: 'all', label: 'Todos' }],
    sortOptions: [
      { value: 'id-asc',   label: 'ID ↑' },
      { value: 'id-desc',  label: 'ID ↓' },
      { value: 'nome-asc', label: 'Nome A→Z' },
      { value: 'nome-desc',label: 'Nome Z→A' },
    ],
  },
};

/* ── Render helpers ──────────────────────────────────────── */
function esc(s) {
  if (!s) return '';
  return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}
function truncate(s, n) {
  if (!s) return '';
  return s.length > n ? esc(s.slice(0, n)) + '…' : esc(s);
}
function editoraNome(id) {
  const e = state.editoras.find(x => x.id === id);
  return e ? esc(e.nome) : `<span class="cell-id">#${id}</span>`;
}

function renderGeneros(generos) {
  if (!generos || !generos.length) return '<span class="cell-muted">—</span>';
  return `<div class="cell-rels">${generos.map(g =>
    `<span class="rel-tag-accent">${esc(g)}</span>`
  ).join('')}</div>`;
}

function renderAutoresLivro(livroId) {
  const rels = state.livrosAutores.filter(la => la.idLivro === livroId);
  if (!rels.length) return '<span class="cell-muted">—</span>';
  return `<div class="cell-rels">${rels.map(la => {
    const a = state.autores.find(x => x.id === la.idAutor);
    return `<span class="rel-tag">${a ? esc(a.nome) : '#'+la.idAutor}</span>`;
  }).join('')}</div>`;
}

function renderLivrosAutor(autorId) {
  const rels = state.livrosAutores.filter(la => la.idAutor === autorId);
  if (!rels.length) return '<span class="cell-muted">—</span>';
  return `<div class="cell-rels">${rels.map(la => {
    const l = state.livros.find(x => x.id === la.idLivro);
    return `<span class="rel-tag">${l ? esc(l.titulo) : '#'+la.idLivro}</span>`;
  }).join('')}</div>`;
}

function actionBtns(id) {
  return `<div class="cell-actions">
    <button class="btn btn-icon btn-sm" onclick="openEdit(${id})" title="Editar">✏️</button>
    <button class="btn btn-icon btn-sm danger" onclick="confirmDelete(${id})" title="Excluir">🗑</button>
  </div>`;
}

/* ── Load & refresh ──────────────────────────────────────── */
async function loadCaches() {
  const [editoras, autores, usuarios, livros, livrosAutores] = await Promise.allSettled([
    get('/api/editoras'),
    get('/api/autores'),
    get('/api/usuarios'),
    get('/api/livros'),
    get('/api/livros-autores'),
  ]);
  state.editoras      = editoras.value      || [];
  state.autores       = autores.value       || [];
  state.usuarios      = usuarios.value      || [];
  state.livros        = livros.value        || [];
  state.livrosAutores = livrosAutores.value || [];
}

async function loadModule(mod) {
  setLoading(true);
  try {
    const cfg = MODULES[mod];
    const data = await get(cfg.apiPath);
    state.records = Array.isArray(data) ? data : [];
  } catch (e) {
    toast('Erro ao carregar dados: ' + e.message, 'error');
    state.records = [];
  }
  setLoading(false);
  applyFiltersAndRender();
  updateBadge(mod, state.records.length);
}

async function refreshAllBadges() {
  const mods = ['livros','editoras','autores','usuarios'];
  for (const m of mods) {
    try {
      const d = await get(MODULES[m].apiPath);
      updateBadge(m, Array.isArray(d) ? d.length : 0);
    } catch {}
  }
}

function updateBadge(mod, n) {
  const el = document.getElementById('badge-' + mod);
  if (el) el.textContent = n;
}

/* ── Filtering & sorting ─────────────────────────────────── */
function applyFiltersAndRender() {
  let data = [...state.records];

  if (state.search) {
    const q = state.search.toLowerCase();
    data = data.filter(r => JSON.stringify(r).toLowerCase().includes(q));
  }

  const { key, dir } = state.sort;
  data.sort((a, b) => {
    let va = a[key], vb = b[key];
    if (va === undefined) return 0;
    if (typeof va === 'string') va = va.toLowerCase();
    if (typeof vb === 'string') vb = vb.toLowerCase();
    if (va < vb) return dir === 'asc' ? -1 : 1;
    if (va > vb) return dir === 'asc' ? 1 : -1;
    return 0;
  });

  state.filtered = data;
  state.page = 1;
  renderTable();
  renderPagination();
}

/* ── Table rendering ─────────────────────────────────────── */
function renderTable() {
  const cfg  = MODULES[state.module];
  const data = state.filtered;
  const start = (state.page - 1) * PAGE_SIZE;
  const page  = data.slice(start, start + PAGE_SIZE);

  const head  = document.getElementById('table-head');
  const body  = document.getElementById('table-body');
  const tbl   = document.getElementById('data-table');
  const empty = document.getElementById('table-empty');

  head.innerHTML = `<tr>${cfg.columns.map(c => {
    const sortable = c.sort !== false;
    const active   = state.sort.key === c.key;
    const arrow    = active ? (state.sort.dir === 'asc' ? ' ↑' : ' ↓') : '';
    return `<th class="${active ? 'sort-active' : ''}" ${sortable ? `onclick="sortBy('${c.key}')"` : ''}>
      ${c.label}${arrow}
    </th>`;
  }).join('')}</tr>`;

  if (!data.length) {
    tbl.style.display = 'none';
    empty.style.display = 'flex';
    return;
  }

  empty.style.display = 'none';
  tbl.style.display = 'table';
  body.innerHTML = page.map(r =>
    `<tr>${cfg.columns.map(c => `<td>${c.render(r)}</td>`).join('')}</tr>`
  ).join('');
}

function renderPagination() {
  const total = state.filtered.length;
  const pages = Math.ceil(total / PAGE_SIZE);
  const cur   = state.page;
  const el    = document.getElementById('pagination');

  if (pages <= 1) { el.innerHTML = ''; return; }

  let html = `<span class="page-info">${total} registros</span>`;
  html += `<button class="page-btn" onclick="goPage(${cur-1})" ${cur===1?'disabled':''}>‹</button>`;

  const range = [...new Set([1, cur-1, cur, cur+1, pages])].filter(p => p >= 1 && p <= pages).sort((a,b)=>a-b);
  let prev = 0;
  for (const p of range) {
    if (p - prev > 1) html += `<span style="color:var(--text-3);padding:0 2px">…</span>`;
    html += `<button class="page-btn ${p===cur?'active':''}" onclick="goPage(${p})">${p}</button>`;
    prev = p;
  }
  html += `<button class="page-btn" onclick="goPage(${cur+1})" ${cur===pages?'disabled':''}>›</button>`;
  el.innerHTML = html;
}

function goPage(p) {
  const pages = Math.ceil(state.filtered.length / PAGE_SIZE);
  if (p < 1 || p > pages) return;
  state.page = p;
  renderTable();
  renderPagination();
}

function sortBy(key) {
  if (state.sort.key === key) {
    state.sort.dir = state.sort.dir === 'asc' ? 'desc' : 'asc';
  } else {
    state.sort.key = key;
    state.sort.dir = 'asc';
  }
  const sel = document.getElementById('sort-select');
  if (sel) sel.value = `${key}-${state.sort.dir}`;
  applyFiltersAndRender();
}

/* ── Filter bar ──────────────────────────────────────────── */
function renderFilterBar() {
  const cfg = MODULES[state.module];

  const chips = document.getElementById('filter-chips');
  chips.innerHTML = cfg.filters.map(f =>
    `<button class="chip ${state.filter === f.value ? 'active' : ''}" onclick="setFilter('${f.value}')">${f.label}</button>`
  ).join('');

  const sel = document.getElementById('sort-select');
  sel.innerHTML = cfg.sortOptions.map(o =>
    `<option value="${o.value}">${o.label}</option>`
  ).join('');
  sel.value = `${state.sort.key}-${state.sort.dir}`;
}

function setFilter(val) {
  state.filter = val;
  renderFilterBar();
  applyFiltersAndRender();
}

/* ── Module switch ───────────────────────────────────────── */
async function switchModule(mod) {
  state.module = mod;
  state.search = '';
  state.filter = 'all';
  state.sort   = { key: 'id', dir: 'asc' };
  state.page   = 1;

  document.getElementById('search-input').value = '';
  document.getElementById('page-title').textContent    = MODULES[mod].label;
  document.getElementById('page-subtitle').textContent = MODULES[mod].subtitle;

  document.querySelectorAll('.nav-item').forEach(el => {
    el.classList.toggle('active', el.dataset.module === mod);
  });

  renderFilterBar();
  await loadModule(mod);
}

/* ── Modal forms ─────────────────────────────────────────── */
function openNew() {
  state.editId = null;
  document.getElementById('modal-title').textContent = 'Novo ' + MODULES[state.module].label.replace(/s$/, '');
  document.getElementById('modal-body').innerHTML = buildForm(state.module, null);
  bindFormEvents();
  openModal();
}

async function openEdit(id) {
  state.editId = id;
  const cfg = MODULES[state.module];
  let record = state.records.find(r => r.id === id);
  if (!record) {
    try { record = await get(cfg.apiPath + '/' + id); } catch {}
  }
  document.getElementById('modal-title').textContent = 'Editar ' + cfg.label.replace(/s$/, '');
  document.getElementById('modal-body').innerHTML = buildForm(state.module, record);
  bindFormEvents();
  openModal();
}

function buildForm(mod, r) {
  if (mod === 'livros')   return formLivros(r);
  if (mod === 'editoras') return formEditoras(r);
  if (mod === 'autores')  return formAutores(r);
  if (mod === 'usuarios') return formUsuarios(r);
  return '';
}

function formLivros(r) {
  const opts = state.editoras.map(e =>
    `<option value="${e.id}" ${r && r.idEditora === e.id ? 'selected' : ''}>${esc(e.nome)}</option>`
  ).join('');

  const selectedAutores = r
    ? state.livrosAutores.filter(la => la.idLivro === r.id).map(la => la.idAutor)
    : [];

  const autoresChips = state.autores.map(a =>
    `<div class="autor-chip ${selectedAutores.includes(a.id) ? 'selected' : ''}"
          data-autor-id="${a.id}" onclick="toggleAutorChip(this)">
      <span class="chip-check">${selectedAutores.includes(a.id) ? '✓' : '+'}</span>
      ${esc(a.nome)}
    </div>`
  ).join('');

  // Gêneros existentes
  const generosAtuais = (r && r.generos) ? r.generos : [];
  const generoTagsHtml = generosAtuais.map(g =>
    `<span class="genero-tag">${esc(g)}<span class="genero-tag-remove" onclick="removeGenero(this)">×</span></span>`
  ).join('');

  return `
    <div class="form-group">
      <label>Título *</label>
      <input class="form-control" id="f-titulo" value="${r ? esc(r.titulo) : ''}" placeholder="Ex: Dom Casmurro" />
    </div>
    <div class="form-row">
      <div class="form-group">
        <label>Editora *</label>
        <select class="form-control" id="f-idEditora">
          <option value="">— selecione —</option>
          ${opts}
        </select>
      </div>
      <div class="form-group">
        <label>ISBN</label>
        <input class="form-control" id="f-isbn" maxlength="13" value="${r ? esc(r.isbn.trim()) : ''}" placeholder="9788535914849" />
        <div class="form-hint">Até 13 dígitos</div>
      </div>
    </div>
    <div class="form-row">
      <div class="form-group">
        <label>Ano de Publicação</label>
        <input class="form-control" type="number" id="f-anoPublicacao" value="${r ? r.anoPublicacao : ''}" placeholder="2024" />
      </div>
      <div class="form-group">
        <label>Preço (R$)</label>
        <input class="form-control" type="number" step="0.01" id="f-preco" value="${r ? r.preco : ''}" placeholder="49.90" />
      </div>
    </div>
    <div class="form-group">
      <label>Sinopse</label>
      <textarea class="form-control" id="f-sinopse" placeholder="Breve descrição...">${r ? esc(r.sinopse) : ''}</textarea>
    </div>
    <div class="form-group">
      <label>Gêneros</label>
      <div class="generos-input-wrap">
        <input class="form-control" id="f-genero-input" placeholder="Ex: Romance" style="flex:1" />
        <button class="btn btn-ghost btn-sm" type="button" onclick="addGenero()">+ Add</button>
      </div>
      <div class="generos-tags" id="generos-tags">${generoTagsHtml}</div>
      <div class="form-hint">Digite e clique em + Add para adicionar gêneros</div>
    </div>
    <div class="form-group">
      <label>Autores</label>
      <div class="autores-list" id="autores-list">${autoresChips}</div>
      <div class="form-hint">Clique para vincular/desvincular autores</div>
    </div>
  `;
}

function formEditoras(r) {
  return `
    <div class="form-group">
      <label>Nome *</label>
      <input class="form-control" id="f-nome" value="${r ? esc(r.nome) : ''}" placeholder="Ex: Companhia das Letras" />
    </div>
    <div class="form-row">
      <div class="form-group">
        <label>Cidade *</label>
        <input class="form-control" id="f-cidade" value="${r ? esc(r.cidade) : ''}" placeholder="São Paulo" />
      </div>
      <div class="form-group">
        <label>Ano de Fundação</label>
        <input class="form-control" type="number" id="f-anoFundacao" value="${r ? r.anoFundacao : ''}" placeholder="1990" />
      </div>
    </div>
  `;
}

function formAutores(r) {
  const dataBr = r && r.dataNascimentoFormatada ? r.dataNascimentoFormatada : '';
  return `
    <div class="form-group">
      <label>Nome *</label>
      <input class="form-control" id="f-nome" value="${r ? esc(r.nome) : ''}" placeholder="Ex: Machado de Assis" />
    </div>
    <div class="form-group">
      <label>Data de Nascimento *</label>
      <input class="form-control" id="f-dataNascimento" value="${dataBr}" placeholder="dd/MM/yyyy" />
      <div class="form-hint">Formato: dd/MM/yyyy</div>
    </div>
    <div class="form-group">
      <label>Biografia</label>
      <textarea class="form-control" id="f-biografia" placeholder="Breve biografia...">${r ? esc(r.biografia) : ''}</textarea>
    </div>
  `;
}

function formUsuarios(r) {
  return `
    <div class="form-group">
      <label>Nome *</label>
      <input class="form-control" id="f-nome" value="${r ? esc(r.nome) : ''}" placeholder="Nome completo" />
    </div>
    <div class="form-group">
      <label>Email *</label>
      <input class="form-control" type="email" id="f-email" value="${r ? esc(r.email) : ''}" placeholder="usuario@email.com" />
    </div>
    <div class="form-group">
      <label>${r ? 'Nova Senha (deixe vazio para não alterar)' : 'Senha *'}</label>
      <input class="form-control" type="password" id="f-senha" placeholder="${r ? '••••••••' : 'Mínimo 4 caracteres'}" />
    </div>
  `;
}

function bindFormEvents() {
  // Enter no campo de gênero adiciona o gênero
  const gi = document.getElementById('f-genero-input');
  if (gi) {
    gi.addEventListener('keydown', e => {
      if (e.key === 'Enter') { e.preventDefault(); addGenero(); }
    });
  }
}

function toggleAutorChip(el) {
  el.classList.toggle('selected');
  const check = el.querySelector('.chip-check');
  check.textContent = el.classList.contains('selected') ? '✓' : '+';
}

function addGenero() {
  const input = document.getElementById('f-genero-input');
  if (!input) return;
  const val = input.value.trim();
  if (!val) return;

  const container = document.getElementById('generos-tags');
  const tag = document.createElement('span');
  tag.className = 'genero-tag';
  tag.innerHTML = `${esc(val)}<span class="genero-tag-remove" onclick="removeGenero(this)">×</span>`;
  container.appendChild(tag);
  input.value = '';
  input.focus();
}

function removeGenero(el) {
  el.parentElement.remove();
}

function getGenerosFromForm() {
  const tags = document.querySelectorAll('#generos-tags .genero-tag');
  return [...tags].map(t => t.textContent.replace('×', '').trim()).filter(Boolean);
}

/* ── Save / CRUD ─────────────────────────────────────────── */
async function saveRecord() {
  const mod = state.module;
  try {
    if (mod === 'livros')   await saveLivro();
    else if (mod === 'editoras') await saveEditora();
    else if (mod === 'autores')  await saveAutor();
    else if (mod === 'usuarios') await saveUsuario();

    closeModal();
    await loadCaches();
    await loadModule(mod);
    toast('Registro salvo com sucesso!', 'success');
  } catch (e) {
    toast(e.message, 'error');
  }
}

async function saveLivro() {
  const titulo    = v('f-titulo');
  const idEditora = Number(v('f-idEditora'));
  if (!titulo)    throw new Error('Título é obrigatório.');
  if (!idEditora) throw new Error('Selecione uma editora.');

  // FIX: inclui generos no body (era o bug que causava o update apagar o livro)
  const body = {
    titulo,
    idEditora,
    isbn:          v('f-isbn'),
    anoPublicacao: Number(v('f-anoPublicacao')) || 0,
    preco:         parseFloat(v('f-preco')) || 0,
    sinopse:       v('f-sinopse'),
    generos:       getGenerosFromForm(),
  };

  let livroId = state.editId;

  if (state.editId) {
    await put('/api/livros/' + state.editId, body);
  } else {
    await post('/api/livros', body);
    // Busca o id do livro recém criado pelo título
    const lista = await get('/api/livros');
    const novo  = lista.slice().reverse().find(l => l.titulo === titulo);
    livroId = novo ? novo.id : null;
  }

  // Sincronizar autores
  if (livroId) {
    const selectedIds  = [...document.querySelectorAll('.autor-chip.selected')]
                          .map(c => Number(c.dataset.autorId));
    const currentRels  = state.livrosAutores.filter(la => la.idLivro === livroId);
    const currentIds   = currentRels.map(la => la.idAutor);

    for (const la of currentRels) {
      if (!selectedIds.includes(la.idAutor)) {
        await del('/api/livros-autores/' + la.id).catch(() => {});
      }
    }
    for (const aid of selectedIds) {
      if (!currentIds.includes(aid)) {
        await post('/api/livros-autores', { idLivro: livroId, idAutor: aid }).catch(() => {});
      }
    }
  }
}

async function saveEditora() {
  const nome = v('f-nome');
  if (!nome) throw new Error('Nome é obrigatório.');
  const body = { nome, cidade: v('f-cidade'), anoFundacao: Number(v('f-anoFundacao')) || 0 };
  if (state.editId) await put('/api/editoras/' + state.editId, body);
  else              await post('/api/editoras', body);
}

async function saveAutor() {
  const nome = v('f-nome');
  const data = v('f-dataNascimento');
  if (!nome) throw new Error('Nome é obrigatório.');
  if (!data) throw new Error('Data de nascimento é obrigatória.');
  const body = { nome, dataNascimento: data, biografia: v('f-biografia') };
  if (state.editId) await put('/api/autores/' + state.editId, body);
  else              await post('/api/autores', body);
}

async function saveUsuario() {
  const nome  = v('f-nome');
  const email = v('f-email');
  const senha = v('f-senha');
  if (!nome)  throw new Error('Nome é obrigatório.');
  if (!email) throw new Error('Email é obrigatório.');
  if (!state.editId && (!senha || senha.length < 4))
    throw new Error('Senha deve ter no mínimo 4 caracteres.');
  const body = { nome, email };
  if (senha) body.senha = senha;
  if (state.editId) await put('/api/usuarios/' + state.editId, body);
  else              await post('/api/usuarios', body);
}

/* ── Delete ──────────────────────────────────────────────── */
let pendingDeleteId = null;

function confirmDelete(id) {
  pendingDeleteId = id;
  const r = state.records.find(x => x.id === id);
  const name = r ? (r.titulo || r.nome || r.email || `#${id}`) : `#${id}`;
  document.getElementById('confirm-msg').textContent =
    `Deseja realmente excluir "${name}"? Esta ação não pode ser desfeita.`;
  document.getElementById('confirm-overlay').classList.add('open');
}

async function doDelete() {
  if (!pendingDeleteId) return;
  const id = pendingDeleteId;
  document.getElementById('confirm-overlay').classList.remove('open');
  pendingDeleteId = null;
  try {
    const cfg = MODULES[state.module];
    await del(cfg.apiPath + '/' + id);
    await loadCaches();
    await loadModule(state.module);
    toast('Registro excluído.', 'success');
  } catch (e) {
    toast('Não foi possível excluir: ' + e.message, 'error');
  }
}

/* ── Modal helpers ───────────────────────────────────────── */
function openModal()  { document.getElementById('modal-overlay').classList.add('open'); }
function closeModal() { document.getElementById('modal-overlay').classList.remove('open'); }

/* ── Toast ───────────────────────────────────────────────── */
function toast(msg, type = 'info') {
  const container = document.getElementById('toast-container');
  const el = document.createElement('div');
  el.className = `toast ${type}`;
  el.innerHTML = `<span class="toast-icon"></span><span class="toast-msg">${esc(msg)}</span>`;
  container.appendChild(el);
  setTimeout(() => {
    el.classList.add('out');
    setTimeout(() => el.remove(), 300);
  }, 3500);
}

/* ── Loading state ───────────────────────────────────────── */
function setLoading(on) {
  document.getElementById('table-loading').style.display = on ? 'flex' : 'none';
  document.getElementById('data-table').style.display    = on ? 'none' : '';
  document.getElementById('table-empty').style.display   = 'none';
}

/* ── Helpers ─────────────────────────────────────────────── */
function v(id) {
  const el = document.getElementById(id);
  return el ? el.value.trim() : '';
}

/* ── Event wiring ────────────────────────────────────────── */
document.querySelectorAll('.nav-item').forEach(btn => {
  btn.addEventListener('click', () => switchModule(btn.dataset.module));
});

document.getElementById('btn-new').addEventListener('click', openNew);

document.getElementById('search-input').addEventListener('input', e => {
  state.search = e.target.value;
  applyFiltersAndRender();
});

document.getElementById('sort-select').addEventListener('change', e => {
  const [key, dir] = e.target.value.split('-');
  state.sort = { key, dir };
  applyFiltersAndRender();
});

document.getElementById('modal-close').addEventListener('click',  closeModal);
document.getElementById('modal-cancel').addEventListener('click', closeModal);
document.getElementById('modal-save').addEventListener('click',   saveRecord);
document.getElementById('modal-overlay').addEventListener('click', e => {
  if (e.target === document.getElementById('modal-overlay')) closeModal();
});

document.getElementById('confirm-cancel').addEventListener('click', () => {
  document.getElementById('confirm-overlay').classList.remove('open');
  pendingDeleteId = null;
});
document.getElementById('confirm-ok').addEventListener('click', doDelete);

document.addEventListener('keydown', e => {
  if (e.key === 'Escape') {
    closeModal();
    document.getElementById('confirm-overlay').classList.remove('open');
  }
  if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
    e.preventDefault();
    document.getElementById('search-input').focus();
  }
});

/* ── Boot ────────────────────────────────────────────────── */
async function init() {
  setLoading(true);
  await loadCaches();
  renderFilterBar();
  await loadModule('livros');
  refreshAllBadges();
}

init();