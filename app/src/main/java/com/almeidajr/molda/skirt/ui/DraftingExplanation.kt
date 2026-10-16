package com.almeidajr.molda.skirt.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.almeidajr.molda.skirt.domain.SkirtRadii
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun DraftingExplanation(inputs: Map<MeasurementField, String>, radii: SkirtRadii) {
    val seam = inputs.getValue(MeasurementField.WaistSeamAllowance).trim().replace('.', ',')
    val hem = inputs.getValue(MeasurementField.HemAllowance).trim().replace('.', ',')
    Text("Como levar os raios ao papel", style = MaterialTheme.typography.titleLarge)
    Text(
        "Cintura é a medida do corpo; folga é o acréscimo escolhido para vestir. " +
            "Margens são tecido reservado para costura e acabamento, não folga. " +
            "Todos os valores abaixo estão em centímetros.",
    )
    DraftingFormulas(inputs, radii)
    val direction = "A margem de cintura de $seam cm avança para dentro; " +
        "a margem de barra de $hem cm avança para fora."
    val labels = listOf(
        "1. Costura da cintura (linha contínua): ${formatRadius(radii.waistStitching)}",
        "2. Corte da cintura (linha tracejada): ${formatRadius(radii.waistCutting)}",
        "3. Barra acabada (linha pontilhada): ${formatRadius(radii.finishedHem)}",
        "4. Corte da barra (traço e ponto): ${formatRadius(radii.hemCutting)}",
    )
    Text("Linhas do diagrama: do centro para fora, 2 → 1 → 3 → 4")
    Text("Centro comum: ponto no canto superior esquerdo do diagrama.")
    SkirtDiagram(radii, (labels + direction).joinToString(". "))
    labels.forEach { Text(it) }
    Text(direction)
    if (radii.waistCutting == radii.waistStitching) {
        Text("Margem de cintura zero: as linhas 1 e 2 coincidem.")
    }
    if (radii.hemCutting == radii.finishedHem) {
        Text("Margem de barra zero: as linhas 3 e 4 coincidem.")
    }
    Text(
        "Diagrama esquemático: mostra apenas um setor da saia godê inteira, sem escala. " +
            "Não é um molde completo, calibrado ou para impressão. Transfira os raios " +
            "ao papel a partir de um mesmo centro. O cálculo não garante o caimento.",
    )
}

@Composable
private fun DraftingFormulas(inputs: Map<MeasurementField, String>, radii: SkirtRadii) {
    val waist = inputs.getValue(MeasurementField.Waist).trim().replace('.', ',')
    val ease = inputs.getValue(MeasurementField.Ease).trim().replace('.', ',')
    val length = inputs.getValue(MeasurementField.Length).trim().replace('.', ',')
    val seam = inputs.getValue(MeasurementField.WaistSeamAllowance).trim().replace('.', ',')
    val hem = inputs.getValue(MeasurementField.HemAllowance).trim().replace('.', ',')
    var formulasExpanded by rememberSaveable(inputs) { mutableStateOf(false) }
    TextButton(onClick = { formulasExpanded = !formulasExpanded }) {
        Text(if (formulasExpanded) "Ocultar fórmulas" else "Mostrar fórmulas")
    }
    if (formulasExpanded) {
        Text("Costura da cintura: r = (cintura + folga) / (2π)")
        Text("r = ($waist + $ease) / (2π) = ${formatRadius(radii.waistStitching)}")
        Text(
            "Barra acabada = r + comprimento = r + $length = ${formatRadius(radii.finishedHem)}",
        )
        Text(
            "Corte da cintura = r − margem de costura = r − $seam = ${formatRadius(
                radii.waistCutting,
            )}",
        )
        Text(
            "Corte da barra = r + comprimento + margem de barra = " +
                "r + $length + $hem = ${formatRadius(
                    radii.hemCutting,
                )}",
        )
        Text(
            "Use r sem arredondamento nos cálculos; " +
                "apenas os resultados exibidos têm duas casas decimais.",
        )
    }
}

@Composable
private fun SkirtDiagram(radii: SkirtRadii, description: String) {
    val color = MaterialTheme.colorScheme.onSurface
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelLarge.copy(color = color)
    Canvas(
        Modifier.fillMaxWidth().height(240.dp).semantics {
            contentDescription =
                description
        },
    ) {
        val center = Offset(16.dp.toPx(), 16.dp.toPx())
        val extent = minOf(size.width - center.x, size.height - center.y) - 8.dp.toPx()
        // Spacing is schematic so even small allowances remain visible.
        val fractions = listOf(
            0.3f,
            if (radii.waistCutting == radii.waistStitching) 0.3f else 0.22f,
            0.82f,
            if (radii.hemCutting == radii.finishedHem) 0.82f else 0.95f,
        )
        val patterns = listOf(
            null,
            PathEffect.dashPathEffect(floatArrayOf(12.dp.toPx(), 6.dp.toPx())),
            PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 5.dp.toPx())),
            PathEffect.dashPathEffect(
                floatArrayOf(12.dp.toPx(), 5.dp.toPx(), 2.dp.toPx(), 5.dp.toPx()),
            ),
        )
        fractions.zip(patterns).forEach { (fraction, pattern) ->
            val radius = extent * fraction
            drawArc(
                color = color,
                startAngle = 0f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(2.dp.toPx(), pathEffect = pattern),
            )
        }
        drawCircle(color, 3.dp.toPx(), center)
        fractions.forEachIndexed { index, fraction ->
            // Different positions around the arcs also distinguish coincident lines.
            val angle = listOf(0.6f, 1.1f, 0.6f, 1.1f)[index]
            val radius = extent * fraction
            drawText(
                textMeasurer,
                (index + 1).toString(),
                topLeft =
                    center +
                        Offset(
                            radius * cos(angle),
                            radius * sin(angle),
                        ),
                style = labelStyle,
            )
        }
    }
}
