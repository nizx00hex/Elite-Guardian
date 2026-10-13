package com.eliteguardian.app.data.repository

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import com.eliteguardian.app.data.database.AppDatabase
import com.eliteguardian.app.data.database.BlockedAppEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.CopyOnWriteArraySet

data class AppInfoItem(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val isBlocked: Boolean
)

class AppBlockerRepository(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val database = AppDatabase.getInstance(context)
    private val dao = database.blockedAppDao()
    private val prefs = context.getSharedPreferences("elite_blocked_apps_prefs", Context.MODE_PRIVATE)

    // Synchronous memory cache backed by SharedPreferences
    val blockedPackageSet = CopyOnWriteArraySet<String>().apply {
        addAll(prefs.getStringSet("blocked_pkgs", emptySet()) ?: emptySet())
    }

    init {
        scope.launch {
            // Sync database with cache
            val active = dao.getActiveBlockedPackageNames()
            blockedPackageSet.addAll(active)
            prefs.edit().putStringSet("blocked_pkgs", blockedPackageSet).apply()

            dao.getActiveBlockedApps().distinctUntilChanged().collect { apps ->
                blockedPackageSet.clear()
                apps.forEach { blockedPackageSet.add(it.packageName) }
                prefs.edit().putStringSet("blocked_pkgs", blockedPackageSet).apply()
            }
        }
    }

    fun getAllBlockedAppsFlow(): Flow<List<BlockedAppEntity>> = dao.getAllBlockedApps()

    suspend fun getInstalledApps(): List<AppInfoItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        val activeBlocked = blockedPackageSet

        val myPackage = context.packageName

        resolveInfos.mapNotNull { resolveInfo ->
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg == myPackage) return@mapNotNull null

            val name = resolveInfo.loadLabel(pm).toString()
            val icon = try {
                resolveInfo.loadIcon(pm)
            } catch (e: Exception) {
                null
            }
            AppInfoItem(
                packageName = pkg,
                appName = name,
                icon = icon,
                isBlocked = activeBlocked.contains(pkg)
            )
        }.distinctBy { it.packageName }.sortedBy { it.appName.lowercase() }
    }

    suspend fun toggleAppBlock(packageName: String, appName: String, isBlocked: Boolean) {
        if (isBlocked) {
            blockedPackageSet.add(packageName)
        } else {
            blockedPackageSet.remove(packageName)
        }
        // Commit synchronously to preferences
        prefs.edit().putStringSet("blocked_pkgs", blockedPackageSet).commit()

        dao.insert(
            BlockedAppEntity(
                packageName = packageName,
                appName = appName,
                isBlocked = isBlocked
            )
        )
    }

    fun isAppBlocked(packageName: String): Boolean {
        if (blockedPackageSet.contains(packageName)) return true
        val saved = prefs.getStringSet("blocked_pkgs", null)
        return saved?.contains(packageName) == true
    }

    companion object {
        @Volatile
        private var INSTANCE: AppBlockerRepository? = null

        fun getInstance(context: Context): AppBlockerRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = AppBlockerRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
