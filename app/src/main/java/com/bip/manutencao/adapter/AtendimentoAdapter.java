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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter da lista "Meus atendimentos" (RecyclerView - NC5), US-05.
 *
 * Mesmo padrão do ChamadoAdapter (US-03/04), mas em arquivo próprio para não
 * mexer no que é da outra história. Cada linha tem o botão "Encerrar"; o clique
 * é repassado para a tela pela interface OnEncerrarListener.
 */
public class AtendimentoAdapter
        extends RecyclerView.Adapter<AtendimentoAdapter.AtendimentoViewHolder> {

    private static final SimpleDateFormat FORMATO_DATA =
            new SimpleDateFormat("dd/MM HH:mm", Locale.forLanguageTag("pt-BR"));

    /** Interface de evento: a tela decide o que fazer ao tocar em "Encerrar". */
    public interface OnEncerrarListener {
        void onEncerrar(Chamado chamado);
    }

    private final List<Chamado> chamados = new ArrayList<>();
    private OnEncerrarListener listenerEncerrar;

    public void setOnEncerrarListener(OnEncerrarListener listener) {
        this.listenerEncerrar = listener;
    }

    /** Troca o conteúdo da lista (chamado a cada atualização do snapshot listener). */
    public void atualizar(List<Chamado> novaLista) {
        chamados.clear();
        chamados.addAll(novaLista);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AtendimentoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_atendimento, parent, false);
        return new AtendimentoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AtendimentoViewHolder holder, int position) {
        Context contexto = holder.itemView.getContext();
        Chamado chamado = chamados.get(position);

        // Selo de prioridade (mesmas cores da fila)
        int textoPrioridade;
        int corPrioridade;
        if (Chamado.PRIORIDADE_ALTA.equals(chamado.getPrioridade())) {
            textoPrioridade = R.string.chamado_prioridade_alta;
            corPrioridade = R.color.bip_erro;
        } else if (Chamado.PRIORIDADE_MEDIA.equals(chamado.getPrioridade())) {
            textoPrioridade = R.string.chamado_prioridade_media;
            corPrioridade = R.color.bip_amarelo;
        } else {
            textoPrioridade = R.string.chamado_prioridade_baixa;
            corPrioridade = R.color.bip_sucesso;
        }
        holder.chipPrioridade.setText(textoPrioridade);
        holder.chipPrioridade.setChipBackgroundColor(
                ColorStateList.valueOf(ContextCompat.getColor(contexto, corPrioridade)));

        holder.tvMaquina.setText(contexto.getString(R.string.chamado_maquina_valor,
                textoOuVazio(chamado.getMaquinaId()), textoOuVazio(chamado.getMaquinaNome())));
        holder.tvDescricao.setText(textoOuVazio(chamado.getDescricao()));

        // "Aberto em 25/11 12:08 · assumido em 25/11 12:30"
        holder.tvInfo.setText(contexto.getString(R.string.atendimentos_info,
                formatar(contexto, chamado.getAbertoEm()),
                formatar(contexto, chamado.getAssumidoEm())));

        // Posição ATUAL (getBindingAdapterPosition): a lista muda em tempo real
        holder.btnEncerrar.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listenerEncerrar != null) {
                listenerEncerrar.onEncerrar(chamados.get(pos));
            }
        });
    }

    @Override
    public int getItemCount() {
        return chamados.size();
    }

    private static String formatar(Context contexto, com.google.firebase.Timestamp horario) {
        return horario == null
                ? contexto.getString(R.string.fila_horario_pendente)
                : FORMATO_DATA.format(horario.toDate());
    }

    private static String textoOuVazio(String texto) {
        return texto == null ? "" : texto;
    }

    /** ViewHolder "static": guarda as referências das Views de uma linha. */
    static class AtendimentoViewHolder extends RecyclerView.ViewHolder {
        final Chip chipPrioridade;
        final TextView tvMaquina;
        final TextView tvDescricao;
        final TextView tvInfo;
        final MaterialButton btnEncerrar;

        AtendimentoViewHolder(@NonNull View itemView) {
            super(itemView);
            chipPrioridade = itemView.findViewById(R.id.chipPrioridadeAtendimento);
            tvMaquina = itemView.findViewById(R.id.tvMaquinaAtendimento);
            tvDescricao = itemView.findViewById(R.id.tvDescricaoAtendimento);
            tvInfo = itemView.findViewById(R.id.tvInfoAtendimento);
            btnEncerrar = itemView.findViewById(R.id.btnEncerrarAtendimento);
        }
    }
}
