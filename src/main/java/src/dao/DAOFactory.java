package src.dao;

import src.service.UsuarioService;

// Fábrica Central de DAOs (Data Access Objects).
// Atua como um contêiner de Injeção de Dependências (DI), garantindo que
//todas as partes do sistema (como o ApiServer) utilizem exatamente as
//mesmas instâncias em memória, evitando concorrência nos arquivos físicos.
// nesta classe que as restrições de Integridade Referencial são "amarradas".

public class DAOFactory {

    private final EditoraDAO     editoraDAO;
    private final AutoresDAO     autoresDAO;
    private final LivroDAO       livroDAO;
    private final LivroAutorDAO  livroAutorDAO;
    private final UsuarioDAO     usuarioDAO;
    private final TagDAO         tagDAO;
    private final TagsLivrosDAO  tagsLivrosDAO;
    private final UsuarioService usuarioService;

    public DAOFactory() throws Exception {

        // 1. Instanciação Base (Abre/Cria os arquivos .bin e .idx)
        this.editoraDAO    = new EditoraDAO();
        this.autoresDAO    = new AutoresDAO();
        this.livroDAO      = new LivroDAO();
        this.livroAutorDAO = new LivroAutorDAO();
        this.usuarioDAO    = new UsuarioDAO();
        this.tagDAO        = new TagDAO();
        this.tagsLivrosDAO = new TagsLivrosDAO();

        // 2. Injeção de Dependências (Garantia de Integridade Referencial)

        // EditoraDAO bloqueia exclusão se houver livros vinculados a ela
        this.editoraDAO.setLivroDAO(this.livroDAO);

        // AutoresDAO bloqueia exclusão se houver vínculos na tabela livros_autores
        this.autoresDAO.setLivroAutorDAO(this.livroAutorDAO);

        // LivroDAO verifica vínculos em livros_autores antes de excluir e remove em cascata os vínculos em tags_livros
        this.livroDAO.setLivroAutorDAO(this.livroAutorDAO);
        this.livroDAO.setTagsLivrosDAO(this.tagsLivrosDAO); 

        // TagDAO bloqueia exclusão se houver livros vinculados (integridade N:N)
        this.tagDAO.setTagsLivrosDAO(this.tagsLivrosDAO);

        // 3. Inicialização de Serviços de Negócio (Camada de Segurança)
        this.usuarioService = new UsuarioService(this.usuarioDAO);
    }

    // Getters 

    public EditoraDAO     getEditoraDAO()     { return this.editoraDAO; }
    public AutoresDAO     getAutoresDAO()     { return this.autoresDAO; }
    public LivroDAO       getLivroDAO()       { return this.livroDAO; }
    public LivroAutorDAO  getLivroAutorDAO()  { return this.livroAutorDAO; }
    public UsuarioDAO     getUsuarioDAO()     { return this.usuarioDAO; }
    public TagDAO         getTagDAO()         { return this.tagDAO; }
    public TagsLivrosDAO  getTagsLivrosDAO()  { return this.tagsLivrosDAO; }
    public UsuarioService getUsuarioService() { return this.usuarioService; }
}