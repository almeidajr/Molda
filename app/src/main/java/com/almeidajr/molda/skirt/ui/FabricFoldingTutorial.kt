package com.almeidajr.molda.skirt.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.rive.Fit
import app.rive.Result
import app.rive.Rive
import app.rive.RiveFile
import app.rive.RiveFileSource
import app.rive.rememberRiveFile
import app.rive.rememberRiveWorkerOrNull
import app.rive.rememberViewModelInstanceResult
import com.almeidajr.molda.R
import com.almeidajr.molda.skirt.domain.SkirtRadii

@Composable
internal fun FabricFoldingTutorial(radii: SkirtRadii) {
    var open by rememberSaveable(radii) { mutableStateOf(false) }
    TextButton(onClick = { open = true }) { Text("Tutorial de dobra do tecido") }
    if (open) {
        Dialog(onDismissRequest = { open = false }) {
            Surface(shape = MaterialTheme.shapes.large) {
                Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "Marcar os raios",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics { heading() },
                    )
                    MarkingAnimation(radii)
                    MarkingInstructions(radii)
                    TextButton(onClick = { open = false }) { Text("Fechar tutorial") }
                }
            }
        }
    }
}

@Composable
private fun MarkingAnimation(radii: SkirtRadii) {
    val worker = rememberRiveWorkerOrNull()
    if (worker == null) {
        Text("Não foi possível iniciar a animação. As instruções escritas continuam disponíveis.")
        return
    }
    val result = rememberRiveFile(RiveFileSource.RawRes.from(R.raw.fabric_folding), worker)
    when (result) {
        is Result.Loading -> Text("Carregando animação…")

        is Result.Error -> Text("Não foi possível abrir a animação. Consulte as instruções abaixo.")

        is Result.Success -> {
            var replay by remember { mutableStateOf(0) }
            key(replay) { MarkingDrawing(result.value, radii, replay > 0) }
            TextButton(onClick = { replay++ }) {
                Text(if (replay == 0) "Reproduzir marcação" else "Repetir marcação")
            }
        }
    }
}

@Composable
private fun MarkingDrawing(file: RiveFile, radii: SkirtRadii, play: Boolean) {
    val result = rememberViewModelInstanceResult(file)
    when (result) {
        is Result.Loading -> Text("Preparando medidas…")

        is Result.Error -> Text(
            "Não foi possível aplicar as medidas. Consulte as instruções abaixo.",
        )

        is Result.Success -> {
            val instance = result.value
            var ready by remember { mutableStateOf(false) }
            LaunchedEffect(instance, radii, play) {
                val markingDiameter = 800f
                // Normalize before converting to Float; large valid centimetre values stay finite.
                instance.setNumber(
                    "waistStitching",
                    (
                        markingDiameter *
                            (radii.waistStitching / radii.hemCutting)
                        ).toFloat(),
                )
                instance.setNumber(
                    "waistCutting",
                    (markingDiameter * (radii.waistCutting / radii.hemCutting)).toFloat(),
                )
                instance.setNumber(
                    "finishedHem",
                    (markingDiameter * (radii.finishedHem / radii.hemCutting)).toFloat(),
                )
                instance.setNumber("hemCutting", markingDiameter)
                instance.setBoolean("play", play)
                ready = true
            }
            if (ready) {
                Rive(
                    file = file,
                    viewModelInstance = instance,
                    playing = play,
                    fit = Fit.Contain(),
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).semantics {
                        contentDescription = "Marcação proporcional em Rive"
                    },
                )
            }
        }
    }
}

@Composable
private fun MarkingInstructions(radii: SkirtRadii) {
    Text("Proporções calculadas; desenho não está em tamanho real.")
    Text(
        "Tamanho do tecido ilustrativo. Pressupõe tecido suficiente; " +
            "a largura e o comprimento reais do tecido não foram verificados.",
    )
    Text(
        "Origem comum: ponto no canto superior esquerdo, encontro das duas dobras fechadas. " +
            "Com fita ou régua presa nesse ponto, marque cada arco mantendo seu raio constante.",
    )
    Text("1. Costura da cintura: ${formatRadius(radii.waistStitching)}")
    Text("Linha contínua: referência de costura, não de corte.")
    Text("2. Corte da cintura: ${formatRadius(radii.waistCutting)}")
    Text("Linha tracejada: fica para dentro da costura, reservando a margem de cintura.")
    Text("3. Barra acabada: ${formatRadius(radii.finishedHem)}")
    Text("Linha pontilhada: referência do comprimento acabado.")
    Text("4. Corte da barra: ${formatRadius(radii.hemCutting)}")
    Text("Linha traço e ponto: fica para fora da barra acabada, reservando a margem de barra.")
    if (radii.waistCutting ==
        radii.waistStitching
    ) {
        Text("Margem de cintura zero: linhas 1 e 2 coincidem.")
    }
    if (radii.hemCutting == radii.finishedHem) Text("Margem de barra zero: linhas 3 e 4 coincidem.")
    Text(
        "Antes de cortar tecido da peça, obtenha instruções separadas para abertura, " +
            "margens do fechamento e cós. Esta demonstração não é um molde completo.",
    )
}
