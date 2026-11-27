package com.bip.manutencao.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

import java.util.List;

/**
 * Modelo de um chamado de manutenção (US-02, usado também por US-03, 04, 05 e 06).
 *
 * No Firestore fica em: chamados/{id}
 *
 * Quem abre o chamado (operador) grava: maquinaId, maquinaNome, descricao,
 * prioridade, status, abertoPor, abertoPorNome e abertoEm.
 * Quem atende (técnico) preenche depois: tecnicoUid, tecnicoNome, assumidoEm,
 * solucao e fechadoEm.
 *
 * O ID do documento NÃO é um campo gravado: a anotação @DocumentId faz o
 * Firestore preencher o campo "id" sozinho ao converter com toObject().
 *
 * Regras para o Firestore converter o documento em objeto (toObject):
 *  1) construtor vazio;
 *  2) getters e setters públicos com o mesmo nome dos campos.
 */
public class Chamado {

    // Valores aceitos em "status" (as regras do Firestore conferem estes textos)
    public static final String STATUS_ABERTO = "aberto";
    public static final String STATUS_EM_ATENDIMENTO = "em_atendimento";
    public static final String STATUS_ENCERRADO = "encerrado";

    // Valores aceitos em "prioridade" (as regras do Firestore conferem estes textos)
    public static final String PRIORIDADE_BAIXA = "baixa";
    public static final String PRIORIDADE_MEDIA = "media";
    public static final String PRIORIDADE_ALTA = "alta";

    // US-14: áreas de manutenção aceitas em "area" (opcional)
    public static final String AREA_MECANICA = "mecanica";
    public static final String AREA_ELETRICA = "eletrica";
    public static final String AREA_HIDRAULICA = "hidraulica";
    public static final String AREA_PNEUMATICA = "pneumatica";

    @DocumentId
    private String id;

    private String maquinaId;     // código da máquina, ex.: MAQ-001
    private String maquinaNome;   // ex.: Injetora 3 (copiado para a fila não precisar buscar de novo)
    private String descricao;
    private String prioridade;
    private String status;

    private String abertoPor;     // uid de quem abriu
    private String abertoPorNome; // nome de quem abriu
    private Timestamp abertoEm;

    private String tecnicoUid;
    private String tecnicoNome;
    private Timestamp assumidoEm;

    private String solucao;
    private Timestamp fechadoEm;

    // US-14: área e habilidades exigidas (opcionais, informadas pelo operador)
    private String area;
    private List<String> habilidades;

    // Construtor vazio: obrigatório para o Firestore montar o objeto sozinho.
    public Chamado() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMaquinaId() { return maquinaId; }
    public void setMaquinaId(String maquinaId) { this.maquinaId = maquinaId; }

    public String getMaquinaNome() { return maquinaNome; }
    public void setMaquinaNome(String maquinaNome) { this.maquinaNome = maquinaNome; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getPrioridade() { return prioridade; }
    public void setPrioridade(String prioridade) { this.prioridade = prioridade; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAbertoPor() { return abertoPor; }
    public void setAbertoPor(String abertoPor) { this.abertoPor = abertoPor; }

    public String getAbertoPorNome() { return abertoPorNome; }
    public void setAbertoPorNome(String abertoPorNome) { this.abertoPorNome = abertoPorNome; }

    public Timestamp getAbertoEm() { return abertoEm; }
    public void setAbertoEm(Timestamp abertoEm) { this.abertoEm = abertoEm; }

    public String getTecnicoUid() { return tecnicoUid; }
    public void setTecnicoUid(String tecnicoUid) { this.tecnicoUid = tecnicoUid; }

    public String getTecnicoNome() { return tecnicoNome; }
    public void setTecnicoNome(String tecnicoNome) { this.tecnicoNome = tecnicoNome; }

    public Timestamp getAssumidoEm() { return assumidoEm; }
    public void setAssumidoEm(Timestamp assumidoEm) { this.assumidoEm = assumidoEm; }

    public String getSolucao() { return solucao; }
    public void setSolucao(String solucao) { this.solucao = solucao; }

    public Timestamp getFechadoEm() { return fechadoEm; }
    public void setFechadoEm(Timestamp fechadoEm) { this.fechadoEm = fechadoEm; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public List<String> getHabilidades() { return habilidades; }
    public void setHabilidades(List<String> habilidades) { this.habilidades = habilidades; }
}