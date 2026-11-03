package com.example.clase18_08zulema

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.clase18_08zulema.data.ApiResult
import com.example.clase18_08zulema.data.Workshop
import com.example.clase18_08zulema.data.WorkshopsApi
import com.example.clase18_08zulema.data.WorkshopsRepository
import com.example.clase18_08zulema.ui.Ui
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import android.os.Build

class MainActivity : AppCompatActivity() {
    private val localNetworkPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) loadWorkshops(search.text?.toString()) else showLocalNetworkPermission()
    }
    private lateinit var page: LinearLayout
    private lateinit var list: LinearLayout
    private lateinit var loading: ProgressBar
    private lateinit var search: TextInputEditText
    private lateinit var tags: ChipGroup
    private lateinit var count: TextView
    private var categories = listOf("Cocina", "Tecnología")
    private var selectedCategory: String? = null
    private var searchTask: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WorkshopsApi.configure(readServerUrl())
        page = Ui.page(this).apply {
            setPadding(Ui.dp(this@MainActivity, 20), Ui.dp(this@MainActivity, 18), Ui.dp(this@MainActivity, 20), Ui.dp(this@MainActivity, 32))
            setBackgroundColor(Color.rgb(245, 249, 251))
        }

        val brandRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val brand = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        brand.addView(label("AULA ABIERTA", 11, Color.rgb(0, 121, 164), true))
        brand.addView(label("Talleres de tu comunidad", 14, Color.rgb(77, 97, 108), false))
        brandRow.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        val connection = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "Conexión"
            textSize = 12f
            isAllCaps = false
            minHeight = Ui.dp(this@MainActivity, 40)
            setPadding(Ui.dp(this@MainActivity, 10), 0, Ui.dp(this@MainActivity, 10), 0)
            setOnClickListener { showConnectionDialog() }
        }
        brandRow.addView(connection)
        page.addView(brandRow)
        page.addView(Ui.gap(this, 24))

        page.addView(Ui.title(this, "Aprende algo nuevo,\ncerca de ti.", 30f))
        page.addView(Ui.gap(this, 8))
        page.addView(Ui.body(this, "Encuentra un espacio para compartir lo que sabes y descubrir algo distinto.", 15f))
        page.addView(Ui.gap(this, 20))
        page.addView(heroCard())
        page.addView(Ui.gap(this, 18))

        val searchWrap = TextInputLayout(this).apply {
            hint = "¿Qué te gustaría aprender?"
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            setBoxCornerRadii(Ui.dp(this@MainActivity, 18).toFloat(), Ui.dp(this@MainActivity, 18).toFloat(), Ui.dp(this@MainActivity, 18).toFloat(), Ui.dp(this@MainActivity, 18).toFloat())
            setBoxStrokeColor(Color.rgb(202, 218, 226))
            setStartIconDrawable(android.R.drawable.ic_menu_search)
            setStartIconTintList(android.content.res.ColorStateList.valueOf(Color.rgb(0, 121, 164)))
            setBoxBackgroundColor(Color.WHITE)
        }
        search = TextInputEditText(this).apply {
            textSize = 15f
            setSingleLine(true)
            setTextColor(Color.rgb(26, 43, 53))
            setPadding(0, Ui.dp(this@MainActivity, 14), Ui.dp(this@MainActivity, 8), Ui.dp(this@MainActivity, 10))
        }
        searchWrap.addView(search)
        page.addView(searchWrap)
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchTask?.let(page::removeCallbacks)
                val task = Runnable { loadWorkshops(search.text?.toString()) }
                searchTask = task
                page.postDelayed(task, 350)
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        page.addView(Ui.gap(this, 20))

        val titleRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        titleRow.addView(Ui.title(this, "Explora talleres", 21f), LinearLayout.LayoutParams(0, -2, 1f))
        count = label("", 12, Color.rgb(91, 111, 121), false)
        titleRow.addView(count)
        page.addView(titleRow)
        page.addView(Ui.gap(this, 10))
        tags = ChipGroup(this).apply {
            isSingleLine = true
            isSingleSelection = true
            isSelectionRequired = true
            setChipSpacingHorizontal(Ui.dp(this@MainActivity, 8))
        }
        page.addView(HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            clipToPadding = false
            addView(tags)
        })
        page.addView(Ui.gap(this, 14))
        renderTags()
        loading = ProgressBar(this).apply { isIndeterminate = true }
        page.addView(loading)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        page.addView(list)

        setContentView(ScrollView(this).apply {
            clipToPadding = false
            addView(page)
        })
    }

    override fun onResume() {
        super.onResume()
        if (::search.isInitialized) {
            if (requiresLocalNetworkPermission() && !hasLocalNetworkPermission()) showLocalNetworkPermission()
            else loadWorkshops(search.text?.toString())
        }
    }

    private fun loadWorkshops(query: String?) {
        if (!::loading.isInitialized) return
        if (requiresLocalNetworkPermission() && !hasLocalNetworkPermission()) {
            showLocalNetworkPermission()
            return
        }
        loading.visibility = View.VISIBLE
        list.removeAllViews()
        WorkshopsRepository.workshops(query, selectedCategory) { result ->
            if (isFinishing || isDestroyed) return@workshops
            loading.visibility = View.GONE
            when (result) {
                is ApiResult.Success -> showWorkshops(result.value)
                is ApiResult.Failure -> showConnectionError(result.message)
            }
        }
    }

    private fun showWorkshops(items: List<Workshop>) {
        val freshCategories = items.mapNotNull { it.category?.takeIf(String::isNotBlank) }
        categories = (listOf("Cocina", "Tecnología") + categories + freshCategories)
            .distinctBy { it.trim().lowercase(Locale("es", "CO")) }
            .sortedBy { it.lowercase(Locale("es", "CO")) }
        renderTags()
        count.text = if (items.size == 1) "1 taller" else "${items.size} talleres"
        if (items.isEmpty()) {
            list.addView(emptyState())
            return
        }
        items.forEachIndexed { index, workshop ->
            val card = workshopCard(workshop)
            card.alpha = 0f
            card.translationY = Ui.dp(this, 12).toFloat()
            list.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = Ui.dp(this@MainActivity, 14) })
            card.animate().alpha(1f).translationY(0f).setDuration(260).setStartDelay((index * 35L).coerceAtMost(180L)).start()
        }
    }

    private fun workshopCard(workshop: Workshop): MaterialCardView {
        val card = MaterialCardView(this).apply {
            radius = Ui.dp(this@MainActivity, 24).toFloat()
            cardElevation = Ui.dp(this@MainActivity, 2).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeColor = Color.rgb(229, 237, 241)
            strokeWidth = Ui.dp(this@MainActivity, 1)
            isClickable = true
            isFocusable = true
            rippleColor = android.content.res.ColorStateList.valueOf(Color.rgb(210, 241, 250))
            setOnClickListener {
                startActivity(Intent(this@MainActivity, com.example.clase18_08zulema.ui.WorkshopDetailActivity::class.java).putExtra("workshop_id", workshop.id))
            }
        }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val imageUrl = workshop.imageUrl?.takeIf { it.startsWith("https://", ignoreCase = true) }
        if (imageUrl != null) {
            val image = ImageView(this).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                contentDescription = "Imagen del taller ${workshop.name}"
                layoutParams = LinearLayout.LayoutParams(-1, Ui.dp(this@MainActivity, 174))
                clipToOutline = true
                background = GradientDrawable().apply { setColor(Color.rgb(224, 241, 247)) }
            }
            Glide.with(this).load(imageUrl)
                .transform(CenterCrop(), RoundedCorners(Ui.dp(this, 20)))
                .placeholder(GradientDrawable().apply { setColor(Color.rgb(224, 241, 247)) })
                .error(GradientDrawable().apply { setColor(Color.rgb(224, 241, 247)) })
                .into(image)
            content.addView(image)
        } else {
            content.addView(categoryBanner(workshop.category ?: "Comunidad"))
        }
        val textContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(this@MainActivity, 17), Ui.dp(this@MainActivity, 14), Ui.dp(this@MainActivity, 17), Ui.dp(this@MainActivity, 16))
        }
        val meta = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val categoryPill = label(workshop.category ?: "Comunidad", 11, Color.rgb(0, 103, 145), true).apply {
            setPadding(Ui.dp(this@MainActivity, 10), Ui.dp(this@MainActivity, 5), Ui.dp(this@MainActivity, 10), Ui.dp(this@MainActivity, 5))
            background = rounded(Color.rgb(224, 246, 252), 40)
        }
        meta.addView(categoryPill, LinearLayout.LayoutParams(0, -2, 1f))
        val availability = when {
            workshop.status != "ACTIVE" -> "Cancelado"
            workshop.availableSlots == 0L -> "Sin cupos"
            else -> "${workshop.availableSlots} cupos"
        }
        meta.addView(label(availability, 12, if (workshop.status == "ACTIVE" && workshop.availableSlots > 0) Color.rgb(0, 112, 91) else Color.rgb(156, 71, 53), true))
        textContent.addView(meta)
        textContent.addView(Ui.gap(this, 10))
        textContent.addView(Ui.title(this, workshop.name, 19f))
        textContent.addView(Ui.gap(this, 6))
        textContent.addView(Ui.body(this, "${formatDate(workshop.dateTime)}  ·  ${workshop.instructor}", 13f))
        textContent.addView(Ui.gap(this, 12))
        val seats = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val people = label("${workshop.registeredPeople}/${workshop.maxCapacity} personas inscritas", 12, Color.rgb(76, 96, 106), false)
        seats.addView(people, LinearLayout.LayoutParams(0, -2, 1f))
        seats.addView(label("Ver detalles  →", 12, Color.rgb(0, 121, 164), true))
        textContent.addView(seats)
        content.addView(textContent)
        card.addView(content)
        return card
    }

    private fun categoryBanner(category: String): View = TextView(this).apply {
        val descriptor = when {
            category.contains("cocin", true) -> "SABORES Y ENCUENTRO"
            category.contains("tecno", true) -> "IDEAS QUE ABREN CAMINO"
            category.contains("huerta", true) || category.contains("ambiente", true) -> "APRENDE CON LA TIERRA"
            else -> "APRENDE EN COMUNIDAD"
        }
        text = "${category.uppercase(Locale.getDefault())}  ·  TALLER PRESENCIAL\n$descriptor"
        textSize = 14f
        setTextColor(Color.WHITE)
        setTypeface(typeface, Typeface.BOLD)
        gravity = Gravity.CENTER_VERTICAL
        setLineSpacing(Ui.dp(this@MainActivity, 5).toFloat(), 1f)
        setPadding(Ui.dp(this@MainActivity, 20), 0, Ui.dp(this@MainActivity, 20), 0)
        layoutParams = LinearLayout.LayoutParams(-1, Ui.dp(this@MainActivity, 118))
        background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.rgb(14, 174, 219), Color.rgb(10, 128, 181))).apply {
            cornerRadius = Ui.dp(this@MainActivity, 20).toFloat()
        }
    }

    private fun heroCard(): MaterialCardView = MaterialCardView(this).apply {
        radius = Ui.dp(this@MainActivity, 26).toFloat()
        cardElevation = Ui.dp(this@MainActivity, 1).toFloat()
        strokeWidth = 0
        setCardBackgroundColor(Color.rgb(11, 164, 211))
        val hero = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(this@MainActivity, 20), Ui.dp(this@MainActivity, 18), Ui.dp(this@MainActivity, 20), Ui.dp(this@MainActivity, 18))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.rgb(15, 181, 224), Color.rgb(0, 131, 187))).apply {
                cornerRadius = Ui.dp(this@MainActivity, 26).toFloat()
            }
        }
        val row = LinearLayout(this@MainActivity).apply { gravity = Gravity.CENTER_VERTICAL }
        val copy = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
        copy.addView(label("UN LUGAR PARA CRECER", 10, Color.WHITE, true))
        copy.addView(Ui.gap(this@MainActivity, 6))
        copy.addView(label("Aprender también\nes compartir.", 20, Color.WHITE, true))
        copy.addView(Ui.gap(this@MainActivity, 5))
        copy.addView(label("Encuentra talleres cerca de ti", 12, Color.rgb(235, 250, 255), false))
        row.addView(copy, LinearLayout.LayoutParams(0, -2, 1f))
        val create = MaterialButton(this@MainActivity).apply {
            text = "Crear taller"
            textSize = 12f
            isAllCaps = false
            setTextColor(Color.rgb(0, 91, 129))
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
            insetTop = 0; insetBottom = 0
            setOnClickListener { startActivity(Intent(this@MainActivity, com.example.clase18_08zulema.ui.WorkshopFormActivity::class.java)) }
        }
        row.addView(create)
        hero.addView(row)
        addView(hero)
    }

    private fun renderTags() {
        if (!::tags.isInitialized) return
        tags.removeAllViews()
        (listOf("Todos") + categories).forEach { label ->
            val chip = Chip(this).apply {
                id = View.generateViewId()
                text = label
                isCheckable = true
                isChecked = if (selectedCategory == null) label == "Todos" else label.equals(selectedCategory, true)
                chipBackgroundColor = android.content.res.ColorStateList.valueOf(if (isChecked) Color.rgb(0, 143, 190) else Color.WHITE)
                setTextColor(if (isChecked) Color.WHITE else Color.rgb(51, 73, 83))
                chipStrokeWidth = Ui.dp(this@MainActivity, 1).toFloat()
                chipStrokeColor = android.content.res.ColorStateList.valueOf(if (isChecked) Color.rgb(0, 143, 190) else Color.rgb(209, 222, 228))
                chipCornerRadius = Ui.dp(this@MainActivity, 24).toFloat()
                setOnClickListener {
                    selectedCategory = label.takeUnless { it == "Todos" }
                    renderTags()
                    loadWorkshops(search.text?.toString())
                }
            }
            tags.addView(chip)
        }
    }

    private fun showConnectionError(message: String) {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(Ui.dp(this@MainActivity, 22), Ui.dp(this@MainActivity, 22), Ui.dp(this@MainActivity, 22), Ui.dp(this@MainActivity, 22))
            background = rounded(Color.WHITE, 24)
        }
        panel.addView(label("No logramos llegar a la API", 17, Color.rgb(32, 49, 58), true))
        panel.addView(Ui.gap(this, 8))
        panel.addView(Ui.body(this, message, 13f))
        panel.addView(Ui.gap(this, 14))
        panel.addView(Ui.button(this, "Configurar servidor") { showConnectionDialog() })
        panel.addView(Ui.button(this, "Volver a intentar") { loadWorkshops(search.text?.toString()) })
        list.addView(panel)
    }

    private fun showLocalNetworkPermission() {
        if (!::loading.isInitialized) return
        loading.visibility = View.GONE
        list.removeAllViews()
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(this@MainActivity, 20), Ui.dp(this@MainActivity, 20), Ui.dp(this@MainActivity, 20), Ui.dp(this@MainActivity, 20))
            background = rounded(Color.WHITE, 24)
        }
        panel.addView(label("Conecta con tu comunidad", 17, Color.rgb(32, 49, 58), true))
        panel.addView(Ui.gap(this, 8))
        panel.addView(Ui.body(this, "Android necesita tu permiso para que esta app consulte el servidor de talleres en la red local. La conexión solo se usa para cargar y guardar talleres.", 13f))
        panel.addView(Ui.gap(this, 14))
        panel.addView(Ui.button(this, "Permitir conexión local") {
            localNetworkPermission.launch("android.permission.ACCESS_LOCAL_NETWORK")
        })
        list.addView(panel)
    }

    private fun requiresLocalNetworkPermission(): Boolean = Build.VERSION.SDK_INT >= 37

    private fun hasLocalNetworkPermission(): Boolean =
        checkSelfPermission("android.permission.ACCESS_LOCAL_NETWORK") == PackageManager.PERMISSION_GRANTED

    private fun showConnectionDialog() {
        val inputLayout = TextInputLayout(this).apply {
            hint = "http://10.0.2.2:8080/"
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            setPadding(Ui.dp(this@MainActivity, 22), Ui.dp(this@MainActivity, 8), Ui.dp(this@MainActivity, 22), 0)
        }
        val input = TextInputEditText(this).apply {
            setSingleLine(true)
            setText(readServerUrl())
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
        }
        inputLayout.addView(input)
        AlertDialog.Builder(this)
            .setTitle("Conectar con el servidor")
            .setMessage("Emulador: deja 10.0.2.2. En un teléfono, escribe la IP local del computador, por ejemplo http://192.168.1.20:8080/; ambos deben estar en la misma red.")
            .setView(inputLayout)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar y probar", null)
            .create().also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val url = normalizeUrl(input.text?.toString().orEmpty())
                        if (url == null) {
                            inputLayout.error = "Usa una dirección http:// o https:// válida"
                            return@setOnClickListener
                        }
                        inputLayout.error = null
                        getSharedPreferences("api_connection", MODE_PRIVATE).edit().putString("base_url", url).apply()
                        WorkshopsApi.configure(url)
                        dialog.dismiss()
                        loadWorkshops(search.text?.toString())
                    }
                }
                dialog.show()
            }
    }

    private fun readServerUrl(): String = getSharedPreferences("api_connection", MODE_PRIVATE)
        .getString("base_url", WorkshopsApi.DEFAULT_BASE_URL)
        ?.replace("http://10.0.2.2:8080/", WorkshopsApi.DEFAULT_BASE_URL)
        ?: WorkshopsApi.DEFAULT_BASE_URL

    private fun normalizeUrl(value: String): String? {
        val candidate = value.trim().let { if (it.endsWith("/")) it else "$it/" }
        val parsed = runCatching { java.net.URI(candidate) }.getOrNull() ?: return null
        if (parsed.scheme !in listOf("http", "https") || parsed.host.isNullOrBlank() || parsed.userInfo != null) return null
        if (parsed.path !in listOf("", "/")) return null
        return candidate
    }

    private fun emptyState(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(Ui.dp(this@MainActivity, 18), Ui.dp(this@MainActivity, 20), Ui.dp(this@MainActivity, 18), Ui.dp(this@MainActivity, 20))
        background = rounded(Color.WHITE, 22)
        addView(label("Todavía no hay talleres en esta categoría", 16, Color.rgb(35, 52, 61), true))
        addView(Ui.gap(this@MainActivity, 6))
        addView(Ui.body(this@MainActivity, "Prueba otro filtro o crea una nueva actividad.", 13f))
    }

    private fun label(textValue: String, size: Int, color: Int, bold: Boolean) = TextView(this).apply {
        text = textValue
        textSize = size.toFloat()
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = Ui.dp(this@MainActivity, radius).toFloat()
    }

    private fun formatDate(value: String): String = runCatching {
        LocalDateTime.parse(value).format(DateTimeFormatter.ofPattern("EEE d MMM · h:mm a", Locale("es", "CO")))
    }.getOrDefault(value)
}
