import src.dao.DAOFactory;
import src.server.ApiServer;

/**
 * Classe principal de inicialização do sistema BiblioSys.
 *
 * Responsável por instanciar a DAOFactory e iniciar o servidor HTTP.
 * Requer a biblioteca org.json no classpath.
 */
public class Main {

    private static final int    PORT     = 8080;
    private static final String WEB_ROOT = "./web";

    public static void main(String[] args) {
        System.out.println("BiblioSys - Inicializando o sistema...");

        try {
            DAOFactory factory = new DAOFactory();
            System.out.println("DAOFactory inicializada com sucesso.");

            ApiServer server = new ApiServer(PORT, factory, WEB_ROOT);
            server.start();

            // Hook de encerramento para garantir fechamento adequado de recursos (Fase 4).
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nEncerrando processos do BiblioSys...");
                System.out.println("Sistema encerrado.");
            }));

            System.out.println("Servidor ativo: http://localhost:" + PORT);
            System.out.println("Pressione [ENTER] no terminal para interromper a execucao.");

            System.in.read();

            System.out.println("Interrupcao solicitada pelo usuario.");
            System.exit(0);

        } catch (Exception e) {
            System.err.println("Erro fatal durante a inicializacao do sistema:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}