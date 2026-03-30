package src.dao;

import src.util.Arquivo;
import src.util.HashExtensivel;
import src.util.ArvoreBMais_String_Int;
import src.model.Livro;
import java.util.ArrayList;

public class LivroDAO {
    private Arquivo<Livro> arqLivros;
    private HashExtensivel indiceIsbn;
    private ArvoreBMais_String_Int indiceTitulo;

    public LivroDAO() throws Exception {
        arqLivros    = new Arquivo<>("livros", Livro.class.getConstructor());
        indiceIsbn   = new HashExtensivel(100, "isbn_diretorio.dir", "isbn_cestos.hash");
        indiceTitulo = new ArvoreBMais_String_Int(5, "titulo_arvore.btree");
    }

    // -------------------------------------------------------
    // BUSCA POR ID
    // -------------------------------------------------------

    public Livro buscarLivroId(int id) throws Exception {
        return arqLivros.read(id);
    }

    // -------------------------------------------------------
    // BUSCA POR ISBN (via Hash Extensível)
    // -------------------------------------------------------

    public Livro buscarLivroIsbn(String isbn) throws Exception {
        int idLivro = indiceIsbn.read(isbn);
        if (idLivro != -1) return arqLivros.read(idLivro);
        return null;
    }

    // -------------------------------------------------------
    // BUSCA POR TÍTULO (via Árvore B+)
    // -------------------------------------------------------

    public Livro buscarLivroTitulo(String titulo) throws Exception {
        int idLivro = indiceTitulo.buscar(titulo);
        if (idLivro != -1) return arqLivros.read(idLivro);
        return null;
    }

    // -------------------------------------------------------
    // LISTAGEM ORDENADA POR TÍTULO (travessia da Árvore B+)
    // Resolve o requisito 3.d do enunciado
    // -------------------------------------------------------

    public ArrayList<Livro> listarLivrosOrdenadosPorTitulo() throws Exception {
        ArrayList<Integer> idsOrdenados = indiceTitulo.listarEmOrdem();
        ArrayList<Livro> livrosOrdenados = new ArrayList<>();

        for (int id : idsOrdenados) {
            if (id > 0) {
                Livro l = arqLivros.read(id);
                if (l != null && l.getLapide()) {
                    livrosOrdenados.add(l);
                }
            }
        }
        return livrosOrdenados;
    }

    // -------------------------------------------------------
    // BUSCA POR EDITORA — relacionamento 1:N
    // -------------------------------------------------------

    public ArrayList<Livro> buscarLivrosPorEditora(int idEditora) throws Exception {
        ArrayList<Livro> resultado = new ArrayList<>();
        ArrayList<Livro> todos = arqLivros.readAll();

        for (Livro l : todos) {
            if (l.getLapide() && l.getIdEditora() == idEditora) {
                resultado.add(l);
            }
        }
        return resultado;
    }

    // -------------------------------------------------------
    // INCLUIR
    // -------------------------------------------------------

    public boolean incluirLivro(Livro livro) throws Exception {
        int idGerado = arqLivros.create(livro);

        if (idGerado > 0) {
            indiceIsbn.create(new String(livro.getIsbn()).trim(), idGerado);
            indiceTitulo.inserir(livro.getTitulo(), idGerado);
            return true;
        }
        return false;
    }

    // -------------------------------------------------------
    // ALTERAR
    // CORREÇÃO 3: título agora pode ser alterado pois
    // delete() foi implementado na Árvore B+.
    // -------------------------------------------------------

    public boolean alterarLivro(Livro livroAtualizado) throws Exception {
        Livro livroAntigo = arqLivros.read(livroAtualizado.getId());
        if (livroAntigo == null) return false;

        String isbnAntigo  = new String(livroAntigo.getIsbn()).trim();
        String isbnNovo    = new String(livroAtualizado.getIsbn()).trim();
        String tituloAntigo = livroAntigo.getTitulo();
        String tituloNovo   = livroAtualizado.getTitulo();

        boolean atualizou = arqLivros.update(livroAtualizado);

        if (atualizou) {
            // Atualiza índice Hash se o ISBN mudou
            if (!isbnAntigo.equals(isbnNovo)) {
                indiceIsbn.delete(isbnAntigo);
                indiceIsbn.create(isbnNovo, livroAtualizado.getId());
            }
            // CORREÇÃO 3: Atualiza índice B+ se o título mudou
            if (!tituloAntigo.equals(tituloNovo)) {
                indiceTitulo.delete(tituloAntigo);
                indiceTitulo.inserir(tituloNovo, livroAtualizado.getId());
            }
        }
        return atualizou;
    }

    // -------------------------------------------------------
    // EXCLUIR
    // -------------------------------------------------------

    public boolean excluirLivro(int id) throws Exception {
        Livro l = arqLivros.read(id);
        if (l == null) return false;

        if (arqLivros.delete(id)) {
            indiceIsbn.delete(new String(l.getIsbn()).trim());
            // CORREÇÃO 3: remove o título da Árvore B+
            indiceTitulo.delete(l.getTitulo());
            return true;
        }
        return false;
    }
}