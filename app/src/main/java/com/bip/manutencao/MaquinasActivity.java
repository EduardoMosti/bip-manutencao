package com.bip.manutencao;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bip.manutencao.adapter.MaquinaAdapter;
import com.bip.manutencao.data.MaquinaRepository;
import com.bip.manutencao.model.Maquina;
import com.bip.manutencao.util.AvisoChamados;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

/**
 * US-01 (C1) - Lista de máquinas cadastradas, em tempo real.
 *
 * Quem abre esta tela: o TÉCNICO (pelo menu da tela inicial do técnico).
 * - Botão "+" (FAB): abre o formulário de cadastro.
 * - Toque numa máquina: abre o QR dela para imprimir.
 *
 * Implementa a interface do Adapter para receber o clique nas linhas.
 *
 * US-13: enquanto esta tela está aberta, o técnico é avisado de chamados novos
 * (AvisoChamados). Quando existir a tela inicial do técnico, ligue o aviso nela também.
 */
public class MaquinasActivity extends AppCompatActivity
        implements MaquinaAdapter.OnMaquinaClickListener {

    private static final String TAG = "MaquinasActivity";

    /** Os 4 estados da tela (padrão do curso): só um aparece por vez. */
    private enum Estado { CARREGANDO, CONTEUDO, VAZIO, ERRO }

    private RecyclerView rvMaquinas;
    private ProgressBar progresso;
    private View layoutVazio;
    private TextView tvErro;

    private MaquinaAdapter adapter;
    private final MaquinaRepository repository = new MaquinaRepository();
    private ListenerRegistration registroListener; // para remover no onStop
    private AvisoChamados avisoChamados;           // US-13: aviso de chamado aberto

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_maquinas);

        // Insets: com EdgeToEdge, o app desenha atrás da barra de status.
        // Este padding evita que a Toolbar fique escondida embaixo dela.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });

        // Toolbar no lugar da ActionBar (o tema é NoActionBar) + seta de voltar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        rvMaquinas = findViewById(R.id.rvMaquinas);
        progresso = findViewById(R.id.progressoMaquinas);
        layoutVazio = findViewById(R.id.layoutVazioMaquinas);
        tvErro = findViewById(R.id.tvErroMaquinas);

        // RecyclerView: lista vertical + adapter com "this" como ouvinte de clique
        adapter = new MaquinaAdapter(this);
        rvMaquinas.setLayoutManager(new LinearLayoutManager(this));
        rvMaquinas.setHasFixedSize(true);
        rvMaquinas.setAdapter(adapter);

        // FAB "+": abre o formulário de cadastro
        FloatingActionButton fab = findViewById(R.id.fabNovaMaquina);
        fab.setOnClickListener(v ->
                startActivity(new Intent(this, CadastroMaquinaActivity.class)));

        // US-13: aviso de chamado novo. A Snackbar aparece acima do FAB.
        avisoChamados = new AvisoChamados(this, findViewById(R.id.main), fab);
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

        // Começa a ouvir a coleção. O callback roda agora e a cada mudança.
        registroListener = repository.ouvirMaquinas(new MaquinaRepository.OnMaquinasListener() {
            @Override
            public void onLista(List<Maquina> maquinas) {
                adapter.atualizar(maquinas);
                mostrar(maquinas.isEmpty() ? Estado.VAZIO : Estado.CONTEUDO);
            }

            @Override
            public void onErro(Exception erro) {
                Log.e(TAG, "Erro ao ouvir máquinas", erro);
                mostrar(Estado.ERRO);
            }
        });

        // US-13: começa a ouvir chamados abertos (só avisa os que chegarem a partir de agora)
        avisoChamados.iniciar();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Para de ouvir quando a tela sai de vista: economiza bateria e dados.
        if (registroListener != null) {
            registroListener.remove();
            registroListener = null;
        }
        // US-13: para de ouvir os chamados junto com a lista
        avisoChamados.parar();
    }

    /** Clique numa linha da lista (vem do Adapter): abre o QR da máquina. */
    @Override
    public void onMaquinaClick(Maquina maquina) {
        Intent intent = new Intent(this, QrCodeActivity.class);
        intent.putExtra(QrCodeActivity.EXTRA_CONTEUDO, maquina.getCodigo());
        intent.putExtra(QrCodeActivity.EXTRA_TITULO, maquina.getNome());
        intent.putExtra(QrCodeActivity.EXTRA_SUBTITULO,
                getString(R.string.maquina_setor_formato, maquina.getSetor()));
        startActivity(intent);
    }

    /** Mostra só a View do estado pedido e esconde as outras. */
    private void mostrar(Estado estado) {
        progresso.setVisibility(estado == Estado.CARREGANDO ? View.VISIBLE : View.GONE);
        rvMaquinas.setVisibility(estado == Estado.CONTEUDO ? View.VISIBLE : View.GONE);
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
