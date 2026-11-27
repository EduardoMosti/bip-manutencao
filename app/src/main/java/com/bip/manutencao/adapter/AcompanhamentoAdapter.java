package com.bip.manutencao.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bip.manutencao.R;
import com.bip.manutencao.model.Chamado;
import com.google.android.material.chip.Chip;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * US-11 - Adapter dos chamados do operador.
 *
 * Mostra em cada linha a situação do chamado e, quando um técnico assumiu,
 * o nome dele e o horário previsto de chegada:
 *  - aberto:          "Nenhum técnico assumiu ainda..."
 *  - em atendimento:  "Carlos chega às 14:35"
 *  - encerrado:       "Atendido por Carlos"
 */
public class AcompanhamentoAdapter
        extends RecyclerView.Adapter<AcompanhamentoAdapter.AcompanhamentoViewHolder> {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final SimpleDateFormat FORMATO_DATA = new SimpleDateFormat("dd/MM HH:mm", PT_BR);
    private static final SimpleDateFormat FORMATO_HORA = new SimpleDateFormat("HH:mm", PT_BR);

    private final List<Chamado> chamados = new ArrayList<>();

    /** Troca o conteúdo da lista (chamado a cada atualização do snapshot listener). */
    public void atualizar(List<Chamado> novaLista) {
        chamados.clear();
        chamados.addAll(novaLista);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AcompanhamentoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_acompanhamento, parent, false);
        return new AcompanhamentoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AcompanhamentoViewHolder holder, int position) {
        Context contexto = holder.itemView.getContext();
        Chamado chamado = chamados.get(position);
        String status = chamado.getStatus();

        String tecnico = chamado.getTecnicoNome() == null || chamado.getTecnicoNome().isEmpty()
                ? contexto.getString(R.string.acompanhamento_tecnico_padrao)
                : chamado.getTecnicoNome();

        // 1) Selo e frase da situação, conforme o status gravado no Firestore
        int textoStatus;
        int corStatus;
        String situacao;
        if (Chamado.STATUS_ENCERRADO.equals(status)) {
            textoStatus = R.string.acompanhamento_status_encerrado;
            corStatus = R.color.bip_sucesso;
            situacao = contexto.getString(R.string.acompanhamento_encerrado, tecnico);
        } else if (Chamado.STATUS_EM_ATENDIMENTO.equals(status)) {
            textoStatus = R.string.acompanhamento_status_em_atendimento;
            corStatus = R.color.bip_amarelo;
            Timestamp previsao = chamado.getPrevisaoChegada();
            situacao = previsao == null
                    ? contexto.getString(R.string.acompanhamento_sem_previsao, tecnico)
                    : contexto.getString(R.string.acompanhamento_previsao, tecnico,
                    FORMATO_HORA.format(previsao.toDate()));
        } else {
            textoStatus = R.string.acompanhamento_status_aberto;
            corStatus = R.color.bip_erro;
            situacao = contexto.getString(R.string.acompanhamento_aguardando);
        }
        holder.chipStatus.setText(textoStatus);
        holder.chipStatus.setChipBackgroundColor(
                ColorStateList.valueOf(ContextCompat.getColor(contexto, corStatus)));
        holder.tvSituacao.setText(situacao);

        // 2) Máquina e descrição
        holder.tvMaquina.setText(contexto.getString(R.string.chamado_maquina_valor,
                textoOuVazio(chamado.getMaquinaId()), textoOuVazio(chamado.getMaquinaNome())));
        holder.tvDescricao.setText(textoOuVazio(chamado.getDescricao()));

        // 3) Quando foi aberto
        String quando = chamado.getAbertoEm() == null
                ? contexto.getString(R.string.acompanhamento_horario_pendente)
                : FORMATO_DATA.format(chamado.getAbertoEm().toDate());
        holder.tvAberto.setText(contexto.getString(R.string.acompanhamento_aberto_em, quando));
    }

    @Override
    public int getItemCount() {
        return chamados.size();
    }

    private static String textoOuVazio(String texto) {
        return texto == null ? "" : texto;
    }

    /** ViewHolder "static": guarda as referências das Views de uma linha. */
    static class AcompanhamentoViewHolder extends RecyclerView.ViewHolder {
        final Chip chipStatus;
        final TextView tvMaquina;
        final TextView tvDescricao;
        final TextView tvSituacao;
        final TextView tvAberto;

        AcompanhamentoViewHolder(@NonNull View itemView) {
            super(itemView);
            chipStatus = itemView.findViewById(R.id.chipStatusAcompanhamento);
            tvMaquina = itemView.findViewById(R.id.tvMaquinaAcompanhamento);
            tvDescricao = itemView.findViewById(R.id.tvDescricaoAcompanhamento);
            tvSituacao = itemView.findViewById(R.id.tvSituacaoAcompanhamento);
            tvAberto = itemView.findViewById(R.id.tvAbertoAcompanhamento);
        }
    }
}
