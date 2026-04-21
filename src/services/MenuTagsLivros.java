package src.services;

import java.util.List;
import java.util.Scanner;
import src.dao.TagLivroDAO;
import src.model.TagLivro;
import src.util.ArvoreBMais;
import src.util.Indexador;

public class MenuTagsLivros {

    private final TagLivroDAO tagLivroDAO;
    private final Scanner console = new Scanner(System.in);

    public MenuTagsLivros() throws Exception {
        Indexador indice = new ArvoreBMais("tags_livros");
        tagLivroDAO = new TagLivroDAO(indice);
    }

    public void menu() {
        int opcao;

        do {
            System.out.println("\n\nAEDsIII");
            System.out.println("-------");
            System.out.println("> Início > Tags-Livros");
            System.out.println("\n1 - Listar tags de um livro");
            System.out.println("2 - Listar livros de uma tag");
            System.out.println("3 - Listar todos os vínculos");
            System.out.println("4 - Vincular tags a um livro");
            System.out.println("5 - Vincular livros a uma tag");
            System.out.println("6 - Alterar vínculo por ID");
            System.out.println("7 - Excluir vínculo por ID");
            System.out.println("8 - Excluir todas as tags de um livro");
            System.out.println("9 - Excluir todos os livros de uma tag");
            System.out.println("0 - Voltar");
            System.out.print("\nOpção: ");

            opcao = lerInt();

            switch (opcao) {
                case 1: listarTagsDoLivro();      break;
                case 2: listarLivrosDaTag();      break;
                case 3: listarTodos();            break;
                case 4: vincularTagsAoLivro();    break;
                case 5: vincularLivrosATag();     break;
                case 6: alterarVinculo();         break;
                case 7: excluirPorId();           break;
                case 8: excluirTagsDoLivro();     break;
                case 9: excluirLivrosDaTag();     break;
                case 0: break;
                default:
                    System.out.println("Opção inválida!");
                    break;
            }
        } while (opcao != 0);
    }

    // --- listar tags de um livro (N:N: um livro tem muitas tags) ---

    private void listarTagsDoLivro() {
        System.out.print("\nID do Livro: ");
        int idLivro = lerInt();

        try {
            List<TagLivro> lista = tagLivroDAO.buscarTagsDoLivro(idLivro);
            if (lista.isEmpty()) {
                System.out.println("Nenhuma tag vinculada ao livro " + idLivro + ".");
            } else {
                System.out.println("\nTags do livro " + idLivro + " (" + lista.size() + " tag(s)):");
                lista.forEach(System.out::println);
            }
        } catch (Exception e) {
            System.out.println("Erro ao buscar tags do livro.");
        }
    }

    // --- listar livros de uma tag (N:N: uma tag pertence a muitos livros) ---

    private void listarLivrosDaTag() {
        System.out.print("\nID da Tag: ");
        int idTag = lerInt();

        try {
            List<TagLivro> lista = tagLivroDAO.buscarLivrosDaTag(idTag);
            if (lista.isEmpty()) {
                System.out.println("Nenhum livro vinculado à tag " + idTag + ".");
            } else {
                System.out.println("\nLivros da tag " + idTag + " (" + lista.size() + " livro(s)):");
                lista.forEach(System.out::println);
            }
        } catch (Exception e) {
            System.out.println("Erro ao buscar livros da tag.");
        }
    }

    // --- listar todos ---

    private void listarTodos() {
        try {
            List<TagLivro> lista = tagLivroDAO.listarTodos();
            if (lista.isEmpty()) {
                System.out.println("Nenhum vínculo cadastrado.");
            } else {
                System.out.println("\nTodos os vínculos Tag-Livro (" + lista.size() + "):");
                lista.forEach(System.out::println);
            }
        } catch (Exception e) {
            System.out.println("Erro ao listar vínculos.");
        }
    }

    // --- vincular múltiplas tags a um livro (N:N) ---

    private void vincularTagsAoLivro() {
        System.out.println("\nVincular tags a um livro");

        System.out.print("\nID do Livro: ");
        int idLivro = lerInt();

        System.out.print("Quantas tags deseja vincular? ");
        int qtd = lerInt();

        if (qtd <= 0) {
            System.out.println("Quantidade inválida.");
            return;
        }

        int[] idsTags = new int[qtd];
        for (int i = 0; i < qtd; i++) {
            System.out.print("ID da Tag " + (i + 1) + ": ");
            idsTags[i] = lerInt();
        }

        try {
            int vinculados = tagLivroDAO.vincularTagsAoLivro(idLivro, idsTags);
            System.out.println(vinculados + " de " + qtd + " tag(s) vinculada(s) ao livro " + idLivro + ".");
        } catch (Exception e) {
            System.out.println("Erro ao vincular tags ao livro.");
        }
    }

    // --- vincular múltiplos livros a uma tag (N:N) ---

    private void vincularLivrosATag() {
        System.out.println("\nVincular livros a uma tag");

        System.out.print("\nID da Tag: ");
        int idTag = lerInt();

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
            int vinculados = tagLivroDAO.vincularLivrosATag(idTag, idsLivros);
            System.out.println(vinculados + " de " + qtd + " livro(s) vinculado(s) à tag " + idTag + ".");
        } catch (Exception e) {
            System.out.println("Erro ao vincular livros à tag.");
        }
    }

    // --- alterar vínculo por ID ---

    private void alterarVinculo() {
        System.out.print("\nID do vínculo a ser alterado: ");
        int id = lerInt();

        try {
            TagLivro tl = tagLivroDAO.buscarPorId(id);
            if (tl == null) {
                System.out.println("Vínculo não encontrado.");
                return;
            }

            System.out.println("Vínculo atual:" + tl);

            System.out.print("\nNovo ID do Livro (vazio para manter " + tl.getIdLivro() + "): ");
            String entradaLivro = console.nextLine().trim();
            if (!entradaLivro.isEmpty()) tl.setIdLivro(Integer.parseInt(entradaLivro));

            System.out.print("Novo ID da Tag (vazio para manter " + tl.getIdTag() + "): ");
            String entradaTag = console.nextLine().trim();
            if (!entradaTag.isEmpty()) tl.setIdTag(Integer.parseInt(entradaTag));

            if (tagLivroDAO.alterarTagLivro(tl)) {
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
            TagLivro tl = tagLivroDAO.buscarPorId(id);
            if (tl == null) {
                System.out.println("Vínculo não encontrado.");
                return;
            }

            System.out.println("Vínculo:" + tl);
            System.out.print("Confirma exclusão? (S/N): ");
            char resp = console.nextLine().trim().charAt(0);

            if (resp == 'S' || resp == 's') {
                if (tagLivroDAO.excluirPorId(id)) {
                    System.out.println("Vínculo excluído com sucesso.");
                } else {
                    System.out.println("Erro ao excluir vínculo.");
                }
            }
        } catch (Exception e) {
            System.out.println("Erro ao excluir vínculo.");
        }
    }

    // --- excluir todas as tags de um livro ---

    private void excluirTagsDoLivro() {
        System.out.print("\nID do Livro: ");
        int idLivro = lerInt();

        try {
            List<TagLivro> lista = tagLivroDAO.buscarTagsDoLivro(idLivro);
            if (lista.isEmpty()) {
                System.out.println("Nenhuma tag vinculada ao livro " + idLivro + ".");
                return;
            }

            System.out.println("\nVínculos encontrados (" + lista.size() + "):");
            lista.forEach(System.out::println);

            System.out.print("\nConfirma exclusão de todos os vínculos? (S/N): ");
            char resp = console.nextLine().trim().charAt(0);

            if (resp == 'S' || resp == 's') {
                int removidos = tagLivroDAO.excluirTagsDoLivro(idLivro);
                System.out.println(removidos + " vínculo(s) excluído(s).");
            }
        } catch (Exception e) {
            System.out.println("Erro ao excluir tags do livro.");
        }
    }

    // --- excluir todos os livros de uma tag ---

    private void excluirLivrosDaTag() {
        System.out.print("\nID da Tag: ");
        int idTag = lerInt();

        try {
            List<TagLivro> lista = tagLivroDAO.buscarLivrosDaTag(idTag);
            if (lista.isEmpty()) {
                System.out.println("Nenhum livro vinculado à tag " + idTag + ".");
                return;
            }

            System.out.println("\nVínculos encontrados (" + lista.size() + "):");
            lista.forEach(System.out::println);

            System.out.print("\nConfirma exclusão de todos os vínculos? (S/N): ");
            char resp = console.nextLine().trim().charAt(0);

            if (resp == 'S' || resp == 's') {
                int removidos = tagLivroDAO.excluirLivrosDaTag(idTag);
                System.out.println(removidos + " vínculo(s) excluído(s).");
            }
        } catch (Exception e) {
            System.out.println("Erro ao excluir livros da tag.");
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