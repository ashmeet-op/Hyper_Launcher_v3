package com.ashmeet.hyperlauncher.activity

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.system.Os
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.ashmeet.hyperlauncher.fragments.auth.AuthHostFragment
import com.ashmeet.hyperlauncher.fragments.home.MainMenuFragment
import com.ashmeet.hyperlauncher.fragments.installer.ContentInstallerFragment
import com.ashmeet.hyperlauncher.fragments.instances.InstanceDirectoryFragment
import com.ashmeet.hyperlauncher.fragments.settings.LauncherPreferenceFragment
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.utils.ShortcutUtils
import com.ashmeet.hyperlauncher.utils.Tools
import com.ashmeet.hyperlauncher.utils.helper.LauncherComposeHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.ashmeet.hyperlauncher.BuildConfig
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.BaseActivity
import net.kdt.pojavlaunch.PojavApplication
import net.kdt.pojavlaunch.authenticator.accounts.Accounts
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore
import net.kdt.pojavlaunch.extra.ExtraListener
import net.kdt.pojavlaunch.instances.InstanceInstaller
import net.kdt.pojavlaunch.instances.Instances
import net.kdt.pojavlaunch.lifecycle.ContextAwareDoneListener
import net.kdt.pojavlaunch.lifecycle.ContextExecutor
import net.kdt.pojavlaunch.modloaders.modpacks.imagecache.IconCacheJanitor
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper
import net.kdt.pojavlaunch.progresskeeper.TaskCountListener
import net.kdt.pojavlaunch.services.ProgressServiceKeeper
import net.kdt.pojavlaunch.tasks.AsyncVersionList
import net.kdt.pojavlaunch.tasks.MoJsonDownloader
import net.kdt.pojavlaunch.tasks.MoJsonExtras
import net.kdt.pojavlaunch.utils.NotificationUtils
import java.io.IOException

class LauncherActivity : BaseActivity(), PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private var mProgressServiceKeeper: ProgressServiceKeeper? = null
    private var mNotificationManager: NotificationManager? = null
    private lateinit var mRequestPermissionLauncher: ActivityResultLauncher<String>

    /* Allows to switch from one button "type" to another */
    private val mFragmentCallbackListener = object : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
            val isMain = f is MainMenuFragment
            LauncherComposeHelper.setSettingsIcon(
                if (isMain) com.ashmeet.hyperlauncher.utils.drawable.MaterialIconUtil.ICON_SETTINGS
                else com.ashmeet.hyperlauncher.utils.drawable.MaterialIconUtil.ICON_HOME
            )
            LauncherComposeHelper.setFileManagerVisible(isMain)
        }
    }

    /* Listener for the back button in settings */
    private val mBackPreferenceListener = ExtraListener<String> { _, value ->
        if (value == "true") onBackPressedDispatcher.onBackPressed()
        false
    }

    /* Listener for the auth method selection screen */
    private val mSelectAuthMethod = ExtraListener<Boolean> { _, value ->
        // The "false" value is used to stop auth method selection
        val manager = supportFragmentManager
        if (!value || manager.isStateSaved) return@ExtraListener false
        val fragment = manager.findFragmentById(R.id.container_fragment)
        // Allow starting the add account only from the main menu, should it be moved to fragment itself ?
        if (fragment !is MainMenuFragment) return@ExtraListener false

        Tools.swapFragment(this, AuthHostFragment::class.java, AuthHostFragment.TAG, null)
        false
    }

    /* Listener for the settings fragment */
    private val mSettingButtonListener = View.OnClickListener {
        val manager = supportFragmentManager
        if (manager.isStateSaved) return@OnClickListener
        val fragment = manager.findFragmentById(R.id.container_fragment)
        if (fragment is MainMenuFragment) {
            Tools.swapFragment(this, LauncherPreferenceFragment::class.java, SETTING_FRAGMENT_TAG, null)
        } else {
            // The setting button doubles as a home button now
            Tools.backToMainMenu(this)
        }
    }

    /* Listener for the instance directory button */
    private val mInstanceDirectoryButtonListener = View.OnClickListener {
        val manager = supportFragmentManager
        if (manager.isStateSaved) return@OnClickListener
        val fragment = manager.findFragmentById(R.id.container_fragment)
        if (fragment is MainMenuFragment) {
            Tools.swapFragment(this, InstanceDirectoryFragment::class.java, InstanceDirectoryFragment.TAG, null)
        }
    }

    /* Listener for the content installer button */
    private val mContentInstallerButtonListener = View.OnClickListener {
        val manager = supportFragmentManager
        if (manager.isStateSaved) return@OnClickListener
        val fragment = manager.findFragmentById(R.id.container_fragment)
        if (fragment is MainMenuFragment) {
            Tools.swapFragment(this, ContentInstallerFragment::class.java, ContentInstallerFragment.TAG, null)
        }
    }

    private val mLaunchGameListener = ExtraListener<Boolean> { _, _ ->
        if (ProgressKeeper.getTaskCount() > 0) {
            Toast.makeText(this, R.string.tasks_ongoing, Toast.LENGTH_LONG).show()
            return@ExtraListener false
        }

        val selectedInstance = Instances.loadSelectedInstance()

        if (selectedInstance == null) {
            Toast.makeText(this, R.string.no_instance, Toast.LENGTH_LONG).show()
            return@ExtraListener false
        }

        if (selectedInstance.installer != null) {
            selectedInstance.installer.start()
            return@ExtraListener false
        }

        if (!Tools.isValidString(selectedInstance.versionId)) {
            Toast.makeText(this, R.string.error_no_version, Toast.LENGTH_LONG).show()
            return@ExtraListener false
        }

        if (Accounts.getCurrent() == null) {
            Toast.makeText(this, R.string.no_saved_accounts, Toast.LENGTH_LONG).show()
            ExtraCore.setValue(ExtraConstants.SELECT_AUTH_METHOD, true)
            return@ExtraListener false
        }
        val normalizedVersionId = MoJsonExtras.normalizeVersionId(selectedInstance.versionId)
        val mcVersion = MoJsonExtras.getListedVersion(normalizedVersionId)
        MoJsonDownloader().start(
            this.assets,
            mcVersion,
            normalizedVersionId,
            ContextAwareDoneListener(this, normalizedVersionId)
        )
        false
    }

    private val mDoubleLaunchPreventionListener = TaskCountListener { taskCount ->
        // Hide the notification that starts the game if there are tasks executing.
        // Prevents the user from trying to launch the game with tasks ongoing.
        if (taskCount > 0) {
            Tools.runOnUiThread {
                mNotificationManager?.cancel(NotificationUtils.NOTIFICATION_ID_GAME_START)
            }
        }
        false
    }

    override fun setFullscreen(): Boolean {
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LauncherComposeHelper.setContent(
            this,
            { mSettingButtonListener.onClick(null) },
            { mContentInstallerButtonListener.onClick(null) },
            { mInstanceDirectoryButtonListener.onClick(null) },
            object : LauncherComposeHelper.OnFragmentViewCreatedListener {
                override fun onCreated(view: FrameLayout) {
                    val fm = supportFragmentManager
                    val f = fm.findFragmentById(R.id.container_fragment)
                    if (f == null) {
                        fm.beginTransaction()
                            .replace(R.id.container_fragment, MainMenuFragment::class.java, null, MainMenuFragment.TAG)
                            .commitAllowingStateLoss()
                    } else {
                        fm.beginTransaction()
                            .replace(R.id.container_fragment, f)
                            .commitAllowingStateLoss()
                    }
                }
            }
        )

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (supportFragmentManager.backStackEntryCount > 0 && findViewById<View>(R.id.container_fragment) == null) {
                    Log.w("LauncherActivity", "onBackPressed: container not ready, ignoring")
                    return
                }
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        })

        try {
            Os.setenv("TMPDIR", Tools.DIR_CACHE?.absolutePath ?: "", true)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

        IconCacheJanitor.runJanitor()

        window.setBackgroundDrawable(null)
        mRequestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isAllowed ->
            if (!isAllowed) Tools.runOnUiThread {
                Toast.makeText(this, R.string.notification_permission_toast, Toast.LENGTH_LONG).show()
            }
        }
        checkNotificationPermission()
        if (LauncherPreferences.PREF_MIGRATION_NOTICE)
            PojavApplication.sExecutorService.submit { checkPreviousInstalls() }

        mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        ProgressKeeper.addTaskCountListener(mDoubleLaunchPreventionListener)
        mProgressServiceKeeper = ProgressServiceKeeper(this).also {
            ProgressKeeper.addTaskCountListener(it)
        }

        ExtraCore.addExtraListener(ExtraConstants.BACK_PREFERENCE, mBackPreferenceListener)
        ExtraCore.addExtraListener(ExtraConstants.SELECT_AUTH_METHOD, mSelectAuthMethod)
        ExtraCore.addExtraListener(ExtraConstants.LAUNCH_GAME, mLaunchGameListener)

        AsyncVersionList().getVersionList { versions ->
            ExtraCore.setValue(ExtraConstants.RELEASE_TABLE, versions)
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val instanceName = intent.getStringExtra(ShortcutUtils.EXTRA_INSTANCE_NAME)
        if (instanceName != null) {
            PojavApplication.sExecutorService.execute {
                try {
                    val instances = Instances.loadAllInstances()
                    for (instance in instances) {
                        if (instance.mInstanceRoot.name == instanceName) {
                            Instances.setSelectedInstance(instance)
                            Tools.runOnUiThread { ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true) }
                            break
                        }
                    }
                } catch (e: IOException) {
                    Log.e("LauncherActivity", "Failed to load instances for shortcut", e)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ContextExecutor.setActivity(this)
        InstanceInstaller.postInstallCheck(this)
    }

    override fun onPause() {
        super.onPause()
        ContextExecutor.clearActivity()
    }

    override fun onStart() {
        super.onStart()
        supportFragmentManager.registerFragmentLifecycleCallbacks(mFragmentCallbackListener, true)
    }

    override fun onDestroy() {
        super.onDestroy()
        mProgressServiceKeeper?.let { ProgressKeeper.removeTaskCountListener(it) }
        ExtraCore.removeExtraListenerFromValue(ExtraConstants.BACK_PREFERENCE, mBackPreferenceListener)
        ExtraCore.removeExtraListenerFromValue(ExtraConstants.SELECT_AUTH_METHOD, mSelectAuthMethod)
        ExtraCore.removeExtraListenerFromValue(ExtraConstants.LAUNCH_GAME, mLaunchGameListener)

        supportFragmentManager.unregisterFragmentLifecycleCallbacks(mFragmentCallbackListener)
    }

    override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat, pref: Preference): Boolean {
        val fragmentName = pref.fragment ?: return false
        return try {
            @Suppress("UNCHECKED_CAST")
            val fragmentClass = classLoader.loadClass(fragmentName) as Class<out Fragment>
            Tools.swapFragment(this, fragmentClass, null, pref.extras)
            true
        } catch (e: ClassNotFoundException) {
            Log.e("LauncherActivity", "Could not find fragment class: $fragmentName", e)
            false
        }
    }

    fun askForPermission(minApi: Int, permission: String) {
        if (Build.VERSION.SDK_INT < minApi) return
        mRequestPermissionLauncher.launch(permission)
    }

    fun checkForPermission(minApi: Int, permission: String): Boolean {
        return Build.VERSION.SDK_INT < minApi ||
                ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_DENIED
    }

    private fun checkNotificationPermission() {
        if (LauncherPreferences.PREF_SKIP_NOTIFICATION_PERMISSION_CHECK ||
            checkForPermission(33, Manifest.permission.POST_NOTIFICATIONS)
        ) {
            return
        }
        showNotificationPermissionReasoning()
    }

    private fun checkPreviousInstalls() {
        val packages = arrayOf("git.artdeell.mjlaunch", "git.artdeell.mojo")
        for (s in packages) {
            if (s == BuildConfig.APPLICATION_ID) continue

            packageManager.getLaunchIntentForPackage(s) ?: continue
            Tools.runOnUiThread {
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.migration_progress_warning_title)
                    .setMessage(R.string.migration_notice)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        LauncherPreferences.DEFAULT_PREF?.edit { putBoolean("migrationNotice", false) }
                    }
                    .setOnDismissListener { LauncherPreferences.PREF_MIGRATION_NOTICE = false }
                    .show()
            }
            break
        }
    }

    private fun showNotificationPermissionReasoning() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.notification_permission_dialog_title)
                .setMessage(R.string.notification_permission_dialog_text)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    askForPermission(33, Manifest.permission.POST_NOTIFICATIONS)
                }
                .setNegativeButton(android.R.string.cancel) { _, _ -> handleNoNotificationPermission() }
                .show()
        }
    }

    private fun handleNoNotificationPermission() {
        LauncherPreferences.PREF_SKIP_NOTIFICATION_PERMISSION_CHECK = true
        LauncherPreferences.DEFAULT_PREF?.edit {
            putBoolean(LauncherPreferences.PREF_KEY_SKIP_NOTIFICATION_CHECK, true)
        }
    }

    companion object {
        const val SETTING_FRAGMENT_TAG = "SETTINGS_FRAGMENT"
    }
}
