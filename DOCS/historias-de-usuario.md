# Historias de usuario - BIP! Chamados de Manutencao

Cada requisito CORE do Projeto 01 (C1 a C6) e do nucleo comum (NC1 a NC8) vira uma historia. Cada historia vira uma issue no GitHub Projects, com responsavel, uma branch propria e um pull request que a fecha.

Perfis: **Operador** e **Tecnico de manutencao**.

## Resumo

| ID | Origem | Perfil | Titulo |
|----|--------|--------|--------|
| US-01 | C1 | Tecnico | Cadastrar maquina e gerar QR |
| US-02 | C2 | Operador | Abrir chamado lendo o QR da maquina |
| US-03 | C3 | Tecnico | Ver fila de chamados em tempo real |
| US-04 | C4 | Tecnico | Assumir chamado |
| US-05 | C5 | Tecnico | Encerrar chamado com a solucao |
| US-06 | C6 | Tecnico | Consultar historico da maquina |
| US-07 | NC1 | Ambos | Cadastro, login, sessao e sair |
| US-08 | NC3, NC4 | Ambos | Perfil definido no console e regras de seguranca |
| US-09 | NC5 | Ambos | Listas com estado vazio |
| US-10 | NC6, NC7 | Ambos | Formularios validados e rotacao de tela |
| US-11 | BIP! | Operador | Acompanhar previsao de chegada do tecnico |
| US-12 | BIP! | Operador | Assinar encerramento com QR da matricula |
| US-13 | BIP! | Tecnico | Ser avisado de chamado aberto |
| US-14 | BIP! | Tecnico | Ver habilidades e areas exigidas pelo chamado |
| US-15 | NC8 | Grupo | Repositorio e README completos |

As historias US-11 a US-14 vem da descricao do aplicativo BIP! e ultrapassam o CORE do professor. Entram como complementares: so devem ser iniciadas depois que US-01 a US-10 estiverem funcionando, inclusive com as regras do Firestore publicadas.

---

## US-01 - Cadastrar maquina e gerar QR (C1)

Como tecnico de manutencao, quero cadastrar uma maquina com codigo, nome e setor para que o operador possa identifica-la por um QR impresso.

- **Dado** que informei codigo, nome e setor, **quando** toco em Salvar, **entao** a maquina aparece na lista e o app mostra um QR com o codigo dela.
- **Dado** que deixei algum campo vazio, **quando** toco em Salvar, **entao** o app mostra mensagem em portugues no campo e nada e gravado.
- **Dado** que informei um codigo ja cadastrado, **quando** toco em Salvar, **entao** o app avisa que o codigo ja existe e nao sobrescreve a maquina.

## US-02 - Abrir chamado lendo o QR da maquina (C2)

Como operador, quero ler o QR da maquina e abrir um chamado com descricao e prioridade para avisar a manutencao sem depender de telefone ou caderno.

- **Dado** um QR de maquina cadastrada, **quando** leio com a camera, **entao** o formulario de chamado abre com a maquina ja preenchida.
- **Dado** um QR que nao corresponde a nenhuma maquina, **quando** leio, **entao** o app informa que a maquina nao foi encontrada e nao abre o formulario.
- **Dado** o formulario preenchido com descricao e prioridade (baixa, media ou alta), **quando** toco em Abrir chamado, **entao** o chamado e gravado com status Aberto, a data de abertura e o meu nome.
- **Dado** que a descricao esta vazia ou a prioridade nao foi escolhida, **quando** toco em Abrir chamado, **entao** o app mostra mensagem em portugues e nada e gravado.

## US-03 - Ver fila de chamados em tempo real (C3)

Como tecnico de manutencao, quero ver a fila de chamados abertos ordenada por prioridade e horario para atender primeiro o que e mais urgente.

- **Dado** chamados abertos com prioridades diferentes, **quando** abro a fila, **entao** os de prioridade alta aparecem primeiro e, dentro da mesma prioridade, os mais antigos primeiro.
- **Dado** que estou com a fila aberta, **quando** um operador abre um chamado em outro aparelho, **entao** o chamado aparece na minha tela sem eu recarregar.
- **Dado** que nao ha chamados abertos, **quando** abro a fila, **entao** o app mostra a tela de estado vazio.

## US-04 - Assumir chamado (C4)

Como tecnico de manutencao, quero assumir um chamado aberto para que outro tecnico nao va atender a mesma maquina.

- **Dado** um chamado com status Aberto, **quando** toco em Assumir, **entao** o status muda para Em atendimento com o meu nome.
- **Dado** um chamado ja assumido por outro tecnico, **quando** toco em Assumir, **entao** o app mostra que ele ja esta em atendimento e nada muda.
- **Dado** dois tecnicos tocando em Assumir quase ao mesmo tempo, **quando** as duas operacoes chegam ao Firestore, **entao** apenas uma e aceita (transacao) e a outra recebe o aviso.

## US-05 - Encerrar chamado com a solucao (C5)

Como tecnico de manutencao, quero encerrar o chamado descrevendo o que foi feito para registrar a solucao e o tempo de parada.

- **Dado** um chamado Em atendimento por mim, **quando** escrevo a solucao e toco em Encerrar, **entao** o status muda para Encerrado e o app grava a data de fechamento ao lado da data de abertura.
- **Dado** que o campo de solucao esta vazio, **quando** toco em Encerrar, **entao** o app mostra mensagem em portugues e o chamado continua Em atendimento.
- **Dado** um chamado assumido por outro tecnico, **quando** tento encerra-lo, **entao** o app nao permite.

## US-06 - Consultar historico da maquina (C6)

Como tecnico de manutencao, quero ver todos os chamados de uma maquina com a duracao de cada reparo para saber quantas vezes ela parou e por quanto tempo.

- **Dado** uma maquina com chamados encerrados, **quando** abro o historico, **entao** vejo cada chamado com descricao, solucao e a duracao entre abertura e fechamento.
- **Dado** um chamado ainda nao encerrado, **quando** abro o historico, **entao** ele aparece sinalizado como em andamento, sem duracao final.
- **Dado** uma maquina sem chamados, **quando** abro o historico, **entao** o app mostra o estado vazio.

## US-07 - Cadastro, login, sessao e sair (NC1)

Como usuario, quero me cadastrar e entrar com e-mail e senha para acessar o app com a minha conta.

- **Dado** e-mail valido e senha aceita, **quando** toco em Cadastrar, **entao** a conta e criada no Firebase Authentication e eu sigo para o app.
- **Dado** credenciais incorretas, **quando** toco em Entrar, **entao** o app mostra mensagem em portugues e permanece na tela de login.
- **Dado** que estou logado, **quando** fecho e reabro o app, **entao** continuo logado.
- **Dado** que estou logado, **quando** toco em Sair no menu, **entao** a sessao termina e volto ao login.

## US-08 - Perfil definido no console e regras de seguranca (NC3, NC4)

Como grupo, queremos que o perfil venha de `usuarios/{uid}` definido no console, com regras de seguranca, para que ninguem escolha ou altere o proprio perfil.

- **Dado** um usuario com `perfil = operador`, **quando** entra, **entao** ve apenas as telas do operador.
- **Dado** um usuario com `perfil = tecnico`, **quando** entra, **entao** ve apenas as telas do tecnico.
- **Dado** um operador autenticado, **quando** tenta assumir ou encerrar um chamado direto no Firestore, **entao** as regras recusam a operacao.
- **Dado** qualquer usuario, **quando** tenta gravar ou alterar o campo `perfil` do proprio documento, **entao** as regras recusam a operacao.

## US-09 - Listas com estado vazio (NC5)

Como usuario, quero que as listas mostrem uma mensagem clara quando nao houver dados para saber que o app esta funcionando.

- **Dado** uma lista sem itens (maquinas, fila ou historico), **quando** a tela abre, **entao** aparece o estado vazio com texto e icone, e nao uma tela em branco.
- **Dado** uma lista que passa a ter itens, **quando** o primeiro e gravado, **entao** o estado vazio some e a lista aparece.

## US-10 - Formularios validados e rotacao de tela (NC6, NC7)

Como usuario, quero que os formularios avisem erros em portugues e preservem o que digitei ao girar a tela.

- **Dado** qualquer formulario, **quando** envio com campo vazio ou numero invalido, **entao** o app mostra mensagem em portugues no campo e nao encerra de forma inesperada.
- **Dado** que digitei parte de um formulario, **quando** giro a tela, **entao** os valores digitados continuam nos campos.

## US-11 - Acompanhar previsao de chegada do tecnico (BIP!)

Como operador, quero ver a previsao de chegada do tecnico ao meu chamado para saber quando a maquina sera atendida.

- **Dado** um chamado aberto por mim e ainda nao assumido, **quando** abro o acompanhamento, **entao** vejo o status Aguardando tecnico.
- **Dado** que um tecnico assumiu o meu chamado, **quando** a tela esta aberta, **entao** ela mostra o nome do tecnico e a previsao de chegada, atualizadas sem recarregar.

## US-12 - Assinar encerramento com QR da matricula (BIP!)

Como operador, quero confirmar o encerramento lendo o QR da minha matricula para registrar que o servico foi entregue e aceito.

- **Dado** um chamado encerrado pelo tecnico, **quando** o operador que o abriu le o QR da propria matricula, **entao** o chamado registra a assinatura com matricula e horario.
- **Dado** que o QR lido pertence a outra matricula, **quando** tento assinar, **entao** o app recusa e o chamado nao recebe assinatura.

## US-13 - Ser avisado de chamado aberto (BIP!)

Como tecnico de manutencao, quero receber um aviso quando um chamado for aberto para reagir sem precisar ficar olhando a fila.

- **Dado** que o app esta aberto, **quando** um chamado novo e criado, **entao** recebo um aviso visivel com maquina e prioridade.
- **Dado** que o aviso apareceu, **quando** toco nele, **entao** abro o detalhe do chamado.

## US-14 - Ver habilidades e areas exigidas pelo chamado (BIP!)

Como tecnico de manutencao, quero ver as habilidades e areas de manutencao exigidas por um chamado para saber se sou a pessoa indicada antes de assumir.

- **Dado** um chamado com area e habilidades informadas, **quando** abro o detalhe, **entao** vejo a area (por exemplo mecanica, eletrica ou hidraulica) e as habilidades listadas.
- **Dado** um chamado sem essas informacoes, **quando** abro o detalhe, **entao** o app indica que nao foram informadas.

## US-15 - Repositorio e README completos (NC8)

Como grupo, queremos um repositorio documentado para que qualquer pessoa consiga configurar e testar o app.

- **Dado** o README, **quando** um colega o le, **entao** encontra integrantes, descricao do problema, prints das telas, passos para configurar o Firebase e as contas de teste de cada perfil.
- **Dado** o repositorio, **quando** inspecionado, **entao** o `google-services.json` nao esta versionado.
