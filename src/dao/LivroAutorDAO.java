package src.dao;

import java.util.List;
import java.util.stream.Collectors;
import src.model.LivroAutor;
import src.util.Arquivo;
import src.util.Indexador;

public class LivroAutorDAO {

    private final Arquivo<LivroAutor> arqLivrosAutores;
    private final Indexador indice;

    public LivroAutorDAO(Indexador indice) throws Exception {
        this.arqLivrosAutores = new Arquivo<>("livros_autores", LivroAutor.class.getConstructor());
        this.indice = indice;
    }

    // --- create ---

    public boolean vincularAutorAoLivro(int idLivro, int idAutor) throws Exception {
        LivroAutor la = new LivroAutor(idLivro, idAutor);
        
        int id = arqLivrosAutores.create(la);
        if (id > 0) {
            indice.inserir(id, indice.buscar(id));
        }

        return id > 0;
    }

    // vincula múltiplos autores a um mesmo livro (N:N — um livro tem muitos autores)
    public int vincularAutoresAoLivro(int idLivro, int[] idsAutores) throws Exception {
        int vinculados = 0;

        for (int idAutor : idsAutores) {
            if (vincularAutorAoLivro(idLivro, idAutor)) {
                vinculados++;
            }
        }

        return vinculados;
    }

    // vincula múltiplos livros a um mesmo autor (N:N — um autor tem muitos livros)
    public int vincularLivrosAoAutor(int idAutor, int[] idsLivros) throws Exception {
        int vinculados = 0;

        for (int idLivro : idsLivros) {
            if (vincularAutorAoLivro(idLivro, idAutor)) {
                vinculados++;
            }
        }

        return vinculados;
    }

    // --- read ---

    public LivroAutor buscarPorId(int id) throws Exception {
        if (indice.buscar(id) < 0) {
            return null;
        }
        return arqLivrosAutores.read(id);
    }

    // retorna todos os registros de vínculo de um livro (todos os autores do livro)
    public List<LivroAutor> buscarAutoresDoLivro(int idLivro) throws Exception {
        return arqLivrosAutores.listarTodos()
                .stream()
                .filter(la -> la.getIdLivro() == idLivro)
                .collect(Collectors.toList());
    }

    // retorna todos os registros de vínculo de um autor (todos os livros do autor)
    public List<LivroAutor> buscarLivrosDoAutor(int idAutor) throws Exception {
        return arqLivrosAutores.listarTodos()
                .stream()
                .filter(la -> la.getIdAutor() == idAutor)
                .collect(Collectors.toList());
    }

    // retorna apenas os IDs dos autores de um livro
    public int[] buscarIdsAutoresDoLivro(int idLivro) throws Exception {
        return buscarAutoresDoLivro(idLivro)
                .stream()
                .mapToInt(LivroAutor::getIdAutor)
                .toArray();
    }

    // retorna apenas os IDs dos livros de um autor
    public int[] buscarIdsLivrosDoAutor(int idAutor) throws Exception {
        return buscarLivrosDoAutor(idAutor)
                .stream()
                .mapToInt(LivroAutor::getIdLivro)
                .toArray();
    }

    public List<LivroAutor> listarTodos() throws Exception {
        return arqLivrosAutores.listarTodos();
    }

    // --- update ---

    public boolean alterarLivroAutor(LivroAutor livroAutor) throws Exception {
        boolean ok = arqLivrosAutores.update(livroAutor);

        if (ok) {
            indice.atualizar(livroAutor.getId(), indice.buscar(livroAutor.getId()));
        }

        return ok;
    }

    // --- delete ---

    public boolean excluirPorId(int id) throws Exception {
        boolean ok = arqLivrosAutores.delete(id);

        if (ok) {
            indice.remover(id);
        }

        return ok;
    }

    // remove todos os vínculos de um livro (ex: ao excluir o livro)
    public int excluirAutoresDoLivro(int idLivro) throws Exception {
        List<LivroAutor> registros = buscarAutoresDoLivro(idLivro);

        int removidos = 0;

        for (LivroAutor la : registros) {
            if (excluirPorId(la.getId())) {
                removidos++;
            }
        }

        return removidos;
    }

    // remove todos os vínculos de um autor (ex: ao excluir o autor)
    public int excluirLivrosDoAutor(int idAutor) throws Exception {
        List<LivroAutor> registros = buscarLivrosDoAutor(idAutor);

        int removidos = 0;

        for (LivroAutor la : registros) {
            if (excluirPorId(la.getId())) {
                removidos++;
            }
        }

        return removidos;
    }
}