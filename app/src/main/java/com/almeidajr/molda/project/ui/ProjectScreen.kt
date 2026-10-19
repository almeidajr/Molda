package com.almeidajr.molda.project.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.almeidajr.molda.R
import com.almeidajr.molda.project.domain.Project
import com.almeidajr.molda.project.domain.ProjectDateFormat
import com.almeidajr.molda.ui.navigation.MoldaTopAppBar

/**
 * An opened project, read-only: its name as the title, where its measurements came from, then
 * its edit, rename and delete actions and the saved result, drawn by [content]. The up arrow
 * shows only with [onUp].
 */
@Composable
fun ProjectScreen(
    project: Project,
    onUp: (() -> Unit)?,
    onEdit: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier.fillMaxSize()) {
        MoldaTopAppBar(project.name.text, onUp = onUp)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 640.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val source = project.snapshot.source
                val measurementDate = source?.measurementDate
                Text(
                    when {
                        source == null -> stringResource(R.string.project_source_manual)

                        measurementDate == null -> stringResource(
                            R.string.project_source_profile_undated,
                            source.profileName,
                        )

                        else -> stringResource(
                            R.string.project_source_profile,
                            source.profileName,
                            measurementDate.format(ProjectDateFormat),
                        )
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(onClick = onEdit, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.project_edit))
                    }
                    ProjectActions(project, onRename, onDelete)
                }
                content()
            }
        }
    }
}
