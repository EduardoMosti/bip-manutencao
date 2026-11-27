package com.bip.manutencao.data;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Repository: lê o perfil do usuário em usuarios/{uid} (US-08).
 *
 * O perfil NUNCA é escolhido nem gravado pelo app: o grupo cria o campo "perfil"
 * à mão no console do Firebase, e as regras do Firestore recusam qualquer tentativa
 * do usuário de gravar ou alterar esse campo. O app só LÊ.
 */
public class UsuarioRepository {

    // Valores aceitos em "perfil" (as regras do Firestore conferem estes textos)
    public static final String PERFIL_OPERADOR = "operador";
    public static final String PERFIL_TECNICO = "tecnico";

    private static final String COLECAO_USUARIOS = "usuarios";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Busca o perfil do usuário.
     *
     * O resultado da Task é:
     *  - "operador" ou "tecnico": perfil válido;
     *  - null: o documento não existe, não tem o campo "perfil" ou o valor não é
     *    nenhum dos dois (ex.: erro de digitação no console). Nesses casos as regras
     *    do Firestore também tratam o usuário como sem perfil, então o app não
     *    mostra ação nenhuma.
     *
     * Se a leitura falhar (sem internet, por exemplo), a Task falha.
     */
    public Task<String> buscarPerfil(String uid) {
        return db.collection(COLECAO_USUARIOS).document(uid).get()
                .continueWith(tarefa -> {
                    DocumentSnapshot doc = tarefa.getResult(); // se a leitura falhou, a Task falha aqui
                    String perfil = doc.getString("perfil");
                    if (PERFIL_OPERADOR.equals(perfil) || PERFIL_TECNICO.equals(perfil)) {
                        return perfil;
                    }
                    return null;
                });
    }
}
