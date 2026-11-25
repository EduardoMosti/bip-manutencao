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

import com.bip.manutencao.adapter.ChamadoAdapter;
import com.bip.manutencao.data.ChamadoRepository;
import com.bip.manutencao.model.Chamado;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
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
 */
public class FilaChamadosActivity extends AppCompatActivity {

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

    /** Seta de voltar da Toolbar: fecha esta tela. */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}