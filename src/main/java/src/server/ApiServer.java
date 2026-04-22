package src.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import src.dao.*;
import src.model.*;
import src.service.UsuarioService;
import src.util.DataUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.Executors;

public class ApiServer {

    private final int        port;
    private final DAOFactory factory;
    private final String     webRoot;

    public ApiServer(int port, DAOFactory factory, String webRoot) {
        this.port    = port;
        this.factory = factory;
        this.webRoot = webRoot;
    }

    public void start() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/livros-autores", new LivrosAutoresHandler());
        server.createContext("/api/tags-livros",    new TagsLivrosHandler());
        server.createContext("/api/livros",         new LivrosHandler());
        server.createContext("/api/autores",        new AutoresHandler());
        server.createContext("/api/editoras",       new EditorasHandler());
        server.createContext("/api/usuarios",       new UsuariosHandler());
        server.createContext("/api/tags",           new TagsHandler());
        server.createContext("/api/auth",           new AuthHandler());
        server.createContext("/",                   new StaticHandler(webRoot));

        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("BiblioSys rodando em http://localhost:" + port);
    }

    // --- STATIC FILES ---
    private static class StaticHandler implements HttpHandler {
        private final Path root;
        StaticHandler(String root) { this.root = Path.of(root).toAbsolutePath().normalize(); }

        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (handleCors(ex)) return;
            String reqPath = ex.getRequestURI().getPath();
            if (reqPath.equals("/")) reqPath = "/index.html";
            Path target = root.resolve(reqPath.substring(1)).normalize();
            if (!target.startsWith(root) || !Files.exists(target) || Files.isDirectory(target)) {
                send(ex, 404, "text/plain", "Not found");
                return;
            }
            String ct = reqPath.endsWith(".js")  ? "application/javascript"
                      : reqPath.endsWith(".css") ? "text/css"
                      : "text/html";
            byte[] body = Files.readAllBytes(target);
            ex.getResponseHeaders().set("Content-Type", ct);
            ex.sendResponseHeaders(200, body.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(body); }
        }
    }

    // --- HANDLER: LIVROS ---
    // Retorna nomeEditora, lista de autores (nomes) e lista de tags (nomes)
    private class LivrosHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (handleCors(ex)) return;
            try {
                LivroDAO dao = factory.getLivroDAO();
                int id = extractId(ex);
                String method = ex.getRequestMethod();

                if ("GET".equals(method)) {
                    if (id > 0) {
                        Livro l = dao.buscarLivroPorId(id);
                        if (l == null) { send404(ex); return; }
                        sendJson(ex, 200, livroToJson(l).toString());
                    } else {
                        String query = ex.getRequestURI().getQuery();
                        boolean porTitulo   = query != null && query.contains("ordem=titulo");
                        boolean decrescente = query != null && query.contains("ordem=id-desc");
                        List<Livro> lista;
                        if (porTitulo) {
                            lista = dao.listarOrdenadoPorTitulo();
                        } else if (decrescente) {
                            lista = dao.listarOrdenadoDecrescentePorId();
                        } else {
                            lista = dao.listarOrdenadoPorId();
                        }
                        JSONArray arr = new JSONArray();
                        for (Livro l : lista) arr.put(livroToJson(l));
                        sendJson(ex, 200, arr.toString());
                    }
                } else if ("POST".equals(method)) {
                    JSONObject j = readBody(ex);
                    Livro novo = new Livro(
                        j.getInt("idEditora"),
                        j.getString("titulo"),
                        padIsbn(j.optString("isbn")),
                        j.optInt("anoPublicacao"),
                        j.optDouble("preco"),
                        j.optString("sinopse")
                    );
                    int novoId = dao.incluirLivro(novo);
                    sendJson(ex, 201, "{\"ok\":true,\"id\":" + novoId + "}");
                } else if ("PUT".equals(method)) {
                    if (id <= 0) { send404(ex); return; }
                    Livro existente = dao.buscarLivroPorId(id);
                    if (existente == null) { send404(ex); return; }
                    JSONObject j = readBody(ex);
                    existente.setIdEditora(j.getInt("idEditora"));
                    existente.setTitulo(j.getString("titulo"));
                    existente.setIsbn(padIsbn(j.optString("isbn")));
                    existente.setAnoPublicacao(j.optInt("anoPublicacao"));
                    existente.setPreco(j.optDouble("preco"));
                    existente.setSinopse(j.optString("sinopse"));
                    boolean ok = dao.alterarLivro(existente);
                    sendJson(ex, ok ? 200 : 500, "{\"ok\":" + ok + "}");
                } else if ("DELETE".equals(method)) {
                    if (id <= 0) { send404(ex); return; }
                    factory.getTagsLivrosDAO().excluirTagsDoLivro(id);
                    boolean ok = dao.excluirLivroEmCascata(id);
                    sendJson(ex, ok ? 200 : 404, "{\"ok\":" + ok + "}");
                } else {
                    sendJson(ex, 405, "{\"erro\":\"Method not allowed\"}");
                }
            } catch (Exception e) { sendError(ex, e); }
        }

        private JSONObject livroToJson(Livro l) {
            // Nome da editora
            String nomeEditora = "Editora #" + l.getIdEditora();
            try {
                Editora ed = factory.getEditoraDAO().buscarEditora(l.getIdEditora());
                if (ed != null) nomeEditora = ed.getNome();
            } catch (Exception ignored) {}

            // Nomes dos autores vinculados
            JSONArray autoresArr = new JSONArray();
            try {
                List<LivroAutor> vinculos = factory.getLivroAutorDAO().buscarAutoresDoLivro(l.getId());
                for (LivroAutor la : vinculos) {
                    Autores a = factory.getAutoresDAO().buscarAutor(la.getIdAutor());
                    if (a != null) {
                        autoresArr.put(new JSONObject().put("id", a.getId()).put("nome", a.getNome()));
                    }
                }
            } catch (Exception ignored) {}

            // Nomes das tags vinculadas
            JSONArray tagsArr = new JSONArray();
            try {
                List<TagsLivros> vinculos = factory.getTagsLivrosDAO().buscarTagsDoLivro(l.getId());
                for (TagsLivros tl : vinculos) {
                    Tag t = factory.getTagDAO().buscarTag(tl.getIdTag());
                    if (t != null) {
                        tagsArr.put(new JSONObject().put("id", t.getId()).put("nome", t.getNome()));
                    }
                }
            } catch (Exception ignored) {}

            return new JSONObject()
                .put("id",            l.getId())
                .put("idEditora",     l.getIdEditora())
                .put("nomeEditora",   nomeEditora)
                .put("titulo",        l.getTitulo())
                .put("isbn",          new String(l.getIsbn()).trim())
                .put("anoPublicacao", l.getAnoPublicacao())
                .put("preco",         l.getPreco())
                .put("sinopse",       l.getSinopse() != null ? l.getSinopse() : "")
                .put("autores",       autoresArr)
                .put("tags",          tagsArr);
        }
    }

    // --- HANDLER: AUTORES ---
    // Retorna lista de livros (títulos) vinculados ao autor
    private class AutoresHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (handleCors(ex)) return;
            try {
                AutoresDAO dao = factory.getAutoresDAO();
                int id = extractId(ex);
                String method = ex.getRequestMethod();

                if ("GET".equals(method)) {
                    JSONArray arr = new JSONArray();
                    for (Autores a : dao.listarOrdenadoPorId()) arr.put(autorToJson(a));
                    sendJson(ex, 200, arr.toString());
                } else if ("POST".equals(method)) {
                    JSONObject j = readBody(ex);
                    long ts = DataUtil.stringToTimestamp(j.getString("dataNascimento"));
                    dao.adicionarAutor(new Autores(j.getString("nome"), ts, j.optString("biografia")));
                    sendJson(ex, 201, "{\"ok\":true}");
                } else if ("PUT".equals(method)) {
                    if (id <= 0) { send404(ex); return; }
                    Autores existente = dao.buscarAutor(id);
                    if (existente == null) { send404(ex); return; }
                    JSONObject j = readBody(ex);
                    existente.setNome(j.getString("nome"));
                    existente.setDataNascimento(DataUtil.stringToTimestamp(j.getString("dataNascimento")));
                    existente.setBiografia(j.optString("biografia"));
                    boolean ok = dao.alterarAutor(existente);
                    sendJson(ex, ok ? 200 : 500, "{\"ok\":" + ok + "}");
                } else if ("DELETE".equals(method)) {
                    if (id <= 0) { send404(ex); return; }
                    boolean ok = dao.excluirAutor(id);
                    sendJson(ex, ok ? 200 : 404, "{\"ok\":" + ok + "}");
                } else {
                    sendJson(ex, 405, "{\"erro\":\"Method not allowed\"}");
                }
            } catch (IllegalStateException e) {
                sendJson(ex, 409, "{\"erro\":\"" + e.getMessage().replace("\"", "'") + "\"}");
            } catch (Exception e) { sendError(ex, e); }
        }

        private JSONObject autorToJson(Autores a) {
            // Livros vinculados ao autor
            JSONArray livrosArr = new JSONArray();
            try {
                List<LivroAutor> vinculos = factory.getLivroAutorDAO().buscarLivrosDoAutor(a.getId());
                for (LivroAutor la : vinculos) {
                    Livro l = factory.getLivroDAO().buscarLivroPorId(la.getIdLivro());
                    if (l != null) {
                        livrosArr.put(new JSONObject().put("id", l.getId()).put("titulo", l.getTitulo()));
                    }
                }
            } catch (Exception ignored) {}

            return new JSONObject()
                .put("id",                     a.getId())
                .put("nome",                   a.getNome())
                .put("dataNascimento",         a.getDataNascimento())
                .put("dataNascimentoFormatada", a.getDataNascimentoFormatada())
                .put("biografia",              a.getBiografia() != null ? a.getBiografia() : "")
                .put("livros",                 livrosArr);
        }
    }

    // --- HANDLER: EDITORAS ---
    private class EditorasHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (handleCors(ex)) return;
            try {
                EditoraDAO dao = factory.getEditoraDAO();
                int id = extractId(ex);
                String method = ex.getRequestMethod();

                if ("GET".equals(method)) {
                    JSONArray arr = new JSONArray();
                    for (Editora e : dao.listarOrdenadoPorId()) {
                        arr.put(new JSONObject()
                            .put("id",          e.getId())
                            .put("nome",        e.getNome())
                            .put("cidade",      e.getCidade())
                            .put("anoFundacao", e.getAnoFundacao()));
                    }
                    sendJson(ex, 200, arr.toString());
                } else if ("POST".equals(method)) {
                    JSONObject j = readBody(ex);
                    dao.incluirEditora(new Editora(j.getString("nome"), j.optString("cidade"), j.optInt("anoFundacao")));
                    sendJson(ex, 201, "{\"ok\":true}");
                } else if ("PUT".equals(method)) {
                    if (id <= 0) { send404(ex); return; }
                    Editora existente = dao.buscarEditora(id);
                    if (existente == null) { send404(ex); return; }
                    JSONObject j = readBody(ex);
                    existente.setNome(j.getString("nome"));
                    existente.setCidade(j.optString("cidade"));
                    existente.setAnoFundacao(j.optInt("anoFundacao"));
                    boolean ok = dao.alterarEditora(existente);
                    sendJson(ex, ok ? 200 : 500, "{\"ok\":" + ok + "}");
                } else if ("DELETE".equals(method)) {
                    if (id <= 0) { send404(ex); return; }
                    boolean ok = dao.excluirEditora(id);
                    sendJson(ex, ok ? 200 : 404, "{\"ok\":" + ok + "}");
                } else {
                    sendJson(ex, 405, "{\"erro\":\"Method not allowed\"}");
                }
            } catch (IllegalStateException e) {
                sendJson(ex, 409, "{\"erro\":\"" + e.getMessage().replace("\"", "'") + "\"}");
            } catch (Exception e) { sendError(ex, e); }
        }
    }

    // --- HANDLER: USUARIOS ---
    // Agora inclui campo multivalorado redesSociais (String[])
    private class UsuariosHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (handleCors(ex)) return;
            try {
                UsuarioDAO     dao     = factory.getUsuarioDAO();
                UsuarioService service = factory.getUsuarioService();
                int id = extractId(ex);
                String method = ex.getRequestMethod();

                if ("GET".equals(method)) {
                    JSONArray arr = new JSONArray();
                    for (Usuarios u : dao.listarOrdenadoPorId()) {
                        arr.put(usuarioToJson(u));
                    }
                    sendJson(ex, 200, arr.toString());
                } else if ("POST".equals(method)) {
                    JSONObject j = readBody(ex);
                    int novoId = service.cadastrar(j.getString("nome"), j.getString("email"), j.getString("senha"));
                    if (novoId == -1) {
                        sendJson(ex, 409, "{\"erro\":\"Email já cadastrado.\"}");
                    } else {
                        // Se vieram redesSociais, salva após criação
                        if (j.has("redesSociais")) {
                            Usuarios u = dao.buscarUsuario(novoId);
                            if (u != null) {
                                u.setRedesSociais(parseStringArray(j, "redesSociais"));
                                dao.alterarUsuario(u);
                            }
                        }
                        sendJson(ex, 201, "{\"ok\":true,\"id\":" + novoId + "}");
                    }
                } else if ("PUT".equals(method)) {
                    if (id <= 0) { send404(ex); return; }
                    Usuarios existente = dao.buscarUsuario(id);
                    if (existente == null) { send404(ex); return; }
                    JSONObject j = readBody(ex);
                    existente.setNome(j.getString("nome"));
                    existente.setEmail(j.getString("email"));
                    if (j.has("senha") && !j.getString("senha").isBlank()) {
                        String senhaXor = UsuarioService.xorCriptografar(
                            j.getString("senha"),
                            j.getString("email") + "AEDs3-G9-2025"
                        );
                        existente.setSenhaXor(senhaXor);
                    }
                    if (j.has("redesSociais")) {
                        existente.setRedesSociais(parseStringArray(j, "redesSociais"));
                    }
                    boolean ok = dao.alterarUsuario(existente);
                    sendJson(ex, ok ? 200 : 500, "{\"ok\":" + ok + "}");
                } else if ("DELETE".equals(method)) {
                    if (id <= 0) { send404(ex); return; }
                    boolean ok = dao.excluirUsuario(id);
                    sendJson(ex, ok ? 200 : 404, "{\"ok\":" + ok + "}");
                } else {
                    sendJson(ex, 405, "{\"erro\":\"Method not allowed\"}");
                }
            } catch (Exception e) { sendError(ex, e); }
        }

        private JSONObject usuarioToJson(Usuarios u) {
            JSONArray rs = new JSONArray();
            if (u.getRedesSociais() != null) for (String r : u.getRedesSociais()) rs.put(r);
            return new JSONObject()
                .put("id",           u.getId())
                .put("nome",         u.getNome())
                .put("email",        u.getEmail())
                .put("redesSociais", rs);
        }
    }

    // --- HANDLER: N:N LIVROS-AUTORES ---
    // Agora retorna nomeLivro e nomeAutor
    private class LivrosAutoresHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (handleCors(ex)) return;
            try {
                LivroAutorDAO dao = factory.getLivroAutorDAO();
                int id = extractId(ex);
                String method = ex.getRequestMethod();

                if ("GET".equals(method)) {
                    JSONArray arr = new JSONArray();
                    for (LivroAutor la : dao.listarTodos()) {
                        String nomeLivro = "Livro #" + la.getIdLivro();
                        String nomeAutor = "Autor #" + la.getIdAutor();
                        try {
                            Livro l = factory.getLivroDAO().buscarLivroPorId(la.getIdLivro());
                            if (l != null) nomeLivro = l.getTitulo();
                        } catch (Exception ignored) {}
                        try {
                            Autores a = factory.getAutoresDAO().buscarAutor(la.getIdAutor());
                            if (a != null) nomeAutor = a.getNome();
                        } catch (Exception ignored) {}

                        arr.put(new JSONObject()
                            .put("id",        la.getId())
                            .put("idLivro",   la.getIdLivro())
                            .put("idAutor",   la.getIdAutor())
                            .put("nomeLivro", nomeLivro)
                            .put("nomeAutor", nomeAutor));
                    }
                    sendJson(ex, 200, arr.toString());
                } else if ("POST".equals(method)) {
                    JSONObject j = readBody(ex);
                    dao.vincularAutorAoLivro(j.getInt("idLivro"), j.getInt("idAutor"));
                    sendJson(ex, 201, "{\"ok\":true}");
                } else if ("DELETE".equals(method)) {
                    if (id <= 0) { send404(ex); return; }
                    boolean ok = dao.excluirPorId(id);
                    sendJson(ex, ok ? 200 : 404, "{\"ok\":" + ok + "}");
                } else {
                    sendJson(ex, 405, "{\"erro\":\"Method not allowed\"}");
                }
            } catch (Exception e) { sendError(ex, e); }
        }
    }

    // --- HANDLER: N:N TAGS-LIVROS ---
    // Agora retorna nomeTag e nomeLivro
    private class TagsLivrosHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (handleCors(ex)) return;
            try {
                TagsLivrosDAO dao = factory.getTagsLivrosDAO();
                int id = extractId(ex);
                String method = ex.getRequestMethod();
                String query  = ex.getRequestURI().getQuery();

                if ("GET".equals(method)) {
                    List<TagsLivros> lista;
                    if (query != null && query.startsWith("idTag=")) {
                        int idTag = Integer.parseInt(query.substring(6));
                        lista = dao.buscarLivrosDaTag(idTag);
                    } else if (query != null && query.startsWith("idLivro=")) {
                        int idLivro = Integer.parseInt(query.substring(8));
                        lista = dao.buscarTagsDoLivro(idLivro);
                    } else {
                        lista = dao.listarTodos();
                    }

                    JSONArray arr = new JSONArray();
                    for (TagsLivros tl : lista) {
                        String nomeTag   = "Tag #"   + tl.getIdTag();
                        String nomeLivro = "Livro #" + tl.getIdLivro();
                        try {
                            Tag t = factory.getTagDAO().buscarTag(tl.getIdTag());
                            if (t != null) nomeTag = t.getNome();
                        } catch (Exception ignored) {}
                        try {
                            Livro l = factory.getLivroDAO().buscarLivroPorId(tl.getIdLivro());
                            if (l != null) nomeLivro = l.getTitulo();
                        } catch (Exception ignored) {}

                        arr.put(new JSONObject()
                            .put("id",        tl.getId())
                            .put("idTag",     tl.getIdTag())
                            .put("idLivro",   tl.getIdLivro())
                            .put("nomeTag",   nomeTag)
                            .put("nomeLivro", nomeLivro));
                    }
                    sendJson(ex, 200, arr.toString());

                } else if ("POST".equals(method)) {
                    JSONObject j = readBody(ex);
                    int idTag   = j.getInt("idTag");
                    int idLivro = j.getInt("idLivro");
                    if (idTag <= 0 || idLivro <= 0) {
                        sendJson(ex, 400, "{\"erro\":\"idTag e idLivro são obrigatórios.\"}");
                        return;
                    }
                    boolean ok = dao.vincularTagAoLivro(idTag, idLivro);
                    sendJson(ex, ok ? 201 : 500, "{\"ok\":" + ok + "}");

                } else if ("DELETE".equals(method)) {
                    if (id <= 0) { send404(ex); return; }
                    boolean ok = dao.excluirPorId(id);
                    sendJson(ex, ok ? 200 : 404, "{\"ok\":" + ok + "}");

                } else {
                    sendJson(ex, 405, "{\"erro\":\"Method not allowed\"}");
                }
            } catch (Exception e) { sendError(ex, e); }
        }
    }

    // --- HANDLER: TAGS ---
    private class TagsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (handleCors(ex)) return;
            try {
                TagDAO dao = factory.getTagDAO();
                int id = extractId(ex);
                String method = ex.getRequestMethod();

                if ("GET".equals(method)) {
                    if (id > 0) {
                        Tag t = dao.buscarTag(id);
                        if (t == null) { send404(ex); return; }
                        sendJson(ex, 200, tagToJson(t).toString());
                    } else {
                        String query = ex.getRequestURI().getQuery();
                        boolean decrescente = query != null && query.contains("ordem=id-desc");
                        List<Tag> lista = decrescente
                            ? dao.listarOrdenadoDecrescentePorId()
                            : dao.listarOrdenadoPorId();
                        JSONArray arr = new JSONArray();
                        for (Tag t : lista) arr.put(tagToJson(t));
                        sendJson(ex, 200, arr.toString());
                    }
                } else if ("POST".equals(method)) {
                    JSONObject j = readBody(ex);
                    String nome = j.getString("nome").trim();
                    if (nome.isEmpty()) {
                        sendJson(ex, 400, "{\"erro\":\"O nome da tag não pode ser vazio.\"}");
                        return;
                    }
                    int novoId = dao.criarTag(new Tag(nome));
                    sendJson(ex, 201, "{\"ok\":true,\"id\":" + novoId + "}");
                } else if ("PUT".equals(method)) {
                    if (id <= 0) { send404(ex); return; }
                    Tag existente = dao.buscarTag(id);
                    if (existente == null) { send404(ex); return; }
                    JSONObject j = readBody(ex);
                    String novoNome = j.getString("nome").trim();
                    if (novoNome.isEmpty()) {
                        sendJson(ex, 400, "{\"erro\":\"O nome da tag não pode ser vazio.\"}");
                        return;
                    }
                    existente.setNome(novoNome);
                    boolean ok = dao.alterarTag(existente);
                    sendJson(ex, ok ? 200 : 500, "{\"ok\":" + ok + "}");
                } else if ("DELETE".equals(method)) {
                    if (id <= 0) { send404(ex); return; }
                    boolean ok = dao.excluirTagEmCascata(id);
                    sendJson(ex, ok ? 200 : 404, "{\"ok\":" + ok + "}");
                } else {
                    sendJson(ex, 405, "{\"erro\":\"Method not allowed\"}");
                }
            } catch (IllegalStateException e) {
                sendJson(ex, 409, "{\"erro\":\"" + e.getMessage().replace("\"", "'") + "\"}");
            } catch (Exception e) { sendError(ex, e); }
        }

        private JSONObject tagToJson(Tag t) {
            return new JSONObject()
                .put("id",   t.getId())
                .put("nome", t.getNome());
        }
    }

    // --- HANDLER: AUTH ---
    private class AuthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (handleCors(ex)) return;
            try {
                if ("POST".equals(ex.getRequestMethod()) && ex.getRequestURI().getPath().endsWith("/login")) {
                    JSONObject j = readBody(ex);
                    Usuarios u = factory.getUsuarioService().login(j.getString("email"), j.getString("senha"));
                    if (u == null) sendJson(ex, 401, "{\"erro\":\"Credenciais invalidas\"}");
                    else           sendJson(ex, 200, new JSONObject().put("id", u.getId()).put("nome", u.getNome()).toString());
                }
            } catch (Exception e) { sendError(ex, e); }
        }
    }

    // AUXILIARES

    private static int extractId(HttpExchange ex) {
        String[] parts = ex.getRequestURI().getPath().split("/");
        if (parts.length >= 4) {
            try { return Integer.parseInt(parts[3]); } catch (NumberFormatException ignored) {}
        }
        return -1;
    }

    private static boolean handleCors(HttpExchange ex) throws IOException {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin",  "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        if ("OPTIONS".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(204, -1); return true; }
        return false;
    }

    private static JSONObject readBody(HttpExchange ex) throws IOException {
        return new JSONObject(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
    }

    private static void sendJson(HttpExchange ex, int status, String json) throws IOException {
        byte[] b = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(status, b.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(b); }
    }

    private static void send(HttpExchange ex, int status, String ct, String msg) throws IOException {
        byte[] b = msg.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", ct);
        ex.sendResponseHeaders(status, b.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(b); }
    }

    private void send404(HttpExchange ex) throws IOException { sendJson(ex, 404, "{\"erro\":\"Not found\"}"); }

    private void sendError(HttpExchange ex, Exception e) throws IOException {
        String msg = e.getMessage() != null ? e.getMessage().replace("\"", "'") : "Erro interno";
        sendJson(ex, 500, "{\"erro\":\"" + msg + "\"}");
    }

    private static char[] padIsbn(String raw) {
        return String.format("%-13s", (raw == null ? "" : raw)).substring(0, 13).toCharArray();
    }

    private static String[] parseStringArray(JSONObject j, String campo) {
        if (!j.has(campo)) return new String[0];
        JSONArray a = j.getJSONArray(campo);
        String[] r = new String[a.length()];
        for (int i = 0; i < a.length(); i++) r[i] = a.getString(i);
        return r;
    }
}