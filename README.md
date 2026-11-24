# BIP! - Chamados de Manutenção

Aplicativo Android (Java) para abertura, fila e atendimento de chamados de manutenção no chão de fábrica. Projeto 01 da atividade "TechInova: apps para o chão de fábrica", disciplina de Desenvolvimento de Aplicativos Móveis.

## Integrantes

| Nome | Responsabilidade principal | GitHub |
|------|----------------------------|--------|
| Eduardo Mosti | Login e sessão, abertura de chamado pelo QR, validações e rotação de tela, documentação | [@EduardoMosti](https://github.com/EduardoMosti) |
| Gabriel Hausmann | Fila em tempo real, assumir e encerrar chamado, perfis e regras de segurança | [@gabHausmann](https://github.com/gabHausmann) |
| Helio do Nascimento Filho | Histórico da máquina, estados vazios, previsão de chegada, habilidades exigidas e visual do app | [@Helinho7](https://github.com/Helinho7) |
| Robson Schram | Cadastro de máquina com QR, assinatura do encerramento pelo QR da matrícula, aviso de chamado aberto | [@robsonschram](https://github.com/robsonschram) |

A divisão completa, história por história, está no quadro do projeto (veja a seção Engenharia de software).

## O problema

Quando uma máquina para, o operador avisa o líder, o líder liga para a manutenção e alguém anota num caderno. No fim do mês ninguém sabe dizer quantas vezes a injetora 3 quebrou nem quanto tempo ela ficou parada.

O BIP! elimina esse intermediário: o operador lê o QR da máquina e abre o chamado em segundos, o técnico vê a fila em tempo real e assume o atendimento, e cada chamado fica registrado com data de abertura, data de fechamento e solução aplicada, formando o histórico da máquina.

## Perfis de usuário

| Perfil | O que faz |
|--------|-----------|
| Operador | Lê o QR da máquina, abre o chamado com descrição e prioridade, acompanha a previsão de chegada do técnico e assina o encerramento. |
| Técnico de manutenção | Recebe aviso de chamado aberto, vê a fila ordenada por prioridade, consulta habilidades e áreas exigidas, assume e encerra o chamado, consulta o histórico da máquina. |

O perfil é definido pelo grupo no console do Firebase (documento `usuarios/{uid}`, campo `perfil`). O usuário nunca escolhe o próprio perfil no app.

## Telas

> Inserir os prints em `DOCS/prints/` e referenciar aqui antes da entrega.

| Tela | Print |
|------|-------|
| Login e cadastro | `DOCS/prints/login.png` |
| Operador: leitura de QR | `DOCS/prints/operador-scan.png` |
| Operador: abertura de chamado | `DOCS/prints/operador-abrir-chamado.png` |
| Técnico: fila de chamados | `DOCS/prints/tecnico-fila.png` |
| Técnico: detalhe e encerramento | `DOCS/prints/tecnico-detalhe.png` |
| Histórico da máquina | `DOCS/prints/historico-maquina.png` |

## Tecnologias

- Android nativo em Java (Android Studio), minSdk 26
- Firebase Authentication (e-mail e senha)
- Cloud Firestore (snapshot listeners e transações)
- Cloudinary (envio de imagens)
- ZXing Android Embedded (leitura de QR Code)
- RecyclerView e Material Components

## Identidade visual

- Amarelo de Alerta Industrial `#FFC107`: botões principais de leitura e ação
- Cinza Grafite Escuro `#212121`: estrutura das telas

## Como configurar o Firebase

O arquivo `google-services.json` NÃO está no repositório (está no `.gitignore`). Quem for rodar o app deve receber o arquivo do grupo por canal privado.

Se for criar um projeto Firebase novo:

1. Acesse o [console do Firebase](https://console.firebase.google.com) e crie o projeto.
2. Adicione um app Android com o `applicationId` definido em `app/build.gradle` (`com.bip.manutencao`).
3. Baixe o `google-services.json` e coloque em `app/google-services.json`.
4. Em **Authentication > Método de login**, ative **E-mail/senha**.
5. Em **Firestore Database**, crie o banco em modo produção.
6. Publique as regras do arquivo [`firestore.rules`](firestore.rules) (console, aba Regras).
7. Crie as contas de teste (seção abaixo) e, para cada uma, crie manualmente no Firestore o documento `usuarios/{uid}` com o campo `perfil` igual a `operador` ou `tecnico`.
8. Cadastre as máquinas de exemplo (seção abaixo).

## Como configurar o Cloudinary

1. Crie uma conta no [Cloudinary](https://cloudinary.com) e anote o **cloud name**.
2. Em Settings > Upload, crie um **upload preset** do tipo *unsigned*.
3. Crie o arquivo `local.properties` na raiz (ele não é versionado) e adicione:

```
cloudinary.cloudName=SEU_CLOUD_NAME
cloudinary.uploadPreset=SEU_UPLOAD_PRESET
```

## Como executar

1. Clone o repositório e abra a pasta no Android Studio.
2. Coloque o `google-services.json` em `app/` e complete a configuração do Cloudinary acima.
3. Aguarde a sincronização do Gradle.
4. Execute em um emulador ou aparelho com Android 8.0 (API 26) ou superior. A leitura de QR exige câmera. No emulador, use um dispositivo Pixel com imagem "Google Play" (API 34 ou superior).
5. O APK de debug da última versão está na aba **Releases**.

## Contas de teste

| Perfil | E-mail | Senha |
|--------|--------|-------|
| Operador | operador@example.com | Bip@2026 |
| Técnico de manutenção | tecnico@example.com | Bip@2026 |

Contas criadas só para demonstração, sem dados reais.

## Máquinas de exemplo

O QR Code de cada máquina guarda o código dela, que é o ID do documento em `maquinas`.

| Código (conteúdo do QR) | Nome | Setor |
|-------------------------|------|-------|
| MAQ-001 | Injetora 3 | Moldagem |
| MAQ-002 | Prensa Hidráulica 1 | Estamparia |
| MAQ-003 | Esteira de Embalagem | Embalagem |

## Modelo de dados (Firestore)

```
usuarios/{uid}        nome, email, perfil, matricula
maquinas/{codigo}     nome, setor
chamados/{id}         maquinaId, descricao, prioridade, status, abertoPor,
                      tecnicoUid, tecnicoNome, abertoEm, assumidoEm,
                      fechadoEm, solucao
```

Status do chamado: `aberto`, `em_atendimento`, `encerrado`. Prioridade: `baixa`, `media`, `alta`.

## Engenharia de software

- Histórias de usuário com critérios de aceite: [`DOCS/historias-de-usuario.md`](DOCS/historias-de-usuario.md)
- Quadro do GitHub Projects: [BIP! no GitHub Projects](https://github.com/users/EduardoMosti/projects/2)
- Fluxo de trabalho (branch, pull request e revisão): [`CONTRIBUTING.md`](CONTRIBUTING.md)
- Checklist CORE: pasta `DOCS/`

## Entregáveis

| Item | Onde |
|------|------|
| Código e README | Este repositório |
| APK de debug | Aba Releases |
| Slides em PDF | `DOCS/` |
| Checklist CORE (Word) | `DOCS/` |
| Vídeo da demonstração | [adicionar link não listado] |
