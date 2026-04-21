package src.services;

import java.util.List;
import java.util.Scanner;
import src.dao.TagDAO;
import src.model.Tag;
import src.util.ArvoreBMais;
import src.util.Indexador;

public class MenuTags {

    private final TagDAO tagDAO;
    private final Scanner console = new Scanner(System.in);

    public MenuTags() throws Exception {
        Indexador indice = new ArvoreBMais("tags");
        tagDAO = new TagDAO(indice);
    }

    public void menu() {
        int opcao;

        do {
            System.out.println("\n\nAEDsIII");
            System.out.println("-------");
            System.out.println("> Início > Tags");
            System.out.println("\n1 - Listar todas as tags");
            System.out.println("2 - Buscar tag por ID");
            System.out.println("3 - Criar tag");
            System.out.println("4 - Alterar tag");
            System.out.println("5 - Excluir tag");
            System.out.println("0 - Voltar");
            System.out.print("\nOpção: ");

            opcao = lerInt();

            switch (opcao) {
                case 1: listarTodos();    break;
                case 2: buscarPorId();    break;
                case 3: criarTag();       break;
                case 4: alterarTag();     break;
                case 5: excluirTag();     break;
                case 0: break;
                default:
                    System.out.println("Opção inválida!");
                    break;
            }
        } while (opcao != 0);
    }

    // --- listar todos ---

    private void listarTodos() {
        try {
            List<Tag> lista = tagDAO.listarTodos();
            if (lista.isEmpty()) {
                System.out.println("Nenhuma tag cadastrada.");
            } else {
                System.out.println("\nTags cadastradas (" + lista.size() + "):");
                lista.forEach(System.out::println);
            }
        } catch (Exception e) {
            System.out.println("Erro ao listar tags.");
        }
    }

    // --- buscar por ID ---

    private void buscarPorId() {
        System.out.print("\nID da Tag: ");
        int id = lerInt();

        try {
            Tag tag = tagDAO.buscarPorId(id);
            if (tag == null) {
                System.out.println("Tag não encontrada.");
            } else {
                System.out.println(tag);
            }
        } catch (Exception e) {
            System.out.println("Erro ao buscar tag.");
        }
    }

    // --- criar ---

    private void criarTag() {
        System.out.println("\nNova Tag");

        System.out.print("Nome: ");
        String nome = console.nextLine().trim();

        if (nome.isEmpty()) {
            System.out.println("Nome não pode ser vazio.");
            return;
        }

        try {
            Tag tag = new Tag(nome);
            int id = tagDAO.criar(tag);

            if (id > 0) {
                System.out.println("Tag criada com sucesso! ID: " + id);
            } else {
                System.out.println("Erro ao criar tag.");
            }
        } catch (Exception e) {
            System.out.println("Erro ao criar tag.");
        }
    }

    // --- alterar ---

    private void alterarTag() {
        System.out.print("\nID da Tag a ser alterada: ");
        int id = lerInt();

        try {
            Tag tag = tagDAO.buscarPorId(id);
            if (tag == null) {
                System.out.println("Tag não encontrada.");
                return;
            }

            System.out.println("Tag atual:" + tag);

            System.out.print("\nNovo nome (vazio para manter \"" + tag.getNome() + "\"): ");
            String nome = console.nextLine().trim();
            if (!nome.isEmpty()) tag.setNome(nome);

            if (tagDAO.alterar(tag)) {
                System.out.println("Tag alterada com sucesso.");
            } else {
                System.out.println("Erro ao alterar tag.");
            }
        } catch (Exception e) {
            System.out.println("Erro ao alterar tag.");
        }
    }

    // --- excluir ---

    private void excluirTag() {
        System.out.print("\nID da Tag a ser excluída: ");
        int id = lerInt();

        try {
            Tag tag = tagDAO.buscarPorId(id);
            if (tag == null) {
                System.out.println("Tag não encontrada.");
                return;
            }

            System.out.println("Tag:" + tag);
            System.out.print("Confirma exclusão? (S/N): ");
            char resp = console.nextLine().trim().charAt(0);

            if (resp == 'S' || resp == 's') {
                if (tagDAO.excluirPorId(id)) {
                    System.out.println("Tag excluída com sucesso.");
                } else {
                    System.out.println("Erro ao excluir tag.");
                }
            }
        } catch (Exception e) {
            System.out.println("Erro ao excluir tag.");
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