# 📚 BiblioSys - Sistema de Gestão de Biblioteca (Grupo 9)

**Trabalho Prático - AEDs III | PUC Minas**
**Professor:** Walisson Ferreira de Carvalho

O **BiblioSys** é um sistema de gestão de acervo bibliográfico desenvolvido para a disciplina de Algoritmos e Estruturas de Dados III. O foco desta segunda fase é a implementação de persistência em disco com estruturas de indexação avançadas (Árvore B+ e Hash Extensível), suporte a relacionamentos complexos e uma interface web funcional.

---

##  Funcionalidades da Fase 2

*   **CRUD Completo**: Operações de inserção, busca, atualização e exclusão lógica para todas as entidades (Livros, Autores, Editoras, Usuários e Tags).
*   **Indexação Híbrida**:
    *   **Hash Extensível**: Utilizado para buscas diretas (chave primária) com performance amortizada $O(1)$.
    *   **Árvore B+**: Utilizada para listagens ordenadas e suporte a futuras buscas por intervalo.
*   **Relacionamentos**:
    *   **1:N (Editora → Livros)**: Implementado obrigatoriamente via **Hash Extensível** (`HashExtensivelLong`) com chave composta (`idEditora | idLivro`). Esta implementação garante que todos os livros de uma editora sejam agrupados e recuperados eficientemente através de uma busca por faixa no Hash Extensível, conforme a especificação.
    *   **N:N (Livro ↔ Autor / Livro ↔ Tag)**: Geridos por tabelas associativas e índices secundários.
*   **Validações de Unicidade**: Implementação de validações para garantir a unicidade de dados críticos:
    *   **ISBN (Livros)**: O sistema agora verifica a unicidade do ISBN tanto na inclusão quanto na alteração de livros, prevenindo duplicatas e mantendo a integridade do acervo.
    *   **E-mail (Usuários)**: Similarmente, a unicidade do e-mail é validada durante o cadastro e a atualização de usuários, assegurando que cada e-mail corresponda a um único usuário no sistema.
*   **Persistência e Reaproveitamento**: Armazenamento binário em disco com gerenciamento de registros excluídos através de uma lista encadeada (*Best-Fit*) para reaproveitamento de espaço.
*   **Ordenação Externa**: Algoritmo de intercalação balanceada para ordenação de grandes volumes de dados por campos textuais (ex: Título do Livro).
*   **Interface Web**: Painel administrativo desenvolvido em HTML/JS puro integrado a uma API REST em Java.

---

##  Tecnologias Utilizadas

*   **Linguagem**: Java 17.
*   **Gestão de Dependências**: Maven.
*   **Persistência**: `RandomAccessFile` (Arquivos binários).
*   **Comunicação**: Servidor HTTP nativo do Java e JSON para intercâmbio de dados.
*   **Front-end**: HTML5, CSS3 e JavaScript Moderno (Vanilla).

---

##  Estrutura do Projeto

*   `/src/main/java/src/dao`: Camada de acesso a dados (Data Access Object).
*   `/src/main/java/src/model`: Classes de entidade (POJOs).
*   `/src/main/java/src/util`: Estruturas de dados fundamentais (Hash Extensível, Árvore B+, Arquivo).
*   `/src/main/java/src/server`: Implementação da API REST e servidor de arquivos estáticos.
*   `/web`: Interface do usuário (Front-end).
*   `/data`: Local de armazenamento dos arquivos `.bin` e índices `.idx` / `.dir.bin`.

---

##  Como Executar

### Pré-requisitos
*   Java JDK 17 ou superior.
*   Maven 3.6+.

### Passos para execução
1.  **Compilar o projeto**:
    ```bash
    mvn clean package
    ```
2.  **Executar o servidor**:
    ```bash
    java -jar target/bibliosys-1.0-SNAPSHOT.jar
    ```
3.  **Aceder à interface**:
    Abra o seu navegador e aceda a `http://localhost:8080`.

---

##  Decisões de Projeto

| Pergunta | Resposta Técnica |
| :--- | :--- |
| **Estrutura dos registros?** | Variável, com campos de tamanho fixo (int, long) e strings precedidas pelo seu tamanho. |
| **Exclusão lógica?** | Implementada via *lápide* (booleano no início do registro). O espaço é devolvido para uma lista de excluídos no cabeçalho. |
| **Relacionamento 1:N?** | Implementado com **Hash Extensível** (`HashExtensivelLong`) usando uma chave composta de 64 bits para agrupar livros por editora. |
| **Persistência dos índices?** | Os diretórios e buckets são persistidos em ficheiros separados e sincronizados em tempo real com as operações de escrita. |
| **Validação de Unicidade?** | Implementada para ISBN (Livros) e E-mail (Usuários) nos respectivos DAOs, prevenindo duplicatas na inclusão e alteração de registros. |

---

##  Membros do Grupo (G9)

*   Fernando Mucci
*   Maria Eduarda P. Brito
*   Luan Oliveira
*   Luiz Felipe Volpe
*   Luisa Campanha

---

