package com.bip.manutencao.data;

import com.bip.manutencao.model.Chamado;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository: a classe que conversa com a coleção "chamados" do Firestore (US-02 e US-03).
 *
 * Ela também tem a busca de máquina pelo código do QR. Ficou aqui, e não no
 * MaquinaRepository (US-01), para não mexer no arquivo de outra história
 * e evitar conflito de merge.
 *
 * Lembrete do curso: o Firestore é ASSÍNCRONO. O resultado só existe
 * dentro do listener da Task, nunca na linha seguinte.
 */
public class ChamadoRepository {

    private static final String COLECAO_CHAMADOS = "chamados";
    private static final String COLECAO_MAQUINAS = "maquinas";
    private static final String COLECAO_USUARIOS = "usuarios";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Busca uma máquina pelo código lido no QR (o código é o ID do documento).
     *
     * O resultado é um DocumentSnapshot que pode vir:
     *  - null: o código é inválido (vazio ou com "/", que o Firestore não aceita);
     *  - com exists() == false: não existe máquina com esse código.
     * Nos dois casos a tela deve avisar "máquina não encontrada".
     */
    public Task<DocumentSnapshot> buscarMaquina(String codigo) {
        if (codigo == null || codigo.trim().isEmpty() || codigo.contains("/")) {
            return Tasks.forResult(null);
        }
        return db.collection(COLECAO_MAQUINAS).document(codigo.trim()).get();
    }

    /**
     * Grava um chamado novo com status "aberto".
     *
     * O Chamado recebido precisa ter: maquinaId, maquinaNome, descricao,
     * prioridade e abertoPor (uid). Se abertoPorNome vier preenchido (ex.: o
     * e-mail), ele só é usado quando o nome não for encontrado em usuarios/{uid}.
     *
     * Por que serverTimestamp()? O horário vem do servidor do Firebase, igual
     * para todos os aparelhos, e não do relógio do celular, que pode estar errado.
     *
     * Por que o ID é automático (document() sem argumento)? Dois operadores podem
     * abrir chamado da mesma máquina; cada chamado precisa de um ID único.
     */
    public Task<Void> abrir(Chamado chamado) {
        DocumentReference usuarioRef =
                db.collection(COLECAO_USUARIOS).document(chamado.getAbertoPor());

        return usuarioRef.get().continueWithTask(tarefaUsuario -> {
            // 1) Descobre o nome de quem abre (o e-mail é o plano B)
            String nome = chamado.getAbertoPorNome();
            if (tarefaUsuario.isSuccessful() && tarefaUsuario.getResult() != null) {
                String nomeGravado = tarefaUsuario.getResult().getString("nome");
                if (nomeGravado != null && !nomeGravado.trim().isEmpty()) {
                    nome = nomeGravado;
                }
            }

            // 2) Monta só os campos que o operador grava
            Map<String, Object> dados = new HashMap<>();
            dados.put("maquinaId", chamado.getMaquinaId());
            dados.put("maquinaNome", chamado.getMaquinaNome());
            dados.put("descricao", chamado.getDescricao());
            dados.put("prioridade", chamado.getPrioridade());
            dados.put("status", Chamado.STATUS_ABERTO);
            dados.put("abertoPor", chamado.getAbertoPor());
            dados.put("abertoPorNome", nome);
            dados.put("abertoEm", FieldValue.serverTimestamp());

            // 3) Grava em chamados/{id automático}
            return db.collection(COLECAO_CHAMADOS).document().set(dados);
        });
    }

    // ------------------------------------------------------------------
    // US-03 (C3): fila de chamados abertos em tempo real
    // ------------------------------------------------------------------

    /** Interface de retorno da fila em tempo real (a Activity implementa). */
    public interface OnChamadosListener {
        void onLista(List<Chamado> chamados);
        void onErro(Exception erro);
    }

    /**
     * Fica "ouvindo" os chamados com status "aberto" (snapshot listener - NC2).
     * Quando um operador abre um chamado em outro aparelho, o callback roda de novo
     * com a lista atualizada, sem o técnico recarregar a tela.
     *
     * Ordem da fila: prioridade alta primeiro e, na mesma prioridade, o mais antigo
     * primeiro. A ordenação é feita aqui, no código, e não no Firestore, porque:
     *  - "prioridade" é texto, e em ordem alfabética "baixa" viria antes de "media";
     *  - ordenar por dois campos no servidor exigiria criar um índice no console.
     * Como a fila só tem chamados abertos, a lista é pequena e ordenar aqui é simples.
     *
     * @return o registro do listener. A Activity DEVE chamar remove() no onStop,
     *         senão o listener continua rodando com a tela fechada (vazamento).
     */
    public ListenerRegistration ouvirFila(OnChamadosListener callback) {
        return db.collection(COLECAO_CHAMADOS)
                .whereEqualTo("status", Chamado.STATUS_ABERTO)
                .addSnapshotListener((snapshot, erro) -> {
                    if (erro != null || snapshot == null) {
                        callback.onErro(erro);
                        return;
                    }
                    List<Chamado> fila = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        // ESTIMATE: se o horário do servidor ainda não chegou, usa uma estimativa
                        Chamado chamado = doc.toObject(Chamado.class,
                                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE);
                        if (chamado != null) {
                            fila.add(chamado);
                        }
                    }
                    Collections.sort(fila, COMPARADOR_FILA);
                    callback.onLista(fila);
                });
    }

    /** Alta (3) vem antes de média (2) e de baixa (1). */
    private static int pesoPrioridade(String prioridade) {
        if (Chamado.PRIORIDADE_ALTA.equals(prioridade)) {
            return 3;
        } else if (Chamado.PRIORIDADE_MEDIA.equals(prioridade)) {
            return 2;
        } else if (Chamado.PRIORIDADE_BAIXA.equals(prioridade)) {
            return 1;
        }
        return 0;
    }

    /** Horário de abertura em milissegundos; sem horário, vai para o fim da fila. */
    private static long milissegundos(Chamado chamado) {
        return chamado.getAbertoEm() == null
                ? Long.MAX_VALUE
                : chamado.getAbertoEm().toDate().getTime();
    }

    /** Critério da fila: maior prioridade primeiro; empatou, o mais antigo primeiro. */
    private static final Comparator<Chamado> COMPARADOR_FILA = (a, b) -> {
        int porPrioridade = Integer.compare(
                pesoPrioridade(b.getPrioridade()), pesoPrioridade(a.getPrioridade()));
        if (porPrioridade != 0) {
            return porPrioridade;
        }
        return Long.compare(milissegundos(a), milissegundos(b));
    };
}