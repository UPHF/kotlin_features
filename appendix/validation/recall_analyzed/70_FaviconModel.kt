package acr.browser.lightning.favicon

import acr.browser.lightning.R
import acr.browser.lightning.extensions.pad
import acr.browser.lightning.extensions.safeUse
import acr.browser.lightning.log.Logger
import acr.browser.lightning.utils.DrawableUtils
import acr.browser.lightning.utils.FileUtils
import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.annotation.ColorInt
import androidx.annotation.WorkerThread
import androidx.core.net.toUri
import io.reactivex.Completable
import io.reactivex.Maybe
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reactive model that can fetch favicons
 * from URLs and also cache them.
 */
@Singleton
class FaviconModel @Inject constructor(
    private val application: Application,
    private val logger: Logger
) {

    private val loaderOptions = BitmapFactory.Options() //#inference
    private val bookmarkIconSize = application.resources.getDimensionPixelSize(R.dimen.material_grid_small_icon) //#inference
    private val faviconCache = object : LruCache<String, Bitmap>(FileUtils.megabytesToBytes(1).toInt()) { //#inference
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    /**
     * Retrieves a favicon from the memory cache.Bitmap may not be present if no bitmap has been
     * added for the URL or if it has been evicted from the memory cache.
     *
     * @param url the URL to retrieve the bitmap for.
     * @return the bitmap associated with the URL, may be null.
     */
    private fun getFaviconFromMemCache(url: String): Bitmap? {
        synchronized(faviconCache) {
            return faviconCache.get(url)
        }
    }

    fun createDefaultBitmapForTitle(title: String?): Bitmap {
        val firstTitleCharacter = title?.takeIf(String::isNotBlank)?.let { it[0] } ?: '?' //#inference,safe_call,lambda,safe_call

        @ColorInt val defaultFaviconColor = DrawableUtils.characterToColorHash(firstTitleCharacter, application) //#inference

        return DrawableUtils.createRoundedLetterImage(
            firstTitleCharacter,
            bookmarkIconSize,
            bookmarkIconSize,
            defaultFaviconColor
        )
    }

    /**
     * Adds a bitmap to the memory cache for the given URL.
     *
     * @param url    the URL to map the bitmap to.
     * @param bitmap the bitmap to store.
     */
    private fun addFaviconToMemCache(url: String, bitmap: Bitmap) {
        synchronized(faviconCache) {
            faviconCache.put(url, bitmap)
        }
    }

    /**
     * Retrieves the favicon for a URL, may be from network or cache.
     *
     * @param url   The URL that we should retrieve the favicon for.
     * @param title The title for the web page.
     */
    fun faviconForUrl(url: String, title: String): Maybe<Bitmap> = Maybe.create { //#lambda
        val uri = url.toUri().toValidUri() //#inference
            ?: return@create it.onSuccess(createDefaultBitmapForTitle(title).pad()) 

        val cachedFavicon = getFaviconFromMemCache(url) //#inference

        if (cachedFavicon != null) {
            return@create it.onSuccess(cachedFavicon.pad())
        }

        val faviconCacheFile = getFaviconCacheFile(application, uri) //#inference

        if (faviconCacheFile.exists()) {
            val storedFavicon = BitmapFactory.decodeFile(faviconCacheFile.path, loaderOptions) //#inference

            if (storedFavicon != null) {
                addFaviconToMemCache(url, storedFavicon)
                return@create it.onSuccess(storedFavicon.pad())
            }
        }

        return@create it.onSuccess(createDefaultBitmapForTitle(title).pad())
    }

    /**
     * Caches a favicon for a particular URL.
     *
     * @param favicon the favicon to cache.
     * @param url     the URL to cache the favicon for.
     * @return an observable that notifies the consumer when it is complete.
     */
    fun cacheFaviconForUrl(favicon: Bitmap, url: String): Completable = Completable.create { emitter -> //#lambda
        val uri = url.toUri().toValidUri() ?: return@create emitter.onComplete() //#inference

        logger.log(TAG, "Caching icon for ${uri.host}") //#string_template
        FileOutputStream(getFaviconCacheFile(application, uri)).safeUse { //#lambda
            favicon.compress(Bitmap.CompressFormat.PNG, 100, it)
            it.flush()
            emitter.onComplete()
        }
    }

    companion object { //#companion

        private const val TAG = "FaviconModel" //#inference

        /**
         * Creates the cache file for the favicon image. File name will be in the form of "hash of URI host".png
         *
         * @param app the context needed to retrieve the cache directory.
         * @param validUri the URI to use as a unique identifier.
         * @return a valid cache file.
         */
        @WorkerThread
        fun getFaviconCacheFile(app: Application, validUri: ValidUri): File {
            val hash = validUri.host.hashCode().toString() //#inference

            return File(app.cacheDir, "$hash.png") //#string_template
        }
    }

}
