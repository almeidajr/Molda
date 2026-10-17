package com.almeidajr.molda.skirt.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import app.rive.Fit
import app.rive.RenderBackend
import app.rive.Result
import app.rive.Rive
import app.rive.RiveFile
import app.rive.RiveFileSource
import app.rive.rememberArtboardResult
import app.rive.rememberRiveFile
import app.rive.rememberRiveWorkerOrNull
import app.rive.rememberViewModelInstanceResult
import com.almeidajr.molda.R
import com.almeidajr.molda.skirt.domain.SkirtRadii

@Composable
internal fun TutorialAnimation(
    radii: SkirtRadii,
    step: FoldingStep,
    motionEnabled: Boolean,
    drawingSize: Dp,
) {
    // Vulkan avoids blank frames on software GLES drivers; Rive falls back when unavailable.
    val worker = rememberRiveWorkerOrNull(renderBackend = RenderBackend.Vulkan)
    val file = worker?.let {
        rememberRiveFile(RiveFileSource.RawRes.from(R.raw.fabric_folding), it)
    }
    var replay by remember(motionEnabled) { mutableIntStateOf(0) }
    var paused by remember(motionEnabled) { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The artboard is a white sheet; the frame around it follows the theme.
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.width(drawingSize).heightIn(min = drawingSize),
        ) {
            Box(Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                when (file) {
                    null -> Text(
                        "Não foi possível iniciar a animação. As instruções escritas continuam disponíveis.",
                    )

                    is Result.Loading -> Text("Carregando animação…")

                    is Result.Error -> Text(
                        "Não foi possível abrir a animação. Consulte as instruções abaixo.",
                    )

                    is Result.Success -> TutorialDrawing(
                        file.value,
                        radii,
                        step,
                        motionEnabled && replay > 0,
                        replay,
                        paused,
                    )
                }
            }
        }
        if (file is Result.Success) {
            FlowRow(horizontalArrangement = Arrangement.Center) {
                TextButton(onClick = {
                    replay++
                    paused = false
                }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(
                        if (step == FoldingStep.Marking) {
                            if (replay == 0) "Reproduzir marcação" else "Repetir marcação"
                        } else {
                            if (replay == 0) "Reproduzir etapa" else "Repetir etapa"
                        },
                    )
                }
                if (motionEnabled && replay > 0) {
                    TextButton(
                        onClick = { paused = !paused },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) { Text(if (paused) "Continuar animação" else "Pausar animação") }
                }
            }
        }
    }
}

@Composable
private fun TutorialDrawing(
    file: RiveFile,
    radii: SkirtRadii,
    step: FoldingStep,
    play: Boolean,
    replay: Int,
    paused: Boolean,
) {
    val artboard = rememberArtboardResult(file, step.artboard)
    if (artboard !is Result.Success) {
        Text("Preparando desenho. As instruções escritas continuam disponíveis.")
        return
    }
    when (val result = rememberViewModelInstanceResult(file)) {
        is Result.Loading -> Text("Preparando medidas…")

        is Result.Error -> Text(
            "Não foi possível aplicar as medidas. Consulte as instruções abaixo.",
        )

        is Result.Success -> {
            val instance = result.value
            var ready by remember(instance, radii) { mutableStateOf(false) }
            LaunchedEffect(instance, radii) {
                val markingDiameter = 800f
                // Normalize before converting to Float; large valid centimetre values stay finite.
                val waistDiameter =
                    (markingDiameter * (radii.waistStitching / radii.hemCutting)).toFloat()
                instance.setNumber("waistStitching", waistDiameter)
                instance.setNumber(
                    "waistCutting",
                    (markingDiameter * (radii.waistCutting / radii.hemCutting)).toFloat(),
                )
                instance.setNumber(
                    "finishedHem",
                    (markingDiameter * (radii.finishedHem / radii.hemCutting)).toFloat(),
                )
                instance.setNumber("hemCutting", markingDiameter)
                ready = true
            }
            LaunchedEffect(instance, play, replay) {
                instance.setBoolean("play", play)
                if (play) instance.fireTrigger("restart")
            }
            if (ready) {
                Rive(
                    file = file,
                    artboard = artboard.value,
                    viewModelInstance = instance,
                    playing = play && !paused,
                    fit = Fit.Contain(),
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).semantics {
                        contentDescription =
                            when (step) {
                                FoldingStep.Marking -> "Marcação proporcional em Rive"
                                FoldingStep.Cutting -> "Corte proporcional em Rive"
                                FoldingStep.Unfolding -> "Círculo desdobrado proporcional em Rive"
                                else -> "Dobra do tecido em Rive: etapa ${step.ordinal + 1}"
                            }
                    },
                )
            }
        }
    }
}

@Composable
internal fun StepInstructions(radii: SkirtRadii, step: FoldingStep) {
    when (step) {
        FoldingStep.Marking -> MarkingInstructions(radii)

        FoldingStep.Cutting, FoldingStep.Unfolding -> {
            CuttingInstructions(step)
            MarkingInstructions(radii, unfolded = step == FoldingStep.Unfolding)
        }

        else -> FoldingInstructions(step)
    }
}

@Composable
private fun MarkingInstructions(radii: SkirtRadii, unfolded: Boolean = false) {
    Text("Proporções calculadas; desenho não está em tamanho real.")
    Text(
        "Tamanho do tecido ilustrativo. Pressupõe tecido suficiente; a largura " +
            "e o comprimento reais do tecido não foram verificados.",
    )
    if (unfolded) {
        Text(
            "Centro do recorte da cintura: mesma origem de todos os raios, agora no centro do círculo.",
        )
    } else {
        Text(
            "Origem comum: ponto no canto superior esquerdo, encontro das duas " +
                "dobras fechadas. Com fita ou régua presa nesse ponto, marque cada arco " +
                "mantendo seu raio constante.",
        )
    }
    val card = cuttingCard(radii)
    card.flatMap { it.entries }.sortedBy { it.number }.forEach { entry ->
        Text("${entry.number}. ${entry.label}: ${entry.value}")
        Text(lineRole(entry.lineStyle))
    }
    coincidenceNotes(card).forEach { Text(it) }
    Text(
        "Antes de cortar tecido da peça, obtenha instruções separadas para " +
            "abertura, margens do fechamento e cós. Esta demonstração não é um " +
            "molde completo.",
    )
}

@Composable
private fun FoldingInstructions(step: FoldingStep) {
    Text(
        "Tecido ilustrativo, sem tamanho real. Pressupõe tecido suficiente; " +
            "largura e comprimento reais não foram verificados.",
    )
    when (step) {
        FoldingStep.Preparation -> FabricPreparationInstructions()

        FoldingStep.FirstFold -> {
            Text(
                "Dobre no sentido do comprimento: leve a ourela esquerda até a direita. " +
                    "Alinhe sem esticar e alise as duas camadas.",
            )
            Text(
                "Resultado: dobra fechada à esquerda (linha contínua); ourelas juntas à " +
                    "direita (pontilhadas); bordas cortadas em cima e embaixo (tracejadas).",
            )
        }

        FoldingStep.SecondFold -> {
            Text(
                "Dobre transversalmente: leve as bordas cortadas de cima até as de " +
                    "baixo. Alinhe e alise as quatro camadas.",
            )
            Text(
                "Resultado: dobras fechadas em cima e à esquerda (linhas contínuas); " +
                    "bordas cortadas embaixo (tracejadas); ourelas à direita (pontilhadas).",
            )
            Text(
                "O ponto no canto superior esquerdo é o encontro das duas dobras " +
                    "fechadas: origem de todos os raios. Não use um canto com bordas " +
                    "abertas.",
            )
            Text(
                "Na próxima etapa, ampliamos esse mesmo quadrado, sem girá-lo. A origem " +
                    "continua no canto superior esquerdo.",
            )
        }

        FoldingStep.Marking, FoldingStep.Cutting, FoldingStep.Unfolding -> Unit
    }
    Text(
        "A imagem parada mostra o resultado da etapa. Reproduza para acompanhar " +
            "o movimento em repetição contínua. Pause para observar; o texto permite " +
            "seguir sem animação ou cores.",
    )
}

@Composable
private fun FabricPreparationInstructions() {
    Text("Comece com papel ou retalho. Alise o tecido sem esticar e respeite o fio.")
    Text(
        "No desenho plano, as ourelas estão à esquerda e à direita (bordas " +
            "pontilhadas); o fio é paralelo a elas. As bordas cortadas estão em " +
            "cima e embaixo (tracejadas).",
    )
    Text(
        "Prepare o tecido conforme seus cuidados e passe os vincos. Não force " +
            "um tecido torcido para alinhar as bordas. Estampas direcionais e " +
            "tecidos com pelo exigem outro estudo de disposição.",
    )
}

@Composable
private fun CuttingInstructions(step: FoldingStep) {
    Text(
        "Pratique em papel ou retalho; a sequência ainda aguarda validação prática por uma pessoa que costura.",
    )
    if (step == FoldingStep.Cutting) {
        Text(
            "Mantenha as quatro camadas alinhadas. Corte primeiro o arco 2, " +
                "da cintura (tracejado), e depois o arco 4, da barra (traço e ponto), " +
                "atravessando as quatro camadas. Retire o miolo e a sobra externa.",
        )
        Text(
            "Não corte as linhas 1 e 3: são referências de costura e barra acabada. " +
                "Não corte em linha reta pelas dobras; elas mantêm o círculo inteiro.",
        )
        Text(
            "A animação volta ao início apenas para repetir a demonstração; não desfaça os cortes.",
        )
    } else {
        Text(
            "O desenho foi reduzido uniformemente para caber o círculo, sem mudar " +
                "as proporções. O quarto cortado começa abaixo e à direita da origem.",
        )
        Text(
            "Desfaça a segunda dobra: abra as duas camadas superiores para cima, " +
                "abrindo um semicírculo à direita. Depois desfaça a primeira dobra, " +
                "levando a camada de cima para a esquerda. Alise sem esticar: " +
                "surge um círculo inteiro com o recorte central da cintura.",
        )
        Text("O círculo desdobrado não é um molde completo e não garante caimento.")
    }
    Text(
        "A imagem parada mostra o resultado. Reproduza ou repita a etapa para " +
            "acompanhar o movimento em repetição contínua. Pause para observar; " +
            "todas as instruções também estão escritas.",
    )
}

private fun lineRole(style: LineStyle): String = when (style) {
    LineStyle.Solid -> "Linha contínua: referência de costura, não de corte."

    LineStyle.Dashed ->
        "Linha tracejada: fica para dentro da costura, reservando a margem de cintura."

    LineStyle.Dotted -> "Linha pontilhada: referência do comprimento acabado."

    LineStyle.DashDot ->
        "Linha traço e ponto: fica para fora da barra acabada, reservando a margem de barra."
}
