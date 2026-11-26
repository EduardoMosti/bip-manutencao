package com.bip.manutencao;

import android.content.Intent;
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

import com.bip.manutencao.adapter.AtendimentoAdapter;
import com.bip.manutencao.data.ChamadoRepository;
import com.bip.manutencao.model.Chamado;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

/**
 * US-05 (C5) - "Meus atendimentos": os chamados que o técnico assumiu e ainda não encerrou.
 *
 * Por que esta tela existe? Ao assumir (US-04) o chamado sai da fila, que só mostra os
 * "aberto". Sem esta lista o técnico não teria como voltar a ele para encerrar.
 *
 * Só aparecem chamados com status "em_atendimento" e tecnicoUid igual ao do usuário
 * logado. Assim, um chamado de outro técnico nem chega a ser mostrado (o Repository
 * e as regras do Firestore ainda barram se alguém tentar encerrar mesmo assim).
 *
 * Mesmo padrão da FilaChamadosActivity: o listener começa no onStart e é removido no onStop.
 * Tocar em "Encerrar" abre a EncerrarChamadoActivity; quando o chamado é encerrado ele
 * some desta lista sozinho (o snapshot listener só traz os "em_atendimento").
 */
public class MeusChamadosActivity extends AppCompatActivity
        implements AtendimentoAdapter.OnEncerrarListener {

    private static final String TAG = "MeusChamados";

    /** Os 4 estados da tela (padrão do curso): só um aparece por vez. */
    private enum Estado { CARREGANDO, CONTEUDO, VAZIO, ERRO }

    private RecyclerView rvAtendimentos;
    private View progresso;
    private View layoutVazio;
    private TextView tvErro;

    private AtendimentoAdapter adapter;
    private final ChamadoRepository repositorio = new ChamadoRepository();
    private ListenerRegistration registroListener; // para remover no onStop

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_meus_chamados);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.atendimentos_titulo);
        }

        rvAtendimentos = findViewById(R.id.rvAtendimentos);
        progresso = findViewById(R.id.progressoAtendimentos);
        layoutVazio = findViewById(R.id.layoutVazioAtendimentos);
        tvErro = findViewById(R.id.tvErroAtendimentos);

        adapter = new AtendimentoAdapter();
        adapter.setOnEncerrarListener(this);
        rvAtendimentos.setLayoutManager(new LinearLayoutManager(this));
        rvAtendimentos.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Segurança: sem usuário logado, não há o que mostrar (as regras recusariam).
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            Toast.makeText(this, R.string.erro_sem_login, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mostrar(Estado.CARREGANDO);

        registroListener = repositorio.ouvirMeusAtendimentos(usuario.getUid(),
                new ChamadoRepository.OnChamadosListener() {
                    @Override
                    public void onLista(List<Chamado> chamados) {
                        adapter.atualizar(chamados);
                        mostrar(chamados.isEmpty() ? Estado.VAZIO : Estado.CONTEUDO);
                    }

                    @Override
                    public void onErro(Exception erro) {
                        Log.e(TAG, "Erro ao ouvir os atendimentos", erro);
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
        rvAtendimentos.setVisibility(estado == Estado.CONTEUDO ? View.VISIBLE : View.GONE);
        layoutVazio.setVisibility(estado == Estado.VAZIO ? View.VISIBLE : View.GONE);
        tvErro.setVisibility(estado == Estado.ERRO ? View.VISIBLE : View.GONE);
    }

    /** Clique em "Encerrar" numa linha (vem do Adapter): abre o formulário da solução. */
    @Override
    public void onEncerrar(Chamado chamado) {
        if (chamado.getId() == null) {
            return;
        }
        Intent intent = new Intent(this, EncerrarChamadoActivity.class);
        intent.putExtra(EncerrarChamadoActivity.EXTRA_ID, chamado.getId());
        intent.putExtra(EncerrarChamadoActivity.EXTRA_MAQUINA_ID, chamado.getMaquinaId());
        intent.putExtra(EncerrarChamadoActivity.EXTRA_MAQUINA_NOME, chamado.getMaquinaNome());
        intent.putExtra(EncerrarChamadoActivity.EXTRA_DESCRICAO, chamado.getDescricao());
        startActivity(intent);
    }

    /** Seta de voltar da Toolbar: fecha esta tela. */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
