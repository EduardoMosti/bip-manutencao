# Fluxo de trabalho do grupo

Regra central: nada de commit direto na `main`. Todo código entra por pull request, revisado e aprovado por outro integrante.

## Ciclo de uma história

1. Escolha uma issue do quadro na coluna **A fazer** e coloque seu nome como responsável. Mova para **Fazendo**.
2. Crie uma branch a partir da `main` atualizada, com o número da história:

   ```
   git checkout main
   git pull
   git checkout -b historia/US-04-assumir-chamado
   ```

3. Faça commits pequenos, com mensagens no imperativo e em português (`Adiciona transação para assumir chamado`).
4. Envie a branch e abra o pull request para a `main`:

   ```
   git push -u origin historia/US-04-assumir-chamado
   ```

5. No corpo do PR, cite a issue com `Closes #N`, para que ela feche sozinha no merge. O número da issue é o número da história mais um (a US-04 é a issue #5).
6. Outro integrante revisa (veja abaixo). Quem abriu o PR não aprova o próprio PR.
7. Com a aprovação, faça o merge. A issue só vai para **Feito** quando todos os critérios de aceite funcionam.

## Convenção de branches

| Prefixo | Uso |
|---------|-----|
| `historia/US-XX-descricao` | Implementação de uma história |
| `correcao/descricao` | Correção de defeito |
| `configuracao/descricao` | Configuração do projeto, dependências, ferramentas |
| `documentacao/descricao` | Documentação, README, slides, checklist |

## Como revisar um PR

Revisar não é clicar em Approve. O revisor deve:

1. Baixar a branch (`git fetch` e `git checkout nome-da-branch`).
2. Rodar o app em emulador ou aparelho.
3. Conferir, um a um, os critérios de aceite da história citada.
4. Verificar que `google-services.json`, chaves e senhas não entraram no commit.
5. Comentar no próprio PR o que não funcionou, em vez de avisar por mensagem privada.
6. Aprovar somente quando todos os critérios passarem.

## Distribuição esperada

Cada integrante deve ter PRs abertos por ele e PRs revisados por ele. O histórico do repositório é usado na avaliação (ES3).
