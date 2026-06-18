package src.service;

import src.dao.LivroDAO;
import src.model.Livro;
import src.search.BoyerMoore;
import src.search.KMP;

import java.util.ArrayList;
import java.util.List;

/**
 * Serviço para realizar buscas textuais utilizando os algoritmos KMP e Boyer-Moore.
 */
public class BuscaService {

    private final LivroDAO livroDAO;

    public BuscaService(LivroDAO livroDAO) {
        this.livroDAO = livroDAO;
    }

    /**
     * Busca livros cujo título contenha o padrão informado, utilizando o algoritmo escolhido.
     * @param padrao    texto a ser procurado
     * @param algoritmo "kmp" ou "bm" (case-insensitive)
     * @return lista de livros que contêm o padrão no título
     * @throws Exception se ocorrer erro na leitura dos dados
     */
    public List<Livro> buscarLivrosPorTitulo(String padrao, String algoritmo) throws Exception {
        if (padrao == null || padrao.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // Obtém todos os livros ativos
        List<Livro> todos = livroDAO.listarTodos();
        List<Livro> resultado = new ArrayList<>();

        boolean usarKMP = algoritmo != null && algoritmo.equalsIgnoreCase("kmp");
        boolean usarBM = algoritmo != null && algoritmo.equalsIgnoreCase("bm");

        // Se o algoritmo não for reconhecido, usa KMP por padrão
        if (!usarKMP && !usarBM) {
            usarKMP = true;
        }

        for (Livro livro : todos) {
            String titulo = livro.getTitulo();
            if (titulo == null) continue;

            List<Integer> posicoes;
            if (usarKMP) {
                posicoes = KMP.buscar(titulo, padrao);
            } else { // BM
                posicoes = BoyerMoore.buscar(titulo, padrao);
            }

            if (!posicoes.isEmpty()) {
                resultado.add(livro);
            }
        }
        return resultado;
    }

    /**
     * Busca livros cujo título ou sinopse contenha o padrão.
     * @param padrao    texto a ser procurado
     * @param algoritmo "kmp" ou "bm"
     * @return lista de livros que contêm o padrão em qualquer um dos campos
     * @throws Exception
     */
    public List<Livro> buscarLivrosPorTituloOuSinopse(String padrao, String algoritmo) throws Exception {
        if (padrao == null || padrao.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<Livro> todos = livroDAO.listarTodos();
        List<Livro> resultado = new ArrayList<>();

        for (Livro livro : todos) {
            String titulo = livro.getTitulo();
            String sinopse = livro.getSinopse();

            boolean encontrou = false;

            if (titulo != null) {
                List<Integer> posTitulo = usarKMP(algoritmo) ? KMP.buscar(titulo, padrao) : BoyerMoore.buscar(titulo, padrao);
                if (!posTitulo.isEmpty()) encontrou = true;
            }

            if (!encontrou && sinopse != null) {
                List<Integer> posSinopse = usarKMP(algoritmo) ? KMP.buscar(sinopse, padrao) : BoyerMoore.buscar(sinopse, padrao);
                if (!posSinopse.isEmpty()) encontrou = true;
            }

            if (encontrou) resultado.add(livro);
        }
        return resultado;
    }

    private boolean usarKMP(String algoritmo) {
        return algoritmo == null || algoritmo.equalsIgnoreCase("kmp");
    }
}