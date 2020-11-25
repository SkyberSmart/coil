package coil.fetch

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import androidx.annotation.VisibleForTesting
import coil.bitmap.BitmapPool
import coil.decode.DataSource
import coil.decode.Options
import coil.network.HttpException
import coil.size.Size
import coil.util.await
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import okio.BufferedSource
import okio.Okio
import okio.buffer
import okio.source
import java.io.BufferedInputStream
import java.io.InputStream

internal class StorageFetcher(private val context: Context) : Fetcher<StorageReference> {

    override fun key(data: StorageReference) = data.path

    override suspend fun fetch(
        pool: BitmapPool,
        data: StorageReference,
        size: Size,
        options: Options
    ): FetchResult {

        val meta = data.getMetadata().await()
        val task = data.getStream().await()

        //bitmap = BitmapFactory.decodeStream(task.stream)
        //eturn@withContext bitmap

        return SourceResult(
            source = task.stream.source().buffer(),
            mimeType = meta.contentType,
            dataSource = DataSource.STORAGE
        )
    }
}
