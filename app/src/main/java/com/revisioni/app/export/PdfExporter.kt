package com.revisioni.app.export

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.revisioni.app.data.Maintenance
import com.revisioni.app.data.VehicleType
import com.revisioni.app.data.backup.VehicleBundle
import com.revisioni.app.ui.components.daysUntil
import com.revisioni.app.ui.components.deadlineLabel
import com.revisioni.app.ui.components.formatDate
import java.io.OutputStream
import java.util.Locale

/**
 * Crea un PDF A4 con una scheda per ogni veicolo: dati, scadenze, riepilogo
 * e storico delle manutenzioni. Ogni veicolo inizia su una pagina nuova e le
 * schede lunghe continuano sulle pagine successive.
 */
object PdfExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f
    private const val FOOTER_SPACE = 34f

    private const val COLOR_ACCENT = 0xFF1565C0.toInt()
    private const val COLOR_TEXT = 0xFF1B1B1F.toInt()
    private const val COLOR_MUTED = 0xFF5F6368.toInt()
    private const val COLOR_LINE = 0xFFD0D5DB.toInt()
    private const val COLOR_RED = 0xFFBA1A1A.toInt()
    private const val COLOR_ORANGE = 0xFFB86E00.toInt()
    private const val COLOR_GREEN = 0xFF2E7D32.toInt()

    fun write(bundles: List<VehicleBundle>, out: OutputStream, generatedAt: Long = System.currentTimeMillis()) {
        val doc = PdfDocument()
        try {
            val writer = PageWriter(doc, generatedAt)
            writer.startPage()
            if (bundles.isEmpty()) {
                writer.line("Nessun veicolo da esportare.", Paints.body)
            }
            bundles.forEachIndexed { index, bundle ->
                if (index > 0) {
                    writer.endPage()
                    writer.startPage()
                }
                drawVehicle(writer, bundle)
            }
            writer.endPage()
            doc.writeTo(out)
        } finally {
            doc.close()
        }
    }

    // ---- contenuto di una scheda ----

    private fun drawVehicle(w: PageWriter, bundle: VehicleBundle) {
        val v = bundle.vehicle

        w.line("SCHEDA VEICOLO", Paints.eyebrow)
        for (part in w.wrap(v.name, Paints.title)) {
            w.line(part, Paints.title)
        }
        val subtitle = listOfNotNull(
            if (v.type == VehicleType.AUTO) "Auto" else "Moto",
            listOf(v.marca, v.modello).filter { it.isNotBlank() }.joinToString(" ").ifBlank { null },
            v.anno?.toString(),
            v.targa.takeIf { it.isNotBlank() }?.let { "Targa $it" }
        ).joinToString("  ·  ")
        w.line(subtitle, Paints.subtitle)
        w.rule(Paints.accentRule, before = 6f, after = 4f)

        w.section("Scadenze")
        deadlineRow(w, "Revisione", v.revisioneScadenza)
        deadlineRow(w, "Assicurazione", v.assicurazioneScadenza)
        if (v.bolloScadenza != null) deadlineRow(w, "Bollo", v.bolloScadenza)

        val maintenances = bundle.maintenances
        if (maintenances.isNotEmpty()) {
            w.section("Riepilogo manutenzioni")
            val total = maintenances.sumOf { it.costo ?: 0.0 }
            val last = maintenances.maxOf { it.data }
            w.line(
                "${maintenances.size} interventi  ·  Speso totale ${money(total)}  ·  Ultimo intervento ${formatDate(last)}",
                Paints.body
            )
        }

        w.section("Manutenzioni")
        if (maintenances.isEmpty()) {
            w.line("Nessuna manutenzione registrata.", Paints.muted)
        } else {
            for (m in maintenances.sortedByDescending { it.data }) {
                maintenanceEntry(w, m)
            }
        }

        if (v.note.isNotBlank()) {
            w.section("Note")
            for (part in w.wrap(v.note, Paints.body)) {
                w.line(part, Paints.body)
            }
        }
    }

    private fun deadlineRow(w: PageWriter, label: String, date: Long?) {
        if (date == null) {
            w.line(label, Paints.bodyBold, rightText = "non impostata", rightPaint = Paints.muted)
            return
        }
        w.line(
            "$label   ${formatDate(date)}",
            Paints.bodyBold,
            rightText = deadlineLabel(date),
            rightPaint = Paints.statusPaint(statusColor(date))
        )
    }

    private fun maintenanceEntry(w: PageWriter, m: Maintenance) {
        class Row(val text: String, val paint: Paint, val rightText: String? = null, val rightPaint: Paint = paint)

        val rows = mutableListOf<Row>()
        rows += Row(m.tipo, Paints.bodyBold, rightText = formatDate(m.data), rightPaint = Paints.muted)

        val details = listOfNotNull(
            m.km?.let { String.format(Locale.ITALY, "%,d km", it) },
            m.costo?.let { money(it) },
            m.officina.takeIf { it.isNotBlank() }
        ).joinToString("  ·  ")
        if (details.isNotEmpty()) rows += Row(details, Paints.body)

        if (m.prossimaScadenza != null) {
            val interval = listOfNotNull(
                m.intervalloMesi?.let { "ogni $it mesi" },
                m.intervalloKm?.let { String.format(Locale.ITALY, "ogni %,d km", it) }
            ).joinToString(", ")
            val text = "Prossima scadenza ${formatDate(m.prossimaScadenza)}" +
                if (interval.isNotEmpty()) " ($interval)" else ""
            rows += Row(
                text, Paints.body,
                rightText = deadlineLabel(m.prossimaScadenza),
                rightPaint = Paints.statusPaint(statusColor(m.prossimaScadenza))
            )
        } else if (m.intervalloMesi != null || m.intervalloKm != null) {
            val interval = listOfNotNull(
                m.intervalloMesi?.let { "ogni $it mesi" },
                m.intervalloKm?.let { String.format(Locale.ITALY, "ogni %,d km", it) }
            ).joinToString(", ")
            rows += Row("Ripetere $interval", Paints.body)
        }

        if (m.note.isNotBlank()) {
            for (part in w.wrap(m.note, Paints.muted)) rows += Row(part, Paints.muted)
        }

        // Una voce non viene spezzata tra due pagine, se sta in una pagina da sola.
        val height = rows.sumOf { it.paint.fontSpacing.toDouble() }.toFloat() + 12f
        w.ensure(minOf(height, w.bodyHeight))
        for (row in rows) {
            w.line(row.text, row.paint, rightText = row.rightText, rightPaint = row.rightPaint)
        }
        w.rule(Paints.lightRule, before = 4f, after = 6f)
    }

    private fun statusColor(date: Long?): Int {
        val days = daysUntil(date) ?: return COLOR_MUTED
        return when {
            days < 0 -> COLOR_RED
            days <= 15 -> COLOR_ORANGE
            else -> COLOR_GREEN
        }
    }

    private fun money(value: Double): String = String.format(Locale.ITALY, "€ %,.2f", value)

    // ---- pennelli ----

    private object Paints {
        private fun paint(size: Float, bold: Boolean = false, color: Int = COLOR_TEXT) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

        val eyebrow = paint(9f, bold = true, color = COLOR_ACCENT)
        val title = paint(22f, bold = true)
        val subtitle = paint(12f, color = COLOR_MUTED)
        val sectionTitle = paint(13f, bold = true, color = COLOR_ACCENT)
        val body = paint(11f)
        val bodyBold = paint(11f, bold = true)
        val muted = paint(10f, color = COLOR_MUTED)
        val footer = paint(8.5f, color = COLOR_MUTED)

        val accentRule = Paint().apply { color = COLOR_ACCENT; strokeWidth = 2f }
        val lightRule = Paint().apply { color = COLOR_LINE; strokeWidth = 0.7f }

        fun statusPaint(color: Int) = paint(10f, bold = true, color = color)
    }

    // ---- gestione delle pagine ----

    private class PageWriter(private val doc: PdfDocument, private val generatedAt: Long) {
        private var page: PdfDocument.Page? = null
        private var canvas: Canvas? = null
        private var pageNumber = 0
        private var y = MARGIN

        val contentWidth = PAGE_WIDTH - 2 * MARGIN
        private val bottom = PAGE_HEIGHT - MARGIN - FOOTER_SPACE
        val bodyHeight = bottom - MARGIN

        fun startPage() {
            pageNumber++
            val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val p = doc.startPage(info)
            page = p
            canvas = p.canvas
            y = MARGIN
        }

        fun endPage() {
            val p = page ?: return
            drawFooter(p.canvas)
            doc.finishPage(p)
            page = null
            canvas = null
        }

        fun ensure(height: Float) {
            if (y + height > bottom) {
                endPage()
                startPage()
            }
        }

        fun line(
            text: String,
            paint: Paint,
            rightText: String? = null,
            rightPaint: Paint = paint
        ) {
            ensure(paint.fontSpacing)
            val c = canvas ?: return
            val baseline = y - paint.fontMetrics.ascent
            c.drawText(text, MARGIN, baseline, paint)
            if (rightText != null) {
                c.drawText(rightText, MARGIN + contentWidth - rightPaint.measureText(rightText), baseline, rightPaint)
            }
            y += paint.fontSpacing
        }

        fun rule(paint: Paint, before: Float, after: Float) {
            ensure(before + after + paint.strokeWidth)
            y += before
            canvas?.drawLine(MARGIN, y, MARGIN + contentWidth, y, paint)
            y += after
        }

        fun section(title: String) {
            // Il titolo non resta mai solo in fondo a una pagina.
            ensure(Paints.sectionTitle.fontSpacing * 3 + 14f)
            y += 14f
            line(title, Paints.sectionTitle)
            rule(Paints.lightRule, before = 2f, after = 6f)
        }

        /** Spezza il testo in righe che stanno nella larghezza della pagina. */
        fun wrap(text: String, paint: Paint): List<String> {
            val lines = mutableListOf<String>()
            for (paragraph in text.split('\n')) {
                if (paragraph.isBlank()) {
                    lines += ""
                    continue
                }
                var current = ""
                for (word in paragraph.trim().split(Regex("\\s+"))) {
                    var rest = word
                    // Parola più larga di una riga intera: la si spezza.
                    while (paint.measureText(rest) > contentWidth) {
                        val fit = paint.breakText(rest, true, contentWidth, null).coerceAtLeast(1)
                        if (current.isNotEmpty()) {
                            lines += current
                            current = ""
                        }
                        lines += rest.substring(0, fit)
                        rest = rest.substring(fit)
                    }
                    val candidate = if (current.isEmpty()) rest else "$current $rest"
                    if (paint.measureText(candidate) <= contentWidth) {
                        current = candidate
                    } else {
                        lines += current
                        current = rest
                    }
                }
                if (current.isNotEmpty()) lines += current
            }
            return lines
        }

        private fun drawFooter(c: Canvas) {
            val lineY = PAGE_HEIGHT - MARGIN - 18f
            c.drawLine(MARGIN, lineY, MARGIN + contentWidth, lineY, Paints.lightRule)
            val baseline = lineY + 6f - Paints.footer.fontMetrics.ascent
            c.drawText("Generato il ${formatDate(generatedAt)} con Le mie scadenze", MARGIN, baseline, Paints.footer)
            val label = "Pagina $pageNumber"
            c.drawText(label, MARGIN + contentWidth - Paints.footer.measureText(label), baseline, Paints.footer)
        }
    }
}
