package src.service;

import src.dao.UsuarioDAO;
import src.model.Usuarios;

/**
 * Serviço de autenticação e gerenciamento de usuários.
 *
 * Criptografia XOR implementada manualmente conforme requisito do enunciado.
 *
 * A chave XOR é derivada do email do usuário para que dois usuários com a
 * mesma senha não tenham o mesmo hash armazenado.
 *
 * Fluxo de cadastro:
 *   1. Recebe email + senha em texto claro
 *   2. Chama xorCriptografar(senha, email) → senhaXor
 *   3. Armazena senhaXor no registro (a senha em texto claro nunca é persistida)
 *
 * Fluxo de login:
 *   1. Recebe email + senha em texto claro
 *   2. Busca usuário pelo email
 *   3. Criptografa a senha digitada com a mesma chave (email)
 *   4. Compara com senhaXor armazenada
 */
public class UsuarioService {

    // Chave base fixa adicional para reforçar o XOR (pode ser configurada)
    private static final String CHAVE_BASE = "AEDs3-G9-2025";

    private final UsuarioDAO usuarioDAO;

    public UsuarioService(UsuarioDAO usuarioDAO) {
        this.usuarioDAO = usuarioDAO;
    }

    // -------------------------------------------------------------------------
    // Cadastro
    // -------------------------------------------------------------------------

    /**
     * Cadastra um novo usuário.
     * A senha é criptografada com XOR antes de ser persistida.
     *
     * @return id gerado, ou -1 se o email já estiver cadastrado
     */
    public int cadastrar(String nome, String email, String senhaTextoClaro)
            throws Exception {

        if (nome == null || nome.isBlank())
            throw new IllegalArgumentException("Nome não pode ser vazio.");
        if (email == null || email.isBlank())
            throw new IllegalArgumentException("Email não pode ser vazio.");
        if (senhaTextoClaro == null || senhaTextoClaro.length() < 4)
            throw new IllegalArgumentException("Senha deve ter no mínimo 4 caracteres.");

        // Verifica se email já existe
        if (usuarioDAO.buscarPorEmail(email) != null) {
            return -1; // email já cadastrado
        }

        String senhaXor = xorCriptografar(senhaTextoClaro, gerarChave(email));
        Usuarios usuario = new Usuarios(-1, nome, email, senhaXor);
        return usuarioDAO.incluirUsuario(usuario);
    }

    // -------------------------------------------------------------------------
    // Login
    // -------------------------------------------------------------------------

    /**
     * Autentica um usuário pelo email e senha.
     *
     * @return o objeto Usuarios autenticado, ou null se credenciais inválidas
     */
    public Usuarios login(String email, String senhaTextoClaro) throws Exception {
        if (email == null || senhaTextoClaro == null) return null;

        Usuarios usuario = usuarioDAO.buscarPorEmail(email);
        if (usuario == null) return null;

        String senhaXorDigitada = xorCriptografar(senhaTextoClaro, gerarChave(email));

        if (senhaXorDigitada.equals(usuario.getSenhaXor())) {
            return usuario;
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Alteração de senha
    // -------------------------------------------------------------------------

    /**
     * Altera a senha de um usuário após validar a senha atual.
     *
     * @return true se a senha foi alterada com sucesso
     */
    public boolean alterarSenha(int idUsuario, String senhaAtual, String novaSenha)
            throws Exception {

        if (novaSenha == null || novaSenha.length() < 4)
            throw new IllegalArgumentException("Nova senha deve ter no mínimo 4 caracteres.");

        Usuarios usuario = usuarioDAO.buscarUsuario(idUsuario);
        if (usuario == null) return false;

        // Valida senha atual
        String senhaAtualXor = xorCriptografar(senhaAtual, gerarChave(usuario.getEmail()));
        if (!senhaAtualXor.equals(usuario.getSenhaXor())) {
            return false; // senha atual incorreta
        }

        String novaSenhaXor = xorCriptografar(novaSenha, gerarChave(usuario.getEmail()));
        usuario.setSenhaXor(novaSenhaXor);
        return usuarioDAO.alterarUsuario(usuario);
    }

    // -------------------------------------------------------------------------
    // XOR — implementação manual conforme exigido pelo enunciado
    // -------------------------------------------------------------------------

    /**
     * Aplica criptografia XOR entre o texto e a chave.
     * A operação é simétrica: xorCriptografar(xorCriptografar(texto, chave), chave) == texto.
     *
     * Algoritmo:
     *   Para cada caractere i do texto:
     *     resultado[i] = texto[i] XOR chave[i % chave.length]
     *   O resultado é convertido para Base64 para ser armazenável como String UTF-8.
     */
    public static String xorCriptografar(String texto, String chave) {
        if (texto == null || texto.isEmpty()) return "";
        if (chave == null || chave.isEmpty()) return texto;

        byte[] textoBytes = texto.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] chaveBytes = chave.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] resultado  = new byte[textoBytes.length];

        for (int i = 0; i < textoBytes.length; i++) {
            resultado[i] = (byte) (textoBytes[i] ^ chaveBytes[i % chaveBytes.length]);
        }

        // Converte para Base64 para que o resultado seja uma String válida e armazenável
        return java.util.Base64.getEncoder().encodeToString(resultado);
    }

    /**
     * Descriptografa um texto que foi criptografado com xorCriptografar.
     * Retorna a senha original em texto claro.
     */
    public static String xorDescriptografar(String textoCriptografado, String chave) {
        if (textoCriptografado == null || textoCriptografado.isEmpty()) return "";
        if (chave == null || chave.isEmpty()) return textoCriptografado;

        byte[] criptBytes = java.util.Base64.getDecoder().decode(textoCriptografado);
        byte[] chaveBytes = chave.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] resultado  = new byte[criptBytes.length];

        for (int i = 0; i < criptBytes.length; i++) {
            resultado[i] = (byte) (criptBytes[i] ^ chaveBytes[i % chaveBytes.length]);
        }

        return new String(resultado, java.nio.charset.StandardCharsets.UTF_8);
    }

    // -------------------------------------------------------------------------
    // Geração de chave — combina email + chave base
    // -------------------------------------------------------------------------

    /**
     * Gera a chave XOR derivada do email do usuário.
     * Combinar com CHAVE_BASE garante que a chave tenha comprimento mínimo adequado.
     */
    private static String gerarChave(String email) {
        // Concatena email com chave base para aumentar a entropia
        return email + CHAVE_BASE;
    }
}
