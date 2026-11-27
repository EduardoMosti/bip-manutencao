package com.bip.manutencao;

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

import com.bip.manutencao.adapter.HistoricoAdapter;
import com.bip.manutencao.data.ChamadoRepository;
import com.bip.manutencao.data.HistoricoRepository;
import com.bip.manutencao.model.Chamado;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

/**
 * US-06 (C6) - Histórico de chamados de uma máquina, com a duração de cada reparo.
 *
 * Critérios de aceite atendidos:
 *  - máquina com chamados encerrados: cada chamado mostra descrição, solução e a
 *    duração entre abertura e fechamento;
 *  - chamado ainda não encerrado: aparece como "em andamento", sem duração final;
 *  - máquina sem chamados: mostra o estado vazio (US-09 / NC5).
 *
 * Recebe pela Intent:
 *  - EXTRA_CODIGO (obrigatório) código da máquina. Ex.: "MAQ-001"
 *  - EXTRA_NOME   (opcional)    nome da máquina. Ex.: "Injetora 3"
 *
 * Mesmo padrão da fila: o listener começa no onStart e é removido no onStop.
 */
public class HistoricoMaquinaActivity extends AppCompatActivity {

    public static final String EXTRA_CODIGO = "extra_codigo_maquina";
    public static final String EXTRA_NOME = "extra_nome_maquina";

    private static final String TAG = "HistoricoMaquina";

    /** Os 4 estados da tela (padrão do curso): só um aparece por vez. */
    private enum Estado { CARREGANDO, CONTEUDO, VAZIO, ERRO }

    private RecyclerView rvHistorico;
    private View progresso;
    private View layoutVazio;
    private TextView tvErro;
    private TextView tvTotal;

    private String codigoMaquina;
    private HistoricoAdapter adapter;
    private final HistoricoRepository repositorio = new HistoricoRepository();
    private ListenerRegistration registroListener; // para remover no onStop

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_historico_maquina);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });

        // 1) Lê a máquina recebida. Sem código não há o que buscar: avisa e fecha.
        codigoMaquina = getIntent().getStringExtra(EXTRA_CODIGO);
        String nomeMaquina = getIntent().getStringExtra(EXTRA_NOME);
        if (codigoMaquina == null || codigoMaquina.trim().isEmpty()) {
            Toast.makeText(this, R.string.historico_sem_maquina, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // 2) Toolbar: título fixo e a máquina no subtítulo ("MAQ-001 - Injetora 3")
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.historico_titulo);
            getSupportActionBar().setSubtitle(nomeMaquina == null || nomeMaquina.isEmpty()
                    ? codigoMaquina
                    : getString(R.string.chamado_maquina_valor, codigoMaquina, nomeMaquina));
        }

        rvHistorico = findViewById(R.id.rvHistorico);
        progresso = findViewById(R.id.progressoHistorico);
        layoutVazio = findViewById(R.id.layoutVazioHistorico);
        tvErro = findViewById(R.id.tvErroHistorico);
        tvTotal = findViewById(R.id.tvTotalHistorico);

        adapter = new HistoricoAdapter();
        rvHistorico.setLayoutManager(new LinearLayoutManager(this));
        rvHistorico.setAdapter(adapter);
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

        // Começa a ouvir o histórico. O callback roda agora e a cada mudança.
        registroListener = repositorio.ouvirHistorico(codigoMaquina,
                new ChamadoRepository.OnChamadosListener() {
                    @Override
                    public void onLista(List<Chamado> chamados) {
                        adapter.atualizar(chamados);
                        tvTotal.setText(getString(R.string.historico_total, chamados.size()));
                        tvTotal.setVisibility(chamados.isEmpty() ? View.GONE : View.VISIBLE);
                        mostrar(chamados.isEmpty() ? Estado.VAZIO : Estado.CONTEUDO);
                    }

                    @Override
                    public void onErro(Exception erro) {
                        Log.e(TAG, "Erro ao ouvir o histórico da máquina", erro);
                        tvTotal.setVisibility(View.GONE);
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
        rvHistorico.setVisibility(estado == Estado.CONTEUDO ? View.VISIBLE : View.GONE);
        layoutVazio.setVisibility(estado == Estado.VAZIO ? View.VISIBLE : View.GONE);
        tvErro.setVisibility(estado == Estado.ERRO ? View.VISIBLE : View.GONE);
    }

    /** Seta de voltar da Toolbar: fecha esta tela. */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
