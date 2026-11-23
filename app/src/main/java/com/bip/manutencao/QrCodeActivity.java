package com.bip.manutencao;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bip.manutencao.util.QrUtil;
import com.google.android.material.appbar.MaterialToolbar;

/**
 * Tela GENÉRICA que mostra um QR Code grande para imprimir ou apresentar.
 *
 * Recebe pela Intent:
 *  - EXTRA_CONTEUDO  (obrigatório) texto que vai dentro do QR. Ex.: "MAQ-001"
 *  - EXTRA_TITULO    (opcional)    ex.: "Injetora 3"
 *  - EXTRA_SUBTITULO (opcional)    ex.: "Setor: Moldagem"
 *
 * Genérica de propósito: a US-01 usa para a máquina e a US-12 vai usar
 * para o QR da matrícula do operador, sem duplicar código.
 */
public class QrCodeActivity extends AppCompatActivity {

    public static final String EXTRA_CONTEUDO = "extra_conteudo";
    public static final String EXTRA_TITULO = "extra_titulo";
    public static final String EXTRA_SUBTITULO = "extra_subtitulo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_qr_code);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // 1) Lê os extras
        String conteudo = getIntent().getStringExtra(EXTRA_CONTEUDO);
        String titulo = getIntent().getStringExtra(EXTRA_TITULO);
        String subtitulo = getIntent().getStringExtra(EXTRA_SUBTITULO);

        // Sem conteúdo não há QR para gerar: avisa e fecha (não derruba o app)
        if (conteudo == null || conteudo.isEmpty()) {
            Toast.makeText(this, R.string.erro_qr_sem_conteudo, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        TextView tvTitulo = findViewById(R.id.tvTituloQr);
        TextView tvSubtitulo = findViewById(R.id.tvSubtituloQr);
        TextView tvConteudo = findViewById(R.id.tvConteudoQr);
        ImageView ivQr = findViewById(R.id.ivQr);

        tvTitulo.setText(titulo != null ? titulo : "");
        tvSubtitulo.setText(subtitulo != null ? subtitulo : "");
        tvConteudo.setText(conteudo); // texto legível embaixo do QR (ajuda se a câmera falhar)

        // 2) Gera o QR no tamanho definido em dimens (menor no modo paisagem)
        int tamanhoPx = getResources().getDimensionPixelSize(R.dimen.qr_tamanho);
        Bitmap qr = QrUtil.gerar(conteudo, tamanhoPx);

        if (qr != null) {
            ivQr.setImageBitmap(qr);
            // Acessibilidade: o leitor de tela fala o que a imagem representa
            ivQr.setContentDescription(getString(R.string.qr_descricao, conteudo));
        } else {
            Toast.makeText(this, R.string.erro_qr_gerar, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
