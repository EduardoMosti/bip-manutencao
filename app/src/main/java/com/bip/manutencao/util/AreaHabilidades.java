package com.bip.manutencao.util;

import android.content.Context;

import com.bip.manutencao.R;
import com.bip.manutencao.model.Chamado;

import java.util.List;

/**
 * US-14 - Monta o texto de área e habilidades exigidas por um chamado,
 * para o técnico saber se é a pessoa indicada antes de assumir.
 *
 * Exemplos:
 *  - "Área: Elétrica · Habilidades: Solda, Automação / CLP"
 *  - "Área: Mecânica"
 *  - "Área e habilidades não informadas" (chamado sem essas informações)
 */
public final class AreaHabilidades {

    private AreaHabilidades() {
    }

    public static String texto(Context contexto, Chamado chamado) {
        String area = nomeDaArea(contexto, chamado.getArea());
        List<String> habilidades = chamado.getHabilidades();
        boolean temHabilidades = habilidades != null && !habilidades.isEmpty();

        if (area == null && !temHabilidades) {
            return contexto.getString(R.string.area_habilidades_nao_informadas);
        }

        StringBuilder texto = new StringBuilder();
        if (area != null) {
            texto.append(contexto.getString(R.string.area_valor, area));
        }
        if (temHabilidades) {
            if (texto.length() > 0) {
                texto.append(contexto.getString(R.string.area_habilidades_separador));
            }
            texto.append(contexto.getString(R.string.habilidades_valor,
                    String.join(", ", habilidades)));
        }
        return texto.toString();
    }

    /** Converte o valor gravado ("eletrica") no nome para a tela ("Elétrica"). */
    private static String nomeDaArea(Context contexto, String area) {
        if (Chamado.AREA_MECANICA.equals(area)) {
            return contexto.getString(R.string.area_mecanica);
        } else if (Chamado.AREA_ELETRICA.equals(area)) {
            return contexto.getString(R.string.area_eletrica);
        } else if (Chamado.AREA_HIDRAULICA.equals(area)) {
            return contexto.getString(R.string.area_hidraulica);
        } else if (Chamado.AREA_PNEUMATICA.equals(area)) {
            return contexto.getString(R.string.area_pneumatica);
        }
        return null;
    }
}
