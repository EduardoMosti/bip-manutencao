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
 * US-06 (C6) - Consulta do histórico de chamados de UMA máquina.
 *
 * Ficou numa classe separada do ChamadoRepository para não mexer no arquivo
 * de outras histórias e evitar conflito de merge.
 */
public class HistoricoRepository {

    private static final String COLECAO_CHAMADOS = "chamados";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Fica "ouvindo" todos os chamados da máquina (snapshot listener - NC2):
     * abertos, em atendimento e encerrados. Se um chamado for encerrado em
     * outro aparelho, a duração aparece aqui sem recarregar a tela.
     *
     * Ordem: o mais recente primeiro. A ordenação é feita aqui no código porque
     * whereEqualTo + orderBy em campos diferentes exigiria criar um índice no console.
     *
     * @param codigoMaquina código da máquina (é o maquinaId gravado no chamado)
     * @return o registro do listener. A Activity DEVE chamar remove() no onStop.
     */
    public ListenerRegistration ouvirHistorico(String codigoMaquina,
                                               ChamadoRepository.OnChamadosListener callback) {
        return db.collection(COLECAO_CHAMADOS)
                .whereEqualTo("maquinaId", codigoMaquina)
                .addSnapshotListener((snapshot, erro) -> {
                    if (erro != null || snapshot == null) {
                        callback.onErro(erro);
                        return;
                    }
                    List<Chamado> lista = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        // ESTIMATE: se o horário do servidor ainda não chegou, usa uma estimativa
                        Chamado chamado = doc.toObject(Chamado.class,
                                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE);
                        if (chamado != null) {
                            lista.add(chamado);
                        }
                    }
                    // Mais recente primeiro (sem data vai para o fim)
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
