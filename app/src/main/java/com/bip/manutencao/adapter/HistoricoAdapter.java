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
 * US-06 (C6) - Adapter da lista do histórico da máquina.
 *
 * Cada linha mostra a situação, a descrição, a solução, quem atendeu,
 * as datas de abertura e fechamento e a DURAÇÃO do reparo
 * (fechadoEm - abertoEm). Chamado ainda não encerrado aparece como
 * "em andamento", sem duração final.
 */
public class HistoricoAdapter extends RecyclerView.Adapter<HistoricoAdapter.HistoricoViewHolder> {

    private static final SimpleDateFormat FORMATO_DATA =
            new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR"));

    private final List<Chamado> chamados = new ArrayList<>();

    /** Troca o conteúdo da lista (chamado a cada atualização do snapshot listener). */
    public void atualizar(List<Chamado> novaLista) {
        chamados.clear();
        chamados.addAll(novaLista);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoricoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_historico, parent, false);
        return new HistoricoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoricoViewHolder holder, int position) {
        Context contexto = holder.itemView.getContext();
        Chamado chamado = chamados.get(position);
        String status = chamado.getStatus();

        // 1) Selo da situação: texto e cor conforme o status gravado no Firestore
        int textoStatus;
        int corStatus;
        if (Chamado.STATUS_ENCERRADO.equals(status)) {
            textoStatus = R.string.historico_status_encerrado;
            corStatus = R.color.bip_sucesso;
        } else if (Chamado.STATUS_EM_ATENDIMENTO.equals(status)) {
            textoStatus = R.string.historico_status_em_atendimento;
            corStatus = R.color.bip_amarelo;
        } else {
            textoStatus = R.string.historico_status_aberto;
            corStatus = R.color.bip_erro;
        }
        holder.chipStatus.setText(textoStatus);
        holder.chipStatus.setChipBackgroundColor(
                ColorStateList.valueOf(ContextCompat.getColor(contexto, corStatus)));

        // 2) Descrição do problema
        holder.tvDescricao.setText(textoOu(chamado.getDescricao(), ""));

        // 3) Solução: só aparece quando existir
        String solucao = chamado.getSolucao();
        if (solucao == null || solucao.trim().isEmpty()) {
            holder.tvSolucao.setVisibility(View.GONE);
        } else {
            holder.tvSolucao.setVisibility(View.VISIBLE);
            holder.tvSolucao.setText(contexto.getString(R.string.historico_solucao, solucao));
        }

        // 4) Técnico: só aparece quando alguém assumiu
        String tecnico = chamado.getTecnicoNome();
        if (tecnico == null || tecnico.trim().isEmpty()) {
            holder.tvTecnico.setVisibility(View.GONE);
        } else {
            holder.tvTecnico.setVisibility(View.VISIBLE);
            holder.tvTecnico.setText(contexto.getString(R.string.historico_tecnico, tecnico));
        }

        // 5) Abertura: "Aberto em 25/11/2026 08:42 por Maria"
        String quem = textoOu(chamado.getAbertoPorNome(),
                contexto.getString(R.string.historico_desconhecido));
        holder.tvAberto.setText(contexto.getString(R.string.historico_aberto_em,
                formatarData(contexto, chamado.getAbertoEm()), quem));

        // 6) Fechamento e duração
        Timestamp abertoEm = chamado.getAbertoEm();
        Timestamp fechadoEm = chamado.getFechadoEm();
        if (fechadoEm != null && abertoEm != null) {
            holder.tvEncerrado.setVisibility(View.VISIBLE);
            holder.tvEncerrado.setText(contexto.getString(R.string.historico_encerrado_em,
                    formatarData(contexto, fechadoEm)));

            long milis = fechadoEm.toDate().getTime() - abertoEm.toDate().getTime();
            holder.tvDuracao.setText(contexto.getString(R.string.historico_duracao,
                    formatarDuracao(contexto, milis)));
            holder.tvDuracao.setTextColor(ContextCompat.getColor(contexto, R.color.bip_amarelo));
        } else {
            // Ainda não encerrado: sem data de fechamento e sem duração final
            holder.tvEncerrado.setVisibility(View.GONE);
            holder.tvDuracao.setText(R.string.historico_em_andamento);
            holder.tvDuracao.setTextColor(
                    ContextCompat.getColor(contexto, R.color.bip_texto_secundario));
        }
    }

    @Override
    public int getItemCount() {
        return chamados.size();
    }

    /**
     * Transforma milissegundos em texto curto:
     * menos de 1 hora -> "45 min"; menos de 1 dia -> "1 h 25 min"; mais -> "2 d 3 h".
     * Valor negativo (relógio errado) vira 0 para não mostrar duração estranha.
     */
    public static String formatarDuracao(Context contexto, long milis) {
        long totalMinutos = Math.max(0, milis / 60000);
        long dias = totalMinutos / (60 * 24);
        long horas = (totalMinutos / 60) % 24;
        long minutos = totalMinutos % 60;

        if (dias > 0) {
            return contexto.getString(R.string.historico_duracao_dias, dias, horas);
        }
        if (horas > 0) {
            return contexto.getString(R.string.historico_duracao_h, horas, minutos);
        }
        return contexto.getString(R.string.historico_duracao_min, minutos);
    }

    private static String formatarData(Context contexto, Timestamp data) {
        if (data == null) {
            return contexto.getString(R.string.historico_horario_pendente);
        }
        return FORMATO_DATA.format(data.toDate());
    }

    private static String textoOu(String texto, String padrao) {
        return texto == null || texto.trim().isEmpty() ? padrao : texto;
    }

    /** ViewHolder "static": guarda as referências das Views de uma linha. */
    static class HistoricoViewHolder extends RecyclerView.ViewHolder {
        final Chip chipStatus;
        final TextView tvDescricao;
        final TextView tvSolucao;
        final TextView tvTecnico;
        final TextView tvAberto;
        final TextView tvEncerrado;
        final TextView tvDuracao;

        HistoricoViewHolder(@NonNull View itemView) {
            super(itemView);
            chipStatus = itemView.findViewById(R.id.chipStatusHistorico);
            tvDescricao = itemView.findViewById(R.id.tvDescricaoHistorico);
            tvSolucao = itemView.findViewById(R.id.tvSolucaoHistorico);
            tvTecnico = itemView.findViewById(R.id.tvTecnicoHistorico);
            tvAberto = itemView.findViewById(R.id.tvAbertoHistorico);
            tvEncerrado = itemView.findViewById(R.id.tvEncerradoHistorico);
            tvDuracao = itemView.findViewById(R.id.tvDuracaoHistorico);
        }
    }
}
