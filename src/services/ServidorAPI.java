package src.services;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.ArrayList;

import src.dao.LivroDAO;
import src.dao.EditoraDAO;
import src.dao.TagLivroDAO;
import src.dao.LivroAutorDAO;
import src.model.Livro;
import src.model.Editora;
import src.model.TagLivro;
import src.model.LivroAutor;

/**
 * ServidorAPI — HTTP REST para o frontend HTML/JS.
 *
 * Rotas implementadas:
 *
 *   Livros
 *     GET    /api/livros                 → lista todos os livros (ordem B+)
 *     GET    /api/livros/{id}            → busca livro por ID
 *     POST   /api/livros                 → cria novo livro
 *     PUT    /api/livros/{id}            → atualiza livro
 *     DELETE /api/livros/{id}            → exclui livro + tags + vínculos
 *
 *   Editoras
 *     GET    /api/editoras               → lista todas as editoras
 *     GET    /api/editoras/{id}          → busca editora por ID
 *     POST   /api/editoras               → cria nova editora
 *     PUT    /api/editoras/{id}          → atualiza editora
 *     DELETE /api/editoras/{id}          → exclui editora
 *
 *   Tags (1:N de Livro)
 *     GET    /api/livros/{id}/tags       → lista tags do livro
 *     POST   /api/livros/{id}/tags       → adiciona tag
 *     DELETE /api/livros/{id}/tags/{tid} → remove tag
 *
 *   Autores vinculados (N:N via LivroAutor)
 *     GET    /api/livros/{id}/autores          → lista vínculos do livro
 *     POST   /api/livros/{id}/autores          → vincula autor
 *     DELETE /api/livros/{id}/autores/{vid}    → desvincula (por ID do vínculo)
 */
public class ServidorAPI {

    public static void iniciar() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

            server.createContext("/api/livros",   new LivrosHandler());
            server.createContext("/api/editoras", new EditorasHandler());

            server.setExecutor(null);
            server.start();
            System.out.println("🚀 API rodando em http://localhost:8080/api");
            System.out.println("   GET  /api/livros   /api/editoras");
            System.out.println("   POST /api/livros   /api/editoras");
            System.out.println("   PUT  /api/livros/{id}   /api/editoras/{id}");
            System.out.println("   DEL  /api/livros/{id}   /api/editoras/{id}");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================================
    // UTILITÁRIOS COMPARTILHADOS
    // =========================================================

    /** Adiciona cabeçalhos CORS para permitir acesso do HTML local */
    private static void addCors(HttpExchange ex) {
        ex.getResponseHeaders().add("Access-Control-Allow-Origin",  "*");
        ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
    }

    /** Responde com JSON e status HTTP */
    private static void responder(HttpExchange ex, int status, String json) throws IOException {
        byte[] bytes = json.getBytes("UTF-8");
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    /** Lê o corpo da requisição como String */
    private static String lerCorpo(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody()) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] b = new byte[1024];
            int n;
            while ((n = is.read(b)) != -1) buf.write(b, 0, n);
            return buf.toString("UTF-8");
        }
    }

    /** Extrai um segmento da URI a partir de um índice (0 = primeira parte após /) */
    private static String segmento(String path, int idx) {
        String[] partes = path.replaceAll("^/+", "").split("/");
        return (idx < partes.length) ? partes[idx] : null;
    }

    // Escapa caracteres especiais de JSON em strings
    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // =========================================================
    // SERIALIZAÇÃO DOS MODELOS PARA JSON
    // =========================================================

    private static String livroJson(Livro l) {
        return "{"
            + "\"id\":"           + l.getId()                              + ","
            + "\"idEditora\":"    + l.getIdEditora()                       + ","
            + "\"titulo\":\""     + esc(l.getTitulo())                     + "\","
            + "\"isbn\":\""       + esc(new String(l.getIsbn()).trim())     + "\","
            + "\"anoPublicacao\":" + l.getAnoPublicacao()                  + ","
            + "\"preco\":"        + l.getPreco()                           + ","
            + "\"sinopse\":\""    + esc(l.getSinopse())                    + "\""
            + "}";
    }

    private static String editoraJson(Editora e) {
        return "{"
            + "\"id\":"          + e.getId()               + ","
            + "\"nome\":\""      + esc(e.getNome())         + "\","
            + "\"cidade\":\""    + esc(e.getCidade())       + "\","
            + "\"anoFundacao\":" + e.getAnoFundacao()
            + "}";
    }

    private static String tagJson(TagLivro t) {
        return "{"
            + "\"id\":"      + t.getId()              + ","
            + "\"idLivro\":" + t.getIdLivro()          + ","
            + "\"tag\":\""   + esc(t.getTag())         + "\""
            + "}";
    }

    private static String livroAutorJson(LivroAutor la) {
        return "{"
            + "\"id\":"      + la.getId()       + ","
            + "\"idLivro\":" + la.getIdLivro()  + ","
            + "\"idAutor\":" + la.getIdAutor()
            + "}";
    }

    // Parser JSON mínimo: extrai valor string de uma chave
    private static String parseString(String json, String chave) {
        String busca = "\"" + chave + "\"";
        int idx = json.indexOf(busca);
        if (idx < 0) return "";
        int colon = json.indexOf(':', idx + busca.length());
        if (colon < 0) return "";
        int start = json.indexOf('"', colon + 1);
        if (start < 0) return "";
        int end = json.indexOf('"', start + 1);
        if (end < 0) return "";
        return json.substring(start + 1, end);
    }

    // Parser JSON mínimo: extrai valor numérico de uma chave
    private static String parseNumber(String json, String chave) {
        String busca = "\"" + chave + "\"";
        int idx = json.indexOf(busca);
        if (idx < 0) return "0";
        int colon = json.indexOf(':', idx + busca.length());
        if (colon < 0) return "0";
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.' || json.charAt(end) == '-')) end++;
        String num = json.substring(start, end).trim();
        return num.isEmpty() ? "0" : num;
    }

    // =========================================================
    // HANDLER: /api/livros  e  /api/livros/{id}  e  subrotas
    // =========================================================
    static class LivrosHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            addCors(ex);

            if ("OPTIONS".equals(ex.getRequestMethod())) {
                ex.sendResponseHeaders(204, -1);
                return;
            }

            // path relativa ao contexto, ex: "" | "/5" | "/5/tags" | "/5/tags/3"
            String path   = ex.getRequestURI().getPath(); // /api/livros/5/tags/3
            String method = ex.getRequestMethod();

            // Normaliza: remove /api/livros do início
            String rel = path.replaceFirst("^/api/livros", ""); // "" | "/5" | "/5/tags" | "/5/tags/3"

            try {
                // ── GET /api/livros ──────────────────────────────────
                if (method.equals("GET") && (rel.isEmpty() || rel.equals("/"))) {
                    LivroDAO dao = new LivroDAO();
                    ArrayList<Livro> lista = dao.listarLivrosOrdenadosPorTitulo();
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < lista.size(); i++) {
                        sb.append(livroJson(lista.get(i)));
                        if (i < lista.size() - 1) sb.append(",");
                    }
                    sb.append("]");
                    responder(ex, 200, sb.toString());
                    return;
                }

                // ── POST /api/livros ─────────────────────────────────
                if (method.equals("POST") && (rel.isEmpty() || rel.equals("/"))) {
                    String body = lerCorpo(ex);
                    String titulo  = parseString(body, "titulo");
                    String isbn    = parseString(body, "isbn");
                    String sinopse = parseString(body, "sinopse");
                    int    idEd    = Integer.parseInt(parseNumber(body, "idEditora"));
                    int    ano     = Integer.parseInt(parseNumber(body, "anoPublicacao"));
                    double preco   = Double.parseDouble(parseNumber(body, "preco"));

                    char[] isbnArr = String.format("%-13s", isbn).substring(0, 13).toCharArray();
                    Livro novo = new Livro(idEd, titulo, isbnArr, ano, preco, sinopse);

                    LivroDAO dao = new LivroDAO();
                    if (dao.incluirLivro(novo)) {
                        responder(ex, 201, livroJson(novo));
                    } else {
                        responder(ex, 500, "{\"erro\":\"Falha ao criar livro\"}");
                    }
                    return;
                }

                // Segmentos após /api/livros
                // rel pode ser "/5", "/5/tags", "/5/tags/3", "/5/autores", "/5/autores/3"
                String[] partes = rel.replaceAll("^/", "").split("/");
                // partes[0] = id do livro, partes[1] = "tags"|"autores", partes[2] = id do sub-recurso

                if (partes.length >= 1 && !partes[0].isEmpty()) {
                    int idLivro = Integer.parseInt(partes[0]);
                    String subRecurso = (partes.length >= 2) ? partes[1] : "";

                    // ── ROTAS DE TAGS ──────────────────────────────
                    if (subRecurso.equals("tags")) {
                        TagLivroDAO tagDao = new TagLivroDAO();

                        // GET /api/livros/{id}/tags
                        if (method.equals("GET") && partes.length == 2) {
                            ArrayList<TagLivro> tags = tagDao.buscarTagsPorLivro(idLivro);
                            StringBuilder sb = new StringBuilder("[");
                            for (int i = 0; i < tags.size(); i++) {
                                sb.append(tagJson(tags.get(i)));
                                if (i < tags.size() - 1) sb.append(",");
                            }
                            sb.append("]");
                            responder(ex, 200, sb.toString());
                            return;
                        }

                        // POST /api/livros/{id}/tags
                        if (method.equals("POST") && partes.length == 2) {
                            String body = lerCorpo(ex);
                            String tagVal = parseString(body, "tag");
                            if (tagVal.isEmpty()) {
                                responder(ex, 400, "{\"erro\":\"Tag não pode ser vazia\"}");
                                return;
                            }
                            TagLivro novaTag = new TagLivro(idLivro, tagVal);
                            if (tagDao.incluirTag(novaTag)) {
                                responder(ex, 201, tagJson(novaTag));
                            } else {
                                responder(ex, 500, "{\"erro\":\"Falha ao criar tag\"}");
                            }
                            return;
                        }

                        // DELETE /api/livros/{id}/tags/{tid}
                        if (method.equals("DELETE") && partes.length == 3) {
                            int idTag = Integer.parseInt(partes[2]);
                            TagLivro tag = tagDao.buscarTagId(idTag);
                            if (tag == null || !tag.getLapide() || tag.getIdLivro() != idLivro) {
                                responder(ex, 404, "{\"erro\":\"Tag não encontrada\"}");
                                return;
                            }
                            if (tagDao.excluirTag(idTag)) {
                                responder(ex, 200, "{\"ok\":true}");
                            } else {
                                responder(ex, 500, "{\"erro\":\"Falha ao excluir tag\"}");
                            }
                            return;
                        }
                    }

                    // ── ROTAS DE AUTORES (N:N) ─────────────────────
                    if (subRecurso.equals("autores")) {
                        LivroAutorDAO laDao = new LivroAutorDAO();

                        // GET /api/livros/{id}/autores
                        if (method.equals("GET") && partes.length == 2) {
                            ArrayList<LivroAutor> lista = laDao.buscarPorLivro(idLivro);
                            StringBuilder sb = new StringBuilder("[");
                            for (int i = 0; i < lista.size(); i++) {
                                sb.append(livroAutorJson(lista.get(i)));
                                if (i < lista.size() - 1) sb.append(",");
                            }
                            sb.append("]");
                            responder(ex, 200, sb.toString());
                            return;
                        }

                        // POST /api/livros/{id}/autores
                        if (method.equals("POST") && partes.length == 2) {
                            String body   = lerCorpo(ex);
                            int idAutor   = Integer.parseInt(parseNumber(body, "idAutor"));
                            if (laDao.existeRelacao(idLivro, idAutor)) {
                                responder(ex, 409, "{\"erro\":\"Autor já vinculado\"}");
                                return;
                            }
                            LivroAutor la = new LivroAutor(idLivro, idAutor);
                            if (laDao.incluir(la)) {
                                responder(ex, 201, livroAutorJson(la));
                            } else {
                                responder(ex, 500, "{\"erro\":\"Falha ao vincular autor\"}");
                            }
                            return;
                        }

                        // DELETE /api/livros/{id}/autores/{vid}
                        if (method.equals("DELETE") && partes.length == 3) {
                            int idVinculo = Integer.parseInt(partes[2]);
                            LivroAutor la = laDao.buscarPorId(idVinculo);
                            if (la == null || !la.getLapide() || la.getIdLivro() != idLivro) {
                                responder(ex, 404, "{\"erro\":\"Vínculo não encontrado\"}");
                                return;
                            }
                            if (laDao.excluir(idVinculo)) {
                                responder(ex, 200, "{\"ok\":true}");
                            } else {
                                responder(ex, 500, "{\"erro\":\"Falha ao desvincular autor\"}");
                            }
                            return;
                        }
                    }

                    // ── ROTAS DE LIVRO POR ID ──────────────────────
                    if (subRecurso.isEmpty()) {
                        LivroDAO dao = new LivroDAO();

                        // GET /api/livros/{id}
                        if (method.equals("GET")) {
                            Livro l = dao.buscarLivroId(idLivro);
                            if (l == null || !l.getLapide()) {
                                responder(ex, 404, "{\"erro\":\"Livro não encontrado\"}");
                            } else {
                                responder(ex, 200, livroJson(l));
                            }
                            return;
                        }

                        // PUT /api/livros/{id}
                        if (method.equals("PUT")) {
                            String body   = lerCorpo(ex);
                            Livro antigo  = dao.buscarLivroId(idLivro);
                            if (antigo == null || !antigo.getLapide()) {
                                responder(ex, 404, "{\"erro\":\"Livro não encontrado\"}");
                                return;
                            }
                            String titulo  = parseString(body, "titulo");
                            String isbn    = parseString(body, "isbn");
                            String sinopse = parseString(body, "sinopse");
                            int    idEd    = Integer.parseInt(parseNumber(body, "idEditora"));
                            int    ano     = Integer.parseInt(parseNumber(body, "anoPublicacao"));
                            double preco   = Double.parseDouble(parseNumber(body, "preco"));

                            antigo.setTitulo(!titulo.isEmpty() ? titulo : antigo.getTitulo());
                            if (!isbn.isEmpty()) {
                                antigo.setIsbn(String.format("%-13s", isbn).substring(0, 13).toCharArray());
                            }
                            if (idEd > 0) antigo.setIdEditora(idEd);
                            if (ano > 0)  antigo.setAnoPublicacao(ano);
                            if (preco >= 0) antigo.setPreco(preco);
                            antigo.setSinopse(sinopse);

                            if (dao.alterarLivro(antigo)) {
                                responder(ex, 200, livroJson(antigo));
                            } else {
                                responder(ex, 500, "{\"erro\":\"Falha ao atualizar livro\"}");
                            }
                            return;
                        }

                        // DELETE /api/livros/{id}
                        if (method.equals("DELETE")) {
                            // Remove tags e vínculos de autores antes de excluir o livro
                            TagLivroDAO  tagDao = new TagLivroDAO();
                            LivroAutorDAO laDao = new LivroAutorDAO();
                            tagDao.excluirTagsPorLivro(idLivro);
                            laDao.excluirPorLivro(idLivro);

                            if (dao.excluirLivro(idLivro)) {
                                responder(ex, 200, "{\"ok\":true}");
                            } else {
                                responder(ex, 404, "{\"erro\":\"Livro não encontrado\"}");
                            }
                            return;
                        }
                    }
                }

                responder(ex, 404, "{\"erro\":\"Rota não encontrada\"}");

            } catch (NumberFormatException nfe) {
                responder(ex, 400, "{\"erro\":\"ID inválido\"}");
            } catch (Exception e) {
                e.printStackTrace();
                responder(ex, 500, "{\"erro\":\"Erro interno: " + esc(e.getMessage()) + "\"}");
            }
        }
    }

    // =========================================================
    // HANDLER: /api/editoras  e  /api/editoras/{id}
    // =========================================================
    static class EditorasHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            addCors(ex);

            if ("OPTIONS".equals(ex.getRequestMethod())) {
                ex.sendResponseHeaders(204, -1);
                return;
            }

            String path   = ex.getRequestURI().getPath();
            String method = ex.getRequestMethod();
            String rel    = path.replaceFirst("^/api/editoras", "");

            try {
                // ── GET /api/editoras ────────────────────────────────
                if (method.equals("GET") && (rel.isEmpty() || rel.equals("/"))) {
                    EditoraDAO dao = new EditoraDAO();
                    ArrayList<Editora> lista = dao.listarTodasEditoras();
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < lista.size(); i++) {
                        sb.append(editoraJson(lista.get(i)));
                        if (i < lista.size() - 1) sb.append(",");
                    }
                    sb.append("]");
                    responder(ex, 200, sb.toString());
                    return;
                }

                // ── POST /api/editoras ───────────────────────────────
                if (method.equals("POST") && (rel.isEmpty() || rel.equals("/"))) {
                    String body  = lerCorpo(ex);
                    String nome  = parseString(body, "nome");
                    String cidade = parseString(body, "cidade");
                    int    ano   = Integer.parseInt(parseNumber(body, "anoFundacao"));

                    Editora nova = new Editora(nome, cidade, ano);
                    EditoraDAO dao = new EditoraDAO();
                    if (dao.incluirEditora(nova)) {
                        responder(ex, 201, editoraJson(nova));
                    } else {
                        responder(ex, 500, "{\"erro\":\"Falha ao criar editora\"}");
                    }
                    return;
                }

                // Rotas com ID
                String idStr = rel.replaceAll("^/", "").split("/")[0];
                if (!idStr.isEmpty()) {
                    int id = Integer.parseInt(idStr);
                    EditoraDAO dao = new EditoraDAO();

                    // GET /api/editoras/{id}
                    if (method.equals("GET")) {
                        Editora e = dao.buscarEditoraId(id);
                        if (e == null || !e.getLapide()) {
                            responder(ex, 404, "{\"erro\":\"Editora não encontrada\"}");
                        } else {
                            responder(ex, 200, editoraJson(e));
                        }
                        return;
                    }

                    // PUT /api/editoras/{id}
                    if (method.equals("PUT")) {
                        Editora ed = dao.buscarEditoraId(id);
                        if (ed == null || !ed.getLapide()) {
                            responder(ex, 404, "{\"erro\":\"Editora não encontrada\"}");
                            return;
                        }
                        String body  = lerCorpo(ex);
                        String nome  = parseString(body, "nome");
                        String cidade = parseString(body, "cidade");
                        int    ano   = Integer.parseInt(parseNumber(body, "anoFundacao"));

                        if (!nome.isEmpty())   ed.setNome(nome);
                        if (!cidade.isEmpty()) ed.setCidade(cidade);
                        if (ano > 0)           ed.setAnoFundacao(ano);

                        if (dao.alterarEditora(ed)) {
                            responder(ex, 200, editoraJson(ed));
                        } else {
                            responder(ex, 500, "{\"erro\":\"Falha ao atualizar editora\"}");
                        }
                        return;
                    }

                    // DELETE /api/editoras/{id}
                    if (method.equals("DELETE")) {
                        Editora ed = dao.buscarEditoraId(id);
                        if (ed == null || !ed.getLapide()) {
                            responder(ex, 404, "{\"erro\":\"Editora não encontrada\"}");
                            return;
                        }
                        if (dao.excluirEditora(id)) {
                            responder(ex, 200, "{\"ok\":true}");
                        } else {
                            responder(ex, 500, "{\"erro\":\"Falha ao excluir editora\"}");
                        }
                        return;
                    }
                }

                responder(ex, 404, "{\"erro\":\"Rota não encontrada\"}");

            } catch (NumberFormatException nfe) {
                responder(ex, 400, "{\"erro\":\"ID inválido\"}");
            } catch (Exception e) {
                e.printStackTrace();
                responder(ex, 500, "{\"erro\":\"Erro interno: " + esc(e.getMessage()) + "\"}");
            }
        }
    }
}