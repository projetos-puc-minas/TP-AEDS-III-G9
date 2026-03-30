import java.util.Scanner;

import src.services.MenuEditoras;
import src.services.MenuLivros;
import src.services.ServidorAPI;

public class Main {
    public static void main(String[] args) {
        ServidorAPI.iniciar();
        Scanner console = new Scanner(System.in);
        int opcao;

        try {
            do {
                System.out.println("\n\nAEDsIII");
                System.out.println("-------");
                System.out.println("> Início");
                System.out.println("\n1 - Editoras");
                System.out.println("2 - Livros (inclui Tags)");
                System.out.println("0 - Sair");

                System.out.print("\nOpção: ");
                try {
                    opcao = Integer.valueOf(console.nextLine());
                } catch (NumberFormatException e) {
                    opcao = -1;
                }

                switch (opcao) {
                    case 1:
                        MenuEditoras menuEditoras = new MenuEditoras();
                        menuEditoras.menu();
                        break;
                    case 2:
                        MenuLivros menuLivros = new MenuLivros();
                        menuLivros.menu();
                        break;
                    case 0:
                        System.out.println("Saindo...");
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