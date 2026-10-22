package com.almeidajr.molda.about.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.almeidajr.molda.R
import com.almeidajr.molda.ui.navigation.MoldaTopAppBar
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer

/**
 * About Molda: what it does, the installed [versionName] and the open-source licences row. The
 * space below the rows is reserved for the GitHub Releases update check.
 */
@Composable
fun AboutScreen(
    versionName: String,
    onUp: () -> Unit,
    onOpenLicences: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        MoldaTopAppBar(stringResource(R.string.about_title), onUp = onUp)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 640.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(R.string.app_name),
                        Modifier.semantics { heading() },
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        stringResource(R.string.about_description),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        stringResource(R.string.about_on_device),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalDivider()
                ListItem(
                    modifier = Modifier.semantics(mergeDescendants = true) {},
                    supportingContent = { Text(versionName) },
                ) { Text(stringResource(R.string.about_version)) }
                ListItem(
                    modifier = Modifier.heightIn(min = 56.dp).clickable(onClick = onOpenLicences),
                    leadingContent = {
                        Icon(painterResource(R.drawable.ic_license), contentDescription = null)
                    },
                    trailingContent = {
                        Icon(
                            painterResource(R.drawable.ic_chevron_right),
                            contentDescription = null,
                        )
                    },
                ) { Text(stringResource(R.string.about_licences)) }
            }
        }
    }
}

/** The open-source licences of the libraries Molda ships, collected at build time. */
@Composable
fun LicencesScreen(onUp: () -> Unit, modifier: Modifier = Modifier) {
    val libraries by produceLibraries(R.raw.aboutlibraries)
    Column(modifier.fillMaxSize()) {
        MoldaTopAppBar(stringResource(R.string.about_licences), onUp = onUp)
        LibrariesContainer(libraries, Modifier.fillMaxSize())
    }
}
