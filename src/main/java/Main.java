import src.dao.DAOFactory;
import src.server.ApiServer;

/**
 * Ponto de entrada do sistema BiblioSys.
 *
 * Inicializa o DAOFactory (que monta todos os DAOs e injeta dependências)
 * e sobe o servidor HTTP na porta 8080.
 *
 * Tabelas ativas:
 *   - Editora     (1:N com Livro)
 *   - Livro       (possui campo multivalorado: generos[])
 *   - Autores
 *   - LivroAutor  (N:N entre Livro e Autores)
 *   - Usuarios    (autenticação XOR)
 *
 * Acesse: http://localhost:8080
 *
 * Dependência necessária (coloque no classpath):
 *   org.json — https://mvnrepository.com/artifact/org.json/json
 */
public class Main {

    private static final int    PORT     = 8080;
    private static final String WEB_ROOT = "./web"; // pasta com index.html, style.css, app.js

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════╗");
        System.out.println("║  BiblioSys — AEDs III · G9   ║");
        System.out.println("╚══════════════════════════════╝");

        DAOFactory factory = null;

        try {
            System.out.print("→ Inicializando DAOs... ");
            factory = new DAOFactory();
            System.out.println("OK");

            System.out.print("→ Subindo servidor HTTP na porta " + PORT + "... ");
            ApiServer server = new ApiServer(PORT, factory, WEB_ROOT);
            server.start();
            System.out.println("OK");

            System.out.println("\nAcesse: http://localhost:" + PORT);
            System.out.println("Pressione ENTER para encerrar.\n");

            System.in.read();

        } catch (Exception e) {
            System.err.println("\n[ERRO FATAL] " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.out.println("→ Encerrando...");
            System.exit(0);
        }
    }
}