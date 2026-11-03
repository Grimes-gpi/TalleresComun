package com.example.clase18_08zulema.ui

import android.os.Bundle
import android.text.InputType
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import com.example.clase18_08zulema.data.ApiResult
import com.example.clase18_08zulema.data.WorkshopRequest
import com.example.clase18_08zulema.data.WorkshopsRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

class WorkshopFormActivity : AppCompatActivity() {
    private var workshopId: Long? = null
    private var selectedDate: LocalDate? = null
    private var selectedTime: LocalTime? = null
    private lateinit var name: com.google.android.material.textfield.TextInputEditText
    private lateinit var description: com.google.android.material.textfield.TextInputEditText
    private lateinit var instructor: com.google.android.material.textfield.TextInputEditText
    private lateinit var category: com.google.android.material.textfield.TextInputEditText
    private lateinit var imageUrl: com.google.android.material.textfield.TextInputEditText
    private lateinit var capacity: com.google.android.material.textfield.TextInputEditText
    private lateinit var dateButton: MaterialButton
    private lateinit var timeButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workshopId = intent.getLongExtra("workshop_id", -1L).takeIf { it > 0 }
        title = if (workshopId == null) "Nuevo taller" else "Editar taller"
        val page = Ui.page(this)
        page.addView(Ui.title(this, title.toString()))
        page.addView(Ui.gap(this, 6))
        page.addView(Ui.body(this, "Completa la información que verá el equipo coordinador."))
        page.addView(Ui.gap(this, 20))
        name = addField(page, "Nombre del taller", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
        description = addField(page, "Descripción", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE)
        instructor = addField(page, "Persona instructora", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
        category = addField(page, "Categoría · Cocina, Tecnología…", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
        imageUrl = addField(page, "Enlace de imagen HTTPS (opcional)", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI)
        capacity = addField(page, "Cupo máximo", InputType.TYPE_CLASS_NUMBER)
        page.addView(Ui.body(this, "Fecha y hora", 14f))
        page.addView(Ui.gap(this, 6))
        dateButton = Ui.button(this, "Seleccionar fecha") { chooseDate() }
        page.addView(dateButton)
        page.addView(Ui.gap(this, 8))
        timeButton = Ui.button(this, "Seleccionar hora") { chooseTime() }
        page.addView(timeButton)
        page.addView(Ui.gap(this, 16))
        page.addView(Ui.button(this, "Guardar taller") { save() })
        page.addView(Ui.gap(this, 8))
        page.addView(Ui.button(this, "Volver", Ui.muted) { finish() })
        setContentView(ScrollView(this).apply { addView(page) })
        workshopId?.let(::loadWorkshop)
    }

    private fun addField(parent: android.widget.LinearLayout, label: String, type: Int) = Ui.field(this, label, type).let { (layout, edit) ->
        parent.addView(layout, android.widget.LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = Ui.dp(this@WorkshopFormActivity, 12) })
        edit
    }

    private fun chooseDate() {
        val selection = selectedDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
            ?: MaterialDatePicker.todayInUtcMilliseconds()
        MaterialDatePicker.Builder.datePicker()
            .setTitleText("Selecciona el día del taller")
            .setSelection(selection)
            .build()
            .apply {
                addOnPositiveButtonClickListener { millis ->
                    selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    dateButton.text = selectedDate!!.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "CO")))
                }
                show(supportFragmentManager, "workshop-date")
            }
    }

    private fun chooseTime() {
        MaterialTimePicker.Builder()
            .setTitleText("Selecciona la hora")
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(selectedTime?.hour ?: 9)
            .setMinute(selectedTime?.minute ?: 0)
            .build()
            .apply {
                addOnPositiveButtonClickListener {
                    selectedTime = LocalTime.of(hour, minute)
                    timeButton.text = selectedTime!!.format(DateTimeFormatter.ofPattern("HH:mm"))
                }
                show(supportFragmentManager, "workshop-time")
            }
    }

    private fun loadWorkshop(id: Long) {
        WorkshopsRepository.workshop(id) { result ->
            when (result) {
                is ApiResult.Success -> {
                    val workshop = result.value
                    name.setText(workshop.name)
                    description.setText(workshop.description)
                    instructor.setText(workshop.instructor)
                    category.setText(workshop.category ?: "Otros")
                    imageUrl.setText(workshop.imageUrl.orEmpty())
                    capacity.setText(workshop.maxCapacity.toString())
                    runCatching { LocalDateTime.parse(workshop.dateTime) }.getOrNull()?.let { dateTime ->
                        selectedDate = dateTime.toLocalDate()
                        selectedTime = dateTime.toLocalTime()
                        dateButton.text = selectedDate!!.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "CO")))
                        timeButton.text = selectedTime!!.format(DateTimeFormatter.ofPattern("HH:mm"))
                    }
                }
                is ApiResult.Failure -> { Ui.toast(this, result.message); finish() }
            }
        }
    }

    private fun save() {
        val max = capacity.text?.toString()?.toIntOrNull()
        val date = selectedDate
        val time = selectedTime
        val categoryValue = category.text?.toString()?.trim().orEmpty()
        val imageValue = imageUrl.text?.toString()?.trim().orEmpty()
        if (name.text.isNullOrBlank() || description.text.isNullOrBlank() || instructor.text.isNullOrBlank() || categoryValue.isBlank() || max == null || max < 1) {
            Ui.toast(this, "Completa los campos y usa un cupo mayor que cero.")
            return
        }
        if (date == null || time == null) {
            Ui.toast(this, "Selecciona el día y la hora del taller.")
            return
        }
        if (imageValue.isNotBlank() && (!imageValue.startsWith("https://", true) || !android.util.Patterns.WEB_URL.matcher(imageValue).matches())) {
            Ui.toast(this, "El enlace de imagen debe ser una URL HTTPS válida.")
            return
        }
        val dateTime = date.atTime(time).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
        val request = WorkshopRequest(
            name.text.toString().trim(), description.text.toString().trim(), instructor.text.toString().trim(),
            categoryValue, imageValue.ifBlank { null }, max, dateTime
        )
        WorkshopsRepository.saveWorkshop(workshopId, request) { result ->
            when (result) {
                is ApiResult.Success -> { Ui.toast(this, "Taller guardado."); finish() }
                is ApiResult.Failure -> Ui.toast(this, result.message)
            }
        }
    }
}
