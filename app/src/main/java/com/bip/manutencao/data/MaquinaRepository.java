package com.bip.manutencao.data;

import com.bip.manutencao.model.Maquina;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository: a ÚNICA classe que conversa com a coleção "maquinas" do Firestore.
 *
 * Por que separar? A Activity cuida da tela; o Repository cuida dos dados.
 * Se amanhã a coleção mudar de nome, só este arquivo muda.
 *
 * Lembrete do curso: o Firestore é ASSÍNCRONO. O resultado só existe
 * dentro do callback (listener) ou da Task — nunca na linha seguinte.
 */
public class MaquinaRepository {

    private static final String COLECAO = "maquinas";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /** Interface de retorno da lista em tempo real (a Activity implementa). */
    public interface OnMaquinasListener {
        void onLista(List<Maquina> maquinas);
        void onErro(Exception erro);
    }

    /**
     * Fica "ouvindo" a coleção maquinas (snapshot listener - NC2).
     * Toda vez que alguém grava/altera uma máquina, em qualquer aparelho,
     * o callback roda de novo com a lista atualizada.
     *
     * @return o registro do listener. A Activity DEVE chamar remove() no onStop,
     *         senão o listener continua rodando com a tela fechada (vazamento).
     */
    public ListenerRegistration ouvirMaquinas(OnMaquinasListener callback) {
        return db.collection(COLECAO)
                .orderBy("nome") // lista em ordem alfabética pelo nome
                .addSnapshotListener((snapshot, erro) -> {
                    if (erro != null || snapshot == null) {
                        callback.onErro(erro);
                        return;
                    }
                    List<Maquina> lista = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Maquina m = doc.toObject(Maquina.class); // converte o documento em objeto
                        if (m != null) {
                            m.setCodigo(doc.getId()); // o código é o ID do documento
                            lista.add(m);
                        }
                    }
                    callback.onLista(lista);
                });
    }

    /**
     * Cadastra uma máquina SEM sobrescrever outra com o mesmo código.
     *
     * Por que transação (runTransaction)?
     * Um set() simples em maquinas/MAQ-001 SUBSTITUI a máquina se ela já existir.
     * Na transação, lemos o documento e gravamos numa operação só (atômica):
     * se ele já existe, abortamos com o erro ALREADY_EXISTS e nada é gravado.
     *
     * @return Task<Void>: a Activity coloca addOnSuccessListener / addOnFailureListener.
     */
    public Task<Void> cadastrar(Maquina maquina) {
        DocumentReference ref = db.collection(COLECAO).document(maquina.getCodigo());

        return db.runTransaction((Transaction.Function<Void>) transacao -> {
            // 1) Lê o documento dentro da transação
            DocumentSnapshot atual = transacao.get(ref);

            // 2) Já existe? Lança exceção -> a transação é cancelada e a Task falha
            if (atual.exists()) {
                throw new FirebaseFirestoreException(
                        "Código já cadastrado",
                        FirebaseFirestoreException.Code.ALREADY_EXISTS);
            }

            // 3) Não existe: grava só nome e setor (o código é o ID do documento)
            Map<String, Object> dados = new HashMap<>();
            dados.put("nome", maquina.getNome());
            dados.put("setor", maquina.getSetor());
            transacao.set(ref, dados);

            return null; // Void: não há valor para devolver
        });
    }
}
