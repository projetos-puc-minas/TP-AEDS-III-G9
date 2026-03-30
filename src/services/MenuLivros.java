package src.services;

import java.util.ArrayList;
import java.util.Scanner;

import src.dao.LivroDAO;
import src.dao.TagLivroDAO;
import src.dao.LivroAutorDAO;
import src.model.Livro;
import src.model.TagLivro;
import src.model.LivroAutor;

public class MenuLivros {
    private LivroDAO livroDAO;
    private TagLivroDAO tagLivroDAO;
    private LivroAutorDAO livroAutorDAO;
    private Scanner console = new Scanner(System.in);

    public MenuLivros() throws Exception {
        livroDAO      = new LivroDAO();
        tagLivroDAO   = new TagLivroDAO();
        livroAutorDAO = new LivroAutorDAO();
    }

    public void menu() {
        int opcao;
        do {
            System.out.println("\n\nAEDsIII");
            System.out.println("-------");
            System.out.println("> Início > Livros");
            System.out.println("\n1 - Buscar livro por ID");
            System.out.println("2 - Incluir livro");
            System.out.println("3 - Alterar livro");
            System.out.println("4 - Excluir livro");
            System.out.println("5 - Listar livros em ordem alfabética");
            System.out.println("6 - Gerenciar tags do livro");
            System.out.println("7 - Gerenciar autores do livro");
            System.out.println("0 - Voltar");
            System.out.print("\nOpção: ");
            try {
                opcao = Integer.valueOf(console.nextLine());
            } catch (NumberFormatException e) {
                opcao = -1;
            }

            switch (opcao) {
                case 1: buscarLivroId();    break;
                case 2: incluirLivro();     break;
                case 3: alterarLivro();     break;
                case 4: excluirLivro();     break;
                case 5: listarOrdenados();  break;
                case 6: menuTags();         break;
                case 7: menuAutores();      break;
                case 0:                     break;
                default: System.out.println("Opção inválida!"); break;
            }
        } while (opcao != 0);
    }

    // -------------------------------------------------------
    // BUSCAR POR ID
    // -------------------------------------------------------

    private void buscarLivroId() {
        System.out.print("\nID do livro: ");
        int id;
        try {
            id = Integer.parseInt(console.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("ID inválido.");
            return;
        }

        try {
            Livro livro = livroDAO.buscarLivroId(id);
            if (livro != null && livro.getLapide()) {
                System.out.println(livro);

                // Mostra tags
                ArrayList<TagLivro> tags = tagLivroDAO.buscarTagsPorLivro(id);
                if (!tags.isEmpty()) {
                    System.out.print("Tags.............: ");
                    for (int i = 0; i < tags.size(); i++) {
                        System.out.print(tags.get(i).getTag());
                        if (i < tags.size() - 1) System.out.print(", ");
                    }
                    System.out.println();
                } else {
                    System.out.println("Tags.............: (nenhuma)");
                }

                // Mostra IDs dos autores vinculados
                ArrayList<LivroAutor> autores = livroAutorDAO.buscarPorLivro(id);
                if (!autores.isEmpty()) {
                    System.out.print("IDs Autores......: ");
                    for (int i = 0; i < autores.size(); i++) {
                        System.out.print(autores.get(i).getIdAutor());
                        if (i < autores.size() - 1) System.out.print(", ");
                    }
                    System.out.println();
                } else {
                    System.out.println("IDs Autores......: (nenhum)");
                }

            } else {
                System.out.println("Livro não encontrado.");
            }
        } catch (Exception e) {
            System.out.println("Erro ao buscar livro.");
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------
    // INCLUIR
    // -------------------------------------------------------

    private void incluirLivro() {
        System.out.println("\nInclusão de Livro");

        System.out.print("\nID da Editora: ");
        int id_editora;
        try {
            id_editora = Integer.parseInt(console.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("ID inválido.");
            return;
        }

        System.out.print("Título: ");
        String titulo = console.nextLine();

        System.out.print("ISBN (13 caracteres): ");
        String isbnStr = console.nextLine();
        char[] isbn = String.format("%-13s", isbnStr).substring(0, 13).toCharArray();

        System.out.print("Ano de Publicação: ");
        int ano;
        try {
            ano = Integer.parseInt(console.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Ano inválido.");
            return;
        }

        System.out.print("Preço: ");
        double preco;
        try {
            preco = Double.parseDouble(console.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Preço inválido.");
            return;
        }

        System.out.print("Sinopse: ");
        String sinopse = console.nextLine();

        try {
            Livro livro = new Livro(id_editora, titulo, isbn, ano, preco, sinopse);
            if (livroDAO.incluirLivro(livro)) {
                System.out.println("Livro incluído com sucesso.");
            } else {
                System.out.println("Erro ao incluir livro.");
            }
        } catch (Exception e) {
            System.out.println("Erro ao incluir livro.");
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------
    // ALTERAR
    // -------------------------------------------------------

    public void alterarLivro() {
        System.out.print("\nID do livro a ser alterado: ");
        int id;
        try {
            id = Integer.parseInt(console.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("ID inválido.");
            return;
        }

        try {
            Livro livro = livroDAO.buscarLivroId(id);
            if (livro == null || !livro.getLapide()) {
                System.out.println("Livro não encontrado.");
                return;
            }

            System.out.println("\nDados atuais:");
            System.out.println(livro);

            System.out.print("\nNovo Título (Enter para manter): ");
            String titulo = console.nextLine();
            if (!titulo.isEmpty()) livro.setTitulo(titulo);

            // CORREÇÃO: permite alterar o ISBN e valida se a nova editora existe
            System.out.print("Novo ISBN (13 caracteres, Enter para manter): ");
            String isbnStr = console.nextLine();
            if (!isbnStr.isEmpty()) {
                char[] novoIsbn = String.format("%-13s", isbnStr).substring(0, 13).toCharArray();
                livro.setIsbn(novoIsbn);
            }

            System.out.print("Novo ID da Editora (Enter para manter): ");
            String idEditoraStr = console.nextLine();
            if (!idEditoraStr.isEmpty()) {
                try {
                    int novoIdEditora = Integer.parseInt(idEditoraStr);
                    // CORREÇÃO: valida se a editora realmente existe antes de salvar
                    src.model.Editora editora = new src.dao.EditoraDAO().buscarEditoraId(novoIdEditora);
                    if (editora == null || !editora.getLapide()) {
                        System.out.println("Editora com ID " + novoIdEditora + " não encontrada. Mantendo a atual.");
                    } else {
                        livro.setIdEditora(novoIdEditora);
                    }
                } catch (NumberFormatException e) {
                    System.out.println("ID inválido, mantendo o atual.");
                }
            }

            System.out.print("Novo Ano de Publicação (Enter para manter): ");
            String anoStr = console.nextLine();
            if (!anoStr.isEmpty()) {
                try {
                    livro.setAnoPublicacao(Integer.parseInt(anoStr));
                } catch (NumberFormatException e) {
                    System.out.println("Ano inválido, mantendo o atual.");
                }
            }

            System.out.print("Novo Preço (Enter para manter): ");
            String precoStr = console.nextLine();
            if (!precoStr.isEmpty()) {
                try {
                    livro.setPreco(Double.parseDouble(precoStr));
                } catch (NumberFormatException e) {
                    System.out.println("Preço inválido, mantendo o atual.");
                }
            }

            System.out.print("Nova Sinopse (Enter para manter): ");
            String sinopse = console.nextLine();
            if (!sinopse.isEmpty()) livro.setSinopse(sinopse);

            if (livroDAO.alterarLivro(livro)) {
                System.out.println("Livro alterado com sucesso.");
            } else {
                System.out.println("Erro ao alterar livro.");
            }
        } catch (Exception e) {
            System.out.println("Erro ao alterar livro.");
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------
    // EXCLUIR
    // Remove tags e vínculos de autores antes de excluir o livro
    // -------------------------------------------------------

    private void excluirLivro() {
        System.out.print("\nID do livro a ser excluído: ");
        int id;
        try {
            id = Integer.parseInt(console.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("ID inválido.");
            return;
        }

        try {
            Livro livro = livroDAO.buscarLivroId(id);
            if (livro == null || !livro.getLapide()) {
                System.out.println("Livro não encontrado.");
                return;
            }

            System.out.println("\nLivro encontrado: " + livro.getTitulo());
            System.out.print("Confirma exclusão? (S/N): ");
            String resp = console.nextLine();

            if (resp.equalsIgnoreCase("S")) {
                // Integridade referencial: remove tags e vínculos com autores
                tagLivroDAO.excluirTagsPorLivro(id);
                livroAutorDAO.excluirPorLivro(id);

                if (livroDAO.excluirLivro(id)) {
                    System.out.println("Livro, suas tags e seus autores excluídos com sucesso.");
                } else {
                    System.out.println("Erro ao excluir livro.");
                }
            }
        } catch (Exception e) {
            System.out.println("Erro ao excluir livro.");
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------
    // LISTAR ORDENADO POR TÍTULO (Árvore B+)
    // -------------------------------------------------------

    private void listarOrdenados() {
        try {
            ArrayList<Livro> livros = livroDAO.listarLivrosOrdenadosPorTitulo();
            if (livros.isEmpty()) {
                System.out.println("\nNenhum livro cadastrado.");
                return;
            }
            System.out.println("\n--- Livros em ordem alfabética ---");
            for (Livro l : livros) {
                System.out.println(l);
                System.out.println("---------------------------------");
            }
        } catch (Exception e) {
            System.out.println("Erro ao listar livros.");
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------
    // SUBMENU DE TAGS
    // -------------------------------------------------------

    private void menuTags() {
        System.out.print("\nID do livro para gerenciar tags: ");
        int idLivro;
        try {
            idLivro = Integer.parseInt(console.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("ID inválido.");
            return;
        }

        try {
            Livro livro = livroDAO.buscarLivroId(idLivro);
            if (livro == null || !livro.getLapide()) {
                System.out.println("Livro não encontrado.");
                return;
            }

            int opcao;
            do {
                System.out.println("\n> Tags do livro: " + livro.getTitulo());
                System.out.println("\n1 - Listar tags");
                System.out.println("2 - Adicionar tag");
                System.out.println("3 - Excluir tag");
                System.out.println("0 - Voltar");
                System.out.print("\nOpção: ");
                try {
                    opcao = Integer.parseInt(console.nextLine());
                } catch (NumberFormatException e) {
                    opcao = -1;
                }

                switch (opcao) {
                    case 1: listarTagsDoLivro(idLivro); break;
                    case 2: adicionarTag(idLivro);      break;
                    case 3: excluirTag(idLivro);        break;
                    case 0:                             break;
                    default: System.out.println("Opção inválida!"); break;
                }
            } while (opcao != 0);

        } catch (Exception e) {
            System.out.println("Erro ao acessar tags.");
            e.printStackTrace();
        }
    }

    private void listarTagsDoLivro(int idLivro) {
        try {
            ArrayList<TagLivro> tags = tagLivroDAO.buscarTagsPorLivro(idLivro);
            if (tags.isEmpty()) {
                System.out.println("Nenhuma tag cadastrada para este livro.");
            } else {
                System.out.println("\nTags cadastradas:");
                for (TagLivro t : tags) {
                    System.out.println("  [ID " + t.getId() + "] " + t.getTag());
                }
            }
        } catch (Exception e) {
            System.out.println("Erro ao listar tags.");
            e.printStackTrace();
        }
    }

    private void adicionarTag(int idLivro) {
        System.out.print("Nova tag: ");
        String tag = console.nextLine();
        if (tag.isEmpty()) {
            System.out.println("Tag não pode ser vazia.");
            return;
        }
        try {
            TagLivro novaTag = new TagLivro(idLivro, tag);
            if (tagLivroDAO.incluirTag(novaTag)) {
                System.out.println("Tag adicionada com sucesso.");
            } else {
                System.out.println("Erro ao adicionar tag.");
            }
        } catch (Exception e) {
            System.out.println("Erro ao adicionar tag.");
            e.printStackTrace();
        }
    }

    private void excluirTag(int idLivro) {
        listarTagsDoLivro(idLivro);
        System.out.print("\nID da tag a excluir: ");
        int idTag;
        try {
            idTag = Integer.parseInt(console.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("ID inválido.");
            return;
        }
        try {
            TagLivro tag = tagLivroDAO.buscarTagId(idTag);
            if (tag == null || !tag.getLapide() || tag.getIdLivro() != idLivro) {
                System.out.println("Tag não encontrada para este livro.");
                return;
            }
            if (tagLivroDAO.excluirTag(idTag)) {
                System.out.println("Tag excluída com sucesso.");
            } else {
                System.out.println("Erro ao excluir tag.");
            }
        } catch (Exception e) {
            System.out.println("Erro ao excluir tag.");
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------
    // SUBMENU DE AUTORES DO LIVRO (N:N — Fase 3)
    // -------------------------------------------------------

    private void menuAutores() {
        System.out.print("\nID do livro para gerenciar autores: ");
        int idLivro;
        try {
            idLivro = Integer.parseInt(console.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("ID inválido.");
            return;
        }

        try {
            Livro livro = livroDAO.buscarLivroId(idLivro);
            if (livro == null || !livro.getLapide()) {
                System.out.println("Livro não encontrado.");
                return;
            }

            int opcao;
            do {
                System.out.println("\n> Autores do livro: " + livro.getTitulo());
                System.out.println("\n1 - Listar autores vinculados");
                System.out.println("2 - Vincular autor");
                System.out.println("3 - Desvincular autor");
                System.out.println("0 - Voltar");
                System.out.print("\nOpção: ");
                try {
                    opcao = Integer.parseInt(console.nextLine());
                } catch (NumberFormatException e) {
                    opcao = -1;
                }

                switch (opcao) {
                    case 1: listarAutoresDoLivro(idLivro); break;
                    case 2: vincularAutor(idLivro);        break;
                    case 3: desvincularAutor(idLivro);     break;
                    case 0:                                break;
                    default: System.out.println("Opção inválida!"); break;
                }
            } while (opcao != 0);

        } catch (Exception e) {
            System.out.println("Erro ao acessar autores.");
            e.printStackTrace();
        }
    }

    private void listarAutoresDoLivro(int idLivro) {
        try {
            ArrayList<LivroAutor> relacoes = livroAutorDAO.buscarPorLivro(idLivro);
            if (relacoes.isEmpty()) {
                System.out.println("Nenhum autor vinculado a este livro.");
            } else {
                System.out.println("\nAutores vinculados:");
                for (LivroAutor la : relacoes) {
                    System.out.println("  [Vínculo ID " + la.getId() + "] ID Autor: " + la.getIdAutor());
                }
            }
        } catch (Exception e) {
            System.out.println("Erro ao listar autores.");
            e.printStackTrace();
        }
    }

    private void vincularAutor(int idLivro) {
        System.out.print("ID do autor a vincular: ");
        int idAutor;
        try {
            idAutor = Integer.parseInt(console.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("ID inválido.");
            return;
        }

        try {
            if (livroAutorDAO.existeRelacao(idLivro, idAutor)) {
                System.out.println("Este autor já está vinculado a este livro.");
                return;
            }
            LivroAutor novaRelacao = new LivroAutor(idLivro, idAutor);
            if (livroAutorDAO.incluir(novaRelacao)) {
                System.out.println("Autor vinculado com sucesso.");
            } else {
                System.out.println("Erro ao vincular autor.");
            }
        } catch (Exception e) {
            System.out.println("Erro ao vincular autor.");
            e.printStackTrace();
        }
    }

    private void desvincularAutor(int idLivro) {
        listarAutoresDoLivro(idLivro);
        System.out.print("\nID do vínculo a excluir: ");
        int idVinculo;
        try {
            idVinculo = Integer.parseInt(console.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("ID inválido.");
            return;
        }
        try {
            LivroAutor la = livroAutorDAO.buscarPorId(idVinculo);
            if (la == null || !la.getLapide() || la.getIdLivro() != idLivro) {
                System.out.println("Vínculo não encontrado para este livro.");
                return;
            }
            if (livroAutorDAO.excluir(idVinculo)) {
                System.out.println("Autor desvinculado com sucesso.");
            } else {
                System.out.println("Erro ao desvincular autor.");
            }
        } catch (Exception e) {
            System.out.println("Erro ao desvincular autor.");
            e.printStackTrace();
        }
    }
}