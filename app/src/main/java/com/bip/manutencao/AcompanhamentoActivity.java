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

import com.bip.manutencao.adapter.AcompanhamentoAdapter;
import com.bip.manutencao.data.AcompanhamentoRepository;
import com.bip.manutencao.data.ChamadoRepository;
import com.bip.manutencao.model.Chamado;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

/**
 * US-11 - O operador acompanha os chamados que abriu e a previsão de chegada do técnico.
 *
 * Critérios de aceite atendidos:
 *  - chamado ainda não assumido: aparece "Aguardando técnico";
 *  - técnico assumiu: aparecem o nome dele e a previsão de chegada,
 *    atualizados sem recarregar (snapshot listener);
 *  - sem chamados: estado vazio (NC5).
 *
 * Mesmo padrão da fila: o listener começa no onStart e é removido no onStop.
 */
public class AcompanhamentoActivity extends AppCompatActivity {

    private static final String TAG = "Acompanhamento";

    /** Os 4 estados da tela (padrão do curso): só um aparece por vez. */
    private enum Estado { CARREGANDO, CONTEUDO, VAZIO, ERRO }

    private RecyclerView rvAcompanhamento;
    private View progresso;
    private View layoutVazio;
    private TextView tvErro;

    private AcompanhamentoAdapter adapter;
    private final AcompanhamentoRepository repositorio = new AcompanhamentoRepository();
    private ListenerRegistration registroListener; // para remover no onStop

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_acompanhamento);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.acompanhamento_titulo);
        }

        rvAcompanhamento = findViewById(R.id.rvAcompanhamento);
        progresso = findViewById(R.id.progressoAcompanhamento);
        layoutVazio = findViewById(R.id.layoutVazioAcompanhamento);
        tvErro = findViewById(R.id.tvErroAcompanhamento);

        adapter = new AcompanhamentoAdapter();
        rvAcompanhamento.setLayoutManager(new LinearLayoutManager(this));
        rvAcompanhamento.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            Toast.makeText(this, R.string.erro_sem_login, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mostrar(Estado.CARREGANDO);

        // Começa a ouvir os chamados do operador. O callback roda agora e a cada mudança.
        registroListener = repositorio.ouvirMeusChamados(usuario.getUid(),
                new ChamadoRepository.OnChamadosListener() {
                    @Override
                    public void onLista(List<Chamado> chamados) {
                        adapter.atualizar(chamados);
                        mostrar(chamados.isEmpty() ? Estado.VAZIO : Estado.CONTEUDO);
                    }

                    @Override
                    public void onErro(Exception erro) {
                        Log.e(TAG, "Erro ao ouvir os chamados do operador", erro);
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
        rvAcompanhamento.setVisibility(estado == Estado.CONTEUDO ? View.VISIBLE : View.GONE);
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
