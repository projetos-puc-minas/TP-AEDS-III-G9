package src.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import src.dao.*;
import src.model.*;
import src.service.UsuarioService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Servidor HTTP embutido (com.sun.net.httpserver).
 *
 * Rotas estáticas:
 *   GET /              → index.html
 *   GET /style.css     → style.css
 *   GET /app.js        → app.js
 *
 * Rotas da API (JSON):
 *   /api/livros              GET, POST
 *   /api/livros/{id}         GET, PUT, DELETE
 *   /api/autores             GET, POST
 *   /api/autores/{id}        GET, PUT, DELETE
 *   /api/editoras            GET, POST
 *   /api/editoras/{id}       GET, PUT, DELETE
 *   /api/usuarios            GET, POST
 *   /api/usuarios/{id}       GET, PUT, DELETE
 *   /api/auth/login          POST
 *   /api/livros-autores      GET, POST
 *   /api/livros-autores/{id} GET, DELETE
 */
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

        server.createContext("/api/livros",        new LivrosHandler());
        server.createContext("/api/autores",        new AutoresHandler());
        server.createContext("/api/editoras",       new EditorasHandler());
        server.createContext("/api/usuarios",       new UsuariosHandler());
        server.createContext("/api/auth",           new AuthHandler());
        server.createContext("/api/livros-autores", new LivrosAutoresHandler());
        // Arquivos estáticos — registrado por último
        server.createContext("/",                   new StaticHandler(webRoot));

        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("✓ BiblioSys rodando em http://localhost:" + port);
    }

    // =========================================================================
    // STATIC FILES
    // =========================================================================

    private static class StaticHandler implements HttpHandler {
        private final Path root;

        StaticHandler(String root) {
            this.root = Path.of(root).toAbsolutePath().normalize();
        }

        @Override
        public void handle(HttpExchange ex) throws IOException {
            if ("OPTIONS".equals(ex.getRequestMethod())) {
                setCorsHeaders(ex);
                ex.sendResponseHeaders(204, -1);
                return;
            }

            String reqPath = ex.getRequestURI().getPath();
            if (reqPath.equals("/")) reqPath = "/index.html";

            // Bloqueia path traversal via canonicalização
            Path target = root.resolve(reqPath.substring(1)).normalize();
            if (!target.startsWith(root)) {
                send(ex, 403, "text/plain", "Forbidden");
                return;
            }

            File f = target.toFile();
            if (!f.exists() || f.isDirectory()) {
                send(ex, 404, "text/plain", "Not found");
                return;
            }

            String mime = mimeFor(reqPath);
            byte[] body = Files.readAllBytes(target);
            setCorsHeaders(ex);
            ex.getResponseHeaders().set("Content-Type", mime);
            ex.sendResponseHeaders(200, body.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(body); }
        }

        private static String mimeFor(String path) {
            if (path.endsWith(".html")) return "text/html; charset=utf-8";
            if (path.endsWith(".css"))  return "text/css; charset=utf-8";
            if (path.endsWith(".js"))   return "application/javascript; charset=utf-8";
            if (path.endsWith(".json")) return "application/json; charset=utf-8";
            if (path.endsWith(".png"))  return "image/png";
            if (path.endsWith(".svg"))  return "image/svg+xml";
            return "application/octet-stream";
        }
    }

    // =========================================================================
    // LIVROS
    // =========================================================================

    private class LivrosHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (handleCors(ex)) return;
            try {
                LivroDAO dao   = factory.getLivroDAO();
                String[] parts = ex.getRequestURI().getPath().split("/");
                boolean hasId  = parts.length >= 4 && !parts[3].isEmpty();
                int id = hasId ? Integer.parseInt(parts[3]) : -1;

                switch (ex.getRequestMethod()) {
                    case "GET": {
                        if (hasId) {
                            Livro l = dao.buscarLivroPorId(id);
                            if (l == null) { send404(ex); return; }
                            sendJson(ex, 200, livroToJson(l).toString());
                        } else {
                            JSONArray arr = new JSONArray();
                            for (Livro l : dao.listarOrdenadoPorId()) arr.put(livroToJson(l));
                            sendJson(ex, 200, arr.toString());
                        }
                        break;
                    }
                    case "POST": {
                        JSONObject j = readBody(ex);
                        Livro l = new Livro(
                            j.getInt("idEditora"),
                            j.getString("titulo"),
                            padIsbn(j.optString("isbn", "")),
                            j.optInt("anoPublicacao", 0),
                            j.optDouble("preco", 0.0),
                            j.optString("sinopse", ""),
                            parseStringArray(j, "generos")
                        );
                        boolean ok = dao.incluirLivro(l);
                        sendJson(ex, ok ? 201 : 500, "{\"ok\":" + ok + "}");
                        break;
                    }
                    case "PUT": {
                        JSONObject j = readBody(ex);
                        Livro l = dao.buscarLivroPorId(id);
                        if (l == null) { send404(ex); return; }
                        l.setTitulo(j.optString("titulo", l.getTitulo()));
                        l.setIdEditora(j.optInt("idEditora", l.getIdEditora()));
                        if (j.has("isbn")) l.setIsbn(padIsbn(j.getString("isbn")));
                        l.setAnoPublicacao(j.optInt("anoPublicacao", l.getAnoPublicacao()));
                        l.setPreco(j.optDouble("preco", l.getPreco()));
                        l.setSinopse(j.optString("sinopse", l.getSinopse()));
                        if (j.has("generos")) l.setGeneros(parseStringArray(j, "generos"));
                        boolean ok = dao.alterarLivro(l);
                        sendJson(ex, ok ? 200 : 500, "{\"ok\":" + ok + "}");
                        break;
                    }
                    case "DELETE": {
                        try {
                            boolean ok = dao.excluirLivro(id);
                            sendJson(ex, ok ? 200 : 404, "{\"ok\":" + ok + "}");
                        } catch (IllegalStateException e) {
                            sendJson(ex, 409, errJson(e.getMessage()));
                        }
                        break;
                    }
                    default: ex.sendResponseHeaders(405, -1);
                }
            } catch (Exception e) {
                sendError(ex, e);
            }
        }

        private JSONObject livroToJson(Livro l) {
            JSONArray gens = new JSONArray();
            if (l.getGeneros() != null) for (String g : l.getGeneros()) gens.put(g);
            return new JSONObject()
                .put("id",            l.getId())
                .put("idEditora",     l.getIdEditora())
                .put("titulo",        l.getTitulo())
                .put("isbn",          new String(l.getIsbn()).trim())
                .put("anoPublicacao", l.getAnoPublicacao())
                .put("preco",         l.getPreco())
                .put("sinopse",       l.getSinopse())
                .put("generos",       gens);
        }
    }

    // =========================================================================
    // AUTORES
    // =========================================================================

    private class AutoresHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (handleCors(ex)) return;
            try {
                AutoresDAO dao = factory.getAutoresDAO();
                String[] parts = ex.getRequestURI().getPath().split("/");
                boolean hasId  = parts.length >= 4 && !parts[3].isEmpty();
                int id = hasId ? Integer.parseInt(parts[3]) : -1;

                switch (ex.getRequestMethod()) {
                    case "GET": {
                        if (hasId) {
                            Autores a = dao.buscarAutor(id);
                            if (a == null) { send404(ex); return; }
                            sendJson(ex, 200, autorToJson(a).toString());
                        } else {
                            JSONArray arr = new JSONArray();
                            for (Autores a : dao.listarOrdenadoPorId()) arr.put(autorToJson(a));
                            sendJson(ex, 200, arr.toString());
                        }
                        break;
                    }
                    case "POST": {
                        JSONObject j = readBody(ex);
                        long ts = src.util.DataUtil.stringToTimestamp(j.getString("dataNascimento"));
                        Autores a = new Autores(-1, j.getString("nome"), ts, j.optString("biografia", ""));
                        int newId = dao.adicionarAutor(a);
                        sendJson(ex, newId > 0 ? 201 : 500, "{\"id\":" + newId + "}");
                        break;
                    }
                    case "PUT": {
                        JSONObject j = readBody(ex);
                        Autores a = dao.buscarAutor(id);
                        if (a == null) { send404(ex); return; }
                        a.setNome(j.optString("nome", a.getNome()));
                        a.setBiografia(j.optString("biografia", a.getBiografia()));
                        if (j.has("dataNascimento")) {
                            a.setDataNascimento(src.util.DataUtil.stringToTimestamp(j.getString("dataNascimento")));
                        }
                        boolean ok = dao.alterarAutor(a);
                        sendJson(ex, ok ? 200 : 500, "{\"ok\":" + ok + "}");
                        break;
                    }
                    case "DELETE": {
                        try {
                            boolean ok = dao.excluirAutor(id);
                            sendJson(ex, ok ? 200 : 404, "{\"ok\":" + ok + "}");
                        } catch (IllegalStateException e) {
                            sendJson(ex, 409, errJson(e.getMessage()));
                        }
                        break;
                    }
                    default: ex.sendResponseHeaders(405, -1);
                }
            } catch (Exception e) {
                sendError(ex, e);
            }
        }

        private JSONObject autorToJson(Autores a) {
            return new JSONObject()
                .put("id",                     a.getId())
                .put("nome",                   a.getNome())
                .put("dataNascimento",          a.getDataNascimento())
                .put("dataNascimentoFormatada", a.getDataNascimentoFormatada())
                .put("biografia",              a.getBiografia());
        }
    }

    // =========================================================================
    // EDITORAS
    // =========================================================================

    private class EditorasHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (handleCors(ex)) return;
            try {
                EditoraDAO dao = factory.getEditoraDAO();
                String[] parts = ex.getRequestURI().getPath().split("/");
                boolean hasId  = parts.length >= 4 && !parts[3].isEmpty();
                int id = hasId ? Integer.parseInt(parts[3]) : -1;

                switch (ex.getRequestMethod()) {
                    case "GET": {
                        if (hasId) {
                            Editora e = dao.buscarEditoraPorId(id);
                            if (e == null) { send404(ex); return; }
                            sendJson(ex, 200, editoraToJson(e).toString());
                        } else {
                            JSONArray arr = new JSONArray();
                            for (Editora e : dao.listarOrdenadoPorId()) arr.put(editoraToJson(e));
                            sendJson(ex, 200, arr.toString());
                        }
                        break;
                    }
                    case "POST": {
                        JSONObject j = readBody(ex);
                        Editora e = new Editora(
                            j.getString("nome"),
                            j.getString("cidade"),
                            j.optInt("anoFundacao", 0)
                        );
                        boolean ok = dao.incluirEditora(e);
                        sendJson(ex, ok ? 201 : 500, "{\"ok\":" + ok + "}");
                        break;
                    }
                    case "PUT": {
                        JSONObject j = readBody(ex);
                        Editora e = dao.buscarEditoraPorId(id);
                        if (e == null) { send404(ex); return; }
                        e.setNome(j.optString("nome", e.getNome()));
                        e.setCidade(j.optString("cidade", e.getCidade()));
                        e.setAnoFundacao(j.optInt("anoFundacao", e.getAnoFundacao()));
                        boolean ok = dao.alterarEditora(e);
                        sendJson(ex, ok ? 200 : 500, "{\"ok\":" + ok + "}");
                        break;
                    }
                    case "DELETE": {
                        try {
                            boolean ok = dao.excluirEditora(id);
                            sendJson(ex, ok ? 200 : 404, "{\"ok\":" + ok + "}");
                        } catch (IllegalStateException e) {
                            sendJson(ex, 409, errJson(e.getMessage()));
                        }
                        break;
                    }
                    default: ex.sendResponseHeaders(405, -1);
                }
            } catch (Exception e) {
                sendError(ex, e);
            }
        }

        private JSONObject editoraToJson(Editora e) {
            return new JSONObject()
                .put("id",          e.getId())
                .put("nome",        e.getNome())
                .put("cidade",      e.getCidade())
                .put("anoFundacao", e.getAnoFundacao());
        }
    }

    // =========================================================================
    // USUÁRIOS
    // =========================================================================

    private class UsuariosHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (handleCors(ex)) return;
            try {
                UsuarioDAO     dao = factory.getUsuarioDAO();
                UsuarioService svc = factory.getUsuarioService();
                String[] parts = ex.getRequestURI().getPath().split("/");
                boolean hasId  = parts.length >= 4 && !parts[3].isEmpty();
                int id = hasId ? Integer.parseInt(parts[3]) : -1;

                switch (ex.getRequestMethod()) {
                    case "GET": {
                        if (hasId) {
                            Usuarios u = dao.buscarUsuario(id);
                            if (u == null) { send404(ex); return; }
                            sendJson(ex, 200, usuarioToJson(u).toString());
                        } else {
                            JSONArray arr = new JSONArray();
                            for (Usuarios u : dao.listarTodos()) arr.put(usuarioToJson(u));
                            sendJson(ex, 200, arr.toString());
                        }
                        break;
                    }
                    case "POST": {
                        JSONObject j = readBody(ex);
                        int newId = svc.cadastrar(
                            j.getString("nome"),
                            j.getString("email"),
                            j.getString("senha")
                        );
                        if (newId == -1) {
                            sendJson(ex, 409, errJson("Email já cadastrado."));
                        } else {
                            sendJson(ex, 201, "{\"id\":" + newId + "}");
                        }
                        break;
                    }
                    case "PUT": {
                        JSONObject j = readBody(ex);
                        Usuarios u = dao.buscarUsuario(id);
                        if (u == null) { send404(ex); return; }
                        u.setNome(j.optString("nome", u.getNome()));
                        u.setEmail(j.optString("email", u.getEmail()));
                        if (j.has("senha") && !j.getString("senha").isBlank()) {
                            String chave    = u.getEmail() + "AEDs3-G9-2025";
                            String senhaXor = UsuarioService.xorCriptografar(j.getString("senha"), chave);
                            u.setSenhaXor(senhaXor);
                        }
                        boolean ok = dao.alterarUsuario(u);
                        sendJson(ex, ok ? 200 : 500, "{\"ok\":" + ok + "}");
                        break;
                    }
                    case "DELETE": {
                        try {
                            boolean ok = dao.excluirUsuario(id);
                            sendJson(ex, ok ? 200 : 404, "{\"ok\":" + ok + "}");
                        } catch (IllegalStateException e) {
                            sendJson(ex, 409, errJson(e.getMessage()));
                        }
                        break;
                    }
                    default: ex.sendResponseHeaders(405, -1);
                }
            } catch (Exception e) {
                sendError(ex, e);
            }
        }

        private JSONObject usuarioToJson(Usuarios u) {
            return new JSONObject()
                .put("id",    u.getId())
                .put("nome",  u.getNome())
                .put("email", u.getEmail());
            // senha nunca é exposta na API
        }
    }

    // =========================================================================
    // AUTH (login)
    // =========================================================================

    private class AuthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (handleCors(ex)) return;
            try {
                String path = ex.getRequestURI().getPath();

                // POST /api/auth/login
                if ("POST".equals(ex.getRequestMethod()) && path.endsWith("/login")) {
                    UsuarioService svc = factory.getUsuarioService();
                    JSONObject j = readBody(ex);
                    Usuarios u = svc.login(j.getString("email"), j.getString("senha"));
                    if (u == null) {
                        sendJson(ex, 401, errJson("Email ou senha incorretos."));
                    } else {
                        JSONObject resp = new JSONObject()
                            .put("id",    u.getId())
                            .put("nome",  u.getNome())
                            .put("email", u.getEmail());
                        sendJson(ex, 200, resp.toString());
                    }
                    return;
                }

                ex.sendResponseHeaders(405, -1);
            } catch (Exception e) {
                sendError(ex, e);
            }
        }
    }

    // =========================================================================
    // LIVROS × AUTORES (N:N)
    // =========================================================================

    private class LivrosAutoresHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (handleCors(ex)) return;
            try {
                LivroAutorDAO dao = factory.getLivroAutorDAO();
                String[] parts   = ex.getRequestURI().getPath().split("/");
                boolean hasId    = parts.length >= 4 && !parts[3].isEmpty();
                int id = hasId ? Integer.parseInt(parts[3]) : -1;

                switch (ex.getRequestMethod()) {
                    case "GET": {
                        if (hasId) {
                            LivroAutor la = dao.buscarPorId(id);
                            if (la == null) { send404(ex); return; }
                            sendJson(ex, 200, laToJson(la).toString());
                        } else {
                            JSONArray arr = new JSONArray();
                            for (LivroAutor la : dao.listarTodos()) arr.put(laToJson(la));
                            sendJson(ex, 200, arr.toString());
                        }
                        break;
                    }
                    case "POST": {
                        JSONObject j = readBody(ex);
                        boolean ok = dao.vincularAutorAoLivro(j.getInt("idLivro"), j.getInt("idAutor"));
                        sendJson(ex, ok ? 201 : 500, "{\"ok\":" + ok + "}");
                        break;
                    }
                    case "DELETE": {
                        boolean ok = dao.excluirPorId(id);
                        sendJson(ex, ok ? 200 : 404, "{\"ok\":" + ok + "}");
                        break;
                    }
                    default: ex.sendResponseHeaders(405, -1);
                }
            } catch (Exception e) {
                sendError(ex, e);
            }
        }

        private JSONObject laToJson(LivroAutor la) {
            return new JSONObject()
                .put("id",      la.getId())
                .put("idLivro", la.getIdLivro())
                .put("idAutor", la.getIdAutor());
        }
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private static boolean handleCors(HttpExchange ex) throws IOException {
        setCorsHeaders(ex);
        if ("OPTIONS".equals(ex.getRequestMethod())) {
            ex.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    private static void setCorsHeaders(HttpExchange ex) {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin",  "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private static JSONObject readBody(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody()) {
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return new JSONObject(body);
        }
    }

    private static void sendJson(HttpExchange ex, int status, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, body.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(body); }
    }

    private static void send(HttpExchange ex, int status, String ct, String msg) throws IOException {
        byte[] body = msg.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", ct);
        ex.sendResponseHeaders(status, body.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(body); }
    }

    private static void send404(HttpExchange ex) throws IOException {
        sendJson(ex, 404, errJson("Não encontrado."));
    }

    private static void sendError(HttpExchange ex, Exception e) throws IOException {
        e.printStackTrace();
        String msg = e.getMessage() != null ? e.getMessage() : "Erro interno";
        sendJson(ex, 500, errJson(msg));
    }

    private static String errJson(String msg) {
        return new JSONObject().put("erro", msg).toString();
    }

    /**
     * Garante que o ISBN tenha exatamente 13 caracteres,
     * truncando se maior ou preenchendo com espaços se menor.
     */
    private static char[] padIsbn(String raw) {
        if (raw == null) raw = "";
        raw = raw.strip();
        if (raw.length() > 13) raw = raw.substring(0, 13);
        StringBuilder sb = new StringBuilder(raw);
        while (sb.length() < 13) sb.append(' ');
        return sb.toString().toCharArray();
    }

    /**
     * Faz o parse de um campo que pode vir como JSONArray ou String com vírgulas.
     * Usado para o campo generos[] do Livro.
     */
    private static String[] parseStringArray(JSONObject j, String campo) {
        if (!j.has(campo)) return new String[0];
        Object v = j.get(campo);
        if (v instanceof JSONArray) {
            JSONArray arr = (JSONArray) v;
            String[] result = new String[arr.length()];
            for (int i = 0; i < arr.length(); i++) result[i] = arr.getString(i).trim();
            return result;
        }
        String str = v.toString().trim();
        if (str.isEmpty()) return new String[0];
        String[] parts = str.split(",");
        for (int i = 0; i < parts.length; i++) parts[i] = parts[i].trim();
        return parts;
    }
}