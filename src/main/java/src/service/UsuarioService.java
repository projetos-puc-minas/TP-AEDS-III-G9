package src.service;

import src.dao.UsuarioDAO;
import src.model.Usuarios;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class UsuarioService {

    // Chave base fixa adicional para reforçar o XOR (funciona como um "salt" estático)
    private static final String CHAVE_BASE = "AEDs3-G9-2025";

    private final UsuarioDAO usuarioDAO;

    public UsuarioService(UsuarioDAO usuarioDAO) {
        this.usuarioDAO = usuarioDAO;
    }

    // Registo

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

    // Login


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

    // Alteração de Senha

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

    // XOR — implementação manual

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

    // Utilitários Internos

    private static String gerarChave(String email) {
        return email + CHAVE_BASE;
    }
}