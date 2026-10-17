package it.securepgp.app.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract

sealed class FolderAccessResult {
    data class Success(
        val folderName: String,
        val canRead: Boolean,
        val canWrite: Boolean,
    ) : FolderAccessResult()

    data class Error(val message: String) : FolderAccessResult()
    data object Cancelled : FolderAccessResult()
}

object StorageHelper {

    fun processSelectedFolder(context: Context, uri: Uri?): FolderAccessResult {
        if (uri == null) {
            return FolderAccessResult.Cancelled
        }

        return try {
            val contentResolver = context.contentResolver

            // Flag di permesso di lettura e scrittura
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            // Richiediamo i permessi persistenti soltanto per quelli effettivamente concessi da Android
            try {
                contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (_: SecurityException) {
                try {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: SecurityException) {
                    // Se fallisce, proseguiremo con la verifica tramite ContentResolver
                }
            }

            // Otteniamo il documentId del tree URI tramite DocumentsContract (API Android native)
            val treeDocumentId = DocumentsContract.getTreeDocumentId(uri)
            val documentUri = DocumentsContract.buildDocumentUriUsingTree(uri, treeDocumentId)

            var folderName = "Cartella selezionata"
            var canWrite = false
            var canRead = false

            // Eseguiamo una query tramite ContentResolver per verificare l'accessibilità reale
            val cursor = contentResolver.query(
                documentUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_FLAGS,
                ),
                null,
                null,
                null,
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val flagsIndex = it.getColumnIndex(DocumentsContract.Document.COLUMN_FLAGS)

                    if (nameIndex != -1) {
                        folderName = it.getString(nameIndex) ?: folderName
                    }
                    if (flagsIndex != -1) {
                        val flags = it.getInt(flagsIndex)
                        canWrite = (((flags and DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE) != 0) ||
                            ((flags and DocumentsContract.Document.FLAG_SUPPORTS_WRITE) != 0))
                    }
                    canRead = true
                }
            }

            if (!canRead) {
                FolderAccessResult.Error("Impossibile leggere le informazioni della cartella selezionata.")
            } else {
                FolderAccessResult.Success(
                    folderName = folderName,
                    canRead = true,
                    canWrite = canWrite,
                )
            }
        } catch (_: SecurityException) {
            FolderAccessResult.Error("Errore di sicurezza: permessi negati o revocati dal sistema.")
        } catch (e: Exception) {
            FolderAccessResult.Error("Errore durante la verifica: ${e.localizedMessage ?: "errore sconosciuto"}.")
        }
    }

    fun checkFolderAccessibility(context: Context, uri: Uri?): FolderAccessResult {
        if (uri == null) {
            return FolderAccessResult.Error("Nessuna cartella selezionata.")
        }

        // 1. Verifichiamo che l'URI disponga dei permessi persistenti attivi
        val persistedPermissions = context.contentResolver.persistedUriPermissions
        val hasPersistedPermission = persistedPermissions.any { perm ->
            perm.uri == uri && (perm.isReadPermission || perm.isWritePermission)
        }

        if (!hasPersistedPermission) {
            return FolderAccessResult.Error("Permessi SAF non più disponibili per la cartella memorizzata.")
        }

        // 2. Controllo in sola lettura dell'accessibilità reale della cartella
        return try {
            val treeDocumentId = DocumentsContract.getTreeDocumentId(uri)
            val documentUri = DocumentsContract.buildDocumentUriUsingTree(uri, treeDocumentId)

            var folderName = "Cartella selezionata"
            var canWrite = false
            var canRead = false

            val cursor = context.contentResolver.query(
                documentUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_FLAGS,
                ),
                null,
                null,
                null,
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val flagsIndex = it.getColumnIndex(DocumentsContract.Document.COLUMN_FLAGS)

                    if (nameIndex != -1) {
                        folderName = it.getString(nameIndex) ?: folderName
                    }
                    if (flagsIndex != -1) {
                        val flags = it.getInt(flagsIndex)
                        canWrite = (((flags and DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE) != 0) ||
                            ((flags and DocumentsContract.Document.FLAG_SUPPORTS_WRITE) != 0))
                    }
                    canRead = true
                }
            }

            if (!canRead) {
                FolderAccessResult.Error("Cartella non più accessibile o non trovata.")
            } else {
                FolderAccessResult.Success(
                    folderName = folderName,
                    canRead = true,
                    canWrite = canWrite,
                )
            }
        } catch (_: SecurityException) {
            FolderAccessResult.Error("Errore di sicurezza: permessi negati o revocati dal sistema.")
        } catch (e: Exception) {
            FolderAccessResult.Error("Cartella non accessibile: ${e.localizedMessage ?: "errore sconosciuto"}.")
        }
    }

    fun saveSelectedFolder(context: Context, uri: Uri) {
        val preferences = context.getSharedPreferences(
            "securepgp_storage",
            Context.MODE_PRIVATE,
        )

        preferences.edit()
            .putString("folder_uri", uri.toString())
            .apply()
    }

    fun getSavedFolderUri(context: Context): Uri? {
        val preferences = context.getSharedPreferences(
            "securepgp_storage",
            Context.MODE_PRIVATE,
        )

        val savedUri = preferences.getString("folder_uri", null)
            ?: return null

        return Uri.parse(savedUri)
    }
}
