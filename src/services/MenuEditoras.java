package src.services;

import java.util.Scanner;

import src.dao.EditoraDAO;
import src.model.Editora;

public class MenuEditoras {
    private EditoraDAO editoraDAO;
    private Scanner console = new Scanner(System.in);

    public MenuEditoras() throws Exception{
        editoraDAO = new EditoraDAO();
    }

    public void menu(){
        int opcao;

        do {
            System.out.println("\n\nAEDsIII");
            System.out.println("-------");
            System.out.println("> Início > Editoras");
            System.out.println("\n1 - Buscar");
            System.out.println("2 - Incluir");
            System.out.println("3 - Alterar");
            System.out.println("4 - Excluir");
            System.out.println("0 - Voltar");
            System.out.print("\nOpção: ");
            try {
                opcao = Integer.valueOf(console.nextLine());
            } catch(NumberFormatException e) {
                opcao = -1;
            }

            switch (opcao) {
                case 1:
                    buscarEditoraId();
                    break;
                case 2:
                    incluirEditora();
                    break;
                case 3:
                    alterarEditora();
                    break;
                case 4:
                    excluirEditora();
                    break;
                case 0:
                    break;
                default:
                    System.out.println("Opção inválida!");
                    break;
            }
        } while (opcao != 0);
    }

    private void buscarEditoraId(){
        System.out.print("\nID do cliente: ");
        int id = console.nextInt();
        console.nextLine();

        try {
            Editora editora = editoraDAO.buscarEditoraId(id);

            if (editora != null) {
                System.out.println(editora);
            } else {
                System.out.println("Editora não encontrada.");
            }
        } catch (Exception e) {
            System.out.println("Erro ao buscar editora.");
        }
    }

    private void incluirEditora() {
        System.out.println("\nInclusão de editora");

        System.out.print("\nNome: ");
        String nome = console.nextLine();

        System.out.print("Cidade sede: ");
        String cidade = console.nextLine();

        System.out.print("Ano de Fundação: ");
        int ano_fundacao = console.nextInt();
        console.nextLine();

        try {
            Editora editora = new Editora(nome, cidade, ano_fundacao);

            if (editoraDAO.incluirEditora(editora)) {
                System.out.println("Editora incluída com sucesso.");
            } else {
                System.out.println("Erro ao incluir editora.");
            }
        } catch (Exception e) {
            System.out.println("Erro ao incluir ediora.");
        }
    }

    public void alterarEditora(){
        System.out.print("\nID da editora a ser alterada: ");

        int id = console.nextInt();
        console.nextLine();

        try {
            Editora editora = editoraDAO.buscarEditoraId(id);
            if (editora == null) {
                System.out.println("Editora não encontrada.");
                return;
            }

            System.out.print("\nNovo nome (vazio para manter): ");
            String nome = console.nextLine();
            if (!nome.isEmpty()) editora.setNome(nome);

            System.out.print("Nova cidade (vazio para manter): ");
            String cidade = console.nextLine();
            if (!cidade.isEmpty()) editora.setCidade(cidade);

            System.out.print("Novo ano de fundação (vazio para manter): ");
            String anoFundacaoStr = console.nextLine();
            if (!anoFundacaoStr.isEmpty())
            editora.setAnoFundacao(Integer.parseInt(anoFundacaoStr));

            if (editoraDAO.alterarEditora(editora)) {
                System.out.println("Editora alterada com sucesso.");
            } else {
                System.out.println("Erro ao alterar editora.");
            }
        } catch (Exception e) {
            System.out.println("Erro ao alterar editora.");
        }
    }

    private void excluirEditora() {
        System.out.print("\nID do cliente a ser excluído: ");
        int id = console.nextInt();
        console.nextLine();

        try {
            Editora editora = editoraDAO.buscarEditoraId(id);
            if (editora == null) {
                System.out.println("Editora não encontrada.");
                return;
            }
        
            System.out.print("Confirma exclusão? (S/N): ");
            char resp = console.next().charAt(0);
            if (resp == 'S' || resp == 's') {
                if (editoraDAO.excluirEditora(id)) {
                    System.out.println("Editora excluída com sucesso.");
                } else {
                    System.out.println("Erro ao excluir editora.");
                }
            }
        } catch (Exception e) {
            System.out.println("Erro ao excluir editora.");
        }
    }
}
