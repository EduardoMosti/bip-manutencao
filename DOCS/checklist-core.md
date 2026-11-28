# Checklist CORE

> Rascunho em Markdown. A entrega exige este quadro em uma tabela Word (.docx) na pasta DOCS. Marcar com honestidade: requisito marcado como feito que nao funciona na demonstracao pesa mais do que um parcial bem explicado.

Legenda: Feito / Parcial / Nao feito

## Nucleo comum

| ID | Requisito | Situacao | Explicacao (obrigatoria se Parcial) |
|----|-----------|----------|-------------------------------------|
| NC1 | Cadastro e login com e-mail e senha, sessao persistente e opcao de sair | Nao feito | |
| NC2 | Dados no Firestore com snapshot listener nas listas principais | Nao feito | |
| NC3 | Regras de seguranca escritas pelo grupo com controle de perfil | Nao feito | |
| NC4 | Perfil em `usuarios/{uid}` definido no console | Nao feito | |
| NC5 | Listas com RecyclerView e estado vazio | Feito | Todas as listas usam RecyclerView e mostram estado vazio: maquinas, fila do tecnico, meus atendimentos e historico da maquina |
| NC6 | Formularios validados com mensagens em portugues | Nao feito | |
| NC7 | Girar a tela nao apaga o que foi digitado | Nao feito | |
| NC8 | Repositorio com README completo e google-services.json fora | Nao feito | |

## Engenharia de software

| ID | Pratica | Situacao | Explicacao |
|----|---------|----------|------------|
| ES1 | Historias de usuario com criterios de aceite | Parcial | Escritas em DOCS/historias-de-usuario.md; falta transformar em issues |
| ES2 | Backlog no GitHub Projects (A fazer, Fazendo, Feito) | Nao feito | |
| ES3 | Branch e pull request revisado por outro integrante | Nao feito | |

## Projeto 01 - Chamado de Manutencao

| ID | Requisito | Situacao | Explicacao (obrigatoria se Parcial) |
|----|-----------|----------|-------------------------------------|
| C1 | Cadastro de maquinas com codigo, nome e setor, com QR do codigo | Nao feito | |
| C2 | Operador le o QR e abre chamado com descricao e prioridade | Nao feito | |
| C3 | Fila do tecnico ordenada por prioridade e horario, em tempo real | Nao feito | |
| C4 | Tecnico assume chamado; dois tecnicos nao assumem o mesmo (transacao) | Nao feito | |
| C5 | Tecnico encerra com a solucao; grava abertura e fechamento | Nao feito | |
| C6 | Historico da maquina com duracao de cada reparo | Nao feito | |
