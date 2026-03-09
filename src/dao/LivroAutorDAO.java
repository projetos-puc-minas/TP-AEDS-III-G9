package src.dao;

import java.util.List;
import java.util.stream.Collectors;
import src.model.LivroAutor;
import src.util.Arquivo;

public class LivroAutorDAO {

    private final Arquivo<LivroAutor> arqLivrosAutores;

    public LivroAutorDAO() throws Exception {
        arqLivrosAutores = new Arquivo<>("livros_autores", LivroAutor.class.getConstructor());
    }

    // --- create ---

    // vincula um único par livro-autor
    public boolean vincularAutorAoLivro(int idLivro, int idAutor) throws Exception {
        return arqLivrosAutores.create(new LivroAutor(idLivro, idAutor)) > 0;
    }

    // vincula múltiplos autores a um mesmo livro (N:N — um livro tem muitos autores)
    public int vincularAutoresAoLivro(int idLivro, int[] idsAutores) throws Exception {
        int vinculados = 0;
        
        for (int idAutor : idsAutores) {
            if (arqLivrosAutores.create(new LivroAutor(idLivro, idAutor)) > 0) vinculados++;
        }

        return vinculados;
    }

    // vincula múltiplos livros a um mesmo autor (N:N — um autor tem muitos livros)
    public int vincularLivrosAoAutor(int idAutor, int[] idsLivros) throws Exception {
        int vinculados = 0;

        for (int idLivro : idsLivros) {
            if (arqLivrosAutores.create(new LivroAutor(idLivro, idAutor)) > 0) vinculados++;
        }

        return vinculados;
    }

    // --- fead ---

    public LivroAutor buscarPorId(int id) throws Exception {
        return arqLivrosAutores.read(id);
    }

    // fetorna todos os registros de vínculo de um livro (todos os autores do livro)
    public List<LivroAutor> buscarAutoresDoLivro(int idLivro) throws Exception {
        return arqLivrosAutores.listarTodos()
                .stream()
                .filter(la -> la.getIdLivro() == idLivro)
                .collect(Collectors.toList());
    }

    // fetorna todos os registros de vínculo de um autor (todos os livros do autor)
    public List<LivroAutor> buscarLivrosDoAutor(int idAutor) throws Exception {
        return arqLivrosAutores.listarTodos()
                .stream()
                .filter(la -> la.getIdAutor() == idAutor)
                .collect(Collectors.toList());
    }

    // fetorna apenas os IDs dos autores de um livro
    public int[] buscarIdsAutoresDoLivro(int idLivro) throws Exception {
        return buscarAutoresDoLivro(idLivro)
                .stream()
                .mapToInt(LivroAutor::getIdAutor)
                .toArray();
    }

    // fetorna apenas os IDs dos livros de um autor
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
        return arqLivrosAutores.update(livroAutor);
    }

    // --- delete ---

    public boolean excluirPorId(int id) throws Exception {
        return arqLivrosAutores.delete(id);
    }

    // remove todos os vínculos de um livro (ex: ao excluir o livro)
    public int excluirAutoresDoLivro(int idLivro) throws Exception {
        List<LivroAutor> registros = buscarAutoresDoLivro(idLivro);
        int removidos = 0;

        for (LivroAutor la : registros) {
            if (arqLivrosAutores.delete(la.getId())) removidos++;
        }

        return removidos;
    }

    // remove todos os vínculos de um autor (ex: ao excluir o autor)
    public int excluirLivrosDoAutor(int idAutor) throws Exception {
        List<LivroAutor> registros = buscarLivrosDoAutor(idAutor);
        int removidos = 0;
        
        for (LivroAutor la : registros) {
            if (arqLivrosAutores.delete(la.getId())) removidos++;
        }

        return removidos;
    }
}