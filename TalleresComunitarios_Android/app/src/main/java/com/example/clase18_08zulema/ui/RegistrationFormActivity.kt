package com.example.clase18_08zulema.ui

import android.os.Bundle
import android.text.InputType
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import com.example.clase18_08zulema.data.ApiResult
import com.example.clase18_08zulema.data.RegistrationRequest
import com.example.clase18_08zulema.data.WorkshopsRepository

class RegistrationFormActivity : AppCompatActivity() {
    private var workshopId: Long = -1
    private lateinit var beneficiary: com.google.android.material.textfield.TextInputEditText
    private lateinit var document: com.google.android.material.textfield.TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workshopId = intent.getLongExtra("workshop_id", -1)
        val workshopName = intent.getStringExtra("workshop_name").orEmpty()
        val page = Ui.page(this)
        page.addView(Ui.title(this, "Inscribir participante"))
        page.addView(Ui.gap(this, 6))
        page.addView(Ui.body(this, workshopName))
        page.addView(Ui.gap(this, 20))
        beneficiary = addField(page, "Nombre completo", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
        document = addField(page, "Documento de identidad", InputType.TYPE_CLASS_TEXT)
        page.addView(Ui.gap(this, 8))
        page.addView(Ui.button(this, "Confirmar inscripción") { register() })
        page.addView(Ui.gap(this, 8))
        page.addView(Ui.button(this, "Cancelar", Ui.muted) { finish() })
        setContentView(ScrollView(this).apply { addView(page) })
    }

    private fun addField(parent: android.widget.LinearLayout, label: String, type: Int) = Ui.field(this, label, type).let { (layout, edit) ->
        parent.addView(layout, android.widget.LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = Ui.dp(this@RegistrationFormActivity, 12) })
        edit
    }

    private fun register() {
        val name = beneficiary.text?.toString()?.trim().orEmpty()
        val idDocument = document.text?.toString()?.trim().orEmpty()
        if (workshopId < 1 || name.isBlank() || idDocument.isBlank()) {
            Ui.toast(this, "Completa el nombre y el documento.")
            return
        }
        WorkshopsRepository.register(RegistrationRequest(workshopId, name, idDocument)) { result ->
            when (result) {
                is ApiResult.Success -> { Ui.toast(this, "Inscripción confirmada para $name."); finish() }
                is ApiResult.Failure -> Ui.toast(this, result.message)
            }
        }
    }
}
