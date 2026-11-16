package dev.horizon.android

import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.FileNotFoundException
import java.util.ArrayList

/** Android SAF adapter only. The allowlist, paths and file operations live in Rust. */
class HorizonDocumentsProvider : DocumentsProvider() {

    override fun onCreate(): Boolean {
        try {
            System.loadLibrary("horizon_android")
        } catch (_: Throwable) {}
        return true
    }

    @Throws(FileNotFoundException::class)
    private fun call(operation: String, id: String, arg: String): String {
        val context = context ?: throw FileNotFoundException("Context is null")
        return nativeCall(context.filesDir.absolutePath, operation, id, arg)
    }

    @Throws(FileNotFoundException::class)
    private fun query(operation: String, id: String, projection: Array<String>?, defaults: Array<String>): Cursor {
        val columns = projection ?: defaults
        val cursor = MatrixCursor(columns)
        try {
            val rows = JSONArray(call(operation, id, ""))
            for (i in 0 until rows.length()) {
                val data = rows.getJSONObject(i)
                val row = cursor.newRow()
                for (column in columns) {
                    var value: Any? = data.opt(column)
                    if ("icon" == column && context != null) {
                        value = context!!.applicationInfo.icon
                    }
                    row.add(if (value === JSONObject.NULL) null else value)
                }
            }
            return cursor
        } catch (_: JSONException) {
            cursor.close()
            throw FileNotFoundException("Invalid Horizon document response")
        }
    }

    @Throws(FileNotFoundException::class)
    private fun changed(operation: String, id: String, arg: String): String {
        val ctx = context ?: throw FileNotFoundException("Context is null")
        val response = call(operation, id, arg)
        ctx.contentResolver.notifyChange(
            DocumentsContract.buildRootsUri("${ctx.packageName}.documents"), null
        )
        var parent = if (id.lastIndexOf('/') < 0) "horizon" else id.substring(0, id.lastIndexOf('/'))
        if ("create" == operation || "mkdir" == operation) parent = id
        ctx.contentResolver.notifyChange(
            DocumentsContract.buildChildDocumentsUri("${ctx.packageName}.documents", parent), null
        )
        return try {
            JSONArray("[$response]").optString(0, null)
        } catch (_: JSONException) {
            throw FileNotFoundException("Invalid document ID")
        }
    }

    @Throws(FileNotFoundException::class)
    override fun queryRoots(projection: Array<String>?): Cursor {
        return query("roots", "", projection, ROOT_COLUMNS)
    }

    @Throws(FileNotFoundException::class)
    override fun queryDocument(id: String, projection: Array<String>?): Cursor {
        return query("document", id, projection, DOCUMENT_COLUMNS)
    }

    @Throws(FileNotFoundException::class)
    override fun queryChildDocuments(id: String, projection: Array<String>?, sortOrder: String?): Cursor {
        val ctx = context ?: throw FileNotFoundException("Context is null")
        val cursor = query("children", id, projection, DOCUMENT_COLUMNS)
        val children = DocumentsContract.buildChildDocumentsUri("${ctx.packageName}.documents", id)
        cursor.setNotificationUri(ctx.contentResolver, children)
        return cursor
    }

    override fun isChildDocument(parent: String, child: String): Boolean {
        return try {
            "true" == call("child", parent, child)
        } catch (_: FileNotFoundException) {
            false
        }
    }

    @Throws(FileNotFoundException::class)
    override fun findDocumentPath(parent: String?, child: String): DocumentsContract.Path {
        return try {
            val ids = JSONArray(call("path", child, parent ?: ""))
            val path = ArrayList<String>()
            for (i in 0 until ids.length()) {
                path.add(ids.getString(i))
            }
            DocumentsContract.Path(if (parent == null) "horizon" else null, path)
        } catch (_: JSONException) {
            throw FileNotFoundException("Invalid document path")
        }
    }

    @Throws(FileNotFoundException::class)
    override fun openDocument(id: String, mode: String, signal: CancellationSignal?): ParcelFileDescriptor {
        signal?.throwIfCanceled()
        val ctx = context ?: throw FileNotFoundException("Context is null")
        val fd = nativeOpen(ctx.filesDir.absolutePath, id, mode)
        return ParcelFileDescriptor.adoptFd(fd)
    }

    @Throws(FileNotFoundException::class)
    override fun createDocument(parent: String, mime: String, name: String): String {
        return changed(
            if (DocumentsContract.Document.MIME_TYPE_DIR == mime) "mkdir" else "create",
            parent,
            name
        )
    }

    @Throws(FileNotFoundException::class)
    override fun renameDocument(id: String, name: String): String {
        return changed("rename", id, name)
    }

    @Throws(FileNotFoundException::class)
    override fun deleteDocument(id: String) {
        changed("delete", id, "")
    }

    companion object {
        private val ROOT_COLUMNS = arrayOf(
            "root_id", "document_id", "title", "flags", "mime_types", "icon"
        )
        private val DOCUMENT_COLUMNS = arrayOf(
            "document_id", "_display_name", "mime_type", "flags", "_size", "last_modified"
        )

        @Suppress("KotlinJniMissingFunction")
        @JvmStatic
        @Throws(FileNotFoundException::class)
        private external fun nativeCall(home: String, operation: String, id: String, arg: String): String

        @Suppress("KotlinJniMissingFunction")
        @JvmStatic
        @Throws(FileNotFoundException::class)
        private external fun nativeOpen(home: String, id: String, mode: String): Int
    }
}
