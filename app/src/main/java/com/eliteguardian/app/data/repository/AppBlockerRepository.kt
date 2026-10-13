package com.eliteguardian.app.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
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

    val blockedPackageSet = CopyOnWriteArraySet<String>()

    init {
        scope.launch {
            dao.getActiveBlockedApps().distinctUntilChanged().collect { apps ->
                blockedPackageSet.clear()
                apps.forEach { blockedPackageSet.add(it.packageName) }
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
        val activeBlocked = dao.getActiveBlockedPackageNames().toSet()

        val myPackage = context.packageName

        resolveInfos.mapNotNull { resolveInfo ->
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg == myPackage) return@mapNotNull null

            val name = resolveInfo.loadLabel(pm).toString()
            val icon = resolveInfo.loadIcon(pm)
            AppInfoItem(
                packageName = pkg,
                appName = name,
                icon = icon,
                isBlocked = activeBlocked.contains(pkg)
            )
        }.distinctBy { it.packageName }.sortedBy { it.appName.lowercase() }
    }

    suspend fun toggleAppBlock(packageName: String, appName: String, isBlocked: Boolean) {
        dao.insert(
            BlockedAppEntity(
                packageName = packageName,
                appName = appName,
                isBlocked = isBlocked
            )
        )
        if (isBlocked) {
            blockedPackageSet.add(packageName)
        } else {
            blockedPackageSet.remove(packageName)
        }
    }

    fun isAppBlocked(packageName: String): Boolean {
        return blockedPackageSet.contains(packageName)
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

