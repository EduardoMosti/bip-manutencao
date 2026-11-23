package com.bip.manutencao.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bip.manutencao.R;
import com.bip.manutencao.model.Maquina;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter da lista de máquinas (RecyclerView - NC5).
 *
 * O Adapter é a "ponte" entre a lista de objetos (List<Maquina>) e as linhas
 * na tela (item_maquina.xml). O RecyclerView pede ao Adapter:
 *  - onCreateViewHolder: "crie uma linha nova" (infla o XML, poucas vezes)
 *  - onBindViewHolder:   "preencha esta linha com o item da posição X" (muitas vezes, ao rolar)
 */
public class MaquinaAdapter extends RecyclerView.Adapter<MaquinaAdapter.MaquinaViewHolder> {

    /** Interface de evento: a Activity implementa e decide o que fazer no clique. */
    public interface OnMaquinaClickListener {
        void onMaquinaClick(Maquina maquina);
    }

    private final List<Maquina> maquinas = new ArrayList<>();
    private final OnMaquinaClickListener listener;

    public MaquinaAdapter(OnMaquinaClickListener listener) {
        this.listener = listener;
    }

    /** Troca o conteúdo da lista (chamado a cada atualização do snapshot listener). */
    public void atualizar(List<Maquina> novaLista) {
        maquinas.clear();
        maquinas.addAll(novaLista);
        notifyDataSetChanged(); // avisa o RecyclerView para redesenhar
    }

    @NonNull
    @Override
    public MaquinaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Infla o layout de UMA linha
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_maquina, parent, false);
        return new MaquinaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MaquinaViewHolder holder, int position) {
        Maquina m = maquinas.get(position);
        holder.tvCodigo.setText(m.getCodigo());
        holder.tvNome.setText(m.getNome());
        holder.tvSetor.setText(holder.itemView.getContext()
                .getString(R.string.maquina_setor_formato, m.getSetor()));

        // Clique na linha: pega a posição ATUAL (getBindingAdapterPosition),
        // porque a lista pode ter mudado desde que a linha foi preenchida.
        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                listener.onMaquinaClick(maquinas.get(pos));
            }
        });
    }

    @Override
    public int getItemCount() {
        return maquinas.size();
    }

    /** ViewHolder "static": guarda as referências das Views de uma linha (evita findViewById repetido). */
    static class MaquinaViewHolder extends RecyclerView.ViewHolder {
        final TextView tvCodigo;
        final TextView tvNome;
        final TextView tvSetor;

        MaquinaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCodigo = itemView.findViewById(R.id.tvCodigoMaquina);
            tvNome = itemView.findViewById(R.id.tvNomeMaquina);
            tvSetor = itemView.findViewById(R.id.tvSetorMaquina);
        }
    }
}
