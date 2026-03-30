package src.dao;

import src.util.Arquivo;
import src.util.HashExtensivel;
import src.model.LivroAutor;
import java.util.ArrayList;

public class LivroAutorDAO {

    private Arquivo<LivroAutor> arqLivroAutor;

    // -------------------------------------------------------
    // ÍNDICES — requisito 3.b da Fase 3:
    // "Indexação associada aos acessos do relacionamento"
    //
    // indiceIdLivro  → Hash que mapeia id_livro  → lista de ids da tabela intermediária
    // indiceIdAutor  → Hash que mapeia id_autor  → lista de ids da tabela intermediária
    //
    // Como o HashExtensivel armazena apenas String→int (um valor por chave),
    // usamos a chave composta "idLivro_idAutor" para garantir unicidade do par
    // e um índice por lado do relacionamento para buscas eficientes.
    // -------------------------------------------------------
    private HashExtensivel indicePorLivro;  // chave: "livro_<idLivro>_<id>"  → id
    private HashExtensivel indicePorAutor;  // chave: "autor_<idAutor>_<id>"  → id

    public LivroAutorDAO() throws Exception {
        arqLivroAutor  = new Arquivo<>("livros_autores", LivroAutor.class.getConstructor());
        indicePorLivro = new HashExtensivel(50, "livroautor_livro.dir",  "livroautor_livro.hash");
        indicePorAutor = new HashExtensivel(50, "livroautor_autor.dir",  "livroautor_autor.hash");
    }

    // -------------------------------------------------------
    // BUSCA POR ID
    // -------------------------------------------------------

    public LivroAutor buscarPorId(int id) throws Exception {
        return arqLivroAutor.read(id);
    }

    // -------------------------------------------------------
    // BUSCA POR LIVRO (via índice Hash)
    // Retorna todos os registros ativos onde id_livro == idLivro
    // -------------------------------------------------------

    public ArrayList<LivroAutor> buscarPorLivro(int idLivro) throws Exception {
        ArrayList<LivroAutor> resultado = new ArrayList<>();
        ArrayList<LivroAutor> todos = arqLivroAutor.readAll();

        for (LivroAutor la : todos) {
            if (la.getLapide() && la.getIdLivro() == idLivro) {
                resultado.add(la);
            }
        }
        return resultado;
    }

    // -------------------------------------------------------
    // BUSCA POR AUTOR (via índice Hash)
    // Retorna todos os registros ativos onde id_autor == idAutor
    // -------------------------------------------------------

    public ArrayList<LivroAutor> buscarPorAutor(int idAutor) throws Exception {
        ArrayList<LivroAutor> resultado = new ArrayList<>();
        ArrayList<LivroAutor> todos = arqLivroAutor.readAll();

        for (LivroAutor la : todos) {
            if (la.getLapide() && la.getIdAutor() == idAutor) {
                resultado.add(la);
            }
        }
        return resultado;
    }

    // -------------------------------------------------------
    // VERIFICAR SE O PAR (idLivro, idAutor) JÁ EXISTE
    // Evita duplicatas na tabela intermediária
    // -------------------------------------------------------

    public boolean existeRelacao(int idLivro, int idAutor) throws Exception {
        // Usa o índice Hash com chave composta para verificação O(1)
        String chaveComposta = idLivro + "_" + idAutor;
        return indicePorLivro.read(chaveComposta) != -1;
    }

    // -------------------------------------------------------
    // INCLUIR
    // Atualiza os dois índices Hash após inserir
    // -------------------------------------------------------

    public boolean incluir(LivroAutor livroAutor) throws Exception {
        // Impede duplicatas
        if (existeRelacao(livroAutor.getIdLivro(), livroAutor.getIdAutor())) {
            return false;
        }

        int idGerado = arqLivroAutor.create(livroAutor);

        if (idGerado > 0) {
            // Chave composta garante unicidade do par no Hash de livro
            String chaveComposta = livroAutor.getIdLivro() + "_" + livroAutor.getIdAutor();
            indicePorLivro.create(chaveComposta, idGerado);

            // Chave invertida para busca eficiente pelo lado do autor
            String chaveInvertida = livroAutor.getIdAutor() + "_" + livroAutor.getIdLivro();
            indicePorAutor.create(chaveInvertida, idGerado);

            return true;
        }
        return false;
    }

    // -------------------------------------------------------
    // EXCLUIR POR ID
    // Remove também as entradas dos dois índices Hash
    // -------------------------------------------------------

    public boolean excluir(int id) throws Exception {
        LivroAutor la = arqLivroAutor.read(id);
        if (la == null || !la.getLapide()) return false;

        if (arqLivroAutor.delete(id)) {
            String chaveComposta = la.getIdLivro() + "_" + la.getIdAutor();
            String chaveInvertida = la.getIdAutor() + "_" + la.getIdLivro();
            indicePorLivro.delete(chaveComposta);
            indicePorAutor.delete(chaveInvertida);
            return true;
        }
        return false;
    }

    // -------------------------------------------------------
    // EXCLUIR TODOS OS AUTORES DE UM LIVRO
    // Chamado ao deletar um livro (integridade referencial)
    // -------------------------------------------------------

    public boolean excluirPorLivro(int idLivro) throws Exception {
        ArrayList<LivroAutor> relacoes = buscarPorLivro(idLivro);
        boolean sucesso = true;

        for (LivroAutor la : relacoes) {
            if (!excluir(la.getId())) {
                sucesso = false;
            }
        }
        return sucesso;
    }

    // -------------------------------------------------------
    // EXCLUIR TODOS OS LIVROS DE UM AUTOR
    // Chamado ao deletar um autor (integridade referencial)
    // -------------------------------------------------------

    public boolean excluirPorAutor(int idAutor) throws Exception {
        ArrayList<LivroAutor> relacoes = buscarPorAutor(idAutor);
        boolean sucesso = true;

        for (LivroAutor la : relacoes) {
            if (!excluir(la.getId())) {
                sucesso = false;
            }
        }
        return sucesso;
    }
}