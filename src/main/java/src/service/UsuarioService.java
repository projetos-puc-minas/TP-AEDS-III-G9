package src.service;

import src.dao.UsuarioDAO;
import src.model.Usuarios;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Serviço de autenticação e gestão de utilizadores.
 *
 * Criptografia XOR implementada manualmente conforme o requisito do enunciado.
 *
 * A chave XOR é derivada do email do utilizador para que dois utilizadores com a
 * mesma senha não tenham o mesmo hash armazenado.
 *
 * Fluxo de registo:
 * 1. Recebe email + senha em texto claro
 * 2. Chama xorCriptografar(senha, email) → senhaXor
 * 3. Armazena senhaXor no registo (a senha em texto claro nunca é persistida)
 *
 * Fluxo de login:
 * 1. Recebe email + senha em texto claro
 * 2. Busca utilizador pelo email
 * 3. Criptografa a senha digitada com a mesma chave (email)
 * 4. Compara com senhaXor armazenada
 */
public class UsuarioService {

    // Chave base fixa adicional para reforçar o XOR (funciona como um "salt" estático)
    private static final String CHAVE_BASE = "AEDs3-G9-2025";

    private final UsuarioDAO usuarioDAO;

    public UsuarioService(UsuarioDAO usuarioDAO) {
        this.usuarioDAO = usuarioDAO;
    }

    // -------------------------------------------------------------------------
    // Registo
    // -------------------------------------------------------------------------

    /**
     * Regista um novo utilizador.
     * A senha é criptografada com XOR antes de ser persistida.
     *
     * @return id gerado, ou -1 se o email já estiver registado
     */
    public int cadastrar(String nome, String email, String senhaTextoClaro) throws Exception {

        if (nome == null || nome.isBlank())
            throw new IllegalArgumentException("O nome não pode estar vazio.");
        if (email == null || email.isBlank())
            throw new IllegalArgumentException("O email não pode estar vazio.");
        if (senhaTextoClaro == null || senhaTextoClaro.length() < 4)
            throw new IllegalArgumentException("A senha deve ter no mínimo 4 caracteres.");

        // Verifica se o email já existe
        if (usuarioDAO.buscarPorEmail(email) != null) {
            return -1; // email já registado
        }

        String senhaXor = xorCriptografar(senhaTextoClaro, gerarChave(email));
        Usuarios usuario = new Usuarios(-1, nome, email, senhaXor);
        return usuarioDAO.incluirUsuario(usuario);
    }

    // -------------------------------------------------------------------------
    // Login
    // -------------------------------------------------------------------------

    /**
     * Autentica um utilizador através do email e senha.
     *
     * @return o objeto Usuarios autenticado, ou null se as credenciais forem inválidas
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
    // Alteração de Senha
    // -------------------------------------------------------------------------

    /**
     * Altera a senha de um utilizador após validar a senha atual.
     *
     * @return true se a senha foi alterada com sucesso
     */
    public boolean alterarSenha(int idUsuario, String senhaAtual, String novaSenha) throws Exception {

        if (novaSenha == null || novaSenha.length() < 4)
            throw new IllegalArgumentException("A nova senha deve ter no mínimo 4 caracteres.");

        Usuarios usuario = usuarioDAO.buscarUsuario(idUsuario);
        if (usuario == null) return false;

        // Valida a senha atual
        String senhaAtualXor = xorCriptografar(senhaAtual, gerarChave(usuario.getEmail()));
        if (!senhaAtualXor.equals(usuario.getSenhaXor())) {
            return false; // senha atual incorreta
        }

        String novaSenhaXor = xorCriptografar(novaSenha, gerarChave(usuario.getEmail()));
        usuario.setSenhaXor(novaSenhaXor);
        return usuarioDAO.alterarUsuario(usuario);
    }

    // -------------------------------------------------------------------------
    // XOR — implementação manual (Requisito do Enunciado)
    // -------------------------------------------------------------------------

    /**
     * Aplica criptografia XOR entre o texto e a chave.
     * A operação é simétrica: xorCriptografar(xorCriptografar(texto, chave), chave) == texto.
     */
    public static String xorCriptografar(String texto, String chave) {
        if (texto == null || texto.isEmpty()) return "";
        if (chave == null || chave.isEmpty()) return texto;

        byte[] textoBytes = texto.getBytes(StandardCharsets.UTF_8);
        byte[] chaveBytes = chave.getBytes(StandardCharsets.UTF_8);
        byte[] resultado  = new byte[textoBytes.length];

        for (int i = 0; i < textoBytes.length; i++) {
            resultado[i] = (byte) (textoBytes[i] ^ chaveBytes[i % chaveBytes.length]);
        }

        // Converte para Base64 para que o resultado seja uma String válida e armazenável em UTF-8
        return Base64.getEncoder().encodeToString(resultado);
    }

    /**
     * Descriptografa um texto que foi criptografado com xorCriptografar.
     * Retorna a senha original em texto claro.
     */
    public static String xorDescriptografar(String textoCriptografado, String chave) {
        if (textoCriptografado == null || textoCriptografado.isEmpty()) return "";
        if (chave == null || chave.isEmpty()) return textoCriptografado;

        byte[] criptBytes = Base64.getDecoder().decode(textoCriptografado);
        byte[] chaveBytes = chave.getBytes(StandardCharsets.UTF_8);
        byte[] resultado  = new byte[criptBytes.length];

        for (int i = 0; i < criptBytes.length; i++) {
            resultado[i] = (byte) (criptBytes[i] ^ chaveBytes[i % chaveBytes.length]);
        }

        return new String(resultado, StandardCharsets.UTF_8);
    }

    // -------------------------------------------------------------------------
    // Utilitários Internos
    // -------------------------------------------------------------------------

    /**
     * Gera a chave XOR derivada do email do utilizador.
     * Combinar com CHAVE_BASE garante que a chave tenha comprimento mínimo adequado.
     */
    private static String gerarChave(String email) {
        return email + CHAVE_BASE;
    }
}