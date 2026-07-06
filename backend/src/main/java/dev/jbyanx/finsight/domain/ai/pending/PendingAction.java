package dev.jbyanx.finsight.domain.ai.pending;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Describe una mutación PROPUESTA que espera confirmación del usuario.
 *
 * Se genera cuando una tool de escritura devuelve un "preview" (sin ejecutar
 * nada) y se guarda en la conversación (Mongo). Cuando el usuario confirma, el
 * CÓDIGO (no el modelo) la ejecuta contra el service correspondiente.
 *
 * type identifica qué mutación es (ej. "PAY_DEBT"); params lleva lo necesario
 * para ejecutarla (ej. debtId, amount). Genérico a propósito, para que replicar
 * nuevas acciones de escritura sea solo agregar un type + su ejecución.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PendingAction {

    /** Tipo de mutación. Ej: "PAY_DEBT". */
    private String type;

    /** Parámetros necesarios para ejecutarla. Ej: {"debtId": "...", "amount": "250000"}. */
    private Map<String, String> params = new HashMap<>();

    /** Resumen legible de la consecuencia, ya calculado, para re-mostrar si hace falta. */
    private String previewText;

    public static PendingAction of(String type, String previewText) {
        return new PendingAction(type, new HashMap<>(), previewText);
    }

    public PendingAction with(String key, String value) {
        this.params.put(key, value);
        return this;
    }

    public String param(String key) {
        return params.get(key);
    }
}