package src.dao;

import src.service.UsuarioService;

/**
 * Fábrica central de DAOs.
 *
 * Tabelas do sistema:
 *   - Editora       (1 lado do 1:N com Livro)
 *   - Livro         (N lado do 1:N; possui campo multivalorado: generos[])
 *   - Autores
 *   - LivroAutor    (tabela intermediária do N:N entre Livro e Autores)
 *   - Usuarios      (autenticação XOR)
 *
 * As instâncias são singletons dentro do DAOFactory — um único factory
 * deve ser criado e reutilizado durante toda a execução.
 */
public class DAOFactory {

    private final EditoraDAO    editoraDAO;
    private final AutoresDAO    autoresDAO;
    private final LivroDAO      livroDAO;
    private final LivroAutorDAO livroAutorDAO;
    private final UsuarioDAO    usuarioDAO;
    private final UsuarioService usuarioService;

    public DAOFactory() throws Exception {

        // 1. Instancia todos os DAOs
        editoraDAO    = new EditoraDAO();
        autoresDAO    = new AutoresDAO();
        livroDAO      = new LivroDAO();
        livroAutorDAO = new LivroAutorDAO();
        usuarioDAO    = new UsuarioDAO();

        // 2. Injeta dependências para integridade referencial

        // EditoraDAO bloqueia exclusão se houver livros vinculados
        editoraDAO.setLivroDAO(livroDAO);

        // AutoresDAO bloqueia exclusão se houver vínculos em livros_autores
        autoresDAO.setLivroAutorDAO(livroAutorDAO);

        // LivroDAO verifica vínculos em livros_autores antes de excluir
        livroDAO.setLivroAutorDAO(livroAutorDAO);

        // 3. Serviço de autenticação com criptografia XOR
        usuarioService = new UsuarioService(usuarioDAO);
    }

    // --- Getters ---

    public EditoraDAO    getEditoraDAO()      { return editoraDAO; }
    public AutoresDAO    getAutoresDAO()      { return autoresDAO; }
    public LivroDAO      getLivroDAO()        { return livroDAO; }
    public LivroAutorDAO getLivroAutorDAO()   { return livroAutorDAO; }
    public UsuarioDAO    getUsuarioDAO()      { return usuarioDAO; }
    public UsuarioService getUsuarioService() { return usuarioService; }
}