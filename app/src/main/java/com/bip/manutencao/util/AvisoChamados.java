package com.bip.manutencao.util;

import android.app.Activity;
import android.util.Log;
import android.view.View;

import com.bip.manutencao.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * US-13 - Avisa o TÉCNICO quando um chamado novo é aberto (com o app aberto).
 *
 * Como funciona:
 *  1) Fica "ouvindo" a coleção chamados com status == "aberto" (snapshot listener).
 *  2) A primeira leitura só registra o que JÁ existia: não gera aviso
 *     (senão o técnico receberia um aviso para cada chamado antigo ao abrir a tela).
 *  3) Nas leituras seguintes, getDocumentChanges() diz O QUE mudou.
 *     Cada documento do tipo ADDED é um chamado novo -> mostra uma Snackbar
 *     "Novo chamado: MAQ-001 · prioridade ALTA" com o botão "Ver".
 *
 * Por que Snackbar e não notificação push?
 *  A história pede o aviso "com o app aberto". Notificação push exigiria
 *  permissão em tempo de execução (POST_NOTIFICATIONS) e um servidor enviando
 *  a mensagem - conteúdo que o curso ainda não cobriu.
 *
 * Por que lê os campos direto do DocumentSnapshot (sem classe Chamado)?
 *  O modelo Chamado é da US-02 (Eduardo). Criar outra classe com o mesmo nome
 *  daria conflito de merge. Quando a dele existir, dá para trocar por toObject().
 *
 * Uso (em qualquer tela do técnico):
 *   aviso = new AvisoChamados(this, findViewById(R.id.main), fab);
 *   onStart -> aviso.iniciar();   onStop -> aviso.parar();
 *
 * ATENÇÃO: só use em telas do TÉCNICO. Para o operador, as regras do Firestore
 * só liberam os chamados abertos por ele, e esta consulta seria recusada.
 */
public class AvisoChamados {

    private static final String TAG = "AvisoChamados";

    /** Quem quiser decidir o que o botão "Ver" faz (ex.: abrir a tela de detalhe do Gabriel). */
    public interface OnVerChamadoListener {
        void onVerChamado(DocumentSnapshot chamado);
    }

    private final Activity activity;
    private final View raiz;   // View onde a Snackbar aparece (normalmente o layout raiz)
    private final View ancora; // a Snackbar aparece ACIMA desta View (ex.: o FAB). Pode ser null

    private OnVerChamadoListener listenerVer; // null = mostra o detalhe num diálogo
    private ListenerRegistration registro;    // para parar de ouvir no onStop
    private boolean primeiraLeitura;          // true até receber a 1ª resposta do servidor

    public AvisoChamados(Activity activity, View raiz, View ancora) {
        this.activity = activity;
        this.raiz = raiz;
        this.ancora = ancora;
    }

    public void setOnVerChamadoListener(OnVerChamadoListener listener) {
        this.listenerVer = listener;
    }

    /** Começa a ouvir. Chamar no onStart da tela. */
    public void iniciar() {
        if (registro != null) {
            return; // já está ouvindo
        }
        primeiraLeitura = true;

        registro = FirebaseFirestore.getInstance()
                .collection("chamados")
                .whereEqualTo("status", "aberto")
                // INCLUDE: também avisa quando os dados passam do cache para o servidor.
                // Usamos isso para saber quando a "foto inicial" do servidor chegou.
                .addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, erro) -> {
                    if (erro != null || snapshot == null) {
                        Log.e(TAG, "Erro ao ouvir chamados abertos", erro);
                        return;
                    }

                    // Leitura inicial: só guarda a situação atual, sem avisar.
                    // A primeira resposta pode vir do cache do celular; esperamos a do servidor.
                    if (primeiraLeitura) {
                        if (!snapshot.getMetadata().isFromCache()) {
                            primeiraLeitura = false;
                        }
                        return;
                    }

                    // Daqui em diante: separa só os chamados que ENTRARAM na lista
                    List<DocumentSnapshot> novos = new ArrayList<>();
                    for (DocumentChange mudanca : snapshot.getDocumentChanges()) {
                        if (mudanca.getType() == DocumentChange.Type.ADDED) {
                            novos.add(mudanca.getDocument());
                        }
                    }
                    if (!novos.isEmpty()) {
                        mostrarAviso(novos);
                    }
                });
    }

    /** Para de ouvir. Chamar no onStop da tela (economiza bateria e dados). */
    public void parar() {
        if (registro != null) {
            registro.remove();
            registro = null;
        }
    }

    /** Mostra a Snackbar com máquina e prioridade, e o botão "Ver". */
    private void mostrarAviso(List<DocumentSnapshot> novos) {
        if (activity.isFinishing()) {
            return;
        }

        // O mais recente é o último da lista; é ele que o botão "Ver" abre
        DocumentSnapshot ultimo = novos.get(novos.size() - 1);

        String texto;
        if (novos.size() == 1) {
            texto = activity.getString(R.string.aviso_novo_chamado,
                    maquina(ultimo), prioridade(ultimo));
        } else {
            texto = activity.getString(R.string.aviso_novos_chamados, novos.size());
        }

        Snackbar snackbar = Snackbar.make(raiz, texto, Snackbar.LENGTH_LONG);
        snackbar.setDuration(8000); // 8 s: tempo para o técnico ler, mesmo de longe
        if (ancora != null) {
            snackbar.setAnchorView(ancora); // não fica por cima do botão "+"
        }
        snackbar.setAction(R.string.aviso_ver, v -> ver(ultimo));
        snackbar.show();
    }

    /** Botão "Ver": usa o listener da tela, ou mostra um diálogo com o detalhe. */
    private void ver(DocumentSnapshot chamado) {
        if (listenerVer != null) {
            listenerVer.onVerChamado(chamado);
            return;
        }
        if (activity.isFinishing()) {
            return;
        }
        String descricao = chamado.getString("descricao");
        new MaterialAlertDialogBuilder(activity)
                .setTitle(activity.getString(R.string.aviso_detalhe_titulo, maquina(chamado)))
                .setMessage(activity.getString(R.string.aviso_detalhe_mensagem,
                        prioridade(chamado),
                        descricao != null ? descricao : "-",
                        abertoEm(chamado)))
                .setPositiveButton(R.string.aviso_ok, null)
                .show();
    }

    // ---------- Leitura segura dos campos (nunca devolve null) ----------

    private String maquina(DocumentSnapshot c) {
        String id = c.getString("maquinaId");
        return id != null ? id : activity.getString(R.string.aviso_nao_informado);
    }

    private String prioridade(DocumentSnapshot c) {
        String p = c.getString("prioridade"); // "baixa", "media" ou "alta"
        return p != null ? p.toUpperCase(Locale.ROOT)
                : activity.getString(R.string.aviso_nao_informado);
    }

    private String abertoEm(DocumentSnapshot c) {
        Timestamp t = c.getTimestamp("abertoEm"); // pode ser null logo após a gravação
        if (t == null) {
            return activity.getString(R.string.aviso_agora);
        }
        return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR"))
                .format(t.toDate());
    }
}
