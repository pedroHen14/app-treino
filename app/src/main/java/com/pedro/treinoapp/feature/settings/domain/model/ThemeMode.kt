package com.pedro.treinoapp.feature.settings.domain.model

/**
 * Tema do app. Ferro (escuro) é o padrão: o app é usado na academia, entre séries, muitas
 * vezes com pouca luz. Giz é o tema claro; [SYSTEM] segue a configuração do aparelho.
 */
enum class ThemeMode {
    FERRO,
    GIZ,
    SYSTEM,
    ;

    companion object {
        val DEFAULT = FERRO

        /** Valor salvo nas preferências; ausente ou desconhecido (ex.: versão antiga) volta ao padrão. */
        fun fromStorage(value: String?): ThemeMode = entries.firstOrNull { it.name == value } ?: DEFAULT
    }
}
