Aqui está a versão atualizada do teu `README.md`, contemplando todas as novidades que implementaste na Fase IV (Compressão, Backup e Restore), mantendo as informações cruciais das fases anteriores. Podes copiar e colar diretamente no teu repositório!

---

# 📚 BiblioSys - Sistema de Gestão de Biblioteca (Grupo 9)

**Trabalho Prático - AEDs III | PUC Minas**
**Professor:** Walisson Ferreira de Carvalho

O **BiblioSys** é um sistema de gestão de acervo bibliográfico desenvolvido para a disciplina de Algoritmos e Estruturas de Dados III. O foco desta **Fase IV** é a implementação de **compressão de dados**, criando mecanismos de backup e restauração que preservam a integridade de todos os arquivos do sistema (dados e índices).

---

##  Novidades da Fase IV (Compressão)

* **Backup Completo em Arquivo Único:** O sistema empacota recursivamente todos os arquivos da diretoria `./data` (registos e índices) num único fluxo de bytes (classe `PacoteBackup`) antes de aplicar a compressão, funcionando como um *backup* completo.
* **Compressão com Huffman:** Implementação de compressão estatística ao nível do byte utilizando uma Árvore de Huffman, com suporte a escrita e leitura bit a bit (`BitOutputStream` e `BitInputStream`).
* **Compressão com LZW:** Implementação de compressão baseada em dicionário (códigos de 12 bits) para mapear sequências repetitivas de bytes, limitando o dicionário a 4096 posições para evitar *overflow*.
* **CLI de Backup e Restore:** Comandos integrados via terminal para paralisar o sistema, comprimir a base atual ou restaurar um backup antigo substituindo a pasta `./data` atual.

## ⚙️ Funcionalidades das Fases Anteriores

* **CRUD Completo**: Operações de inserção, busca, atualização e exclusão lógica com reaproveitamento de espaço (*Best-Fit*) para todas as entidades.
* **Indexação Híbrida**:
* **Hash Extensível**: Utilizado para buscas diretas (chave primária) com performance amortizada O(1).
* **Árvore B+**: Utilizada para listagens ordenadas e suporte a futuras buscas por intervalo.


* **Relacionamentos**:
* **1:N (Editora → Livros)**: Implementado obrigatoriamente via Hash Extensível (`HashExtensivelLong`) com chave composta de 64 bits.
* **N:N (Livro ↔ Autor / Livro ↔ Tag)**: Geridos por tabelas associativas e índices secundários.


* **Validações de Unicidade**: O sistema previne duplicatas de ISBN (Livros) e E-mail (Usuários) na inclusão e alteração.
* **Interface Web**: Painel administrativo desenvolvido em HTML/JS puro integrado a uma API REST nativa do Java (`ApiServer`).

---

## 🛠️ Tecnologias Utilizadas

* **Linguagem**: Java 17.
* **Gestão de Dependências**: Maven.
* **Persistência**: `RandomAccessFile` (Arquivos binários).
* **Comunicação**: Servidor HTTP nativo do Java e JSON para intercâmbio de dados.
* **Front-end**: HTML5, CSS3 e JavaScript Moderno (Vanilla).

---

## 📁 Estrutura do Projeto

* `/src/main/java/src/compression`: Lógica dos algoritmos de compressão (Huffman, LZW e utilitários de Bits).
* `/src/main/java/src/dao`: Camada de acesso a dados (Data Access Object).
* `/src/main/java/src/model`: Classes de entidade (POJOs).
* `/src/main/java/src/util`: Estruturas de dados fundamentais (Hash Extensível, Árvore B+, Arquivo, Empacotamento).
* `/src/main/java/src/server`: Implementação da API REST e servidor estático.
* `/web`: Interface do usuário (Front-end).
* `/data`: Local de armazenamento dos arquivos `.bin` e índices (sincronizado em tempo real).
* `/backups`: Diretoria gerada automaticamente com os arquivos comprimidos (`.bin`).

---

## 💻 Como Executar

### Pré-requisitos

* Java JDK 17 ou superior.
* Maven 3.6+.

### Compilação

Na raiz do projeto, execute:

```bash
mvn clean package

```

### 1. Iniciar o Servidor e a Interface Web (Modo Normal)

Para iniciar a API e aceder ao sistema no navegador:

```bash
java -cp target/classes Main
# Ou através do .jar gerado na pasta target

```

Acesse `http://localhost:8080` no seu navegador.

### 2. Realizar um Backup Completo (Fase IV)

Para compactar e salvar o estado atual do banco de dados utilizando Huffman e LZW:

```bash
java -cp target/classes Main --backup

```

*Isto irá gerar relatórios de compressão no terminal com o tamanho do ficheiro original, comprimido e a taxa de compressão*. *Os arquivos gerados ficarão na pasta* `./backups`.

### 3. Restaurar um Backup (Fase IV)

Para descompactar um arquivo de backup e substituir a base de dados atual (o sistema faz automaticamente uma cópia de segurança da base que será sobrescrita):

```bash
java -cp target/classes Main --restore backups/backup_huffman.bin

```

*(Também é possível utilizar o arquivo* `backup_lzw.bin`*)*.

---

## 🧠 Decisões de Projeto

| Recurso/Problema | Resposta Técnica |
| --- | --- |
| **Arquitetura da Compressão** | Como a compressão deve atuar como backup de múltiplos arquivos, criamos o `PacoteBackup`, que consolida os arquivos recursivamente num fluxo de bytes único (metadados de caminho + conteúdo) antes de comprimir. |
| **Manipulação de Bits (Huffman)** | Java atua no mínimo ao nível do *byte*. Implementamos `BitOutputStream` e `BitInputStream` para acumular buffers parciais, permitindo a gravação de códigos de árvore variados sem corromper os dados. |
| **Crescimento do LZW** | Limitamos o tamanho do código em 12 bits e a tabela a 4096 posições. Ao atingir o teto, o dicionário congela, evitando `OutOfMemory` ou quebra de decodificação. |
| **Estrutura dos registros** | Variável, com campos de tamanho fixo (int, long) e strings precedidas pelo seu tamanho em bytes. |
| **Exclusão lógica** | Implementada via *lápide* (booleano). O espaço livre encadeia-se na lista *Best-Fit* salva no cabeçalho. |

---

## 👥 Membros do Grupo (G9)

* Fernando Mucci
* Maria Eduarda P. Brito
* Luan Oliveira
* Luiz Felipe Volpe
* Luisa Campanha


