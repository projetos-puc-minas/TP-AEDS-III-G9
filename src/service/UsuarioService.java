package src.service;

import java.util.Scanner;
import src.dao.UsuarioDAO;
import src.model.Usuarios;

public class UsuarioService
{
    
    private UsuarioDAO usuarioDAO;

    private Scanner console = new Scanner(System.in);


    public UsuarioService() throws Exception
    {
        this.usuarioDAO = new UsuarioDAO();
    }


    public void menu() {
        int opcao;

        do {
            System.out.println("\n\nAEDsIII");
            System.out.println("-------");
            System.out.println("> Início > Usuários");
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
                    buscarUsuarioId();
                    break;
                case 2:
                    incluirUsuario();
                    break;
                case 3:
                    alterarUsuario();
                    break;
                case 4:
                    excluirUsuario();
                    break;
                case 0:
                    break;
                default:
                    System.out.println("Opção inválida!");
                    break;
            }
        } while (opcao != 0);
    }

    private void buscarUsuarioId() {
        System.out.print("\nID do usuário: ");
        int id = console.nextInt();
        console.nextLine(); 

        try {
            Usuarios usuario = usuarioDAO.buscarUsuario(id);

            if (usuario != null) {
                System.out.println("\n--- Dados do Usuário ---");
                System.out.println("ID: " + usuario.getId());
                System.out.println("Nome: " + usuario.getNome());
                System.out.println("Email: " + usuario.getEmail());
            } else {
                System.out.println("Usuário não encontrado.");
            }
        } catch (Exception e) {
            System.out.println("Erro ao buscar usuário.");
        }
    }

    private void incluirUsuario() {
        System.out.println("\nInclusão de usuário");

        System.out.print("\nNome: ");
        String nome = console.nextLine();

        System.out.print("Email: ");
        String email = console.nextLine();

        System.out.print("Senha: ");
        String senha = console.nextLine();

        try {
            Usuarios usuario = new Usuarios(-1, nome, email);
            usuario.SetSenhaXor(senha);

            // O DAO retorna o ID gerado (int) no método create
            int idGerado = usuarioDAO.incluirUsuario(usuario);
            
            if (idGerado > 0) {
                System.out.println("Usuário incluído com sucesso. ID gerado: " + idGerado);
            } else {
                System.out.println("Erro ao incluir usuário.");
            }
        } catch (Exception e) {
            System.out.println("Erro ao incluir usuário.");
        }
    }

    public void alterarUsuario() {
        System.out.print("\nID do usuário a ser alterado: ");

        int id = console.nextInt();
        console.nextLine();

        try {
            Usuarios usuario = usuarioDAO.buscarUsuario(id);
            if (usuario == null) {
                System.out.println("Usuário não encontrado.");
                return;
            }

            System.out.print("\nNovo nome: ");
            String nome = console.nextLine();
            if (!nome.isEmpty()) usuario.setNome(nome);

            System.out.print("Novo email: ");
            String email = console.nextLine();
            if (!email.isEmpty()) usuario.setEmail(email);

            System.out.print("Nova senha: ");
            String senha = console.nextLine();
            if (!senha.isEmpty()) usuario.SetSenhaXor(senha);

            if (usuarioDAO.alterarUsuario(usuario)) {
                System.out.println("Usuário alterado com sucesso.");
            } else {
                System.out.println("Erro ao alterar usuário.");
            }
        } catch (Exception e) {
            System.out.println("Erro ao alterar usuário.");
        }
    }

    private void excluirUsuario() {
        System.out.print("\nID do usuário a ser excluído: ");
        int id = console.nextInt();
        console.nextLine();

        try {
            Usuarios usuario = usuarioDAO.buscarUsuario(id);
            if (usuario == null) {
                System.out.println("Usuário não encontrado.");
                return;
            }
        
            System.out.print("Confirma exclusão do usuário " + usuario.getNome() + "? (S/N): ");
            char resp = console.next().charAt(0);
            console.nextLine(); // Limpa o buffer após o char

            if (resp == 'S' || resp == 's') {
                if (usuarioDAO.excluirUsuario(id)) {
                    System.out.println("Usuário excluído com sucesso.");
                } else {
                    System.out.println("Erro ao excluir usuário.");
                }
            }
        } catch (Exception e) {
            System.out.println("Erro ao excluir usuário.");
        }
    }
    
    /*public int cadastrar(String nome, String email, String senha) throws Exception
    {
        // Validação simples de nome e email
        if (nome.isEmpty() || email.isEmpty()) {
            throw new Exception("Nome e Email são obrigatórios.");
        }

        // Cria o objeto.
        Usuarios novoUsuario = new Usuarios(0, nome, email);

        novoUsuario.SetSenhaXor(senha);//Sem criptografia ADD NO TP 5

        // Chama o DAO e retorna o ID
        return usuarioDAO.incluirUsuario(novoUsuario);
    }

    public Usuarios buscar(int id) throws Exception
    {
        return usuarioDAO.buscarUsuario(id);
    }

    public boolean atualizar(int id, String nome, String email) throws Exception
    {
        Usuarios user = usuarioDAO.buscarUsuario(id);
        if (user != null)
        {
            user.setNome(nome);
            user.setEmail(email);
            return usuarioDAO.alterarUsuario(user);
        }

        return false;
    }

    public boolean excluir(int id) throws Exception
    {
        return usuarioDAO.excluirUsuario(id);
    }*/
}

