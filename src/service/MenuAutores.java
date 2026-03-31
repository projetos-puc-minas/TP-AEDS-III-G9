package src.service;

import java.util.Scanner;
import src.dao.AutoresDAO;
import src.model.Autores;
import src.util.DataUtil;

public class MenuAutores {
    private Scanner sc = new Scanner(System.in);

    private AutoresDAO autoresDAO;

    public MenuAutores() throws Exception {
        autoresDAO = new AutoresDAO();
    }

    public void menu() {
        int opcao;

        do {
            System.out.println("\n\nAEDs III");
            System.out.println("-={x}=-");
            System.out.println("> Início > Autores");
            System.out.println("\n1- Buscar");
            System.out.println("2 - Adicionar");
            System.out.println("3 - Alterar");
            System.out.println("4 - Excluir");
            System.out.println("0 - Voltar");
            System.out.println("\nOpcao: ");

            try {
                opcao = Integer.valueOf(sc.nextLine());
            } catch (NumberFormatException e) {
                opcao = -1;
            }

            switch (opcao) {
                case 1 -> buscarAutorID();

                case 2 -> adicionarAutor();

                case 3 -> alterarAutor();

                case 4 -> excluirAutor();

                case 0 -> System.out.println("Voltando...");

                default -> System.out.println("Opção Inválida!");
            }
        } while (opcao != 0);
    }

    private void buscarAutorID() {
        System.out.println("\nID do Autor: ");
        int id = sc.nextInt();
        sc.nextLine();

        try {
            Autores autor = autoresDAO.buscarAutor(id);

            if (autor != null) {
                System.out.println("\n--- Dados do Autor ---");
                System.out.println("ID: " + autor.getId());
                System.out.println("Nome: " + autor.getNome());
                System.out.println("Data de nascimento: " + DataUtil.timestampToString(autor.getDataNascimento()));
                System.out.println("Biografia: " + autor.getBiografia());
            } else {
                System.out.println("\nAutor não encontrado.");
            }
        } catch (Exception e) {
            System.out.println("Erro ao buscar autor.");
        }
    }

    private void adicionarAutor() {
        System.out.println("\nAdicionar autor");

        System.out.println("Nome: ");
        String nome = sc.nextLine();

        System.out.println("Data de nascimento [dd/mm/aaaa]: ");
        String dataNascimentoStr = sc.nextLine();

        System.out.println("Biografia: ");
        String biografia = sc.nextLine();

        try {
            long dataNascimento = DataUtil.stringToTimestamp(dataNascimentoStr);
            Autores autor = new Autores(-1, nome, dataNascimento, biografia);
            
            int idGerado = autoresDAO.adicionarAutor(autor);
            
            if(idGerado > 0) {
                System.out.println("\nAutor adicionado com sucesso! ID: " + idGerado);
            } else {
                System.out.println("\nErro ao incluir autor.");
            }
        } catch (Exception e) {
            System.out.println("Erro ao adicionar autor.");
        }
    }

    private void alterarAutor() {
        System.out.println("\nAlterar autor");

        System.out.println("ID do Autor: ");
        int id = sc.nextInt();
        sc.nextLine();

        try {
            Autores autor = autoresDAO.buscarAutor(id);

            if (autor != null) {
                System.out.println("\nDados atuais do autor:");
                System.out.println("Nome: " + autor.getNome());
                System.out.println("Data de nascimento: " + DataUtil.timestampToString(autor.getDataNascimento()));
                System.out.println("Biografia: " + autor.getBiografia());

                System.out.println("\nNovo nome (deixe em branco para manter): ");
                String novoNome = sc.nextLine();
                if (!novoNome.isBlank()) {
                    autor.setNome(novoNome);
                }

                System.out.println("Nova data de nascimento [dd/mm/aaaa] (deixe em branco para manter): ");
                String novaDataStr = sc.nextLine();
                if (!novaDataStr.isBlank()) {
                    autor.setDataNascimento(DataUtil.stringToTimestamp(novaDataStr));
                }

                System.out.println("Nova biografia (deixe em branco para manter): ");
                String novaBiografia = sc.nextLine();
                if (!novaBiografia.isBlank()) {
                    autor.setBiografia(novaBiografia);
                }

                if (autoresDAO.alterarAutor(autor)) {
                    System.out.println("Autor alterado com sucesso!");
                } else {
                    System.out.println("Erro ao alterar autor.");
                }
            } else {
                System.out.println("Autor não encontrado.");
            }
        } catch (Exception e) {
            System.out.println("Erro ao alterar autor.");
        }
    }

    private void excluirAutor() {
        System.out.println("\nExcluir autor");

        System.out.println("ID do Autor: ");
        int id = sc.nextInt();
        sc.nextLine();

        try {
            Autores autor = autoresDAO.buscarAutor(id);

            if (autor != null) {
                System.out.println("\nAutor a ser excluído:");
                System.out.println("Nome: " + autor.getNome());
                System.out.println("Data de nascimento: " + DataUtil.timestampToString(autor.getDataNascimento()));

                System.out.println("\nTem certeza que deseja excluir? (s/n): ");
                String resposta = sc.nextLine();

                if (resposta.equalsIgnoreCase("s")) {
                    if (autoresDAO.excluirAutor(id)) {
                        System.out.println("Autor excluído com sucesso!");
                    } else {
                        System.out.println("Erro ao excluir autor.");
                    }
                } else {
                    System.out.println("Operação cancelada.");
                }
            } else {
                System.out.println("Autor não encontrado.");
            }
        } catch (Exception e) {
            System.out.println("Erro ao excluir autor.");
        }
    }

}
