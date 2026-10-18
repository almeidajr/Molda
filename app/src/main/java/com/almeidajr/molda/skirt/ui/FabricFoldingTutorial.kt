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
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalResources
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
import com.almeidajr.molda.measurement.Length
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
        // The artboards are transparent: this frame is the page behind the drawing.
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
            val palette = tutorialPalette(MaterialTheme.colorScheme)
            var ready by remember(instance, radii) { mutableStateOf(false) }
            LaunchedEffect(instance, radii, palette) {
                palette.forEach { (name, color) -> instance.setColor(name, color.toArgb()) }
                val markingDiameter = 800f

                // Normalize before converting to Float; large valid centimetre values stay finite.
                fun diameter(radius: Length) =
                    (markingDiameter * (radius.centimetres / radii.hemCutting.centimetres))
                        .toFloat()
                instance.setNumber("waistStitching", diameter(radii.waistStitching))
                instance.setNumber("waistCutting", diameter(radii.waistCutting))
                instance.setNumber("finishedHem", diameter(radii.finishedHem))
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

/**
 * The drawing's colour properties by view-model name. The cutting card's roles: fabric is its
 * paper and lines its ink, so the drawing reads on the light, dark and dynamic schemes alike.
 */
internal fun tutorialPalette(colors: ColorScheme): Map<String, Color> = mapOf(
    "ink" to colors.onSecondaryContainer,
    "fabric" to colors.secondaryContainer,
    "fabricFold" to colors.tertiaryContainer,
    "accent" to colors.tertiary,
)

/**
 * One step's written lesson: the action to take now, the context that changes it, then the
 * supporting detail behind a collapsed "Detalhes" disclosure.
 */
@Composable
internal fun StepInstructions(radii: SkirtRadii, step: FoldingStep) {
    Text(stepLead(step), style = MaterialTheme.typography.titleMedium)
    when (step) {
        FoldingStep.Preparation -> {
            Text(
                "Prepare o tecido conforme seus cuidados e passe os vincos. Não force " +
                    "um tecido torcido para alinhar as bordas.",
            )
            Text("Estampas direcionais e tecidos com pelo exigem outro estudo de disposição.")
        }

        FoldingStep.FirstFold -> Text("Alinhe sem esticar e alise as duas camadas.")

        FoldingStep.SecondFold -> Text(
            "Alinhe e alise as quatro camadas. O encontro das duas dobras fechadas, no " +
                "canto superior esquerdo, é a origem de todos os raios; não use um canto " +
                "com bordas abertas.",
        )

        FoldingStep.Marking -> MarkingContext(radii)

        FoldingStep.Cutting -> {
            Text(
                "O arco 2 é o corte da cintura (tracejado); o 4, o corte da barra " +
                    "(traço e ponto). Depois retire o miolo e a sobra externa.",
            )
            Text(
                "Não corte as linhas 1 e 3: são referências de costura e barra acabada. " +
                    "Não corte em linha reta pelas dobras; elas mantêm o círculo inteiro.",
            )
            coincidenceNotes(cuttingCard(radii, LocalResources.current::getString)).forEach {
                Text(it)
            }
            Text(
                "Para tecido da peça, abertura, margens do fechamento e cós pedem instruções " +
                    "à parte antes do corte.",
            )
        }

        FoldingStep.Unfolding -> {
            Text(
                "Primeiro surge um semicírculo à direita; depois, um círculo inteiro com o " +
                    "recorte central da cintura.",
            )
            Text(
                "Centro do recorte da cintura: mesma origem de todos os raios, agora no " +
                    "centro do círculo.",
            )
        }
    }
    Disclosure("Detalhes") {
        // Supporting text, quieter than the lead and context through the smaller type.
        ProvideTextStyle(
            MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            stepDetails(step).forEach { Text(it) }
            Text(
                "A imagem parada mostra o resultado da etapa. Reproduza ou repita a etapa para " +
                    "acompanhar o movimento em repetição contínua. Pause para observar; todas " +
                    "as instruções também estão escritas, para seguir sem animação ou cores.",
            )
        }
    }
}

/** The one action to take now, short enough to read under the pinned drawing. */
internal fun stepLead(step: FoldingStep): String = when (step) {
    FoldingStep.Preparation -> "Alise o tecido sem esticar e respeite o fio."

    FoldingStep.FirstFold ->
        "Dobre no sentido do comprimento, levando a ourela esquerda até a direita."

    FoldingStep.SecondFold ->
        "Dobre transversalmente, levando as bordas cortadas de cima até as de baixo."

    FoldingStep.Marking ->
        "Prenda fita ou régua no encontro das duas dobras fechadas e marque cada arco " +
            "com raio constante."

    FoldingStep.Cutting ->
        "Corte o arco 2 e depois o arco 4, atravessando as quatro camadas alinhadas."

    FoldingStep.Unfolding ->
        "Desfaça a segunda dobra, abrindo as duas camadas superiores para cima, e depois " +
            "a primeira, levando a camada de cima para a esquerda; alise sem esticar."
}

/** The current radii in key order, then where each line sits and which are not cut. */
@Composable
private fun MarkingContext(radii: SkirtRadii) {
    val card = cuttingCard(radii, LocalResources.current::getString)
    // The four radii read as one key: closer together than the paragraphs around them.
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        card.flatMap { it.entries }.sortedBy { it.number }.forEach { entry ->
            Text("${entry.number}. ${entry.label}: ${entry.value}")
        }
    }
    Text(
        "As linhas 1 (contínua) e 3 (pontilhada) são referências de costura e do " +
            "comprimento acabado, não de corte.",
    )
    Text(
        "A linha 2 (tracejada) fica para dentro da 1, reservando a margem de cintura; " +
            "a 4 (traço e ponto) fica para fora da 3, reservando a margem de barra.",
    )
    coincidenceNotes(card).forEach { Text(it) }
}

/** Conventions, honest limits and practice notes that support the step but don't change it. */
private fun stepDetails(step: FoldingStep): List<String> = when (step) {
    FoldingStep.Preparation -> listOf(
        "No desenho plano, as ourelas estão à esquerda e à direita (bordas pontilhadas); " +
            "o fio é paralelo a elas. As bordas cortadas estão em cima e embaixo (tracejadas).",
        "Comece com papel ou retalho.",
        IllustrativeFabric,
    )

    FoldingStep.FirstFold -> listOf(
        "Resultado: dobra fechada à esquerda (linha contínua); ourelas juntas à direita " +
            "(pontilhadas); bordas cortadas em cima e embaixo (tracejadas).",
        IllustrativeFabric,
    )

    FoldingStep.SecondFold -> listOf(
        "Resultado: dobras fechadas em cima e à esquerda (linhas contínuas); bordas " +
            "cortadas embaixo (tracejadas); ourelas à direita (pontilhadas).",
        "Na próxima etapa, ampliamos esse mesmo quadrado, sem girá-lo. A origem continua " +
            "no canto superior esquerdo.",
        IllustrativeFabric,
    )

    FoldingStep.Marking -> listOf(ProportionalDrawing, ConstructionBoundary)

    FoldingStep.Cutting -> listOf(
        "A animação volta ao início apenas para repetir a demonstração; não desfaça os cortes.",
        PracticeFirst,
        ProportionalDrawing,
        ConstructionBoundary,
    )

    FoldingStep.Unfolding -> listOf(
        "O desenho foi reduzido uniformemente para caber o círculo, sem mudar as " +
            "proporções. O quarto cortado começa abaixo e à direita da origem.",
        "O círculo desdobrado não é um molde completo e não garante caimento.",
        PracticeFirst,
        ProportionalDrawing,
        ConstructionBoundary,
    )
}

// Not `const`: ktlint then demands SCREAMING_SNAKE names, which Detekt's naming rule rejects.
@Suppress("MayBeConstant")
private val IllustrativeFabric =
    "Tecido ilustrativo, sem tamanho real. Pressupõe tecido suficiente; largura e " +
        "comprimento reais não foram verificados."

@Suppress("MayBeConstant")
private val ProportionalDrawing =
    "Proporções calculadas; desenho não está em tamanho real. O tamanho do tecido é " +
        "ilustrativo: pressupõe tecido suficiente, e a largura e o comprimento reais do " +
        "tecido não foram verificados."

@Suppress("MayBeConstant")
private val PracticeFirst =
    "Pratique em papel ou retalho; a sequência ainda aguarda validação prática por uma " +
        "pessoa que costura."

@Suppress("MayBeConstant")
private val ConstructionBoundary =
    "Antes de cortar tecido da peça, obtenha instruções separadas para abertura, margens " +
        "do fechamento e cós. Esta demonstração não é um molde completo."
