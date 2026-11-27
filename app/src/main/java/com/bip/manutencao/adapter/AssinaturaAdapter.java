package com.bip.manutencao.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bip.manutencao.R;
import com.bip.manutencao.model.Chamado;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter da lista "Assinar encerramento" (RecyclerView - NC5), US-12.
 *
 * Mostra os chamados que o operador abriu, o técnico já encerrou e ainda não foram
 * assinados. O clique em "Assinar" é repassado para a tela pela interface
 * OnAssinarListener (a tela abre o leitor de QR).
 */
public class AssinaturaAdapter
        extends RecyclerView.Adapter<AssinaturaAdapter.AssinaturaViewHolder> {

    private static final SimpleDateFormat FORMATO_DATA =
            new SimpleDateFormat("dd/MM HH:mm", Locale.forLanguageTag("pt-BR"));

    /** Interface de evento: a tela decide o que fazer ao tocar em "Assinar". */
    public interface OnAssinarListener {
        void onAssinar(Chamado chamado);
    }

    private final List<Chamado> chamados = new ArrayList<>();
    private OnAssinarListener listenerAssinar;

    public void setOnAssinarListener(OnAssinarListener listener) {
        this.listenerAssinar = listener;
    }

    /** Troca o conteúdo da lista (chamado a cada atualização do snapshot listener). */
    public void atualizar(List<Chamado> novaLista) {
        chamados.clear();
        chamados.addAll(novaLista);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AssinaturaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_assinatura, parent, false);
        return new AssinaturaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AssinaturaViewHolder holder, int position) {
        Context contexto = holder.itemView.getContext();
        Chamado chamado = chamados.get(position);

        holder.tvMaquina.setText(contexto.getString(R.string.chamado_maquina_valor,
                textoOuVazio(chamado.getMaquinaId()), textoOuVazio(chamado.getMaquinaNome())));
        holder.tvDescricao.setText(textoOuVazio(chamado.getDescricao()));
        holder.tvSolucao.setText(contexto.getString(R.string.assinatura_solucao,
                textoOuVazio(chamado.getSolucao())));

        // "Técnico: Carlos · Encerrado em 25/11 14:10"
        String tecnico = contexto.getString(R.string.assinatura_tecnico,
                textoOuVazio(chamado.getTecnicoNome()));
        String encerrado = contexto.getString(R.string.assinatura_encerrado_em,
                formatar(contexto, chamado.getFechadoEm()));
        holder.tvInfo.setText(tecnico + " · " + encerrado);

        // Posição ATUAL (getBindingAdapterPosition): a lista muda em tempo real
        holder.btnAssinar.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listenerAssinar != null) {
                listenerAssinar.onAssinar(chamados.get(pos));
            }
        });
    }

    @Override
    public int getItemCount() {
        return chamados.size();
    }

    private static String formatar(Context contexto, Timestamp horario) {
        return horario == null
                ? contexto.getString(R.string.fila_horario_pendente)
                : FORMATO_DATA.format(horario.toDate());
    }

    private static String textoOuVazio(String texto) {
        return texto == null ? "" : texto;
    }

    /** ViewHolder "static": guarda as referências das Views de uma linha. */
    static class AssinaturaViewHolder extends RecyclerView.ViewHolder {
        final TextView tvMaquina;
        final TextView tvDescricao;
        final TextView tvSolucao;
        final TextView tvInfo;
        final MaterialButton btnAssinar;

        AssinaturaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMaquina = itemView.findViewById(R.id.tvMaquinaAssinatura);
            tvDescricao = itemView.findViewById(R.id.tvDescricaoAssinatura);
            tvSolucao = itemView.findViewById(R.id.tvSolucaoAssinatura);
            tvInfo = itemView.findViewById(R.id.tvInfoAssinatura);
            btnAssinar = itemView.findViewById(R.id.btnAssinar);
        }
    }
}
