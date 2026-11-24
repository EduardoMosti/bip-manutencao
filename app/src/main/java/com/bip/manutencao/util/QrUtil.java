package com.bip.manutencao.util;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

/**
 * Gera a IMAGEM de um QR Code a partir de um texto.
 *
 * Usa o BarcodeEncoder da biblioteca ZXing Android Embedded, que JÁ está no
 * projeto (é a mesma lib que o Eduardo usa para LER o QR). O material do
 * professor (Projeto 07) cita exatamente esta classe.
 *
 * Fica em util/ porque será reaproveitada:
 *  - US-01: QR da máquina (conteúdo = código, ex.: MAQ-001)
 *  - US-12: QR do crachá/matrícula do operador
 */
public final class QrUtil {

    private static final String TAG = "QrUtil";

    private QrUtil() {
        // classe utilitária: só métodos static, ninguém precisa criar objeto dela
    }

    /**
     * @param conteudo    texto que vai "dentro" do QR
     * @param tamanhoPx   largura/altura da imagem em PIXELS
     * @return o Bitmap do QR, ou null se der erro (a tela trata o null)
     */
    public static Bitmap gerar(String conteudo, int tamanhoPx) {
        try {
            BarcodeEncoder encoder = new BarcodeEncoder();
            return encoder.encodeBitmap(conteudo, BarcodeFormat.QR_CODE, tamanhoPx, tamanhoPx);
        } catch (WriterException e) {
            Log.e(TAG, "Falha ao gerar QR de: " + conteudo, e);
            return null;
        }
    }
}
