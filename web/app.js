const API = '';  // mesmo host — requests vão para /api/...

//  ESTADO GLOBAL
let currentPage  = 'dashboard';
let currentOrder = {};          // { section: 'asc' | 'desc' }

//  TEMA CLARO / ESCURO
function initTheme() {
  const saved = localStorage.getItem('bibliosys-theme') || 'dark';
  document.documentElement.setAttribute('data-theme', saved);
  updateThemeBtn(saved);
}

function toggleTheme() {
  const current = document.documentElement.getAttribute('data-theme') || 'dark';
  const next = current === 'dark' ? 'light' : 'dark';
  document.documentElement.setAttribute('data-theme', next);
  localStorage.setItem('bibliosys-theme', next);
  updateThemeBtn(next);
}

function updateThemeBtn(theme) {
  const btn = document.getElementById('theme-toggle');
  if (btn) btn.textContent = theme === 'dark' ? '☀️' : '🌙';
}

//  NAVEGAÇÃO
function navigate(page, el) {
  document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
  el.classList.add('active');

  document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
  document.getElementById('page-' + page).classList.add('active');

  const labels = {
    dashboard:     'Dashboard',
    livros:        'Livros',
    autores:       'Autores',
    editoras:      'Editoras',
    livros_autores:'Livros-Autores',
    tags:          'Tags',
    tags_livros:   'Tags-Livros',
    usuarios:      'Usuários'
  };
  document.getElementById('breadcrumb-current').textContent = labels[page] || page;
  currentPage = page;

  if (page === 'dashboard') loadDashboard();
  else                      loadTable(page);
}

//  API HELPERS
async function apiFetch(path, opts = {}) {
  const res = await fetch(API + path, {
    headers: { 'Content-Type': 'application/json' },
    ...opts
  });
  const text = await res.text();
  const data = text ? JSON.parse(text) : {};
  if (!res.ok) throw new Error(data.erro || `HTTP ${res.status}`);
  return data;
}

const apiGet    = path         => apiFetch(path);
const apiPost   = (path, body) => apiFetch(path, { method: 'POST',   body: JSON.stringify(body) });
const apiPut    = (path, body) => apiFetch(path, { method: 'PUT',    body: JSON.stringify(body) });
const apiDelete = path         => apiFetch(path, { method: 'DELETE' });

//  DASHBOARD
async function loadDashboard() {
  const sections = [
    { key: 'livros',         endpoint: '/api/livros',         label: '📖 Livros',         statId: 'dash-livros'     },
    { key: 'autores',        endpoint: '/api/autores',        label: '✍️ Autores',        statId: 'dash-autores'    },
    { key: 'editoras',       endpoint: '/api/editoras',       label: '🏢 Editoras',       statId: 'dash-editoras'   },
    { key: 'livros_autores', endpoint: '/api/livros-autores', label: '🔗 Livros-Autores', statId: 'dash-vinculos'   },
    { key: 'tags',           endpoint: '/api/tags',           label: '🏷️ Tags',           statId: 'dash-tags'       },
    { key: 'tags_livros',    endpoint: '/api/tags-livros',    label: '🔖 Tags-Livros',    statId: 'dash-tags-livros'},
    { key: 'usuarios',       endpoint: '/api/usuarios',       label: '👤 Usuários',       statId: 'dash-usuarios'   },
  ];

  for (const s of sections) {
    try {
      const data = await apiGet(s.endpoint);
      const count = Array.isArray(data) ? data.length : 0;
      const el = document.getElementById(s.statId);
      if (el) el.textContent = count;
      const dtEl = document.getElementById('dt-' + s.key);
      if (dtEl) dtEl.textContent = count;
    } catch { /* silencioso */ }
  }
}

//  TABELAS — carregamento e renderização
async function loadTable(section) {
  const order = currentOrder[section] || 'asc';
  const endpointMap = {
    livros:         `/api/livros${order === 'desc' ? '?ordem=id-desc' : ''}`,
    autores:        '/api/autores',
    editoras:       '/api/editoras',
    livros_autores: '/api/livros-autores',
    tags:           `/api/tags${order === 'desc' ? '?ordem=id-desc' : ''}`,
    tags_livros:    '/api/tags-livros',
    usuarios:       '/api/usuarios',
  };

  const tbodyMap = {
    livros:         'livros-tbody',
    autores:        'autores-tbody',
    editoras:       'editoras-tbody',
    livros_autores: 'livros-autores-tbody',
    tags:           'tags-tbody',
    tags_livros:    'tags-livros-tbody',
    usuarios:       'usuarios-tbody',
  };

  const tbodyId = tbodyMap[section];
  const tbody = document.getElementById(tbodyId);
  if (!tbody) return;

  tbody.innerHTML = `<tr class="loading-row"><td colspan="10"><span class="spinner"></span>Carregando...</td></tr>`;

  try {
    const data = await apiGet(endpointMap[section]);
    renderRows(section, tbody, data);
  } catch (e) {
    tbody.innerHTML = `<tr><td colspan="10"><div class="empty-state"><div class="empty-icon">⚠️</div><p>Erro ao carregar: ${e.message}</p></div></td></tr>`;
  }
}

// Helper para renderizar chips de uma lista [{id, nome}] ou array de strings
function renderChips(items, colorClass = '') {
  if (!items || items.length === 0) return '<span style="color:var(--text3)">—</span>';
  return items.map(item => {
    const label = typeof item === 'string' ? item : (item.nome || item.titulo || String(item));
    return `<span class="tag-pill ${colorClass}" style="margin:1px 2px">${esc(label)}</span>`;
  }).join('');
}

function renderRows(section, tbody, data) {
  if (!data || data.length === 0) {
    tbody.innerHTML = `<tr><td colspan="10"><div class="empty-state"><div class="empty-icon">📭</div><p>Nenhum registro encontrado</p></div></td></tr>`;
    return;
  }

  const renderers = {
    // Livros: mostra nome da editora, chips de autores e chips de tags
    livros: r => `
      <td><span class="id-badge">#${r.id}</span></td>
      <td><strong>${esc(r.titulo)}</strong></td>
      <td><span class="tag-pill blue">${esc(r.nomeEditora || ('Ed. #' + r.idEditora))}</span></td>
      <td><span class="id-badge">${esc(r.isbn) || '—'}</span></td>
      <td>${r.anoPublicacao || '—'}</td>
      <td>R$ ${r.preco ? r.preco.toFixed(2) : '0,00'}</td>
      <td style="min-width:120px">${renderChips(r.autores)}</td>
      <td style="min-width:120px">${renderChips(r.tags, 'yellow')}</td>
      <td class="actions-cell">
        <button class="btn btn-ghost btn-sm" onclick="openModal('livros',${r.id})">✏️</button>
        <button class="btn btn-danger btn-sm" onclick="confirmDelete('livros',${r.id},'${esc(r.titulo)}')">🗑️</button>
      </td>`,

    // Autores: mostra chips dos livros vinculados
    autores: r => `
      <td><span class="id-badge">#${r.id}</span></td>
      <td><div class="row-name-cell"><div class="row-avatar">${esc(r.nome).charAt(0).toUpperCase()}</div><strong>${esc(r.nome)}</strong></div></td>
      <td>${esc(r.dataNascimentoFormatada) || '—'}</td>
      <td style="max-width:200px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;color:var(--text2)">${esc(r.biografia) || '—'}</td>
      <td style="min-width:140px">${renderChips(r.livros ? r.livros.map(l => ({nome: l.titulo})) : [], 'blue')}</td>
      <td class="actions-cell">
        <button class="btn btn-ghost btn-sm" onclick="openModal('autores',${r.id})">✏️</button>
        <button class="btn btn-danger btn-sm" onclick="confirmDelete('autores',${r.id},'${esc(r.nome)}')">🗑️</button>
      </td>`,

    editoras: r => `
      <td><span class="id-badge">#${r.id}</span></td>
      <td><strong>${esc(r.nome)}</strong></td>
      <td>${esc(r.cidade) || '—'}</td>
      <td>${r.anoFundacao || '—'}</td>
      <td class="actions-cell">
        <button class="btn btn-ghost btn-sm" onclick="openModal('editoras',${r.id})">✏️</button>
        <button class="btn btn-danger btn-sm" onclick="confirmDelete('editoras',${r.id},'${esc(r.nome)}')">🗑️</button>
      </td>`,

    // Livros-Autores: mostra nomes ao invés de IDs
    livros_autores: r => `
      <td><span class="id-badge">#${r.id}</span></td>
      <td><span class="tag-pill blue">📖 ${esc(r.nomeLivro || ('Livro #' + r.idLivro))}</span></td>
      <td><span class="tag-pill">✍️ ${esc(r.nomeAutor || ('Autor #' + r.idAutor))}</span></td>
      <td class="actions-cell">
        <button class="btn btn-danger btn-sm" onclick="confirmDelete('livros_autores',${r.id},'vínculo #${r.id}')">🗑️</button>
      </td>`,

    tags: r => `
      <td><span class="id-badge">#${r.id}</span></td>
      <td><span class="tag-pill">🏷️ ${esc(r.nome)}</span></td>
      <td class="actions-cell">
        <button class="btn btn-ghost btn-sm" onclick="openModal('tags',${r.id})">✏️</button>
        <button class="btn btn-danger btn-sm" onclick="confirmDelete('tags',${r.id},'${esc(r.nome)}')">🗑️</button>
      </td>`,

    // Tags-Livros: mostra nomes ao invés de IDs
    tags_livros: r => `
      <td><span class="id-badge">#${r.id}</span></td>
      <td><span class="tag-pill yellow">🏷️ ${esc(r.nomeTag || ('Tag #' + r.idTag))}</span></td>
      <td><span class="tag-pill blue">📖 ${esc(r.nomeLivro || ('Livro #' + r.idLivro))}</span></td>
      <td class="actions-cell">
        <button class="btn btn-danger btn-sm" onclick="confirmDelete('tags_livros',${r.id},'vínculo #${r.id}')">🗑️</button>
      </td>`,

    // Usuários: mostra redesSociais como chips
    usuarios: r => `
      <td><span class="id-badge">#${r.id}</span></td>
      <td><div class="row-name-cell"><div class="row-avatar">${esc(r.nome).charAt(0).toUpperCase()}</div><strong>${esc(r.nome)}</strong></div></td>
      <td style="color:var(--text2)">${esc(r.email)}</td>
      <td style="min-width:160px">${renderChips(r.redesSociais || [], 'blue')}</td>
      <td class="actions-cell">
        <button class="btn btn-ghost btn-sm" onclick="openModal('usuarios',${r.id})">✏️</button>
        <button class="btn btn-danger btn-sm" onclick="confirmDelete('usuarios',${r.id},'${esc(r.nome)}')">🗑️</button>
      </td>`,
  };

  const renderer = renderers[section];
  if (!renderer) return;

  window._tableCache = window._tableCache || {};
  window._tableCache[section] = data;

  tbody.innerHTML = data.map(r => `<tr>${renderer(r)}</tr>`).join('');
}

//  BUSCA POR HASH EXTENSÍVEL — O(1) amortizado
const _hashEndpointMap = {
  livros:         (id) => `/api/livros/${id}`,
  autores:        (id) => `/api/autores/${id}`,
  editoras:       (id) => `/api/editoras/${id}`,
  livros_autores: (id) => `/api/livros-autores/${id}`,
  tags:           (id) => `/api/tags/${id}`,
  tags_livros:    (id) => `/api/tags-livros/${id}`,
  usuarios:       (id) => `/api/usuarios/${id}`,
};

const _hashTbodyMap = {
  livros:         'livros-tbody',
  autores:        'autores-tbody',
  editoras:       'editoras-tbody',
  livros_autores: 'livros-autores-tbody',
  tags:           'tags-tbody',
  tags_livros:    'tags-livros-tbody',
  usuarios:       'usuarios-tbody',
};

const _hashCols = {
  livros: 9, autores: 6, editoras: 5,
  livros_autores: 4, tags: 3, tags_livros: 4, usuarios: 5
};

async function buscarPorHash(section) {
  const input = document.getElementById(`${section}-hash-input`);
  const id    = input ? parseInt(input.value) : NaN;

  if (!id || id < 1) {
    toast('Digite um ID válido para buscar', true);
    return;
  }

  const tbody = document.getElementById(_hashTbodyMap[section]);
  const cols  = _hashCols[section] || 6;
  tbody.innerHTML = `<tr class="loading-row"><td colspan="${cols}"><span class="spinner"></span>Consultando Hash Extensível...</td></tr>`;

  try {
    const record = await apiGet(_hashEndpointMap[section](id));
    // A API retorna objeto único — envolve em array para renderRows
    renderRows(section, tbody, [record]);

    // Destaca a linha encontrada
    const row = tbody.querySelector('tr');
    if (row) {
      row.classList.add('hash-result-row');
      setTimeout(() => row.classList.remove('hash-result-row'), 2500);
    }

    toast(`Registro #${id} localizado via Hash Extensível — O(1)`, false, true);
  } catch (e) {
    tbody.innerHTML = `<tr><td colspan="${cols}"><div class="empty-state"><div class="empty-icon">🔍</div><p>Nenhum registro encontrado com ID #${id}</p></div></td></tr>`;
    toast(`ID #${id} não encontrado`, true);
  }
}

//  ORDENAÇÃO (Árvore B+ crescente / decrescente)
function setOrder(section, order, btnEl) {
  currentOrder[section] = order;
  const parent = btnEl.closest('.order-toggle');
  if (parent) parent.querySelectorAll('.order-btn').forEach(b => b.classList.remove('active'));
  btnEl.classList.add('active');
  loadTable(section);
}

//  SELECTS DINÂMICOS — Livros-Autores e Tags-Livros

// Cache dos dados para os selects
let _selectCache = { livros: [], autores: [], tags: [] };

async function loadSelectOptions() {
  try {
    const [livros, autores, tags] = await Promise.all([
      apiGet('/api/livros'),
      apiGet('/api/autores'),
      apiGet('/api/tags'),
    ]);
    _selectCache.livros  = Array.isArray(livros)  ? livros  : [];
    _selectCache.autores = Array.isArray(autores) ? autores : [];
    _selectCache.tags    = Array.isArray(tags)    ? tags    : [];
  } catch (e) {
    // silencioso — o modal mostrará "sem dados"
  }
}

//  VÍNCULOS INLINE — chips dentro dos modais de Livro e Autor

let _vinculosState = {};

function initVinculos(section, record) {
  if (section === 'livros') {
    const autores = (record && record.autores) ? record.autores.map(a => ({
      id: a.id, nome: a.nome, vinculoId: a.vinculoId || null
    })) : [];
    const tags = (record && record.tags) ? record.tags.map(t => ({
      id: t.id, nome: t.nome, vinculoId: t.vinculoId || null
    })) : [];
    _vinculosState.livros = {
      autoresIniciais: autores.map(x => ({...x})),
      autoresAtuais:   autores.map(x => ({...x})),
      tagsIniciais:    tags.map(x => ({...x})),
      tagsAtuais:      tags.map(x => ({...x})),
    };
    renderVinculoChips('livros', 'autor');
    renderVinculoChips('livros', 'tag');
    // Popula datalists para autocomplete
    populateDatalist('livros-autor-datalist', _selectCache.autores, 'nome');
    populateDatalist('livros-tag-datalist',   _selectCache.tags,    'nome');
    // Limpa campos de busca
    const ia = document.getElementById('livros-autor-search-input');
    const it = document.getElementById('livros-tag-search-input');
    if (ia) ia.value = '';
    if (it) it.value = '';
  } else if (section === 'autores') {
    const livros = (record && record.livros) ? record.livros.map(l => ({
      id: l.id, titulo: l.titulo, vinculoId: l.vinculoId || null
    })) : [];
    _vinculosState.autores = {
      livrosIniciais: livros.map(x => ({...x})),
      livrosAtuais:   livros.map(x => ({...x})),
    };
    renderVinculoChips('autores', 'livro');
    populateDatalist('autores-livro-datalist', _selectCache.livros, 'titulo');
    const il = document.getElementById('autores-livro-search-input');
    if (il) il.value = '';
  }
}

function populateDatalist(datalistId, items, labelField) {
  const dl = document.getElementById(datalistId);
  if (!dl) return;
  dl.innerHTML = items.map(item =>
    `<option value="${esc(item[labelField])}" data-id="${item.id}">`
  ).join('');
}

function filterVinculoOptions(datalistId, items, query, labelField) {
  // Datalist filtra nativo do browser — só popula se precisar restringir
  populateDatalist(datalistId, items.filter(i =>
    i[labelField].toLowerCase().includes(query.toLowerCase())
  ), labelField);
}

function renderVinculoChips(section, tipo) {
  // section: 'livros' | 'autores'  ; tipo: 'autor' | 'tag' | 'livro'
  let items, containerId, labelField;
  if (section === 'livros' && tipo === 'autor') {
    items = _vinculosState.livros?.autoresAtuais || [];
    containerId = 'livros-autores-chips';
    labelField = 'nome';
  } else if (section === 'livros' && tipo === 'tag') {
    items = _vinculosState.livros?.tagsAtuais || [];
    containerId = 'livros-tags-chips';
    labelField = 'nome';
  } else if (section === 'autores' && tipo === 'livro') {
    items = _vinculosState.autores?.livrosAtuais || [];
    containerId = 'autores-livros-chips';
    labelField = 'titulo';
  }

  const container = document.getElementById(containerId);
  if (!container) return;

  if (!items || items.length === 0) {
    container.innerHTML = '<span class="vinculos-empty">Nenhum vínculo ainda</span>';
    return;
  }

  container.innerHTML = items.map((item, idx) =>
    `<span class="vinculos-chip">
      ${esc(item[labelField])}
      <button type="button" class="vinculos-chip-remove" onclick="removeVinculo('${section}','${tipo}',${idx})" title="Remover">✕</button>
    </span>`
  ).join('');
}

function addVinculo(section, tipo) {
  let inputId, cacheKey, labelField;
  if (section === 'livros' && tipo === 'autor') {
    inputId = 'livros-autor-search-input'; cacheKey = 'autores'; labelField = 'nome';
  } else if (section === 'livros' && tipo === 'tag') {
    inputId = 'livros-tag-search-input'; cacheKey = 'tags'; labelField = 'nome';
  } else if (section === 'autores' && tipo === 'livro') {
    inputId = 'autores-livro-search-input'; cacheKey = 'livros'; labelField = 'titulo';
  }

  const input = document.getElementById(inputId);
  const query = input ? input.value.trim() : '';
  if (!query) { toast('Digite o nome para buscar', true); return; }

  // Encontra o item pelo label (case-insensitive)
  const found = _selectCache[cacheKey].find(i =>
    i[labelField].toLowerCase() === query.toLowerCase()
  );
  if (!found) {
    toast(`"${query}" não encontrado. Escolha da lista de sugestões.`, true);
    return;
  }

  // Evita duplicatas
  let lista;
  if (section === 'livros' && tipo === 'autor')  lista = _vinculosState.livros.autoresAtuais;
  if (section === 'livros' && tipo === 'tag')     lista = _vinculosState.livros.tagsAtuais;
  if (section === 'autores' && tipo === 'livro')  lista = _vinculosState.autores.livrosAtuais;

  if (lista.some(x => x.id === found.id)) {
    toast('Esse vínculo já existe', true); return;
  }

  lista.push({ id: found.id, [labelField]: found[labelField], vinculoId: null });
  input.value = '';
  renderVinculoChips(section, tipo);
}

function removeVinculo(section, tipo, idx) {
  if (section === 'livros' && tipo === 'autor')  _vinculosState.livros.autoresAtuais.splice(idx, 1);
  if (section === 'livros' && tipo === 'tag')    _vinculosState.livros.tagsAtuais.splice(idx, 1);
  if (section === 'autores' && tipo === 'livro') _vinculosState.autores.livrosAtuais.splice(idx, 1);
  renderVinculoChips(section, tipo);
}


async function syncVinculos(section, recordId) {
  const erros = [];

  if (section === 'livros') {
    const st = _vinculosState.livros;

    // Autores: remover os que saíram
    for (const ini of st.autoresIniciais) {
      if (!st.autoresAtuais.some(a => a.id === ini.id)) {
        // Precisa do id do vínculo — busca via API
        try {
          const todos = await apiGet('/api/livros-autores');
          const vinculo = todos.find(v => v.idLivro === recordId && v.idAutor === ini.id);
          if (vinculo) await apiDelete(`/api/livros-autores/${vinculo.id}`);
        } catch (e) { erros.push(e.message); }
      }
    }
    // Autores: criar os novos
    for (const cur of st.autoresAtuais) {
      if (!st.autoresIniciais.some(a => a.id === cur.id)) {
        try { await apiPost('/api/livros-autores', { idLivro: recordId, idAutor: cur.id }); }
        catch (e) { erros.push(e.message); }
      }
    }

    // Tags: remover
    for (const ini of st.tagsIniciais) {
      if (!st.tagsAtuais.some(t => t.id === ini.id)) {
        try {
          const todos = await apiGet('/api/tags-livros');
          const vinculo = todos.find(v => v.idLivro === recordId && v.idTag === ini.id);
          if (vinculo) await apiDelete(`/api/tags-livros/${vinculo.id}`);
        } catch (e) { erros.push(e.message); }
      }
    }
    // Tags: criar
    for (const cur of st.tagsAtuais) {
      if (!st.tagsIniciais.some(t => t.id === cur.id)) {
        try { await apiPost('/api/tags-livros', { idTag: cur.id, idLivro: recordId }); }
        catch (e) { erros.push(e.message); }
      }
    }

  } else if (section === 'autores') {
    const st = _vinculosState.autores;

    // Livros: remover
    for (const ini of st.livrosIniciais) {
      if (!st.livrosAtuais.some(l => l.id === ini.id)) {
        try {
          const todos = await apiGet('/api/livros-autores');
          const vinculo = todos.find(v => v.idLivro === ini.id && v.idAutor === recordId);
          if (vinculo) await apiDelete(`/api/livros-autores/${vinculo.id}`);
        } catch (e) { erros.push(e.message); }
      }
    }
    // Livros: criar
    for (const cur of st.livrosAtuais) {
      if (!st.livrosIniciais.some(l => l.id === cur.id)) {
        try { await apiPost('/api/livros-autores', { idLivro: cur.id, idAutor: recordId }); }
        catch (e) { erros.push(e.message); }
      }
    }
  }

  if (erros.length > 0) toast('Alguns vínculos falharam: ' + erros.join('; '), true);
}

function populateSelect(selectId, items, labelFn) {
  const sel = document.getElementById(selectId);
  if (!sel) return;
  sel.innerHTML = items.length === 0
    ? '<option value="" disabled>Nenhum registro encontrado</option>'
    : items.map(item => `<option value="${item.id}">${esc(labelFn(item))}</option>`).join('');
  // Nenhum item pré-selecionado por padrão
  sel.selectedIndex = -1;
}

function filterSelectOptions(selectId, query) {
  const sel = document.getElementById(selectId);
  if (!sel) return;
  const q = query.toLowerCase();
  Array.from(sel.options).forEach(opt => {
    opt.style.display = opt.text.toLowerCase().includes(q) ? '' : 'none';
  });
}

function getSelectValue(selectId) {
  const sel = document.getElementById(selectId);
  return sel && sel.value ? parseInt(sel.value) : null;
}

function clearSelectSearch(searchId, selectId, items, labelFn) {
  const inp = document.getElementById(searchId);
  if (inp) inp.value = '';
  populateSelect(selectId, items, labelFn);
}

//  REDES SOCIAIS — widget de chips editáveis
let redesSociaisAtual = [];

function initRedesSociais(lista) {
  redesSociaisAtual = lista ? [...lista] : [];
  renderRedesSociais();
}

function renderRedesSociais() {
  const container = document.getElementById('rs-chips');
  if (!container) return;
  container.innerHTML = redesSociaisAtual.map((rs, i) =>
    `<span class="rs-chip">
      ${esc(rs)}
      <button class="rs-chip-remove" onclick="removeRedeSocial(${i})" title="Remover">✕</button>
    </span>`
  ).join('');
}

function addRedeSocial() {
  const input = document.getElementById('rs-input');
  const val = input ? input.value.trim() : '';
  if (!val) return;
  redesSociaisAtual.push(val);
  input.value = '';
  renderRedesSociais();
}

function removeRedeSocial(i) {
  redesSociaisAtual.splice(i, 1);
  renderRedesSociais();
}

//  MODALS — abertura e preenchimento
async function openModal(section, id) {
  const isEdit = !!id;
  let record = null;

  if (isEdit) {
    const cache = window._tableCache && window._tableCache[section];
    record = cache ? cache.find(r => r.id === id) : null;
    if (!record) {
      try { record = await apiGet(`/api/${section.replace(/_/g,'-')}/${id}`); }
      catch { toast('Erro ao carregar registro', true); return; }
    }
  }

  const titles = {
    livros:         isEdit ? 'Editar Livro'         : 'Novo Livro',
    autores:        isEdit ? 'Editar Autor'          : 'Novo Autor',
    editoras:       isEdit ? 'Editar Editora'        : 'Nova Editora',
    livros_autores: 'Novo Vínculo Livro-Autor',
    tags:           isEdit ? 'Editar Tag'            : 'Nova Tag',
    tags_livros:    'Novo Vínculo Tag-Livro',
    usuarios:       isEdit ? 'Editar Usuário'        : 'Novo Usuário',
  };

  document.getElementById(`modal-${section}-title`).textContent = titles[section];

  // Limpa inputs de texto e textareas (mas não os selects — estes são repovoados abaixo)
  document.querySelectorAll(`#modal-${section} input[type="text"], #modal-${section} input[type="number"], #modal-${section} input[type="email"], #modal-${section} input[type="password"], #modal-${section} textarea`)
    .forEach(f => f.value = '');
  document.getElementById(`${section}-edit-id`).value = id || '';

  if (record) {
    if (section === 'livros') {
      setVal(`livros-titulo`,       record.titulo);
      setVal(`livros-idEditora`,    record.idEditora);
      setVal(`livros-isbn`,         new String(record.isbn || '').trim());
      setVal(`livros-anoPublicacao`,record.anoPublicacao);
      setVal(`livros-preco`,        record.preco);
      setVal(`livros-sinopse`,      record.sinopse);
      setVal(`livros-generos`,      (record.generos || []).join(', '));
      await loadSelectOptions();
      initVinculos('livros', record);
    } else if (section === 'autores') {
      setVal(`autores-nome`,           record.nome);
      setVal(`autores-dataNascimento`, record.dataNascimentoFormatada || '');
      setVal(`autores-biografia`,      record.biografia);
      await loadSelectOptions();
      initVinculos('autores', record);
    } else if (section === 'editoras') {
      setVal(`editoras-nome`,        record.nome);
      setVal(`editoras-cidade`,      record.cidade);
      setVal(`editoras-anoFundacao`, record.anoFundacao);
    } else if (section === 'tags') {
      setVal(`tags-nome`, record.nome);
    } else if (section === 'livros_autores') {
      // Para edição não há suporte (vínculo é imutável), mas deixa selects prontos
    } else if (section === 'tags_livros') {
      // Para edição não há suporte (vínculo é imutável), mas deixa selects prontos
    } else if (section === 'usuarios') {
      setVal(`usuarios-nome`,  record.nome);
      setVal(`usuarios-email`, record.email);
      const senhaGroup = document.getElementById('usuarios-senha-group');
      if (senhaGroup) senhaGroup.style.display = isEdit ? 'none' : '';
      // Inicializa widget de redes sociais
      initRedesSociais(record.redesSociais || []);
    }
  } else {
    const senhaGroup = document.getElementById('usuarios-senha-group');
    if (senhaGroup) senhaGroup.style.display = '';
    if (section === 'usuarios') initRedesSociais([]);
    if (section === 'livros') {
      await loadSelectOptions();
      initVinculos('livros', null);
    }
    if (section === 'autores') {
      await loadSelectOptions();
      initVinculos('autores', null);
    }
  }

  // Para modais de vínculo: carrega/popula os selects dinâmicos
  if (section === 'livros_autores') {
    await loadSelectOptions();
    // Reseta campos de busca
    const ls = document.getElementById('livros_autores-livro-search');
    const as = document.getElementById('livros_autores-autor-search');
    if (ls) ls.value = '';
    if (as) as.value = '';
    populateSelect('livros_autores-idLivro', _selectCache.livros,  r => `#${r.id} — ${r.titulo}`);
    populateSelect('livros_autores-idAutor', _selectCache.autores, r => `#${r.id} — ${r.nome}`);
  } else if (section === 'tags_livros') {
    await loadSelectOptions();
    const ts = document.getElementById('tags_livros-tag-search');
    const ls = document.getElementById('tags_livros-livro-search');
    if (ts) ts.value = '';
    if (ls) ls.value = '';
    populateSelect('tags_livros-idTag',   _selectCache.tags,   r => `#${r.id} — ${r.nome}`);
    populateSelect('tags_livros-idLivro', _selectCache.livros, r => `#${r.id} — ${r.titulo}`);
  }

  document.getElementById(`modal-${section}`).classList.add('open');
}

function closeModal(section) {
  document.getElementById(`modal-${section}`).classList.remove('open');
}

function setVal(id, val) {
  const el = document.getElementById(id);
  if (el) el.value = val ?? '';
}

//  SAVE — POST ou PUT
async function saveRecord(section) {
  const editId = document.getElementById(`${section}-edit-id`).value;
  const isEdit = !!editId;
  let body = {};

  try {
    if (section === 'livros') {
      const titulo = getVal('livros-titulo');
      if (!titulo) { toast('Título é obrigatório', true); return; }
      body = {
        titulo,
        idEditora:    parseInt(getVal('livros-idEditora')) || 0,
        isbn:         getVal('livros-isbn'),
        anoPublicacao:parseInt(getVal('livros-anoPublicacao')) || 0,
        preco:        parseFloat(getVal('livros-preco')) || 0,
        sinopse:      getVal('livros-sinopse'),
        generos:      getVal('livros-generos').split(',').map(s => s.trim()).filter(Boolean)
      };
    } else if (section === 'autores') {
      const nome = getVal('autores-nome');
      const data = getVal('autores-dataNascimento');
      if (!nome) { toast('Nome é obrigatório', true); return; }
      if (!data)  { toast('Data de nascimento é obrigatória (dd/MM/yyyy)', true); return; }
      body = {
        nome,
        dataNascimento: data,
        biografia: getVal('autores-biografia')
      };
    } else if (section === 'editoras') {
      const nome = getVal('editoras-nome');
      if (!nome) { toast('Nome é obrigatório', true); return; }
      body = {
        nome,
        cidade:      getVal('editoras-cidade'),
        anoFundacao: parseInt(getVal('editoras-anoFundacao')) || 0
      };
    } else if (section === 'livros_autores') {
      const idLivro = getSelectValue('livros_autores-idLivro');
      const idAutor = getSelectValue('livros_autores-idAutor');
      if (!idLivro) { toast('Selecione um livro na lista', true); return; }
      if (!idAutor) { toast('Selecione um autor na lista', true); return; }
      body = { idLivro, idAutor };
    } else if (section === 'tags') {
      const nome = getVal('tags-nome').trim();
      if (!nome) { toast('Nome da tag é obrigatório', true); return; }
      body = { nome };
    } else if (section === 'tags_livros') {
      const idTag   = getSelectValue('tags_livros-idTag');
      const idLivro = getSelectValue('tags_livros-idLivro');
      if (!idTag)   { toast('Selecione uma tag na lista', true); return; }
      if (!idLivro) { toast('Selecione um livro na lista', true); return; }
      body = { idTag, idLivro };
    } else if (section === 'usuarios') {
      const nome  = getVal('usuarios-nome');
      const email = getVal('usuarios-email');
      if (!nome || !email) { toast('Nome e e-mail são obrigatórios', true); return; }
      body = { nome, email, redesSociais: redesSociaisAtual };
      const senha = getVal('usuarios-senha');
      if (senha) body.senha = senha;
      if (!isEdit && !senha) { toast('Senha obrigatória para novo usuário', true); return; }
    }

    const apiPath = '/api/' + section.replace(/_/g, '-');

    let savedId = editId ? parseInt(editId) : null;
    if (isEdit) {
      await apiPut(`${apiPath}/${editId}`, body);
      toast(`Registro atualizado com sucesso!`);
    } else {
      const res = await apiPost(apiPath, body);
      // A API retorna { id: N } ou { idCriado: N } dependendo do endpoint
      savedId = res.id || res.idCriado || res.idAutor || res.idLivro || null;
      toast(`Registro criado com sucesso!`);
    }

    // Sincroniza vínculos de autores/tags para livros, e livros para autores
    if ((section === 'livros' || section === 'autores') && savedId) {
      await syncVinculos(section, savedId);
    }

    closeModal(section);
    loadTable(section);
    loadDashboard();

  } catch (e) {
    toast(e.message, true);
  }
}

function getVal(id) {
  const el = document.getElementById(id);
  return el ? el.value.trim() : '';
}

//  DELETE
let pendingDelete = null;

function confirmDelete(section, id, label) {
  pendingDelete = { section, id };
  const sectionLabels = {
    livros: 'livro', autores: 'autor', editoras: 'editora',
    livros_autores: 'vínculo', tags: 'tag', tags_livros: 'vínculo tag-livro', usuarios: 'usuário'
  };
  document.getElementById('confirm-msg').textContent =
    `Tem certeza que deseja excluir o(a) ${sectionLabels[section] || 'registro'} "${label}"? Esta ação não pode ser desfeita.`;
  document.getElementById('confirm-dialog').classList.add('open');
}

async function executeDelete() {
  if (!pendingDelete) return;
  const { section, id } = pendingDelete;
  const apiPath = '/api/' + section.replace(/_/g, '-');
  try {
    await apiDelete(`${apiPath}/${id}`);
    toast('Registro excluído com sucesso!');
    closeConfirm();
    loadTable(section);
    loadDashboard();
  } catch (e) {
    toast(e.message, true);
    closeConfirm();
  }
}

function closeConfirm() {
  document.getElementById('confirm-dialog').classList.remove('open');
  pendingDelete = null;
}

//  TOAST
function toast(msg, error = false, warn = false) {
  const container = document.getElementById('toast-container');
  const el = document.createElement('div');
  el.className = 'toast' + (error ? ' error' : warn ? ' warn' : '');
  el.textContent = error ? '⚠️ ' + msg : warn ? '⚡ ' + msg : '✅ ' + msg;
  container.appendChild(el);
  setTimeout(() => el.remove(), 3200);
}

//  HELPERS
function esc(str) {
  return String(str ?? '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;').replace(/'/g,'&#39;');
}

// Fecha modais ao clicar no overlay
document.addEventListener('DOMContentLoaded', () => {
  initTheme();

  document.querySelectorAll('.modal-overlay').forEach(overlay => {
    overlay.addEventListener('click', e => {
      if (e.target === overlay) {
        overlay.classList.remove('open');
        pendingDelete = null;
      }
    });
  });

  loadDashboard();
});