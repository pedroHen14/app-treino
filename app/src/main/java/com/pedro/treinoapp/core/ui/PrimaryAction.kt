package com.pedro.treinoapp.core.ui

import androidx.annotation.IdRes
import androidx.appcompat.widget.Toolbar
import com.google.android.material.button.MaterialButton
import com.pedro.treinoapp.R

/**
 * Botão brasa de um item de menu com `app:actionLayout="@layout/action_primary_button"`: a ação
 * principal da tela (ex.: Finalizar, Salvar). Item com actionLayout não repassa o toque ao
 * listener do menu, por isso o clique é ligado em [bindPrimaryAction].
 */
fun Toolbar.primaryActionButton(@IdRes itemId: Int): MaterialButton? =
    menu.findItem(itemId)?.actionView?.findViewById(R.id.action_button)

/** Liga o botão brasa do item [itemId]: texto igual ao título do item e [onClick] no toque. */
fun Toolbar.bindPrimaryAction(@IdRes itemId: Int, onClick: () -> Unit) {
    val button = primaryActionButton(itemId) ?: return
    button.text = menu.findItem(itemId)?.title
    button.setOnClickListener { onClick() }
}
