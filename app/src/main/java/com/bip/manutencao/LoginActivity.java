package com.bip.manutencao;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final String ESTADO_MODO_CADASTRO = "modo_cadastro";

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private TextInputLayout tilNome;
    private TextInputLayout tilEmail;
    private TextInputLayout tilSenha;
    private TextInputEditText edtNome;
    private TextInputEditText edtEmail;
    private TextInputEditText edtSenha;
    private MaterialButton btnPrincipal;
    private MaterialButton btnAlternar;
    private CircularProgressIndicator progressLogin;

    private boolean modoCadastro = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tilNome = findViewById(R.id.tilNome);
        tilEmail = findViewById(R.id.tilEmail);
        tilSenha = findViewById(R.id.tilSenha);
        edtNome = findViewById(R.id.edtNome);
        edtEmail = findViewById(R.id.edtEmail);
        edtSenha = findViewById(R.id.edtSenha);
        btnPrincipal = findViewById(R.id.btnPrincipal);
        btnAlternar = findViewById(R.id.btnAlternar);
        progressLogin = findViewById(R.id.progressLogin);

        if (savedInstanceState != null) {
            modoCadastro = savedInstanceState.getBoolean(ESTADO_MODO_CADASTRO, false);
        }
        aplicarModo();

        btnPrincipal.setOnClickListener(v -> enviar());
        btnAlternar.setOnClickListener(v -> {
            modoCadastro = !modoCadastro;
            limparErros();
            aplicarModo();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Sessao: se ja existe usuario logado, segue direto para o app
        if (auth.getCurrentUser() != null) {
            abrirApp();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ESTADO_MODO_CADASTRO, modoCadastro);
    }

    private void aplicarModo() {
        if (modoCadastro) {
            tilNome.setVisibility(View.VISIBLE);
            btnPrincipal.setText(R.string.login_cadastrar);
            btnAlternar.setText(R.string.login_alternar_para_entrar);
        } else {
            tilNome.setVisibility(View.GONE);
            btnPrincipal.setText(R.string.login_entrar);
            btnAlternar.setText(R.string.login_alternar_para_cadastro);
        }
    }

    private void limparErros() {
        tilNome.setError(null);
        tilEmail.setError(null);
        tilSenha.setError(null);
    }

    private void enviar() {
        limparErros();
        if (!validar()) {
            return;
        }

        String email = textoDe(edtEmail);
        String senha = edtSenha.getText() == null ? "" : edtSenha.getText().toString();

        carregando(true);
        if (modoCadastro) {
            cadastrar(textoDe(edtNome), email, senha);
        } else {
            entrar(email, senha);
        }
    }

    private boolean validar() {
        boolean valido = true;

        if (modoCadastro && textoDe(edtNome).isEmpty()) {
            tilNome.setError(getString(R.string.erro_nome_vazio));
            valido = false;
        }

        String email = textoDe(edtEmail);
        if (email.isEmpty()) {
            tilEmail.setError(getString(R.string.erro_email_vazio));
            valido = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.erro_email_invalido));
            valido = false;
        }

        String senha = edtSenha.getText() == null ? "" : edtSenha.getText().toString();
        if (senha.isEmpty()) {
            tilSenha.setError(getString(R.string.erro_senha_vazia));
            valido = false;
        } else if (modoCadastro && senha.length() < 6) {
            tilSenha.setError(getString(R.string.erro_senha_curta));
            valido = false;
        }

        return valido;
    }

    private void entrar(String email, String senha) {
        auth.signInWithEmailAndPassword(email, senha)
                .addOnCompleteListener(task -> {
                    carregando(false);
                    if (task.isSuccessful()) {
                        abrirApp();
                    } else {
                        mostrarErro(task.getException());
                    }
                });
    }

    private void cadastrar(String nome, String email, String senha) {
        auth.createUserWithEmailAndPassword(email, senha)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        carregando(false);
                        mostrarErro(task.getException());
                        return;
                    }

                    FirebaseUser usuario = auth.getCurrentUser();
                    if (usuario == null) {
                        carregando(false);
                        Toast.makeText(this, R.string.erro_generico, Toast.LENGTH_LONG).show();
                        return;
                    }

                    // O perfil nao e gravado aqui: ele e definido pelo grupo no console
                    Map<String, Object> dados = new HashMap<>();
                    dados.put("nome", nome);
                    dados.put("email", email);

                    db.collection("usuarios").document(usuario.getUid())
                            .set(dados)
                            .addOnCompleteListener(gravacao -> {
                                carregando(false);
                                abrirApp();
                            });
                });
    }

    private void mostrarErro(Exception e) {
        if (e instanceof FirebaseAuthUserCollisionException) {
            tilEmail.setError(getString(R.string.erro_email_em_uso));
        } else if (e instanceof FirebaseAuthWeakPasswordException) {
            tilSenha.setError(getString(R.string.erro_senha_curta));
        } else if (e instanceof FirebaseAuthInvalidCredentialsException
                || e instanceof FirebaseAuthInvalidUserException) {
            if (modoCadastro) {
                tilEmail.setError(getString(R.string.erro_email_invalido));
            } else {
                tilSenha.setError(getString(R.string.erro_credenciais));
            }
        } else if (e instanceof FirebaseNetworkException) {
            Toast.makeText(this, R.string.erro_rede, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, R.string.erro_generico, Toast.LENGTH_LONG).show();
        }
    }

    private void carregando(boolean carregando) {
        progressLogin.setVisibility(carregando ? View.VISIBLE : View.GONE);
        btnPrincipal.setEnabled(!carregando);
        btnAlternar.setEnabled(!carregando);
    }

    private String textoDe(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim();
    }

    private void abrirApp() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}