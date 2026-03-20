
import java.util.Scanner;
import src.service.UsuarioService; 

public class Main
{
 
    public static void main(String[] args)
    { 
        Scanner console = new Scanner(System.in);
        int opcao = -1; 

        try {
            do {
                System.out.println("\n\nAEDsIII");
                System.out.println("-------");
                System.out.println("> Início");
                System.out.println("\n1 - Usuários"); 
                System.out.println("0 - Sair");

                System.out.print("\nOpção: ");
                try
                {
                    opcao = Integer.parseInt(console.nextLine());
                } catch(NumberFormatException e) {
                    opcao = -1;
                }

                switch(opcao)
                {
                    case 1:
                        
                        UsuarioService usuarioService = new UsuarioService();
                        usuarioService.menu(); 
                        break;
                    case 0:
                        System.out.println("Saindo do sistema...");
                        break;
                    default:
                        System.out.println("Opção inválida!");
                        break;
                }
            } while(opcao != 0);

        } catch(Exception e){
            System.err.println("Erro fatal no sistema: " + e.getMessage());
            e.printStackTrace();
        } finally {
            console.close();
        }
    }
}