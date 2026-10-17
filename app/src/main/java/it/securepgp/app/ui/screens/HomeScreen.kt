package it.securepgp.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.securepgp.app.storage.FolderAccessResult
import it.securepgp.app.storage.StorageHelper
import it.securepgp.app.storage.TestFileStorage
import it.securepgp.app.storage.TestResult
import it.securepgp.app.ui.components.SectionCard
import it.securepgp.app.ui.theme.SecurePGPTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    var folderStatus by remember { mutableStateOf("Verifica cartella in corso...") }
    var isFolderAccessible by remember { mutableStateOf(false) }
    var currentSavedUri by remember { mutableStateOf<Uri?>(null) }
    var testResultStatus by remember { mutableStateOf("") }

    // Ripristino e verifica asincrona all'avvio della schermata
    LaunchedEffect(Unit) {
        val savedUri = StorageHelper.getSavedFolderUri(context)
        currentSavedUri = savedUri
        if (savedUri == null) {
            folderStatus = "Nessuna cartella selezionata."
            isFolderAccessible = false
        } else {
            val result = StorageHelper.checkFolderAccessibility(context, savedUri)
            when (result) {
                is FolderAccessResult.Success -> {
                    folderStatus = "Cartella selezionata: '${result.folderName}'\n" +
                        "Lettura: ${if (result.canRead) "Concessa" else "Negata"} | " +
                        "Scrittura: ${if (result.canWrite) "Concessa" else "Negata"}"
                    isFolderAccessible = true
                }
                is FolderAccessResult.Error -> {
                    folderStatus = "Stato: ${result.message}"
                    isFolderAccessible = false
                }
                is FolderAccessResult.Cancelled -> {
                    folderStatus = "Nessuna cartella selezionata."
                    isFolderAccessible = false
                }
            }
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            StorageHelper.saveSelectedFolder(context, uri)
            currentSavedUri = uri
            val result = StorageHelper.processSelectedFolder(context, uri)
            when (result) {
                is FolderAccessResult.Success -> {
                    folderStatus = "Cartella selezionata: '${result.folderName}'\n" +
                        "Lettura: ${if (result.canRead) "Concessa" else "Negata"} | " +
                        "Scrittura: ${if (result.canWrite) "Concessa" else "Negata"}"
                    isFolderAccessible = true
                }
                is FolderAccessResult.Error -> {
                    folderStatus = "Stato: ${result.message}"
                    isFolderAccessible = false
                }
                is FolderAccessResult.Cancelled -> {
                    folderStatus = "Selezione annullata dall'utente."
                    isFolderAccessible = false
                }
            }
        } else {
            folderStatus = "Selezione annullata dall'utente."
        }
        testResultStatus = ""
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SecurePGP",
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Avviso di versione iniziale
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    text = "Versione iniziale — funzioni in sviluppo",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Medium,
                )
            }

            // Sezione 1: Crittografia e decifratura
            SectionCard(
                title = "Crittografia e decifratura",
                description = "Sezione pianificata per cifrare e decifrare messaggi e file. La logica di protezione sarà implementata nelle prossime fasi.",
            )

            // Sezione 2: Chiavi OpenPGP
            SectionCard(
                title = "Chiavi OpenPGP",
                description = "Sezione pianificata per la generazione, l'importazione e l'organizzazione delle chiavi pubbliche e private.",
            )

            // Sezione 3: Archivio su scheda SD
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = "Archivio su scheda SD",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sezione pianificata per la gestione e il salvataggio dei dati su memoria esterna rimovibile.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { folderPickerLauncher.launch(null) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = "Seleziona cartella")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = folderStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val savedUri = currentSavedUri ?: StorageHelper.getSavedFolderUri(context)
                            val res = TestFileStorage.runIoTest(context, savedUri)
                            testResultStatus = when (res) {
                                is TestResult.NoFolderSelected -> "Nessuna cartella selezionata."
                                is TestResult.FileAlreadyExists -> "Conflitto: il file '${res.fileName}' è già presente nella cartella. Test annullato per sicurezza."
                                is TestResult.Success -> {
                                    "Test I/O completato con successo!\n" +
                                        "・ Scrittura: OK\n" +
                                        "・ Rilettura e confronto: OK\n" +
                                        "・ Cancellazione file di test: ${if (res.fileDeleted) "Eseguita (cartella pulita)" else "Non eseguita (file conservato)"}"
                                }
                                is TestResult.Failure -> "Errore durante il test: ${res.message}"
                            }
                        },
                        enabled = isFolderAccessible && currentSavedUri != null,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = "Esegui test lettura/scrittura")
                    }

                    if (testResultStatus.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = testResultStatus,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            // Sezione 4: Crediti e ricariche offline
            SectionCard(
                title = "Crediti e ricariche offline",
                description = "Sezione pianificata per la consultazione dei crediti e la gestione delle ricariche in modalità offline.",
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    SecurePGPTheme {
        HomeScreen()
    }
}
