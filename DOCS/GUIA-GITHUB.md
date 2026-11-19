# Guia de criacao do repositorio (passo a passo manual)

Este guia e para o grupo executar no GitHub. Nenhum commit ou envio e feito automaticamente.

## 1. Criar o repositorio

1. Um integrante cria o repositorio, por exemplo `bip-manutencao`, em **github.com/new**.
2. Visibilidade: a que o professor exigir (se for privado, adicione o professor como colaborador).
3. Nao marque README nem .gitignore na criacao, porque ja existem nesta estrutura.
4. Em **Settings > Collaborators**, adicione os outros tres integrantes.

## 2. Primeiro envio (feito pelo grupo)

Este e o unico momento em que a `main` recebe conteudo direto, para existir uma base. Depois dele, so por pull request.

```
git init
git add .
git commit -m "Estrutura inicial do repositorio"
git branch -M main
git remote add origin https://github.com/USUARIO/bip-manutencao.git
git push -u origin main
```

## 3. Proteger a main (ES3)

Em **Settings > Branches > Add branch protection rule**, branch `main`:

- Require a pull request before merging
- Require approvals: 1
- Dismiss stale pull request approvals when new commits are pushed

## 4. Quadro no GitHub Projects (ES2)

1. Aba **Projects > New project > Board**.
2. Campo **Status** com exatamente tres colunas: `A fazer`, `Fazendo`, `Feito`.
3. Vincule o quadro ao repositorio.

## 5. Issues das historias (ES1)

1. Crie uma issue por historia de `DOCS/historias-de-usuario.md`, usando o modelo "Historia de usuario".
2. Defina um responsavel em cada issue.
3. Adicione todas ao quadro, na coluna `A fazer`.
4. Marque os criterios de aceite como concluidos somente depois de testar no app.

## 6. Pasta DOCS na entrega

Ao final, a pasta `DOCS/` deve conter:

- Slides em PDF (os mesmos da apresentacao)
- Checklist CORE em Word (.docx)
- Prints das telas em `DOCS/prints/`

## 7. APK de debug

Gere com `./gradlew assembleDebug` e anexe a uma **Release** do repositorio. O arquivo fica em `app/build/outputs/apk/debug/`.
