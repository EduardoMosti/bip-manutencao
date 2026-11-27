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
import com.bip.manutencao.util.AreaHabilidades;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter da fila de chamados (RecyclerView - NC5), US-03.
 *
 * Igual ao MaquinaAdapter: a "ponte" entre a lista de objetos (List<Chamado>)
 * e as linhas na tela (item_chamado.xml).
 *  - onCreateViewHolder: cria uma linha nova (infla o XML, poucas vezes)
 *  - onBindViewHolder:   preenche a linha com o chamado da posição X (ao rolar)
 *
 * A lista já chega ordenada do ChamadoRepository (prioridade e depois horário).
 *
 * US-04: cada linha tem o botão "Assumir". O clique é repassado para a tela
 * pela interface OnAssumirListener (padrão do curso: interface de evento no Adapter).
 */
public class ChamadoAdapter extends RecyclerView.Adapter<ChamadoAdapter.ChamadoViewHolder> {

    private static final SimpleDateFormat FORMATO_DATA =
            new SimpleDateFormat("dd/MM HH:mm", Locale.forLanguageTag("pt-BR"));

    /** US-04: interface de evento - a tela decide o que fazer ao tocar em "Assumir". */
    public interface OnAssumirListener {
        void onAssumir(Chamado chamado);
    }

    private final List<Chamado> chamados = new ArrayList<>();
    private OnAssumirListener listenerAssumir; // pode ser null (botão some)

    /** US-04: liga o botão "Assumir" das linhas a quem vai tratar o clique. */
    public void setOnAssumirListener(OnAssumirListener listener) {
        this.listenerAssumir = listener;
        notifyDataSetChanged(); // redesenha para mostrar o botão
    }

    /** Troca o conteúdo da lista (chamado a cada atualização do snapshot listener). */
    public void atualizar(List<Chamado> novaLista) {
        chamados.clear();
        chamados.addAll(novaLista);
        notifyDataSetChanged(); // avisa o RecyclerView para redesenhar
    }

    @NonNull
    @Override
    public ChamadoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chamado, parent, false);
        return new ChamadoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChamadoViewHolder holder, int position) {
        Context contexto = holder.itemView.getContext();
        Chamado chamado = chamados.get(position);

        // Selo de prioridade: texto e cor conforme a prioridade do chamado
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
        // US-14: área e habilidades exigidas (ou "não informadas")
        holder.tvArea.setText(AreaHabilidades.texto(contexto, chamado));

        // "Aberto por Fulano em 25/11 12:08" (sem horário ainda: "agora")
        String quem = chamado.getAbertoPorNome() == null || chamado.getAbertoPorNome().isEmpty()
                ? contexto.getString(R.string.fila_operador_desconhecido)
                : chamado.getAbertoPorNome();
        String quando = chamado.getAbertoEm() == null
                ? contexto.getString(R.string.fila_horario_pendente)
                : FORMATO_DATA.format(chamado.getAbertoEm().toDate());
        holder.tvInfo.setText(contexto.getString(R.string.fila_aberto_info, quem, quando));

        // US-04: botão "Assumir". Usa a posição ATUAL (getBindingAdapterPosition),
        // porque a fila muda em tempo real e a linha pode ter mudado de lugar.
        holder.btnAssumir.setVisibility(listenerAssumir != null ? View.VISIBLE : View.GONE);
        holder.btnAssumir.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listenerAssumir != null) {
                listenerAssumir.onAssumir(chamados.get(pos));
            }
        });
    }

    @Override
    public int getItemCount() {
        return chamados.size();
    }

    private static String textoOuVazio(String texto) {
        return texto == null ? "" : texto;
    }

    /** ViewHolder "static": guarda as referências das Views de uma linha. */
    static class ChamadoViewHolder extends RecyclerView.ViewHolder {
        final Chip chipPrioridade;
        final TextView tvMaquina;
        final TextView tvDescricao;
        final TextView tvInfo;
        final TextView tvArea; // US-14
        final MaterialButton btnAssumir;

        ChamadoViewHolder(@NonNull View itemView) {
            super(itemView);
            chipPrioridade = itemView.findViewById(R.id.chipPrioridade);
            tvMaquina = itemView.findViewById(R.id.tvMaquinaChamado);
            tvDescricao = itemView.findViewById(R.id.tvDescricaoChamado);
            tvInfo = itemView.findViewById(R.id.tvInfoChamado);
            tvArea = itemView.findViewById(R.id.tvAreaChamado);
            btnAssumir = itemView.findViewById(R.id.btnAssumirChamado);
        }
    }
}