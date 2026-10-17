package it.securepgp.app.storage

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract

sealed class TestResult {
    data object NoFolderSelected : TestResult()
    data class FileAlreadyExists(val fileName: String) : TestResult()
    data class Success(
        val fileName: String,
        val writeSuccess: Boolean,
        val readSuccess: Boolean,
        val contentMatched: Boolean,
        val fileDeleted: Boolean,
    ) : TestResult()

    data class Failure(val message: String) : TestResult()
}

object TestFileStorage {
    const val TEST_FILE_NAME = "securepgp_io_test.txt"
    private const val TEST_FILE_CONTENT = "SECUREPGP_IO_TEST_OK"

    fun runIoTest(context: Context, treeUri: Uri?): TestResult {
        if (treeUri == null) {
            return TestResult.NoFolderSelected
        }

        return try {
            val contentResolver = context.contentResolver

            // Document URI della cartella selezionata dall'utente
            val treeDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
            val parentDocumentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocumentId)

            // 1. Controllo preventivo di esistenza di securepgp_io_test.txt
            val existingFileUri = findExistingChildFile(context, treeUri, parentDocumentUri, TEST_FILE_NAME)
            if (existingFileUri != null) {
                return TestResult.FileAlreadyExists(TEST_FILE_NAME)
            }

            // 2. Creazione del nuovo documento e conservazione dell'URI esatto generato da questa esecuzione
            val createdDocumentUri = DocumentsContract.createDocument(
                contentResolver,
                parentDocumentUri,
                "text/plain",
                TEST_FILE_NAME,
            ) ?: return TestResult.Failure("Impossibile creare il documento di test nella cartella.")

            var contentMatched: Boolean

            try {
                // 3. Scrittura del testo fisso
                contentResolver.openOutputStream(createdDocumentUri, "w")?.use { output ->
                    output.write(TEST_FILE_CONTENT.toByteArray(Charsets.UTF_8))
                } ?: return TestResult.Failure("Impossibile aprire il flusso di scrittura per il file di test.")

                // 4. Rilettura e confronto del contenuto
                contentResolver.openInputStream(createdDocumentUri)?.use { input ->
                    val readBytes = input.readBytes()
                    val readText = String(readBytes, Charsets.UTF_8)
                    contentMatched = (readText == TEST_FILE_CONTENT)
                } ?: return TestResult.Failure("Impossibile aprire il flusso di lettura per il file di test.")

                // 5. Se la verifica del contenuto è riuscita, procediamo all'eliminazione ESCLUSIVA di createdDocumentUri
                val fileDeleted = if (contentMatched) {
                    try {
                        DocumentsContract.deleteDocument(contentResolver, createdDocumentUri)
                    } catch (_: Exception) {
                        false
                    }
                } else {
                    false
                }

                TestResult.Success(
                    fileName = TEST_FILE_NAME,
                    writeSuccess = true,
                    readSuccess = true,
                    contentMatched = contentMatched,
                    fileDeleted = fileDeleted,
                )
            } catch (e: Exception) {
                // In caso di errore durante scrittura/lettura, NON cerchiamo file per nome.
                // Proviamo a rimuovere SOLO il documento creato se l'URI è valido
                try {
                    DocumentsContract.deleteDocument(contentResolver, createdDocumentUri)
                } catch (_: Exception) {
                    // Ignorato per sicurezza
                }
                TestResult.Failure("Errore durante l'operazione di I/O: ${e.localizedMessage ?: "errore sconosciuto"}.")
            }
        } catch (_: SecurityException) {
            TestResult.Failure("Errore di sicurezza SAF: permessi revocati o negati dal sistema.")
        } catch (e: Exception) {
            TestResult.Failure("Errore imprevisto: ${e.localizedMessage ?: "errore sconosciuto"}.")
        }
    }

    private fun findExistingChildFile(
        context: Context,
        treeUri: Uri,
        parentDocumentUri: Uri,
        targetName: String,
    ): Uri? {
        return try {
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                treeUri,
                DocumentsContract.getDocumentId(parentDocumentUri),
            )
            val cursor = context.contentResolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                ),
                null,
                null,
                null,
            )

            cursor?.use {
                val idIndex = it.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = it.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)

                while (it.moveToNext()) {
                    val name = if (nameIndex != -1) it.getString(nameIndex) else null
                    if (name == targetName) {
                        val docId = if (idIndex != -1) it.getString(idIndex) else null
                        if (docId != null) {
                            return DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                        }
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }
}
