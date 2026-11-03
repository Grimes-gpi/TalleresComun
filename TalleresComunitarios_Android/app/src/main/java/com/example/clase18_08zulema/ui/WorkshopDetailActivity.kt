package com.example.clase18_08zulema.ui

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.clase18_08zulema.MainActivity
import com.example.clase18_08zulema.data.ApiResult
import com.example.clase18_08zulema.data.Registration
import com.example.clase18_08zulema.data.Workshop
import com.example.clase18_08zulema.data.WorkshopsRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class WorkshopDetailActivity : AppCompatActivity() {
    private var workshopId: Long = -1
    private lateinit var page: LinearLayout
    private lateinit var details: LinearLayout
    private lateinit var registrations: LinearLayout
    private var current: Workshop? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workshopId = intent.getLongExtra("workshop_id", -1)
        page = Ui.page(this)
        page.addView(Ui.body(this, "DETALLE DEL TALLER", 12f))
        page.addView(Ui.gap(this, 8))
        details = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        page.addView(details)
        page.addView(Ui.gap(this, 14))
        page.addView(Ui.title(this, "Personas inscritas", 19f))
        page.addView(Ui.gap(this, 6))
        registrations = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        page.addView(registrations)
        page.addView(Ui.gap(this, 14))
        page.addView(Ui.button(this, "Volver a talleres", Ui.muted) { finish() })
        setContentView(ScrollView(this).apply { addView(page) })
    }

    override fun onResume() {
        super.onResume()
        if (workshopId > 0) load()
    }

    private fun load() {
        details.removeAllViews()
        details.addView(Ui.body(this, "Cargando información…"))
        WorkshopsRepository.workshop(workshopId) { result ->
            when (result) {
                is ApiResult.Success -> { current = result.value; showWorkshop(result.value); loadRegistrations() }
                is ApiResult.Failure -> { details.removeAllViews(); details.addView(Ui.body(this, result.message)); details.addView(Ui.button(this, "Reintentar") { load() }) }
            }
        }
    }

    private fun showWorkshop(workshop: Workshop) {
        details.removeAllViews()
        details.addView(Ui.title(this, workshop.name))
        details.addView(Ui.gap(this, 8))
        details.addView(Ui.body(this, workshop.category ?: "Otros", 13f))
        details.addView(Ui.gap(this, 8))
        details.addView(Ui.body(this, workshop.description))
        details.addView(Ui.gap(this, 12))
        details.addView(Ui.body(this, "A cargo de ${workshop.instructor}"))
        details.addView(Ui.body(this, formatDate(workshop.dateTime)))
        details.addView(Ui.body(this, "${workshop.registeredPeople} de ${workshop.maxCapacity} inscritos · ${workshop.availableSlots} cupos libres"))
        details.addView(Ui.gap(this, 12))
        if (workshop.status == "ACTIVE" && workshop.availableSlots > 0) {
            details.addView(Ui.button(this, "Inscribir participante") {
                startActivity(Intent(this, RegistrationFormActivity::class.java)
                    .putExtra("workshop_id", workshop.id).putExtra("workshop_name", workshop.name))
            })
        } else {
            details.addView(Ui.body(this, if (workshop.status == "ACTIVE") "Este taller ya no tiene cupos." else "Este taller está cancelado."))
        }
        if (workshop.status == "ACTIVE") {
            details.addView(Ui.gap(this, 8))
            details.addView(Ui.button(this, "Editar taller", Ui.green) {
                startActivity(Intent(this, WorkshopFormActivity::class.java).putExtra("workshop_id", workshop.id))
            })
            details.addView(Ui.gap(this, 8))
            details.addView(Ui.button(this, "Cancelar taller", Ui.orange) { confirmCancel(workshop) })
        }
    }

    private fun confirmCancel(workshop: Workshop) {
        AlertDialog.Builder(this)
            .setTitle("¿Cancelar el taller?")
            .setMessage("“${workshop.name}” dejará de aceptar inscripciones.")
            .setNegativeButton("Volver", null)
            .setPositiveButton("Cancelar taller") { _, _ ->
                WorkshopsRepository.cancelWorkshop(workshop.id) { result ->
                    when (result) {
                        is ApiResult.Success -> { Ui.toast(this, "Taller cancelado."); load() }
                        is ApiResult.Failure -> Ui.toast(this, result.message)
                    }
                }
            }.show()
    }

    private fun loadRegistrations() {
        registrations.removeAllViews()
        registrations.addView(Ui.body(this, "Cargando inscripciones…"))
        WorkshopsRepository.registrations(workshopId) { result ->
            registrations.removeAllViews()
            when (result) {
                is ApiResult.Success -> showRegistrations(result.value)
                is ApiResult.Failure -> {
                    registrations.addView(Ui.body(this, result.message))
                    registrations.addView(Ui.button(this, "Reintentar") { loadRegistrations() })
                }
            }
        }
    }

    private fun showRegistrations(items: List<Registration>) {
        if (items.isEmpty()) {
            registrations.addView(Ui.body(this, "Todavía no hay personas inscritas."))
            return
        }
        items.forEach { person ->
            registrations.addView(Ui.body(this, "${person.beneficiaryName}  ·  ${person.beneficiaryDocument}"))
            registrations.addView(Ui.gap(this, 6))
        }
    }

    private fun formatDate(value: String): String = runCatching {
        LocalDateTime.parse(value).format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM · h:mm a", Locale("es", "CO")))
    }.getOrDefault(value.replace('T', ' '))
}
