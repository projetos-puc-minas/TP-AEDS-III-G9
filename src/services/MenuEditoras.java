package src.services;

import java.util.ArrayList;
import java.util.Scanner;

import src.dao.EditoraDAO;
import src.dao.LivroDAO;
import src.model.Editora;
import src.model.Livro;

public class MenuEditoras {
    private EditoraDAO editoraDAO;
    private LivroDAO livroDAO;
    private Scanner console = new Scanner(System.in);

    public MenuEditoras() throws Exception {
        editoraDAO = new EditoraDAO();
        livroDAO = new LivroDAO();
    }

    public void menu() {
        int opcao;

        do {
            System.out.println("\n\nAEDsIII");
            System.out.println("-------");
            System.out.println("> Início > Editoras");
            System.out.println("\n1 - Buscar editora por ID");
            System.out.println("2 - Incluir editora");
            System.out.println("3 - Alterar editora");
            System.out.println("4 - Excluir editora");
            System.out.println("5 - Listar todas as editoras");
            System.out.println("6 - Listar livros de uma editora");
            System.out.println("0 - Voltar");
            System.out.print("\nOpção: ");
            try {
                opcao = Integer.valueOf(console.nextLine());
            } catch (NumberFormatException e) {
                opcao = -1;
            }

            switch (opcao) {
                case 1: buscarEditoraId();           break;
                case 2: incluirEditora();            break;
                case 3: alterarEditora();            break;
                case 4: excluirEditora();            break;
                case 5: listarTodasEditoras();       break;
                case 6: listarLivrosDaEditora();     break;
                case 0:                              break;
                default: System.out.println("Opção inválida!"); break;
            }
        } while (opcao != 0);
    }

    // -------------------------------------------------------
    // BUSCAR POR ID
    // -------------------------------------------------------

    private void buscarEditoraId() {
        // CORREÇÃO: mensagem correta (era "ID do cliente")
        System.out.print("\nID da editora: ");
        int id;
        try {
            id = Integer.parseInt(console.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("ID inválido.");
            return;
        }

        try {
            Editora editora = editoraDAO.buscarEditoraId(id);
            if (editora != null && editora.getLapide()) {
                System.out.println(editora);
            } else {
                System.out.println("Editora não encontrada.");
            }
        } catch (Exception e) {
            System.out.println("Erro ao buscar editora.");
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------
    // INCLUIR
    // -------------------------------------------------------

    private void incluirEditora() {
        System.out.println("\nInclusão de editora");

        System.out.print("\nNome: ");
        String nome = console.nextLine();

        System.out.print("Cidade sede: ");
        String cidade = console.nextLine();

        System.out.print("Ano de Fundação: ");
        int ano_fundacao;
        try {
            ano_fundacao = Integer.parseInt(console.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Ano inválido.");
            return;
        }

        try {
            Editora editora = new Editora(nome, cidade, ano_fundacao);
            if (editoraDAO.incluirEditora(editora)) {
                System.out.println("Editora incluída com sucesso.");
            } else {
                System.out.println("Erro ao incluir editora.");
            }
        } catch (Exception e) {
            System.out.println("Erro ao incluir editora.");
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------
    // ALTERAR
    // -------------------------------------------------------

    public void alterarEditora() {
        System.out.print("\nID da editora a ser alterada: ");
        int id;
        try {
            id = Integer.parseInt(console.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("ID inválido.");
            return;
        }

        try {
            Editora editora = editoraDAO.buscarEditoraId(id);
            if (editora == null || !editora.getLapide()) {
                System.out.println("Editora não encontrada.");
                return;
            }

            System.out.println("\nDados atuais:");
            System.out.println(editora);

            System.out.print("\nNovo nome (Enter para manter): ");
            String nome = console.nextLine();
            if (!nome.isEmpty()) editora.setNome(nome);

            System.out.print("Nova cidade (Enter para manter): ");
            String cidade = console.nextLine();
            if (!cidade.isEmpty()) editora.setCidade(cidade);

            System.out.print("Novo ano de fundação (Enter para manter): ");
            String anoStr = console.nextLine();
            if (!anoStr.isEmpty()) {
                try {
                    editora.setAnoFundacao(Integer.parseInt(anoStr));
                } catch (NumberFormatException e) {
                    System.out.println("Ano inválido, mantendo o atual.");
                }
            }

            if (editoraDAO.alterarEditora(editora)) {
                System.out.println("Editora alterada com sucesso.");
            } else {
                System.out.println("Erro ao alterar editora.");
            }
        } catch (Exception e) {
            System.out.println("Erro ao alterar editora.");
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------
    // EXCLUIR
    // -------------------------------------------------------

    private void excluirEditora() {
        // CORREÇÃO: mensagem correta (era "ID do cliente a ser excluído")
        System.out.print("\nID da editora a ser excluída: ");
        int id;
        try {
            id = Integer.parseInt(console.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("ID inválido.");
            return;
        }

        try {
            Editora editora = editoraDAO.buscarEditoraId(id);
            if (editora == null || !editora.getLapide()) {
                System.out.println("Editora não encontrada.");
                return;
            }

            // Avisa se houver livros vinculados
            ArrayList<Livro> livrosVinculados = livroDAO.buscarLivrosPorEditora(id);
            if (!livrosVinculados.isEmpty()) {
                System.out.println("\nAtenção: esta editora possui " +
                    livrosVinculados.size() + " livro(s) cadastrado(s).");
                System.out.println("Exclua ou transfira os livros antes de excluir a editora.");
                return;
            }

            System.out.println("\nEditora encontrada: " + editora.getNome());
            System.out.print("Confirma exclusão? (S/N): ");
            String resp = console.nextLine();

            if (resp.equalsIgnoreCase("S")) {
                if (editoraDAO.excluirEditora(id)) {
                    System.out.println("Editora excluída com sucesso.");
                } else {
                    System.out.println("Erro ao excluir editora.");
                }
            }
        } catch (Exception e) {
            System.out.println("Erro ao excluir editora.");
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------
    // LISTAR TODAS
    // -------------------------------------------------------

    private void listarTodasEditoras() {
        try {
            ArrayList<Editora> editoras = editoraDAO.listarTodasEditoras();
            if (editoras.isEmpty()) {
                System.out.println("\nNenhuma editora cadastrada.");
                return;
            }
            System.out.println("\n--- Editoras cadastradas ---");
            for (Editora e : editoras) {
                System.out.println(e);
                System.out.println("----------------------------");
            }
        } catch (Exception e) {
            System.out.println("Erro ao listar editoras.");
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------
    // LISTAR LIVROS DE UMA EDITORA — demonstra o relacionamento 1:N
    // -------------------------------------------------------

    private void listarLivrosDaEditora() {
        System.out.print("\nID da editora: ");
        int id;
        try {
            id = Integer.parseInt(console.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("ID inválido.");
            return;
        }

        try {
            Editora editora = editoraDAO.buscarEditoraId(id);
            if (editora == null || !editora.getLapide()) {
                System.out.println("Editora não encontrada.");
                return;
            }

            ArrayList<Livro> livros = livroDAO.buscarLivrosPorEditora(id);
            if (livros.isEmpty()) {
                System.out.println("\nNenhum livro cadastrado para a editora: " + editora.getNome());
                return;
            }

            System.out.println("\n--- Livros da editora: " + editora.getNome() + " ---");
            for (Livro l : livros) {
                System.out.println(l);
                System.out.println("------------------------------------------");
            }
        } catch (Exception e) {
            System.out.println("Erro ao listar livros da editora.");
            e.printStackTrace();
        }
    }
}