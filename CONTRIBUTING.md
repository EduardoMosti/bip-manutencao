# Fluxo de trabalho do grupo

Regra central: nada de commit direto na `main`. Todo codigo entra por pull request, revisado e aprovado por outro integrante.

## Ciclo de uma historia

1. Escolha uma issue do quadro na coluna **A fazer** e coloque seu nome como responsavel. Mova para **Fazendo**.
2. Crie uma branch a partir da `main` atualizada, com o numero da historia:

   ```
   git checkout main
   git pull
   git checkout -b feature/US-04-assumir-chamado
   ```

3. Faca commits pequenos, com mensagens no imperativo e em portugues (`Adiciona transacao para assumir chamado`).
4. Envie a branch e abra o pull request para a `main`:

   ```
   git push -u origin feature/US-04-assumir-chamado
   ```

5. No corpo do PR, cite a issue com `Closes #N`, para que ela feche sozinha no merge.
6. Outro integrante revisa (veja abaixo). Quem abriu o PR nao aprova o proprio PR.
7. Com a aprovacao, faca o merge. A issue so vai para **Feito** quando todos os criterios de aceite funcionam.

## Convencao de branches

| Prefixo | Uso |
|---------|-----|
| `feature/US-XX-descricao` | Implementacao de uma historia |
| `fix/descricao` | Correcao de defeito |
| `docs/descricao` | Documentacao, README, slides, checklist |

## Como revisar um PR

Revisar nao e clicar em Approve. O revisor deve:

1. Baixar a branch (`git fetch` e `git checkout nome-da-branch`).
2. Rodar o app em emulador ou aparelho.
3. Conferir, um a um, os criterios de aceite da historia citada.
4. Verificar que `google-services.json`, chaves e senhas nao entraram no commit.
5. Comentar no proprio PR o que nao funcionou, em vez de avisar por mensagem privada.
6. Aprovar somente quando todos os criterios passarem.

## Distribuicao esperada

Cada integrante deve ter PRs abertos por ele e PRs revisados por ele. O historico do repositorio e usado na avaliacao (ES3).
