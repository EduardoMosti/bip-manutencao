# BIP! - Chamados de Manutencao

Aplicativo Android (Java) para abertura, fila e atendimento de chamados de manutencao no chao de fabrica. Projeto 01 da atividade "TechInova: apps para o chao de fabrica", disciplina de Desenvolvimento de Aplicativos Moveis.

## Integrantes

| Nome | Responsabilidade principal | GitHub |
|------|----------------------------|--------|
| [Nome do integrante 1] | [ex.: autenticacao e perfis] | [@usuario] |
| [Nome do integrante 2] | [ex.: leitura de QR e abertura de chamado] | [@usuario] |
| [Nome do integrante 3] | [ex.: fila do tecnico e transacao] | [@usuario] |
| [Nome do integrante 4] | [ex.: historico, regras do Firestore e UI] | [@usuario] |

## O problema

Quando uma maquina para, o operador avisa o lider, o lider liga para a manutencao e alguem anota num caderno. No fim do mes ninguem sabe dizer quantas vezes a injetora 3 quebrou nem quanto tempo ela ficou parada.

O BIP! elimina esse intermediario: o operador le o QR da maquina e abre o chamado em segundos, o tecnico ve a fila em tempo real e assume o atendimento, e cada chamado fica registrado com data de abertura, data de fechamento e solucao aplicada, formando o historico da maquina.

## Perfis de usuario

| Perfil | O que faz |
|--------|-----------|
| Operador | Le o QR da maquina, abre o chamado com descricao e prioridade, acompanha a previsao de chegada do tecnico e assina o encerramento. |
| Tecnico de manutencao | Recebe aviso de chamado aberto, ve a fila ordenada por prioridade, consulta habilidades e areas exigidas, assume e encerra o chamado, consulta o historico da maquina. |

O perfil e definido pelo grupo no console do Firebase (documento `usuarios/{uid}`, campo `perfil`). O usuario nunca escolhe o proprio perfil no app.

## Telas

> Inserir os prints em `DOCS/prints/` e referenciar aqui antes da entrega.

| Tela | Print |
|------|-------|
| Login e cadastro | `DOCS/prints/login.png` |
| Operador: leitura de QR | `DOCS/prints/operador-scan.png` |
| Operador: abertura de chamado | `DOCS/prints/operador-abrir-chamado.png` |
| Tecnico: fila de chamados | `DOCS/prints/tecnico-fila.png` |
| Tecnico: detalhe e encerramento | `DOCS/prints/tecnico-detalhe.png` |
| Historico da maquina | `DOCS/prints/historico-maquina.png` |

## Tecnologias

- Android nativo em Java (Android Studio)
- Firebase Authentication (e-mail e senha)
- Cloud Firestore (snapshot listeners e transacoes)
- Cloudinary (envio de imagens)
- ZXing / ZXing Android Embedded (leitura e geracao de QR Code)
- RecyclerView e Material Components

## Identidade visual

- Amarelo de Alerta Industrial `#FFC107`: botoes principais de leitura e acao
- Cinza Grafite Escuro `#212121`: estrutura das telas

## Como configurar o Firebase

O arquivo `google-services.json` NAO esta no repositorio (esta no `.gitignore`). Cada integrante deve gerar o proprio ou receber o do grupo por canal privado.

1. Acesse o [console do Firebase](https://console.firebase.google.com) e crie (ou abra) o projeto do grupo.
2. Adicione um app Android com o `applicationId` definido em `app/build.gradle` e cadastre a impressao digital SHA-1 de cada integrante (`./gradlew signingReport`).
3. Baixe o `google-services.json` e coloque em `app/google-services.json`.
4. Em **Authentication > Metodo de login**, ative **E-mail/senha**.
5. Em **Firestore Database**, crie o banco em modo producao.
6. Publique as regras do arquivo `firestore.rules` (console, aba Regras).
7. Crie as contas de teste (secao abaixo) e, para cada uma, crie manualmente no Firestore o documento `usuarios/{uid}` com o campo `perfil` igual a `operador` ou `tecnico`.

## Como configurar o Cloudinary

1. Crie uma conta no [Cloudinary](https://cloudinary.com) e anote o **cloud name**.
2. Em Settings > Upload, crie um **upload preset** do tipo *unsigned*.
3. Crie o arquivo `local.properties` na raiz (ele nao e versionado) e adicione:

```
cloudinary.cloudName=SEU_CLOUD_NAME
cloudinary.uploadPreset=SEU_UPLOAD_PRESET
```

## Como executar

1. Clone o repositorio e abra a pasta no Android Studio.
2. Complete a configuracao do Firebase e do Cloudinary acima.
3. Aguarde a sincronizacao do Gradle.
4. Execute em um emulador ou aparelho com Android 8.0 (API 26) ou superior. A leitura de QR exige camera.
5. O APK de debug da ultima versao esta na aba **Releases**.

## Contas de teste

> Preencher com as contas reais criadas no Firebase do grupo.

| Perfil | E-mail | Senha |
|--------|--------|-------|
| Operador | [operador@exemplo.com] | [definir] |
| Tecnico de manutencao | [tecnico@exemplo.com] | [definir] |

## Modelo de dados (Firestore)

```
usuarios/{uid}        nome, email, perfil, matricula
maquinas/{codigo}     nome, setor
chamados/{id}         maquinaId, descricao, prioridade, status, abertoPor,
                      tecnicoUid, tecnicoNome, abertoEm, fechadoEm, solucao
```

Status do chamado: `aberto`, `em_atendimento`, `encerrado`. Prioridade: `baixa`, `media`, `alta`.

## Engenharia de software

- Historias de usuario com criterios de aceite: [`DOCS/historias-de-usuario.md`](DOCS/historias-de-usuario.md)
- Quadro do GitHub Projects: [adicionar link do quadro]
- Fluxo de trabalho (branch, pull request e revisao): [`CONTRIBUTING.md`](CONTRIBUTING.md)
- Checklist CORE: pasta `DOCS/`

## Entregaveis

| Item | Onde |
|------|------|
| Codigo e README | Este repositorio |
| APK de debug | Aba Releases |
| Slides em PDF | `DOCS/` |
| Checklist CORE (Word) | `DOCS/` |
| Video da demonstracao | [adicionar link nao listado] |
