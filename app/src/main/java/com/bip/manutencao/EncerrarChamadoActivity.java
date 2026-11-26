package com.bip.manutencao;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bip.manutencao.data.ChamadoRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestoreException;

/**
 * US-05 (C5) - Encerrar chamado com a solução.
 *
 * Recebe o chamado por Intent (vem da MeusChamadosActivity), mostra o resumo e pede
 * a solução. Critérios de aceite atendidos:
 *  - solução preenchida + "Encerrar": o Repository muda o status para "encerrado" e grava
 *    fechadoEm (ao lado do abertoEm que já está no documento);
 *  - solução vazia: mensagem em português no campo e NADA é gravado (o chamado continua
 *    em atendimento);
 *  - chamado de outro técnico: o Repository recusa (e as regras do Firestore também) e
 *    a tela avisa quem está atendendo.
 *
 * Rotação: o campo da solução tem id, então o Android guarda o texto digitado sozinho.
 * Durante a gravação a rotação é travada (padrão do grupo, US-10).
 */
public class EncerrarChamadoActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "chamadoId";
    public static final String EXTRA_MAQUINA_ID = "maquinaId";
    public static final String EXTRA_MAQUINA_NOME = "maquinaNome";
    public static final String EXTRA_DESCRICAO = "descricao";

    private TextInputLayout tilSolucao;
    private TextInputEditText edtSolucao;
    private CircularProgressIndicator progressEncerrar;
    private MaterialButton btnEncerrar;

    private final ChamadoRepository repositorio = new ChamadoRepository();
    private String chamadoId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_encerrar_chamado);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });

        chamadoId = getIntent().getStringExtra(EXTRA_ID);
        // Sem chamado não existe o que encerrar
        if (chamadoId == null || chamadoId.trim().isEmpty()) {
            finish();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.encerrar_titulo);
        }

        TextView tvMaquina = findViewById(R.id.tvMaquinaEncerrar);
        TextView tvDescricao = findViewById(R.id.tvDescricaoEncerrar);
        tilSolucao = findViewById(R.id.tilSolucao);
        edtSolucao = findViewById(R.id.edtSolucao);
        progressEncerrar = findViewById(R.id.progressEncerrar);
        btnEncerrar = findViewById(R.id.btnEncerrar);

        tvMaquina.setText(getString(R.string.chamado_maquina_valor,
                texto(getIntent().getStringExtra(EXTRA_MAQUINA_ID)),
                texto(getIntent().getStringExtra(EXTRA_MAQUINA_NOME))));
        tvDescricao.setText(texto(getIntent().getStringExtra(EXTRA_DESCRICAO)));

        btnEncerrar.setOnClickListener(v -> encerrar());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void encerrar() {
        tilSolucao.setError(null);

        String solucao = edtSolucao.getText() == null
                ? "" : edtSolucao.getText().toString().trim();
        if (solucao.isEmpty()) {
            // Nada é gravado: o chamado continua em atendimento
            tilSolucao.setError(getString(R.string.encerrar_erro_solucao_vazia));
            return;
        }

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            Toast.makeText(this, R.string.erro_sem_login, Toast.LENGTH_LONG).show();
            return;
        }

        definirCarregando(true);
        // "this" nos listeners: são removidos sozinhos se a tela for destruída
        repositorio.encerrar(chamadoId, usuario.getUid(), solucao)
                .addOnSuccessListener(this, unused -> {
                    Toast.makeText(this, R.string.encerrar_sucesso, Toast.LENGTH_LONG).show();
                    setResult(RESULT_OK);
                    finish(); // a lista "Meus atendimentos" tira o chamado sozinha
                })
                .addOnFailureListener(this, erro -> {
                    definirCarregando(false);
                    tratarErro(erro);
                });
    }

    /** Traduz o erro técnico em mensagem em português. */
    private void tratarErro(Exception erro) {
        if (erro instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException e = (FirebaseFirestoreException) erro;
            switch (e.getCode()) {
                case FAILED_PRECONDITION: {
                    // Já encerrado, ou de outro técnico (a mensagem traz o nome dele)
                    String aviso = e.getMessage();
                    String mensagem;
                    if (ChamadoRepository.JA_ENCERRADO.equals(aviso)) {
                        mensagem = getString(R.string.encerrar_ja_encerrado);
                    } else if (aviso == null || aviso.trim().isEmpty()) {
                        mensagem = getString(R.string.encerrar_de_outro_tecnico_sem_nome);
                    } else {
                        mensagem = getString(R.string.encerrar_de_outro_tecnico, aviso);
                    }
                    avisarEFechar(mensagem);
                    return;
                }
                case NOT_FOUND:
                    avisarEFechar(getString(R.string.encerrar_nao_encontrado));
                    return;
                case PERMISSION_DENIED:
                    avisarEFechar(getString(R.string.encerrar_sem_permissao));
                    return;
                case UNAVAILABLE:
                    Toast.makeText(this, R.string.encerrar_erro_rede, Toast.LENGTH_LONG).show();
                    return;
                default:
                    break;
            }
        }
        Toast.makeText(this, R.string.encerrar_erro_gravar, Toast.LENGTH_LONG).show();
    }

    /** O chamado não pode mais ser encerrado por este técnico: avisa e volta para a lista. */
    private void avisarEFechar(String mensagem) {
        new MaterialAlertDialogBuilder(this)
                .setMessage(mensagem)
                .setPositiveButton(R.string.encerrar_ok, null)
                .setOnDismissListener(d -> finish())
                .show();
    }

    /** Liga/desliga o modo "gravando": progresso, botão travado e rotação travada. */
    private void definirCarregando(boolean carregando) {
        progressEncerrar.setVisibility(carregando ? View.VISIBLE : View.GONE);
        btnEncerrar.setEnabled(!carregando);
        setRequestedOrientation(carregando
                ? ActivityInfo.SCREEN_ORIENTATION_LOCKED
                : ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor;
    }
}
