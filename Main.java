import java.util.Scanner;
import src.service.UsuarioService;
import src.services.MenuEditoras;
import src.services.MenuLivrosAutores;

public class Main {

    public static void main(String[] args) {
        Scanner console = new Scanner(System.in);
        int opcao = -1;

        try {
            do {
                System.out.println("\n\nAEDsIII");
                System.out.println("-------");
                System.out.println("> Início");
                System.out.println("\n1 - Editoras");
                System.out.println("2 - Livros-Autores");
                System.out.println("3 - Usuários");
                System.out.println("0 - Sair");

                System.out.print("\nOpção: ");
                
                try {
                    opcao = Integer.parseInt(console.nextLine());
                } catch (NumberFormatException e) {
                    System.out.println("Erro: Por favor, digite apenas números.");
                    opcao = -1;
                }

                // O switch só processa se a opção for um número válido
                switch (opcao)
                {
                    case 1:
                        new MenuEditoras().menu();
                        break;
                    case 2:
                        new MenuLivrosAutores().menu();
                        break;
                    case 3:
                        new UsuarioService().menu();
                        break;
                    case 0:
                        System.out.println("Saindo...");
                        break;
                    case -1:
                        break;
                    default:
                        System.out.println("Opção inválida!");
                        break;
                }

            } while (opcao != 0);

        } catch (Exception e) {
            System.err.println("Erro fatal no sistema");
            e.printStackTrace();
        } finally {
            console.close();
        }
    }
}