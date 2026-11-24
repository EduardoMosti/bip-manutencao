package com.bip.manutencao;

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
import com.bip.manutencao.model.Chamado;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestoreException;

public class AbrirChamadoActivity extends AppCompatActivity {

    public static final String EXTRA_CODIGO = "codigo";
    public static final String EXTRA_NOME = "nome";
    public static final String EXTRA_SETOR = "setor";

    private TextInputLayout tilDescricao;
    private TextInputEditText edtDescricao;
    private ChipGroup chipsPrioridade;
    private TextView tvErroPrioridade;
    private CircularProgressIndicator progressChamado;
    private MaterialButton btnAbrirChamado;

    private ChamadoRepository repositorio;
    private String codigoMaquina;
    private String nomeMaquina;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_abrir_chamado);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });

        codigoMaquina = getIntent().getStringExtra(EXTRA_CODIGO);
        nomeMaquina = getIntent().getStringExtra(EXTRA_NOME);
        String setor = getIntent().getStringExtra(EXTRA_SETOR);

        // Sem máquina lida não existe chamado para abrir
        if (codigoMaquina == null || codigoMaquina.trim().isEmpty()) {
            finish();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.chamado_titulo);
        }

        TextView tvMaquina = findViewById(R.id.tvMaquina);
        TextView tvSetor = findViewById(R.id.tvSetor);
        tilDescricao = findViewById(R.id.tilDescricao);
        edtDescricao = findViewById(R.id.edtDescricao);
        chipsPrioridade = findViewById(R.id.chipsPrioridade);
        tvErroPrioridade = findViewById(R.id.tvErroPrioridade);
        progressChamado = findViewById(R.id.progressChamado);
        btnAbrirChamado = findViewById(R.id.btnAbrirChamado);

        repositorio = new ChamadoRepository();

        tvMaquina.setText(getString(R.string.chamado_maquina_valor, codigoMaquina,
                nomeMaquina == null ? "" : nomeMaquina));
        if (setor == null || setor.trim().isEmpty()) {
            tvSetor.setVisibility(View.GONE);
        } else {
            tvSetor.setText(getString(R.string.chamado_setor_valor, setor));
        }

        // Some o aviso de prioridade assim que o operador escolher uma
        chipsPrioridade.setOnCheckedStateChangeListener((grupo, ids) ->
                tvErroPrioridade.setVisibility(View.GONE));

        btnAbrirChamado.setOnClickListener(v -> abrirChamado());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private String prioridadeSelecionada() {
        int id = chipsPrioridade.getCheckedChipId();
        if (id == R.id.chipBaixa) {
            return Chamado.PRIORIDADE_BAIXA;
        } else if (id == R.id.chipMedia) {
            return Chamado.PRIORIDADE_MEDIA;
        } else if (id == R.id.chipAlta) {
            return Chamado.PRIORIDADE_ALTA;
        }
        return null;
    }

    private void abrirChamado() {
        tilDescricao.setError(null);
        tvErroPrioridade.setVisibility(View.GONE);

        String descricao = edtDescricao.getText() == null
                ? "" : edtDescricao.getText().toString().trim();
        String prioridade = prioridadeSelecionada();

        boolean valido = true;
        if (descricao.isEmpty()) {
            tilDescricao.setError(getString(R.string.chamado_erro_descricao_vazia));
            valido = false;
        }
        if (prioridade == null) {
            tvErroPrioridade.setVisibility(View.VISIBLE);
            valido = false;
        }
        if (!valido) {
            return;
        }

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            Toast.makeText(this, R.string.chamado_erro_gravar, Toast.LENGTH_LONG).show();
            return;
        }

        Chamado chamado = new Chamado();
        chamado.setMaquinaId(codigoMaquina);
        chamado.setMaquinaNome(nomeMaquina);
        chamado.setDescricao(descricao);
        chamado.setPrioridade(prioridade);
        chamado.setAbertoPor(usuario.getUid());
        // Nome de reserva: o repositório usa o nome de usuarios/{uid} quando existir
        chamado.setAbertoPorNome(usuario.getEmail());

        definirCarregando(true);
        repositorio.abrir(chamado)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, R.string.chamado_sucesso, Toast.LENGTH_LONG).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    definirCarregando(false);
                    Toast.makeText(this, mensagemDeErro(e), Toast.LENGTH_LONG).show();
                });
    }

    private int mensagemDeErro(Exception e) {
        if (e instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException.Code codigo = ((FirebaseFirestoreException) e).getCode();
            if (codigo == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                return R.string.chamado_erro_permissao;
            }
            if (codigo == FirebaseFirestoreException.Code.UNAVAILABLE) {
                return R.string.chamado_erro_rede;
            }
        }
        return R.string.chamado_erro_gravar;
    }

    private void definirCarregando(boolean carregando) {
        progressChamado.setVisibility(carregando ? View.VISIBLE : View.GONE);
        btnAbrirChamado.setEnabled(!carregando);
    }
}