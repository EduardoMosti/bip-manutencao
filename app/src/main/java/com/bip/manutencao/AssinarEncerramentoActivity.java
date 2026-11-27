package com.bip.manutencao;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bip.manutencao.adapter.AssinaturaAdapter;
import com.bip.manutencao.data.ChamadoRepository;
import com.bip.manutencao.model.Chamado;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.List;

/**
 * US-12 (BIP!) - O operador assina o encerramento lendo o QR da própria matrícula.
 *
 * A lista mostra, em tempo real, os chamados que ESTE operador abriu, que o técnico já
 * encerrou e que ainda não têm assinatura. Ao tocar em "Assinar", abre o leitor de QR
 * (ZXing, o mesmo da leitura de máquina). O texto lido é a matrícula, e quem decide
 * é o ChamadoRepository.assinar():
 *  - matrícula do QR igual à cadastrada em usuarios/{uid}: grava a assinatura
 *    (matrícula e horário) e o chamado sai da lista;
 *  - matrícula de outra pessoa: o app recusa e o chamado NÃO recebe assinatura.
 * As regras do Firestore conferem a mesma coisa, como segunda barreira.
 *
 * Mesmo padrão das outras listas: o listener começa no onStart e é removido no onStop.
 */
public class AssinarEncerramentoActivity extends AppCompatActivity
        implements AssinaturaAdapter.OnAssinarListener {

    private static final String TAG = "AssinarEncerramento";
    private static final String ESTADO_CHAMADO = "chamadoEmAssinatura";

    /** Os 4 estados da tela (padrão do curso): só um aparece por vez. */
    private enum Estado { CARREGANDO, CONTEUDO, VAZIO, ERRO }

    private RecyclerView rvAssinaturas;
    private View progresso;
    private View progressoAssinando;
    private View layoutVazio;
    private TextView tvErro;

    private AssinaturaAdapter adapter;
    private final ChamadoRepository repositorio = new ChamadoRepository();
    private ListenerRegistration registroListener; // para remover no onStop

    // ID do chamado que está sendo assinado (guardado na rotação: o leitor de QR sai da tela)
    private String chamadoEmAssinatura;
    private boolean assinando = false;

    // Leitor de QR Code (ZXing): devolve o texto do QR, que é a matrícula
    private final ActivityResultLauncher<ScanOptions> leitorQr =
            registerForActivityResult(new ScanContract(), resultado -> {
                if (resultado.getContents() != null && chamadoEmAssinatura != null) {
                    assinar(chamadoEmAssinatura, resultado.getContents());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_assinar_encerramento);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });

        if (savedInstanceState != null) {
            chamadoEmAssinatura = savedInstanceState.getString(ESTADO_CHAMADO);
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.assinatura_titulo);
        }

        rvAssinaturas = findViewById(R.id.rvAssinaturas);
        progresso = findViewById(R.id.progressoAssinaturas);
        progressoAssinando = findViewById(R.id.progressoAssinando);
        layoutVazio = findViewById(R.id.layoutVazioAssinaturas);
        tvErro = findViewById(R.id.tvErroAssinaturas);

        adapter = new AssinaturaAdapter();
        adapter.setOnAssinarListener(this);
        rvAssinaturas.setLayoutManager(new LinearLayoutManager(this));
        rvAssinaturas.setAdapter(adapter);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ESTADO_CHAMADO, chamadoEmAssinatura);
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

        registroListener = repositorio.ouvirAguardandoAssinatura(usuario.getUid(),
                new ChamadoRepository.OnChamadosListener() {
                    @Override
                    public void onLista(List<Chamado> chamados) {
                        adapter.atualizar(chamados);
                        mostrar(chamados.isEmpty() ? Estado.VAZIO : Estado.CONTEUDO);
                    }

                    @Override
                    public void onErro(Exception erro) {
                        Log.e(TAG, "Erro ao ouvir os chamados para assinar", erro);
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
        rvAssinaturas.setVisibility(estado == Estado.CONTEUDO ? View.VISIBLE : View.GONE);
        layoutVazio.setVisibility(estado == Estado.VAZIO ? View.VISIBLE : View.GONE);
        tvErro.setVisibility(estado == Estado.ERRO ? View.VISIBLE : View.GONE);
    }

    /** Clique em "Assinar" numa linha (vem do Adapter): abre o leitor do QR da matrícula. */
    @Override
    public void onAssinar(Chamado chamado) {
        if (assinando || chamado.getId() == null) {
            return;
        }
        chamadoEmAssinatura = chamado.getId();

        ScanOptions opcoes = new ScanOptions();
        opcoes.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        opcoes.setPrompt(getString(R.string.assinatura_prompt_scanner));
        opcoes.setBeepEnabled(false);
        opcoes.setOrientationLocked(false);
        leitorQr.launch(opcoes);
    }

    private void assinar(String chamadoId, String matriculaLida) {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            Toast.makeText(this, R.string.erro_sem_login, Toast.LENGTH_LONG).show();
            return;
        }

        definirAssinando(true);
        // "this" nos listeners: são removidos sozinhos se a tela for destruída
        repositorio.assinar(chamadoId, usuario.getUid(), matriculaLida)
                .addOnSuccessListener(this, unused -> {
                    definirAssinando(false);
                    chamadoEmAssinatura = null;
                    // o chamado some da lista sozinho (o snapshot listener só traz os sem assinatura)
                    Toast.makeText(this, R.string.assinatura_sucesso, Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(this, erro -> {
                    definirAssinando(false);
                    tratarErro(erro);
                });
    }

    /** Traduz o erro técnico em mensagem em português. */
    private void tratarErro(Exception erro) {
        if (erro instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException e = (FirebaseFirestoreException) erro;
            switch (e.getCode()) {
                case FAILED_PRECONDITION:
                    avisar(mensagemDaRecusa(e.getMessage()));
                    return;
                case NOT_FOUND:
                    avisar(getString(R.string.assinatura_erro_nao_encontrado));
                    return;
                case PERMISSION_DENIED:
                    avisar(getString(R.string.assinatura_erro_sem_permissao));
                    return;
                case UNAVAILABLE:
                    Toast.makeText(this, R.string.assinatura_erro_rede, Toast.LENGTH_LONG).show();
                    return;
                default:
                    break;
            }
        }
        Log.e(TAG, "Falha ao assinar", erro);
        Toast.makeText(this, R.string.assinatura_erro_gravar, Toast.LENGTH_LONG).show();
    }

    /** O Repository recusa com uma mensagem-código; aqui vira texto para o operador. */
    private String mensagemDaRecusa(String codigo) {
        if (ChamadoRepository.ASSINATURA_OUTRA_MATRICULA.equals(codigo)) {
            return getString(R.string.assinatura_erro_outra_matricula);
        }
        if (ChamadoRepository.ASSINATURA_SEM_MATRICULA.equals(codigo)) {
            return getString(R.string.assinatura_erro_sem_matricula);
        }
        if (ChamadoRepository.ASSINATURA_JA_ASSINADO.equals(codigo)) {
            return getString(R.string.assinatura_erro_ja_assinado);
        }
        if (ChamadoRepository.ASSINATURA_NAO_ENCERRADO.equals(codigo)) {
            return getString(R.string.assinatura_erro_nao_encerrado);
        }
        return getString(R.string.assinatura_erro_gravar);
    }

    private void avisar(String mensagem) {
        new MaterialAlertDialogBuilder(this)
                .setMessage(mensagem)
                .setPositiveButton(R.string.assinatura_ok, null)
                .show();
    }

    /** Liga/desliga o modo "verificando": progresso, cliques travados e rotação travada. */
    private void definirAssinando(boolean valor) {
        assinando = valor;
        progressoAssinando.setVisibility(valor ? View.VISIBLE : View.GONE);
        setRequestedOrientation(valor
                ? ActivityInfo.SCREEN_ORIENTATION_LOCKED
                : ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    /** Seta de voltar da Toolbar: fecha esta tela. */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
