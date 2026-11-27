package com.bip.manutencao;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bip.manutencao.data.UsuarioRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Tela inicial. US-08: mostra só as ações do perfil do usuário, lido de usuarios/{uid}.
 *  - operador: Abrir chamado;
 *  - técnico: Fila de chamados, Meus atendimentos e Máquinas;
 *  - sem perfil definido: nenhuma ação, só o aviso.
 * Esconder botão é só a parte visual: quem garante a segurança são as regras do Firestore.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Main";

    private FirebaseAuth auth;
    private TextView tvPerfil;
    private TextView tvAvisoPerfil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();
        FirebaseUser usuario = auth.getCurrentUser();
        if (usuario == null) {
            // Sem sessao ativa: volta para o login
            voltarAoLogin();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TextView tvEmail = findViewById(R.id.tvEmail);
        tvEmail.setText(usuario.getEmail());
        tvPerfil = findViewById(R.id.tvPerfil);
        tvAvisoPerfil = findViewById(R.id.tvAvisoPerfil);

        // US-02: o operador começa o chamado lendo a máquina
        findViewById(R.id.btnAbrirChamado).setOnClickListener(v ->
                startActivity(new Intent(this, LeituraMaquinaActivity.class)));

        // US-11: o operador acompanha os chamados que abriu
        findViewById(R.id.btnMeusChamados).setOnClickListener(v ->
                startActivity(new Intent(this, AcompanhamentoActivity.class)));

        // US-03: o técnico vê a fila de chamados abertos
        findViewById(R.id.btnFilaChamados).setOnClickListener(v ->
                startActivity(new Intent(this, FilaChamadosActivity.class)));

        // US-05: o técnico vê os chamados que assumiu e encerra com a solução
        findViewById(R.id.btnMeusAtendimentos).setOnClickListener(v ->
                startActivity(new Intent(this, MeusChamadosActivity.class)));

        // US-01: o técnico abre a lista de máquinas (cadastro e QR de cada uma)
        findViewById(R.id.btnMaquinas).setOnClickListener(v ->
                startActivity(new Intent(this, MaquinasActivity.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser usuario = auth.getCurrentUser();
        if (usuario == null) {
            return;
        }
        // Lê de novo a cada vez que a tela volta: o perfil pode ter sido definido no console
        mostrarAcoes(null);
        tvAvisoPerfil.setVisibility(View.GONE);
        // "this" nos listeners: são removidos sozinhos se a tela for destruída
        new UsuarioRepository().buscarPerfil(usuario.getUid())
                .addOnSuccessListener(this, perfil -> {
                    mostrarAcoes(perfil);
                    if (perfil == null) {
                        avisar(R.string.perfil_nao_definido);
                    }
                })
                .addOnFailureListener(this, erro -> {
                    Log.e(TAG, "Falha ao ler o perfil", erro);
                    avisar(R.string.perfil_erro);
                });
    }

    /** Mostra só os botões do perfil. Perfil null: nenhum botão. */
    private void mostrarAcoes(String perfil) {
        boolean operador = UsuarioRepository.PERFIL_OPERADOR.equals(perfil);
        boolean tecnico = UsuarioRepository.PERFIL_TECNICO.equals(perfil);

        findViewById(R.id.btnAbrirChamado).setVisibility(operador ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnMeusChamados).setVisibility(operador ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnFilaChamados).setVisibility(tecnico ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnMeusAtendimentos).setVisibility(tecnico ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnMaquinas).setVisibility(tecnico ? View.VISIBLE : View.GONE);

        if (operador) {
            tvPerfil.setText(getString(R.string.perfil_rotulo, getString(R.string.perfil_operador)));
            tvPerfil.setVisibility(View.VISIBLE);
        } else if (tecnico) {
            tvPerfil.setText(getString(R.string.perfil_rotulo, getString(R.string.perfil_tecnico)));
            tvPerfil.setVisibility(View.VISIBLE);
        } else {
            tvPerfil.setVisibility(View.GONE);
        }
    }

    private void avisar(int texto) {
        tvAvisoPerfil.setText(texto);
        tvAvisoPerfil.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_principal, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.acao_sair) {
            auth.signOut();
            voltarAoLogin();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void voltarAoLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}