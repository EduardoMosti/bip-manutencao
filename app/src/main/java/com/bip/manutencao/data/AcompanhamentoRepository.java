package com.bip.manutencao.data;

import com.bip.manutencao.model.Chamado;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * US-11 - Chamados abertos pelo próprio operador, em tempo real.
 *
 * A consulta filtra por abertoPor == uid porque a regra do Firestore só deixa o
 * operador ler os chamados que ele mesmo abriu. Sem esse filtro a consulta é recusada.
 *
 * Classe separada do ChamadoRepository para evitar conflito de merge.
 */
public class AcompanhamentoRepository {

    private static final String COLECAO_CHAMADOS = "chamados";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Fica "ouvindo" os chamados do operador (snapshot listener - NC2). Quando o
     * técnico assume, o nome dele e a previsão de chegada aparecem sem recarregar.
     * Ordem: o mais recente primeiro (ordenado aqui para não precisar de índice).
     *
     * @return o registro do listener. A Activity DEVE chamar remove() no onStop.
     */
    public ListenerRegistration ouvirMeusChamados(String operadorUid,
                                                  ChamadoRepository.OnChamadosListener callback) {
        return db.collection(COLECAO_CHAMADOS)
                .whereEqualTo("abertoPor", operadorUid)
                .addSnapshotListener((snapshot, erro) -> {
                    if (erro != null || snapshot == null) {
                        callback.onErro(erro);
                        return;
                    }
                    List<Chamado> lista = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Chamado chamado = doc.toObject(Chamado.class,
                                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE);
                        if (chamado != null) {
                            lista.add(chamado);
                        }
                    }
                    Collections.sort(lista, (a, b) -> comparar(b.getAbertoEm(), a.getAbertoEm()));
                    callback.onLista(lista);
                });
    }

    private static int comparar(Timestamp a, Timestamp b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        return a.compareTo(b);
    }
}
