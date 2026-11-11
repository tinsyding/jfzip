package com.tinsyding.jfzip.domain

import android.content.ContentResolver
import android.net.Uri
import com.tinsyding.jfzip.util.displayNameOf
import com.tinsyding.jfzip.util.extensionFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ZipBuilder {
    suspend fun build(
        resolver: ContentResolver,
        uris: List<Uri>,
        prefix: String,
        output: File,
        onProgress: (Int) -> Unit,
    ) = withContext(Dispatchers.IO) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(output))).use { zip ->
            zip.setLevel(Deflater.NO_COMPRESSION)

            uris.forEachIndexed { index, uri ->
                coroutineContext.ensureActive()

                val extension = extensionFor(
                    mimeType = resolver.getType(uri),
                    displayName = resolver.displayNameOf(uri),
                )
                zip.putNextEntry(ZipEntry(imageEntryName(prefix, index + 1, extension)))

                resolver.openInputStream(uri)?.use { input ->
                    input.copyTo(zip, 64 * 1024)
                } ?: throw IOException("Cannot open $uri")

                zip.closeEntry()
                onProgress(index + 1)
            }
        }
    }
}
