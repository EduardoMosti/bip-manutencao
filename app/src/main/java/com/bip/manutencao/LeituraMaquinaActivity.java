package com.bip.manutencao;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bip.manutencao.data.ChamadoRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

public class LeituraMaquinaActivity extends AppCompatActivity {

    private TextInputLayout tilCodigo;
    private TextInputEditText edtCodigo;
    private MaterialButton btnLerQr;
    private MaterialButton btnBuscar;
    private CircularProgressIndicator progressLeitura;

    private ChamadoRepository repositorio;

    // Leitor de QR Code (ZXing)
    private final ActivityResultLauncher<ScanOptions> leitorQr =
            registerForActivityResult(new ScanContract(), resultado -> {
                if (resultado.getContents() != null) {
                    edtCodigo.setText(resultado.getContents());
                    buscarMaquina(resultado.getContents());
                }
            });

    // Quando o chamado é aberto com sucesso, esta tela também é encerrada
    private final ActivityResultLauncher<Intent> formulario =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    resultado -> {
                        if (resultado.getResultCode() == RESULT_OK) {
                            finish();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_leitura_maquina);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.leitura_titulo);
        }

        tilCodigo = findViewById(R.id.tilCodigo);
        edtCodigo = findViewById(R.id.edtCodigo);
        btnLerQr = findViewById(R.id.btnLerQr);
        btnBuscar = findViewById(R.id.btnBuscar);
        progressLeitura = findViewById(R.id.progressLeitura);

        repositorio = new ChamadoRepository();

        btnLerQr.setOnClickListener(v -> abrirLeitor());
        btnBuscar.setOnClickListener(v -> buscarMaquina(textoDigitado()));
        edtCodigo.setOnEditorActionListener((view, acao, evento) -> {
            if (acao == EditorInfo.IME_ACTION_SEARCH) {
                buscarMaquina(textoDigitado());
                return true;
            }
            return false;
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private String textoDigitado() {
        return edtCodigo.getText() == null ? "" : edtCodigo.getText().toString();
    }

    private void abrirLeitor() {
        tilCodigo.setError(null);
        ScanOptions opcoes = new ScanOptions();
        opcoes.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        opcoes.setPrompt(getString(R.string.leitura_prompt_scanner));
        opcoes.setBeepEnabled(false);
        opcoes.setOrientationLocked(false);
        leitorQr.launch(opcoes);
    }

    private void buscarMaquina(String texto) {
        tilCodigo.setError(null);
        String codigo = texto == null ? "" : texto.trim();
        if (codigo.isEmpty()) {
            tilCodigo.setError(getString(R.string.leitura_erro_codigo_vazio));
            return;
        }

        definirCarregando(true);
        repositorio.buscarMaquina(codigo)
                .addOnSuccessListener(doc -> {
                    definirCarregando(false);
                    if (doc == null || !doc.exists()) {
                        tilCodigo.setError(
                                getString(R.string.leitura_erro_nao_encontrada, codigo));
                        return;
                    }
                    Intent intent = new Intent(this, AbrirChamadoActivity.class);
                    intent.putExtra(AbrirChamadoActivity.EXTRA_CODIGO, doc.getId());
                    intent.putExtra(AbrirChamadoActivity.EXTRA_NOME, doc.getString("nome"));
                    intent.putExtra(AbrirChamadoActivity.EXTRA_SETOR, doc.getString("setor"));
                    formulario.launch(intent);
                })
                .addOnFailureListener(e -> {
                    definirCarregando(false);
                    tilCodigo.setError(getString(R.string.leitura_erro_buscar));
                });
    }

    private void definirCarregando(boolean carregando) {
        progressLeitura.setVisibility(carregando ? View.VISIBLE : View.GONE);
        btnLerQr.setEnabled(!carregando);
        btnBuscar.setEnabled(!carregando);
    }
}