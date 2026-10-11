package com.pedro.treinoapp.core.text

import java.text.Normalizer
import java.util.Locale

private val whitespace = Regex("\\s+")
private val combiningMarks = Regex("\\p{Mn}+")

/** Remove espaços das pontas e junta espaços repetidos: "  Supino   reto " -> "Supino reto". */
fun String.normalizeName(): String = trim().replace(whitespace, " ")

/** Chave de unicidade: ignora maiúsculas e minúsculas, inclusive em letras acentuadas. */
fun String.toNameKey(): String = normalizeName().lowercase(Locale.ROOT)

/** Chave de busca: além de maiúsculas, ignora acentos ("elevacao" encontra "Elevação"). */
fun String.toSearchKey(): String =
    Normalizer.normalize(toNameKey(), Normalizer.Form.NFD).replace(combiningMarks, "")
