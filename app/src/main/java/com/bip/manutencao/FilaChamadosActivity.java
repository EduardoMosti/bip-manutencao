package com.bip.manutencao;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bip.manutencao.adapter.ChamadoAdapter;
import com.bip.manutencao.data.ChamadoRepository;
import com.bip.manutencao.model.Chamado;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

/**
 * US-03 (C3) - Fila de chamados abertos do técnico, em tempo real.
 *
 * Critérios de aceite atendidos:
 *  - ordem: prioridade alta primeiro e, dentro da mesma prioridade, o mais antigo primeiro
 *    (a ordenação está no ChamadoRepository);
 *  - tempo real: um chamado aberto em outro aparelho aparece aqui sem recarregar;
 *  - estado vazio: sem chamados abertos, aparece a mensagem em vez de tela em branco.
 *
 * Mesmo padrão da MaquinasActivity: o listener começa no onStart e é removido no onStop.
 *
 * US-04 (C4) - Assumir chamado:
 *  - cada linha tem o botão "Assumir"; a tela pede confirmação e chama a transação;
 *  - deu certo: o chamado sai sozinho da fila (o listener só mostra os "aberto");
 *  - já assumido por outro técnico: mostra quem está atendendo e nada muda.
 * A tela implementa a interface do Adapter para receber o clique no botão.
 */
public class FilaChamadosActivity extends AppCompatActivity
        implements ChamadoAdapter.OnAssumirListener {

    private static final String TAG = "FilaChamados";

    /** Os 4 estados da tela (padrão do curso): só um aparece por vez. */
    private enum Estado { CARREGANDO, CONTEUDO, VAZIO, ERRO }

    private RecyclerView rvFila;
    private View progresso;
    private View layoutVazio;
    private TextView tvErro;

    private ChamadoAdapter adapter;
    private final ChamadoRepository repositorio = new ChamadoRepository();
    private ListenerRegistration registroListener; // para remover no onStop
    private boolean assumindo; // US-04: evita dois cliques seguidos enquanto grava

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_fila_chamados);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.fila_titulo);
        }

        rvFila = findViewById(R.id.rvFila);
        progresso = findViewById(R.id.progressoFila);
        layoutVazio = findViewById(R.id.layoutVazioFila);
        tvErro = findViewById(R.id.tvErroFila);

        adapter = new ChamadoAdapter();
        adapter.setOnAssumirListener(this); // US-04: o botão "Assumir" chama onAssumir()
        rvFila.setLayoutManager(new LinearLayoutManager(this));
        rvFila.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Segurança: sem usuário logado, não há o que mostrar (as regras recusariam).
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, R.string.erro_sem_login, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mostrar(Estado.CARREGANDO);

        // Começa a ouvir a fila. O callback roda agora e a cada mudança.
        registroListener = repositorio.ouvirFila(new ChamadoRepository.OnChamadosListener() {
            @Override
            public void onLista(List<Chamado> chamados) {
                adapter.atualizar(chamados);
                mostrar(chamados.isEmpty() ? Estado.VAZIO : Estado.CONTEUDO);
            }

            @Override
            public void onErro(Exception erro) {
                Log.e(TAG, "Erro ao ouvir a fila de chamados", erro);
                mostrar(Estado.ERRO);
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Para de ouvir quando a tela sai de vista: economiza bateria e dados.
        if (registroListener != null) {
            registroListener.remove();
            registroListener = null;
        }
    }

    /** Mostra só a View do estado pedido e esconde as outras. */
    private void mostrar(Estado estado) {
        progresso.setVisibility(estado == Estado.CARREGANDO ? View.VISIBLE : View.GONE);
        rvFila.setVisibility(estado == Estado.CONTEUDO ? View.VISIBLE : View.GONE);
        layoutVazio.setVisibility(estado == Estado.VAZIO ? View.VISIBLE : View.GONE);
        tvErro.setVisibility(estado == Estado.ERRO ? View.VISIBLE : View.GONE);
    }

    // ------------------------------------------------------------------
    // US-04 (C4): assumir chamado
    // ------------------------------------------------------------------

    /** Clique em "Assumir" numa linha (vem do Adapter): pede confirmação antes. */
    @Override
    public void onAssumir(Chamado chamado) {
        if (assumindo) {
            return; // já tem uma gravação em andamento
        }
        String maquina = getString(R.string.chamado_maquina_valor,
                texto(chamado.getMaquinaId()), texto(chamado.getMaquinaNome()));

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.assumir_confirmar_titulo)
                .setMessage(getString(R.string.assumir_confirmar_mensagem,
                        maquina, texto(chamado.getDescricao())))
                .setNegativeButton(R.string.assumir_cancelar, null)
                .setPositiveButton(R.string.assumir_botao, (dialogo, botao) -> assumir(chamado))
                .show();
    }

    /** Chama a transação do Repository e trata o resultado. */
    private void assumir(Chamado chamado) {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null || chamado.getId() == null) {
            Toast.makeText(this, R.string.erro_sem_login, Toast.LENGTH_LONG).show();
            return;
        }

        setAssumindo(true);
        // "this" nos listeners: são removidos sozinhos se a tela for destruída
        repositorio.assumir(chamado.getId(), usuario.getUid(), usuario.getEmail())
                .addOnSuccessListener(this, unused -> {
                    setAssumindo(false);
                    // Não precisa tirar da lista à mão: o listener da fila faz isso
                    Snackbar.make(findViewById(R.id.main),
                            R.string.assumir_sucesso, Snackbar.LENGTH_LONG).show();
                })
                .addOnFailureListener(this, erro -> {
                    setAssumindo(false);
                    tratarErroAssumir(erro);
                });
    }

    /** Traduz o erro técnico em mensagem em português. */
    private void tratarErroAssumir(Exception erro) {
        Log.e(TAG, "Falha ao assumir chamado", erro);
        String mensagem = getString(R.string.assumir_erro);

        if (erro instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException e = (FirebaseFirestoreException) erro;
            switch (e.getCode()) {
                case FAILED_PRECONDITION: {
                    // A mensagem da exceção traz o nome de quem já assumiu
                    String quem = e.getMessage();
                    mensagem = quem == null || quem.trim().isEmpty()
                            ? getString(R.string.assumir_ja_assumido_sem_nome)
                            : getString(R.string.assumir_ja_assumido, quem);
                    break;
                }
                case NOT_FOUND:
                    mensagem = getString(R.string.assumir_nao_encontrado);
                    break;
                case PERMISSION_DENIED:
                    mensagem = getString(R.string.assumir_sem_permissao);
                    break;
                default:
                    break;
            }
        }
        // Diálogo (e não Toast): o técnico precisa ler quem já está atendendo
        new MaterialAlertDialogBuilder(this)
                .setMessage(mensagem)
                .setPositiveButton(R.string.assumir_ok, null)
                .show();
    }

    /**
     * Liga/desliga o modo "assumindo": mostra o progresso e trava a rotação,
     * igual ao padrão do grupo (US-10), para não perder a resposta do Firestore.
     */
    private void setAssumindo(boolean valor) {
        assumindo = valor;
        progresso.setVisibility(valor ? View.VISIBLE : View.GONE);
        setRequestedOrientation(valor
                ? ActivityInfo.SCREEN_ORIENTATION_LOCKED
                : ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor;
    }

    /** Seta de voltar da Toolbar: fecha esta tela. */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}