package com.bip.manutencao;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bip.manutencao.data.MaquinaRepository;
import com.bip.manutencao.model.Maquina;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.Locale;

/**
 * US-01 (C1) - Formulário de cadastro de máquina.
 *
 * Critérios de aceite atendidos:
 *  - campos vazios -> mensagem em português no campo e nada é gravado (NC6);
 *  - código repetido -> avisa e NÃO sobrescreve (transação no Repository);
 *  - sucesso -> abre a tela com o QR da máquina recém-criada.
 *
 * Rotação (NC7): os TextInputEditText têm android:id, então o próprio Android
 * salva e restaura o texto digitado. Só guardamos à mão o estado "salvando".
 */
public class CadastroMaquinaActivity extends AppCompatActivity {

    private static final String TAG = "CadastroMaquina";
    private static final String CHAVE_SALVANDO = "salvando";

    // Código: letras maiúsculas, números e hífen, de 3 a 20 caracteres (ex.: MAQ-004).
    // Evita "/" (proibido em ID do Firestore) e espaços (atrapalham a leitura do QR).
    private static final String REGEX_CODIGO = "[A-Z0-9-]{3,20}";

    private TextInputLayout tilCodigo, tilNome, tilSetor;
    private TextInputEditText etCodigo, etNome, etSetor;
    private MaterialButton btnSalvar;
    private ProgressBar progresso;

    private final MaquinaRepository repository = new MaquinaRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cadastro_maquina);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            // ime() = teclado: soma a altura do teclado para o botão Salvar não ficar escondido
            Insets barras = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        tilCodigo = findViewById(R.id.tilCodigo);
        tilNome = findViewById(R.id.tilNome);
        tilSetor = findViewById(R.id.tilSetor);
        etCodigo = findViewById(R.id.etCodigo);
        etNome = findViewById(R.id.etNome);
        etSetor = findViewById(R.id.etSetor);
        btnSalvar = findViewById(R.id.btnSalvarMaquina);
        progresso = findViewById(R.id.progressoCadastro);

        btnSalvar.setOnClickListener(v -> salvar());

        // Se a tela girou no meio do salvamento, volta com o botão travado
        if (savedInstanceState != null && savedInstanceState.getBoolean(CHAVE_SALVANDO)) {
            setSalvando(true);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(CHAVE_SALVANDO, progresso.getVisibility() == View.VISIBLE);
    }

    /** Valida os campos; se tudo estiver certo, grava no Firestore. */
    private void salvar() {
        // 1) Lê e limpa os textos (trim tira espaços do começo/fim)
        String codigo = texto(etCodigo).toUpperCase(Locale.ROOT); // "maq-004" vira "MAQ-004"
        String nome = texto(etNome);
        String setor = texto(etSetor);

        // 2) Limpa erros antigos
        tilCodigo.setError(null);
        tilNome.setError(null);
        tilSetor.setError(null);

        // 3) Valida cada campo (mensagem em português no próprio campo)
        boolean valido = true;
        if (codigo.isEmpty()) {
            tilCodigo.setError(getString(R.string.erro_codigo_vazio));
            valido = false;
        } else if (!codigo.matches(REGEX_CODIGO)) {
            tilCodigo.setError(getString(R.string.erro_codigo_invalido));
            valido = false;
        }
        if (nome.isEmpty()) {
            tilNome.setError(getString(R.string.erro_maquina_nome_vazio));
            valido = false;
        }
        if (setor.isEmpty()) {
            tilSetor.setError(getString(R.string.erro_setor_vazio));
            valido = false;
        }
        if (!valido) {
            return; // nada é gravado
        }

        // 4) Grava. "this" no listener = ele é removido sozinho se a tela for destruída
        //    (evita mexer em Views de uma Activity que já não existe).
        setSalvando(true);
        Maquina maquina = new Maquina(codigo, nome, setor);

        repository.cadastrar(maquina)
                .addOnSuccessListener(this, unused -> {
                    Toast.makeText(this, R.string.maquina_cadastrada, Toast.LENGTH_SHORT).show();
                    abrirQr(maquina); // critério: "o app mostra um QR com o código dela"
                    finish();         // fecha o formulário; a lista já se atualizou sozinha
                })
                .addOnFailureListener(this, erro -> {
                    setSalvando(false);
                    tratarErro(erro, codigo);
                });
    }

    /** Traduz o erro técnico em mensagem em português para o usuário. */
    private void tratarErro(Exception erro, String codigo) {
        Log.e(TAG, "Falha ao cadastrar máquina " + codigo, erro);

        if (erro instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException.Code code = ((FirebaseFirestoreException) erro).getCode();

            if (code == FirebaseFirestoreException.Code.ALREADY_EXISTS) {
                tilCodigo.setError(getString(R.string.erro_codigo_existente, codigo));
                return;
            }
            if (code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                Toast.makeText(this, R.string.erro_sem_permissao, Toast.LENGTH_LONG).show();
                return;
            }
        }
        // Qualquer outro caso (ex.: sem internet: transação precisa estar online)
        Toast.makeText(this, R.string.erro_salvar_generico, Toast.LENGTH_LONG).show();
    }

    private void abrirQr(Maquina maquina) {
        Intent intent = new Intent(this, QrCodeActivity.class);
        intent.putExtra(QrCodeActivity.EXTRA_CONTEUDO, maquina.getCodigo());
        intent.putExtra(QrCodeActivity.EXTRA_TITULO, maquina.getNome());
        intent.putExtra(QrCodeActivity.EXTRA_SUBTITULO,
                getString(R.string.maquina_setor_formato, maquina.getSetor()));
        startActivity(intent);
    }

    /** Liga/desliga o modo "salvando": mostra o progresso e trava o botão (evita clique duplo). */
    private void setSalvando(boolean salvando) {
        progresso.setVisibility(salvando ? View.VISIBLE : View.GONE);
        btnSalvar.setEnabled(!salvando);
        // Trava a rotação durante o salvamento: a tela não é recriada sem receber a resposta
        setRequestedOrientation(salvando
                ? ActivityInfo.SCREEN_ORIENTATION_LOCKED
                : ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    /** Lê o texto de um campo sem risco de NullPointerException. */
    private String texto(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
