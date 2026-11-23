package com.bip.manutencao.model;

import java.io.Serializable;

/**
 * Modelo de uma máquina da fábrica (US-01).
 *
 * No Firestore fica em: maquinas/{codigo}  ->  campos: nome, setor
 *
 * O CÓDIGO não é gravado como campo: ele é o ID do documento.
 * Por isso, depois de ler do Firestore, o Repository chama setCodigo(doc.getId()).
 *
 * Regras que o Firestore exige para converter o documento em objeto (toObject):
 *  1) construtor vazio;
 *  2) getters e setters públicos com o mesmo nome dos campos.
 *
 * "implements Serializable" permite mandar a máquina inteira de uma tela
 * para outra com intent.putExtra(...).
 */
public class Maquina implements Serializable {

    private String codigo; // ex.: MAQ-001 (é o conteúdo do QR e o ID do documento)
    private String nome;   // ex.: Injetora 3
    private String setor;  // ex.: Moldagem

    // Construtor vazio: obrigatório para o Firestore montar o objeto sozinho.
    public Maquina() {
    }

    // Construtor completo: usado pelo formulário de cadastro.
    public Maquina(String codigo, String nome, String setor) {
        this.codigo = codigo;
        this.nome = nome;
        this.setor = setor;
    }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getSetor() { return setor; }
    public void setSetor(String setor) { this.setor = setor; }
}
