/* ============================================================
   app.js — Biblioteca Pessoal · AEDsIII G9
   Views: livros | editoras | tags
   Tags agora são uma view de tabela completa com CRUD próprio.
   ============================================================ */

'use strict';

const API_BASE = 'http://localhost:8080/api';

// ── STATE ────────────────────────────────────────────────────
const state = {
  view:     'livros',
  livros:   [],
  editoras: [],
  tags:     [],   // todas as tags (todos os livros)
  query:    '',
};

// ── DOM HELPERS ──────────────────────────────────────────────
const $  = (sel, ctx = document) => ctx.querySelector(sel);
const $$ = (sel, ctx = document) => [...ctx.querySelectorAll(sel)];

// ── API ──────────────────────────────────────────────────────
const api = {
  async get(path) {
    const r = await fetch(`${API_BASE}${path}`);
    if (!r.ok) throw new Error(`HTTP ${r.status}`);
    return r.json();
  },
  async post(path, body) {
    const r = await fetch(`${API_BASE}${path}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    });
    if (!r.ok) throw new Error(`HTTP ${r.status}`);
    return r.json();
  },
  async put(path, body) {
    const r = await fetch(`${API_BASE}${path}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    });
    if (!r.ok) throw new Error(`HTTP ${r.status}`);
    return r.json();
  },
  async delete(path) {
    const r = await fetch(`${API_BASE}${path}`, { method: 'DELETE' });
    if (!r.ok) throw new Error(`HTTP ${r.status}`);
    return true;
  },
};

// ── TOAST ────────────────────────────────────────────────────
function toast(msg, type = 'info') {
  const box = $('#toastContainer');
  const t   = document.createElement('div');
  t.className = `toast toast-${type}`;
  t.innerHTML = `<span class="toast-dot"></span><span>${escHtml(msg)}</span>`;
  box.appendChild(t);
  setTimeout(() => {
    t.classList.add('hiding');
    t.addEventListener('animationend', () => t.remove(), { once: true });
  }, 3300);
}

// ── CONFIRM ──────────────────────────────────────────────────
function confirmDialog(msg) {
  return new Promise(resolve => {
    $('#confirmMsg').textContent = msg;
    $('#confirmBackdrop').classList.add('open');

    function close(val) {
      $('#confirmBackdrop').classList.remove('open');
      $('#confirmOk').replaceWith($('#confirmOk').cloneNode(true));
      $('#confirmCancel').replaceWith($('#confirmCancel').cloneNode(true));
      resolve(val);
    }
    $('#confirmOk').addEventListener('click',     () => close(true),  { once: true });
    $('#confirmCancel').addEventListener('click', () => close(false), { once: true });
    $('#confirmBackdrop').addEventListener('click', e => {
      if (e.target === $('#confirmBackdrop')) close(false);
    }, { once: true });
  });
}

// ── MODAL CRUD ───────────────────────────────────────────────
const Modal = {
  _resolve: null,
  open(title, bodyHTML) {
    $('#modalTitle').textContent = title;
    $('#modalBody').innerHTML    = bodyHTML;
    $('#modalBackdrop').classList.add('open');
    setTimeout(() => {
      const first = $('#modalBody input, #modalBody select, #modalBody textarea');
      if (first) first.focus();
    }, 50);
    return new Promise(res => { this._resolve = res; });
  },
  resolve(value) {
    $('#modalBackdrop').classList.remove('open');
    if (this._resolve) { this._resolve(value); this._resolve = null; }
  },
};

(function wireModal() {
  $('#modalSave').addEventListener('click',  () => Modal.resolve(collectForm('#modalBody')));
  $('#modalCancel').addEventListener('click',() => Modal.resolve(null));
  $('#modalClose').addEventListener('click', () => Modal.resolve(null));
  $('#modalBackdrop').addEventListener('click', e => {
    if (e.target === $('#modalBackdrop')) Modal.resolve(null);
  });
  // Enter salva (exceto textarea)
  document.addEventListener('keydown', e => {
    if (e.key === 'Enter' && $('#modalBackdrop').classList.contains('open') && e.target.tagName !== 'TEXTAREA') {
      e.preventDefault();
      Modal.resolve(collectForm('#modalBody'));
    }
    if (e.key === 'Escape') {
      if ($('#detailBackdrop').classList.contains('open'))  DetailModal.close();
      else if ($('#modalBackdrop').classList.contains('open')) Modal.resolve(null);
      else if ($('#confirmBackdrop').classList.contains('open')) {
        $('#confirmCancel').click();
      }
    }
  });
})();

function collectForm(ctxSel) {
  const data = {};
  $$('input, select, textarea', $(ctxSel)).forEach(inp => {
    if (!inp.name) return;
    if (inp.type === 'number') {
      const n = Number(inp.value);
      data[inp.name] = inp.value === '' || isNaN(n) ? null : n;
    } else {
      data[inp.name] = inp.value;
    }
  });
  return data;
}

// ── MODAL DETALHES ───────────────────────────────────────────
const DetailModal = {
  open(title, bodyHTML) {
    $('#detailTitle').textContent = title;
    $('#detailBody').innerHTML    = bodyHTML;
    $('#detailBackdrop').classList.add('open');
  },
  close() { $('#detailBackdrop').classList.remove('open'); },
};

(function wireDetail() {
  $('#detailClose').addEventListener('click', () => DetailModal.close());
  $('#detailBackdrop').addEventListener('click', e => {
    if (e.target === $('#detailBackdrop')) DetailModal.close();
  });
})();

// ── UTILS ────────────────────────────────────────────────────
function escHtml(s) {
  if (s == null) return '';
  return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}

function fmtPreco(v) {
  if (v == null || v === '' || isNaN(Number(v))) return '—';
  return Number(v).toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function editoraNomeRaw(id) {
  return state.editoras.find(e => e.id === id)?.nome ?? '';
}

function editoraNome(id) {
  const ed = state.editoras.find(e => e.id === id);
  return ed
    ? escHtml(ed.nome)
    : `<span class="sem-editora">${id ? 'ID ' + id : '—'}</span>`;
}

function livroTitulo(id) {
  const l = state.livros.find(l => l.id === id);
  return l ? escHtml(l.titulo) : `<span class="sem-editora">ID ${id}</span>`;
}

function livrosDaEditora(id) {
  return state.livros.filter(l => l.idEditora === id).length;
}

// Cor de tag cíclica (0–5) baseada no ID
function tagColor(id) { return id % 6; }

function tagChip(tag, idx) {
  const color = tagColor(typeof idx === 'number' ? idx : tag.id);
  return `<span class="tag-chip" data-color="${color}">${escHtml(tag.tag ?? tag)}</span>`;
}

function actionBtns(id, extras = '') {
  return `<div class="actions-group">
    ${extras}
    <button class="btn btn-icon btn-edit"   data-id="${id}" title="Editar">
      <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
        <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
        <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
      </svg>
    </button>
    <button class="btn btn-icon btn-delete" data-id="${id}" title="Excluir">
      <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
        <polyline points="3 6 5 6 21 6"/>
        <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/>
        <path d="M10 11v6M14 11v6"/>
        <path d="M9 6V4h6v2"/>
      </svg>
    </button>
  </div>`;
}

function renderEmpty(icon, title, desc) {
  return `<div class="empty-state">
    <div class="empty-icon">${icon}</div>
    <h3>${title}</h3>
    <p>${desc}</p>
  </div>`;
}

// ── RENDER LIVROS ────────────────────────────────────────────
function renderLivros() {
  const area  = $('#contentArea');
  const query = state.query.toLowerCase();

  const lista = state.livros.filter(l =>
    !query ||
    l.titulo?.toLowerCase().includes(query) ||
    l.isbn?.toLowerCase().includes(query) ||
    editoraNomeRaw(l.idEditora).toLowerCase().includes(query)
  );

  if (!lista.length) {
    area.innerHTML = `<div class="table-wrap fade-in">${renderEmpty('📖', 'Nenhum livro encontrado', query ? 'Tente outro termo de busca.' : 'Clique em "Novo" para cadastrar o primeiro livro.')}</div>`;
    return;
  }

  const rows = lista.map((l, i) => `
    <tr style="animation-delay:${i * 22}ms">
      <td class="td-id">${l.id}</td>
      <td class="td-title">${escHtml(l.titulo ?? '—')}</td>
      <td class="td-meta td-isbn"><span class="isbn-code">${escHtml(l.isbn ?? '—')}</span></td>
      <td class="td-meta">${l.anoPublicacao ?? '—'}</td>
      <td class="td-price">R$ ${fmtPreco(l.preco)}</td>
      <td class="td-meta">${editoraNome(l.idEditora)}</td>
      <td class="td-actions">${actionBtns(l.id,
        `<button class="btn btn-icon btn-detail" data-id="${l.id}" title="Tags &amp; Autores">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
            <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
          </svg>
        </button>`)
      }</td>
    </tr>`).join('');

  area.innerHTML = `
    <div class="table-wrap fade-in">
      <table>
        <thead><tr>
          <th>#</th><th>Título</th><th class="td-isbn">ISBN</th>
          <th>Ano</th><th>Preço</th><th>Editora</th><th>Ações</th>
        </tr></thead>
        <tbody>${rows}</tbody>
      </table>
    </div>`;

  $$('.btn-detail', area).forEach(b => b.addEventListener('click', () => openDetalhes(+b.dataset.id)));
  $$('.btn-edit',   area).forEach(b => b.addEventListener('click', () => editarLivro(+b.dataset.id)));
  $$('.btn-delete', area).forEach(b => b.addEventListener('click', () => excluirLivro(+b.dataset.id)));
}

// ── RENDER EDITORAS ──────────────────────────────────────────
function renderEditoras() {
  const area  = $('#contentArea');
  const query = state.query.toLowerCase();

  const lista = state.editoras.filter(e =>
    !query ||
    e.nome?.toLowerCase().includes(query) ||
    e.cidade?.toLowerCase().includes(query)
  );

  if (!lista.length) {
    area.innerHTML = `<div class="table-wrap fade-in">${renderEmpty('🏛️', 'Nenhuma editora encontrada', query ? 'Tente outro termo de busca.' : 'Clique em "Novo" para cadastrar a primeira editora.')}</div>`;
    return;
  }

  const rows = lista.map((e, i) => `
    <tr style="animation-delay:${i * 22}ms">
      <td class="td-id">${e.id}</td>
      <td class="td-title">${escHtml(e.nome ?? '—')}</td>
      <td class="td-meta">${escHtml(e.cidade ?? '—')}</td>
      <td class="td-meta">${e.anoFundacao ?? '—'}</td>
      <td class="td-meta">${livrosDaEditora(e.id)} livro(s)</td>
      <td class="td-actions">${actionBtns(e.id)}</td>
    </tr>`).join('');

  area.innerHTML = `
    <div class="table-wrap fade-in">
      <table>
        <thead><tr>
          <th>#</th><th>Nome</th><th>Cidade</th><th>Fundação</th><th>Livros</th><th>Ações</th>
        </tr></thead>
        <tbody>${rows}</tbody>
      </table>
    </div>`;

  $$('.btn-edit',   area).forEach(b => b.addEventListener('click', () => editarEditora(+b.dataset.id)));
  $$('.btn-delete', area).forEach(b => b.addEventListener('click', () => excluirEditora(+b.dataset.id)));
}

// ── RENDER TAGS ──────────────────────────────────────────────
function renderTags() {
  const area  = $('#contentArea');
  const query = state.query.toLowerCase();

  const lista = state.tags.filter(t =>
    !query ||
    t.tag?.toLowerCase().includes(query) ||
    livroTitulo(t.idLivro).toLowerCase().includes(query)
  );

  if (!lista.length) {
    area.innerHTML = `<div class="table-wrap fade-in">${renderEmpty('🏷️', 'Nenhuma tag encontrada', query ? 'Tente outro termo de busca.' : 'Clique em "Novo" para cadastrar a primeira tag.')}</div>`;
    return;
  }

  const rows = lista.map((t, i) => `
    <tr style="animation-delay:${i * 22}ms">
      <td class="td-id">${t.id}</td>
      <td class="td-title">${tagChip(t, t.id)}</td>
      <td class="td-meta">${livroTitulo(t.idLivro)}</td>
      <td class="td-actions">${actionBtns(t.id)}</td>
    </tr>`).join('');

  area.innerHTML = `
    <div class="table-wrap fade-in">
      <table>
        <thead><tr>
          <th>#</th><th>Tag</th><th>Livro</th><th>Ações</th>
        </tr></thead>
        <tbody>${rows}</tbody>
      </table>
    </div>`;

  $$('.btn-edit',   area).forEach(b => b.addEventListener('click', () => editarTag(+b.dataset.id)));
  $$('.btn-delete', area).forEach(b => b.addEventListener('click', () => excluirTag(+b.dataset.id)));
}

// ── CRUD LIVROS ──────────────────────────────────────────────
async function novoLivro() {
  const optsEditora = state.editoras.map(e =>
    `<option value="${e.id}">${escHtml(e.nome)}</option>`
  ).join('');

  const data = await Modal.open('Novo Livro', `
    <div class="form-grid">
      <div class="form-group full">
        <label>Título *</label>
        <input type="text" name="titulo" placeholder="Ex: Dom Casmurro" autocomplete="off" />
      </div>
      <div class="form-row">
        <div class="form-group">
          <label>ISBN (13 dígitos)</label>
          <input type="text" name="isbn" maxlength="13" placeholder="9788535914849" autocomplete="off" />
        </div>
        <div class="form-group">
          <label>Ano de Publicação</label>
          <input type="number" name="anoPublicacao" placeholder="2023" min="1" max="2099" />
        </div>
      </div>
      <div class="form-row">
        <div class="form-group">
          <label>Preço (R$)</label>
          <input type="number" name="preco" placeholder="49.90" step="0.01" min="0" />
        </div>
        <div class="form-group">
          <label>Editora</label>
          <select name="idEditora">
            <option value="">— Sem editora —</option>
            ${optsEditora}
          </select>
        </div>
      </div>
      <div class="form-group full">
        <label>Sinopse</label>
        <textarea name="sinopse" placeholder="Breve descrição do livro…"></textarea>
      </div>
    </div>`);

  if (!data || !data.titulo?.trim()) return;

  try {
    const payload = {
      ...data,
      idEditora:     data.idEditora     != null ? Number(data.idEditora)     : 0,
      anoPublicacao: data.anoPublicacao != null ? Number(data.anoPublicacao) : 0,
      preco:         data.preco        != null ? Number(data.preco)          : 0,
    };
    const novo = await api.post('/livros', payload);
    state.livros.push(novo);
    renderView(); atualizarStats();
    toast(`"${novo.titulo}" cadastrado!`, 'success');
  } catch (e) { toast('Erro ao cadastrar livro: ' + e.message, 'error'); }
}

async function editarLivro(id) {
  const livro = state.livros.find(l => l.id === id);
  if (!livro) return;

  const optsEditora = state.editoras.map(e =>
    `<option value="${e.id}" ${e.id === livro.idEditora ? 'selected' : ''}>${escHtml(e.nome)}</option>`
  ).join('');

  const data = await Modal.open(`Editar Livro #${id}`, `
    <div class="form-grid">
      <div class="form-group full">
        <label>Título *</label>
        <input type="text" name="titulo" value="${escHtml(livro.titulo ?? '')}" autocomplete="off" />
      </div>
      <div class="form-row">
        <div class="form-group">
          <label>ISBN (13 dígitos)</label>
          <input type="text" name="isbn" maxlength="13" value="${escHtml(livro.isbn ?? '')}" autocomplete="off" />
        </div>
        <div class="form-group">
          <label>Ano de Publicação</label>
          <input type="number" name="anoPublicacao" value="${livro.anoPublicacao ?? ''}" min="1" max="2099" />
        </div>
      </div>
      <div class="form-row">
        <div class="form-group">
          <label>Preço (R$)</label>
          <input type="number" name="preco" value="${livro.preco ?? ''}" step="0.01" min="0" />
        </div>
        <div class="form-group">
          <label>Editora</label>
          <select name="idEditora">
            <option value="">— Sem editora —</option>
            ${optsEditora}
          </select>
        </div>
      </div>
      <div class="form-group full">
        <label>Sinopse</label>
        <textarea name="sinopse">${escHtml(livro.sinopse ?? '')}</textarea>
      </div>
    </div>`);

  if (!data || !data.titulo?.trim()) return;

  try {
    const payload = {
      id, titulo: data.titulo, isbn: data.isbn ?? '', sinopse: data.sinopse ?? '',
      idEditora:     data.idEditora     != null && data.idEditora !== ''     ? Number(data.idEditora)     : 0,
      anoPublicacao: data.anoPublicacao != null && data.anoPublicacao !== '' ? Number(data.anoPublicacao) : 0,
      preco:         data.preco        != null && data.preco !== ''          ? Number(data.preco)         : 0,
    };
    const atualizado = await api.put(`/livros/${id}`, payload);
    const idx = state.livros.findIndex(l => l.id === id);
    if (idx >= 0) state.livros[idx] = atualizado;
    renderView();
    toast(`"${atualizado.titulo}" atualizado!`, 'success');
  } catch (e) { toast('Erro ao atualizar livro: ' + e.message, 'error'); }
}

async function excluirLivro(id) {
  const livro = state.livros.find(l => l.id === id);
  if (!await confirmDialog(`Excluir "${livro?.titulo ?? 'este livro'}"?\nIsso removerá também suas tags e vínculos.`)) return;
  try {
    await api.delete(`/livros/${id}`);
    state.livros = state.livros.filter(l => l.id !== id);
    // Remove tags do estado local que pertenciam ao livro
    state.tags = state.tags.filter(t => t.idLivro !== id);
    renderView(); atualizarStats();
    toast('Livro excluído.', 'info');
  } catch (e) { toast('Erro: ' + e.message, 'error'); }
}

// ── CRUD EDITORAS ─────────────────────────────────────────────
async function novaEditora() {
  const data = await Modal.open('Nova Editora', `
    <div class="form-grid">
      <div class="form-group full">
        <label>Nome *</label>
        <input type="text" name="nome" placeholder="Ex: Companhia das Letras" autocomplete="off" />
      </div>
      <div class="form-row">
        <div class="form-group">
          <label>Cidade Sede</label>
          <input type="text" name="cidade" placeholder="Ex: São Paulo" autocomplete="off" />
        </div>
        <div class="form-group">
          <label>Ano de Fundação</label>
          <input type="number" name="anoFundacao" placeholder="1985" min="1" max="2099" />
        </div>
      </div>
    </div>`);

  if (!data || !data.nome?.trim()) return;

  try {
    const payload = { nome: data.nome, cidade: data.cidade ?? '', anoFundacao: data.anoFundacao != null ? Number(data.anoFundacao) : 0 };
    const nova = await api.post('/editoras', payload);
    state.editoras.push(nova);
    renderView(); atualizarStats();
    toast(`"${nova.nome}" cadastrada!`, 'success');
  } catch (e) { toast('Erro: ' + e.message, 'error'); }
}

async function editarEditora(id) {
  const ed = state.editoras.find(e => e.id === id);
  if (!ed) return;

  const data = await Modal.open(`Editar Editora #${id}`, `
    <div class="form-grid">
      <div class="form-group full">
        <label>Nome *</label>
        <input type="text" name="nome" value="${escHtml(ed.nome ?? '')}" autocomplete="off" />
      </div>
      <div class="form-row">
        <div class="form-group">
          <label>Cidade Sede</label>
          <input type="text" name="cidade" value="${escHtml(ed.cidade ?? '')}" autocomplete="off" />
        </div>
        <div class="form-group">
          <label>Ano de Fundação</label>
          <input type="number" name="anoFundacao" value="${ed.anoFundacao ?? ''}" min="1" max="2099" />
        </div>
      </div>
    </div>`);

  if (!data || !data.nome?.trim()) return;

  try {
    const payload = { id, nome: data.nome, cidade: data.cidade ?? '', anoFundacao: data.anoFundacao != null ? Number(data.anoFundacao) : 0 };
    const atualizada = await api.put(`/editoras/${id}`, payload);
    const idx = state.editoras.findIndex(e => e.id === id);
    if (idx >= 0) state.editoras[idx] = atualizada;
    renderView();
    toast(`"${atualizada.nome}" atualizada!`, 'success');
  } catch (e) { toast('Erro: ' + e.message, 'error'); }
}

async function excluirEditora(id) {
  const ed  = state.editoras.find(e => e.id === id);
  const qtd = livrosDaEditora(id);
  if (qtd > 0) { toast(`Esta editora possui ${qtd} livro(s). Remova-os antes.`, 'error'); return; }
  if (!await confirmDialog(`Excluir a editora "${ed?.nome ?? 'esta editora'}"?`)) return;
  try {
    await api.delete(`/editoras/${id}`);
    state.editoras = state.editoras.filter(e => e.id !== id);
    renderView(); atualizarStats();
    toast('Editora excluída.', 'info');
  } catch (e) { toast('Erro: ' + e.message, 'error'); }
}

// ── CRUD TAGS (view completa) ────────────────────────────────
async function novaTag() {
  const optsLivro = state.livros.map(l =>
    `<option value="${l.id}">${escHtml(l.titulo)}</option>`
  ).join('');

  if (!optsLivro) {
    toast('Cadastre pelo menos um livro antes de criar tags.', 'error');
    return;
  }

  const data = await Modal.open('Nova Tag', `
    <div class="form-grid">
      <div class="form-group full">
        <label>Livro *</label>
        <select name="idLivro">
          <option value="">— Selecione um livro —</option>
          ${optsLivro}
        </select>
      </div>
      <div class="form-group full">
        <label>Tag *</label>
        <input type="text" name="tag" placeholder="Ex: ficção científica" maxlength="60" autocomplete="off" />
      </div>
    </div>`);

  if (!data || !data.tag?.trim() || !data.idLivro) {
    if (data && !data.idLivro) toast('Selecione um livro.', 'error');
    return;
  }

  try {
    const idLivro = Number(data.idLivro);
    const nova = await api.post(`/livros/${idLivro}/tags`, { tag: data.tag.trim(), idLivro });
    state.tags.push(nova);
    renderView(); atualizarStats();
    toast(`Tag "${nova.tag}" criada!`, 'success');
  } catch (e) { toast('Erro ao criar tag: ' + e.message, 'error'); }
}

async function editarTag(id) {
  const tag = state.tags.find(t => t.id === id);
  if (!tag) return;

  const optsLivro = state.livros.map(l =>
    `<option value="${l.id}" ${l.id === tag.idLivro ? 'selected' : ''}>${escHtml(l.titulo)}</option>`
  ).join('');

  const data = await Modal.open(`Editar Tag #${id}`, `
    <div class="form-grid">
      <div class="form-group full">
        <label>Livro *</label>
        <select name="idLivro">
          <option value="">— Selecione um livro —</option>
          ${optsLivro}
        </select>
      </div>
      <div class="form-group full">
        <label>Tag *</label>
        <input type="text" name="tag" value="${escHtml(tag.tag ?? '')}" maxlength="60" autocomplete="off" />
      </div>
    </div>`);

  if (!data || !data.tag?.trim() || !data.idLivro) return;

  try {
    // Para editar uma tag: delete a antiga e cria nova no livro correto
    // (a API de tags é por livro; se o livro mudou, precisa remover do antigo)
    await api.delete(`/livros/${tag.idLivro}/tags/${id}`);
    const idLivroNovo = Number(data.idLivro);
    const nova = await api.post(`/livros/${idLivroNovo}/tags`, { tag: data.tag.trim(), idLivro: idLivroNovo });

    // Atualiza estado local
    const idx = state.tags.findIndex(t => t.id === id);
    if (idx >= 0) state.tags[idx] = nova; else state.tags.push(nova);

    renderView();
    toast(`Tag atualizada!`, 'success');
  } catch (e) { toast('Erro ao atualizar tag: ' + e.message, 'error'); }
}

async function excluirTag(id) {
  const tag = state.tags.find(t => t.id === id);
  if (!await confirmDialog(`Excluir a tag "${tag?.tag ?? 'esta tag'}"?`)) return;
  try {
    await api.delete(`/livros/${tag.idLivro}/tags/${id}`);
    state.tags = state.tags.filter(t => t.id !== id);
    renderView(); atualizarStats();
    toast('Tag excluída.', 'info');
  } catch (e) { toast('Erro: ' + e.message, 'error'); }
}

// ── DETALHES DO LIVRO (tags + autores no modal) ──────────────
async function openDetalhes(idLivro) {
  const livro = state.livros.find(l => l.id === idLivro);
  if (!livro) return;

  let tags    = state.tags.filter(t => t.idLivro === idLivro);
  let autores = [];

  await Promise.allSettled([
    api.get(`/livros/${idLivro}/tags`).then(r    => { tags    = r; }),
    api.get(`/livros/${idLivro}/autores`).then(r => { autores = r; }),
  ]);

  function buildBody() {
    const tagsHTML = tags.length
      ? `<div class="tag-list">${tags.map((t, i) => `
          <span class="tag-chip-rm">
            ${escHtml(t.tag)}
            <button class="tag-rm-btn" data-tag-id="${t.id}" title="Remover">×</button>
          </span>`).join('')}</div>`
      : '<span class="empty-hint">Nenhuma tag.</span>';

    const autoresHTML = autores.length
      ? autores.map(a => `
          <div class="autor-item">
            <div class="autor-item-info">
              <span class="autor-label">Autor</span>
              <span class="autor-id">ID <strong>${a.idAutor}</strong></span>
            </div>
            <button class="btn btn-icon btn-delete" data-vinculo-id="${a.id}" title="Desvincular">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <path d="M18 6 6 18M6 6l12 12"/>
              </svg>
            </button>
          </div>`).join('')
      : '<span class="empty-hint">Nenhum autor vinculado.</span>';

    return `
      <div class="detail-grid">
        <div class="detail-field full">
          <span class="detail-label">Título</span>
          <span class="detail-value detail-title-val">${escHtml(livro.titulo)}</span>
        </div>
        <div class="detail-field">
          <span class="detail-label">ISBN</span>
          <span class="detail-value"><span class="isbn-code">${escHtml(livro.isbn ?? '—')}</span></span>
        </div>
        <div class="detail-field">
          <span class="detail-label">Editora</span>
          <span class="detail-value">${editoraNome(livro.idEditora)}</span>
        </div>
        <div class="detail-field">
          <span class="detail-label">Ano</span>
          <span class="detail-value">${livro.anoPublicacao ?? '—'}</span>
        </div>
        <div class="detail-field">
          <span class="detail-label">Preço</span>
          <span class="detail-value price-val">R$ ${fmtPreco(livro.preco)}</span>
        </div>
        <div class="detail-field full">
          <span class="detail-label">Sinopse</span>
          <span class="detail-value sinopse-val">${escHtml(livro.sinopse ?? '—')}</span>
        </div>
      </div>

      <div class="detail-section">
        <div class="detail-section-header">
          <span class="detail-section-title">🏷️ Tags</span>
          <span class="detail-section-count">${tags.length}</span>
        </div>
        <div class="tag-input-row">
          <input type="text" id="newTagInput" placeholder="Nova tag…" maxlength="60" autocomplete="off" />
          <button class="btn btn-sm btn-primary" id="addTagBtn">Adicionar</button>
        </div>
        ${tagsHTML}
      </div>

      <div class="detail-section">
        <div class="detail-section-header">
          <span class="detail-section-title">✍️ Autores</span>
          <span class="detail-section-count">${autores.length}</span>
        </div>
        <div class="autor-input-row">
          <input type="number" id="newAutorInput" placeholder="ID do Autor…" min="1" />
          <button class="btn btn-sm btn-primary" id="addAutorBtn">Vincular</button>
        </div>
        <div class="autor-list" id="autorList">${autoresHTML}</div>
      </div>`;
  }

  DetailModal.open(`📖 ${livro.titulo}`, buildBody());

  async function rerender() {
    const results = await Promise.allSettled([
      api.get(`/livros/${idLivro}/tags`),
      api.get(`/livros/${idLivro}/autores`),
    ]);
    if (results[0].status === 'fulfilled') {
      tags = results[0].value;
      // sincroniza estado global de tags
      state.tags = state.tags.filter(t => t.idLivro !== idLivro).concat(tags);
      atualizarStats();
    }
    if (results[1].status === 'fulfilled') autores = results[1].value;
    $('#detailBody').innerHTML = buildBody();
    bindDetailEvents();
  }

  function bindDetailEvents() {
    const tagInput   = $('#newTagInput');
    const addTagBtn  = $('#addTagBtn');
    const autorInput = $('#newAutorInput');
    const addAutorBtn= $('#addAutorBtn');

    tagInput?.addEventListener('keydown', e => { if (e.key === 'Enter') { e.preventDefault(); addTagBtn?.click(); } });
    autorInput?.addEventListener('keydown', e => { if (e.key === 'Enter') { e.preventDefault(); addAutorBtn?.click(); } });

    addTagBtn?.addEventListener('click', async () => {
      const v = tagInput?.value?.trim();
      if (!v) { toast('Informe o texto da tag.', 'error'); return; }
      try {
        await api.post(`/livros/${idLivro}/tags`, { tag: v, idLivro });
        toast('Tag adicionada!', 'success');
        await rerender();
      } catch (e) { toast('Erro: ' + e.message, 'error'); }
    });

    $$('.tag-rm-btn').forEach(b => b.addEventListener('click', async () => {
      try {
        await api.delete(`/livros/${idLivro}/tags/${b.dataset.tagId}`);
        toast('Tag removida.', 'info');
        await rerender();
      } catch (e) { toast('Erro: ' + e.message, 'error'); }
    }));

    addAutorBtn?.addEventListener('click', async () => {
      const idAutor = parseInt(autorInput?.value ?? '');
      if (!idAutor || idAutor <= 0) { toast('ID de autor inválido.', 'error'); return; }
      try {
        await api.post(`/livros/${idLivro}/autores`, { idLivro, idAutor });
        toast('Autor vinculado!', 'success');
        await rerender();
      } catch (e) {
        toast(e.message.includes('409') ? 'Autor já vinculado.' : 'Erro: ' + e.message, 'error');
      }
    });

    $$('[data-vinculo-id]').forEach(b => b.addEventListener('click', async () => {
      try {
        await api.delete(`/livros/${idLivro}/autores/${b.dataset.vinculoId}`);
        toast('Autor desvinculado.', 'info');
        await rerender();
      } catch (e) { toast('Erro: ' + e.message, 'error'); }
    }));
  }

  bindDetailEvents();
}

// ── ROUTER ───────────────────────────────────────────────────
function setView(view) {
  state.view  = view;
  state.query = '';
  $('#searchInput').value = '';
  $$('.nav-item').forEach(n => n.classList.toggle('active', n.dataset.view === view));
  $('#viewTitle').textContent = { livros: 'Livros', editoras: 'Editoras', tags: 'Tags' }[view] ?? view;
  renderView();
}

function renderView() {
  if (state.view === 'livros')   renderLivros();
  if (state.view === 'editoras') renderEditoras();
  if (state.view === 'tags')     renderTags();
}

// ── STATS ─────────────────────────────────────────────────────
function atualizarStats() {
  $('#statLivros').textContent   = state.livros.length;
  $('#statEditoras').textContent = state.editoras.length;
  $('#statTags').textContent     = state.tags.length;
}

// ── LOAD ─────────────────────────────────────────────────────
async function loadAll() {
  $('#contentArea').innerHTML = `<div class="loading-state"><div class="spinner"></div><p>Conectando…</p></div>`;
  try {
    // Carrega livros e editoras em paralelo
    const [livros, editoras] = await Promise.all([
      api.get('/livros').catch(() => []),
      api.get('/editoras').catch(() => []),
    ]);
    state.livros   = livros   ?? [];
    state.editoras = editoras ?? [];

    // Carrega todas as tags de todos os livros em paralelo
    const tagResults = await Promise.allSettled(
      state.livros.map(l => api.get(`/livros/${l.id}/tags`))
    );
    state.tags = tagResults
      .filter(r => r.status === 'fulfilled')
      .flatMap(r => r.value);

    atualizarStats();
    renderView();
  } catch (e) {
    $('#contentArea').innerHTML = `<div class="table-wrap fade-in">${renderEmpty('⚠️', 'Servidor indisponível', 'Certifique-se de que o servidor Java está rodando em localhost:8080.')}</div>`;
    toast('Não foi possível conectar ao servidor.', 'error');
  }
}

// ── INIT ─────────────────────────────────────────────────────
(function init() {
  $$('.nav-item').forEach(btn =>
    btn.addEventListener('click', () => setView(btn.dataset.view))
  );

  $('#sidebarToggle').addEventListener('click', () => {
    const sb = $('#sidebar');
    window.innerWidth <= 600 ? sb.classList.toggle('open') : sb.classList.toggle('collapsed');
  });

  $('#btnNovo').addEventListener('click', () => {
    if (state.view === 'livros')   novoLivro();
    if (state.view === 'editoras') novaEditora();
    if (state.view === 'tags')     novaTag();
  });

  let searchTimer;
  $('#searchInput').addEventListener('input', e => {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(() => { state.query = e.target.value.trim(); renderView(); }, 200);
  });

  document.addEventListener('click', e => {
    const sb = $('#sidebar');
    if (window.innerWidth <= 600 && sb.classList.contains('open') &&
        !sb.contains(e.target) && e.target !== $('#sidebarToggle')) {
      sb.classList.remove('open');
    }
  });

  loadAll();
})();