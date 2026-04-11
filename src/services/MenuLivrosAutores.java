package src.services;

import java.util.List;
import java.util.Scanner;
import src.dao.LivroAutorDAO;
import src.model.LivroAutor;
import src.util.ArvoreBMais;
import src.util.Indexador;

public class MenuLivrosAutores {

    private final LivroAutorDAO livroAutorDAO;
    private final Scanner console = new Scanner(System.in);

    public MenuLivrosAutores() throws Exception {
        Indexador indice = new ArvoreBMais("livros_autores");
        livroAutorDAO = new LivroAutorDAO(indice);
    }

    public void menu() {
        int opcao;

        do {
            System.out.println("\n\nAEDsIII");
            System.out.println("-------");
            System.out.println("> Início > Livros-Autores");
            System.out.println("\n1 - Listar autores de um livro");
            System.out.println("2 - Listar livros de um autor");
            System.out.println("3 - Listar todos os vínculos");
            System.out.println("4 - Vincular autores a um livro");
            System.out.println("5 - Vincular livros a um autor");
            System.out.println("6 - Alterar vínculo por ID");
            System.out.println("7 - Excluir vínculo por ID");
            System.out.println("8 - Excluir todos os autores de um livro");
            System.out.println("9 - Excluir todos os livros de um autor");
            System.out.println("0 - Voltar");
            System.out.print("\nOpção: ");

            opcao = lerInt();

            switch (opcao) {
                case 1: listarAutoresDoLivro(); break;
                case 2: listarLivrosDoAutor(); break;
                case 3: listarTodos(); break;
                case 4: vincularAutoresAoLivro(); break;
                case 5: vincularLivrosAoAutor(); break;
                case 6: alterarVinculo(); break;
                case 7: excluirPorId(); break;
                case 8: excluirAutoresDoLivro(); break;
                case 9: excluirLivrosDoAutor(); break;
                case 0: break;
                default:
                    System.out.println("Opção inválida!");
                    break;
            }
        } while (opcao != 0);
    }

    // --- listar autores de um livro (N:N: um livro tem muitos autores) ---

    private void listarAutoresDoLivro() {
        System.out.print("\nID do Livro: ");
        int idLivro = lerInt();

        try {
            List<LivroAutor> lista = livroAutorDAO.buscarAutoresDoLivro(idLivro);
            if (lista.isEmpty()) {
                System.out.println("Nenhum autor vinculado ao livro " + idLivro + ".");
            } else {
                System.out.println("\nAutores do livro " + idLivro + " (" + lista.size() + " autor(es)):");
                lista.forEach(System.out::println);
            }
        } catch (Exception e) {
            System.out.println("Erro ao buscar autores do livro.");
        }
    }

    // --- listar livros de um autor (N:N: um autor tem muitos livros) ---0,

    private void listarLivrosDoAutor() {
        System.out.print("\nID do Autor: ");
        int idAutor = lerInt();

        try {
            List<LivroAutor> lista = livroAutorDAO.buscarLivrosDoAutor(idAutor);
            if (lista.isEmpty()) {
                System.out.println("Nenhum livro vinculado ao autor " + idAutor + ".");
            } else {
                System.out.println("\nLivros do autor " + idAutor + " (" + lista.size() + " livro(s)):");
                lista.forEach(System.out::println);
            }
        } catch (Exception e) {
            System.out.println("Erro ao buscar livros do autor.");
        }
    }

    // --- listar todos ---

    private void listarTodos() {
        try {
            List<LivroAutor> lista = livroAutorDAO.listarTodos();
            if (lista.isEmpty()) {
                System.out.println("Nenhum vínculo cadastrado.");
            } else {
                System.out.println("\nTodos os vínculos Livro-Autor (" + lista.size() + "):");
                lista.forEach(System.out::println);
            }
        } catch (Exception e) {
            System.out.println("Erro ao listar vínculos.");
        }
    }

    // --- vincular múltiplos autores a um livro (N:N) ---

    private void vincularAutoresAoLivro() {
        System.out.println("\nVincular autores a um livro");

        System.out.print("\nID do Livro: ");
        int idLivro = lerInt();

        System.out.print("Quantos autores deseja vincular? ");
        int qtd = lerInt();

        if (qtd <= 0) {
            System.out.println("Quantidade inválida.");
            return;
        }

        int[] idsAutores = new int[qtd];
        for (int i = 0; i < qtd; i++) {
            System.out.print("ID do Autor " + (i + 1) + ": ");
            idsAutores[i] = lerInt();
        }

        try {
            int vinculados = livroAutorDAO.vincularAutoresAoLivro(idLivro, idsAutores);
            System.out.println(vinculados + " de " + qtd + " autor(es) vinculado(s) ao livro " + idLivro + ".");
        } catch (Exception e) {
            System.out.println("Erro ao vincular autores ao livro.");
        }
    }

    // --- vincular múltiplos livros a um autor (N:N) ---

    private void vincularLivrosAoAutor() {
        System.out.println("\nVincular livros a um autor");

        System.out.print("\nID do Autor: ");
        int idAutor = lerInt();

        System.out.print("Quantos livros deseja vincular? ");
        int qtd = lerInt();

        if (qtd <= 0) {
            System.out.println("Quantidade inválida.");
            return;
        }

        int[] idsLivros = new int[qtd];
        for (int i = 0; i < qtd; i++) {
            System.out.print("ID do Livro " + (i + 1) + ": ");
            idsLivros[i] = lerInt();
        }

        try {
            int vinculados = livroAutorDAO.vincularLivrosAoAutor(idAutor, idsLivros);
            System.out.println(vinculados + " de " + qtd + " livro(s) vinculado(s) ao autor " + idAutor + ".");
        } catch (Exception e) {
            System.out.println("Erro ao vincular livros ao autor.");
        }
    }

    // --- alterar vínculo por ID ---

    private void alterarVinculo() {
        System.out.print("\nID do vínculo a ser alterado: ");
        int id = lerInt();

        try {
            LivroAutor la = livroAutorDAO.buscarPorId(id);
            if (la == null) {
                System.out.println("Vínculo não encontrado.");
                return;
            }

            System.out.println("Vínculo atual:" + la);

            System.out.print("\nNovo ID do Livro (vazio para manter " + la.getIdLivro() + "): ");
            String entradaLivro = console.nextLine().trim();
            if (!entradaLivro.isEmpty()) la.setIdLivro(Integer.parseInt(entradaLivro));

            System.out.print("Novo ID do Autor (vazio para manter " + la.getIdAutor() + "): ");
            String entradaAutor = console.nextLine().trim();
            if (!entradaAutor.isEmpty()) la.setIdAutor(Integer.parseInt(entradaAutor));

            if (livroAutorDAO.alterarLivroAutor(la)) {
                System.out.println("Vínculo alterado com sucesso.");
            } else {
                System.out.println("Erro ao alterar vínculo.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Valor inválido informado.");
        } catch (Exception e) {
            System.out.println("Erro ao alterar vínculo.");
        }
    }

    // --- excluir vínculo por ID ---

    private void excluirPorId() {
        System.out.print("\nID do vínculo a ser excluído: ");
        int id = lerInt();

        try {
            LivroAutor la = livroAutorDAO.buscarPorId(id);
            if (la == null) {
                System.out.println("Vínculo não encontrado.");
                return;
            }

            System.out.println("Vínculo:" + la);
            System.out.print("Confirma exclusão? (S/N): ");
            char resp = console.nextLine().trim().charAt(0);

            if (resp == 'S' || resp == 's') {
                if (livroAutorDAO.excluirPorId(id)) {
                    System.out.println("Vínculo excluído com sucesso.");
                } else {
                    System.out.println("Erro ao excluir vínculo.");
                }
            }
        } catch (Exception e) {
            System.out.println("Erro ao excluir vínculo.");
        }
    }

    // --- excluir todos os autores de um livro ---

    private void excluirAutoresDoLivro() {
        System.out.print("\nID do Livro: ");
        int idLivro = lerInt();

        try {
            List<LivroAutor> lista = livroAutorDAO.buscarAutoresDoLivro(idLivro);
            if (lista.isEmpty()) {
                System.out.println("Nenhum autor vinculado ao livro " + idLivro + ".");
                return;
            }

            System.out.println("\nVínculos encontrados (" + lista.size() + "):");
            lista.forEach(System.out::println);

            System.out.print("\nConfirma exclusão de todos os vínculos? (S/N): ");
            char resp = console.nextLine().trim().charAt(0);

            if (resp == 'S' || resp == 's') {
                int removidos = livroAutorDAO.excluirAutoresDoLivro(idLivro);
                System.out.println(removidos + " vínculo(s) excluído(s).");
            }
        } catch (Exception e) {
            System.out.println("Erro ao excluir autores do livro.");
        }
    }

    // --- excluir todos os livros de um autor ---

    private void excluirLivrosDoAutor() {
        System.out.print("\nID do Autor: ");
        int idAutor = lerInt();

        try {
            List<LivroAutor> lista = livroAutorDAO.buscarLivrosDoAutor(idAutor);
            if (lista.isEmpty()) {
                System.out.println("Nenhum livro vinculado ao autor " + idAutor + ".");
                return;
            }

            System.out.println("\nVínculos encontrados (" + lista.size() + "):");
            lista.forEach(System.out::println);

            System.out.print("\nConfirma exclusão de todos os vínculos? (S/N): ");
            char resp = console.nextLine().trim().charAt(0);

            if (resp == 'S' || resp == 's') {
                int removidos = livroAutorDAO.excluirLivrosDoAutor(idAutor);
                System.out.println(removidos + " vínculo(s) excluído(s).");
            }
        } catch (Exception e) {
            System.out.println("Erro ao excluir livros do autor.");
        }
    }

    // --- utilitário ---
    
    private int lerInt() {
        try {
            return Integer.parseInt(console.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}