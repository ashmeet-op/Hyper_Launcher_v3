package com.ashmeet.hyperlauncher.components.spinner

import com.ashmeet.hyperlauncher.utils.translation.translatedText
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.PojavApplication
import com.ashmeet.hyperlauncher.utils.Tools
import net.kdt.pojavlaunch.authenticator.AuthType
import net.kdt.pojavlaunch.authenticator.accounts.Account
import net.kdt.pojavlaunch.authenticator.accounts.Accounts
import net.kdt.pojavlaunch.authenticator.listener.LoginListener
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore
import net.kdt.pojavlaunch.extra.ExtraListener
import java.io.IOException
import androidx.compose.ui.tooling.preview.Preview
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper
import com.ashmeet.hyperlauncher.utils.SkinUtils
import com.ashmeet.hyperlauncher.theme.PojavTheme

@Composable
fun AccountSpinnerCompose(
    modifier: Modifier = Modifier,
    hideDivider: Boolean = false,
    containerColor: Color = MaterialTheme.colorScheme.surface
) {
    val context = LocalContext.current
    var accounts by remember { mutableStateOf<List<Account>>(emptyList()) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var expanded by remember { mutableStateOf(false) }

    val loginListener = remember {
        object : LoginListener {
            override fun onLoginDone(account: Account?) {
                if (account != null) {
                    Accounts.setCurrent(account)
                    ExtraCore.setValue(ExtraConstants.REFRESH_ACCOUNT_SPINNER, true)
                }
            }

            override fun onLoginError(errorMessage: Throwable?) {
            }

            override fun onLoginProgress(step: Int) {
            }

            override fun setMaxLoginProgress(max: Int) {
            }
        }
    }

    val refreshAccount: (Account) -> Unit = { account ->
        if (ProgressKeeper.getTaskCount() == 0) {
            PojavApplication.sExecutorService.execute {
                val refreshAccount = account.reload() ?: return@execute
                val authType = refreshAccount.authType
                if (authType.requiresLogin() && System.currentTimeMillis() > refreshAccount.expiresAt) {
                    authType.createAuth().refreshAccount(loginListener, refreshAccount)
                }
            }
        } else {
            ProgressKeeper.waitUntilDone {
                PojavApplication.sExecutorService.execute {
                    val refreshAccount = account.reload() ?: return@execute
                    val authType = refreshAccount.authType
                    if (authType.requiresLogin() && System.currentTimeMillis() > refreshAccount.expiresAt) {
                        authType.createAuth().refreshAccount(loginListener, refreshAccount)
                    }
                }
            }
        }
    }

    val reloadAccounts: (Boolean) -> Unit = { notifyOthers ->
        PojavApplication.sExecutorService.execute {
            try {
                val loadedAccounts = Accounts.load()
                Tools.runOnUiThread {
                    accounts = loadedAccounts.accounts
                    selectedIndex = loadedAccounts.selectionIndex

                    if (selectedIndex >= 0 && selectedIndex < accounts.size) {
                        refreshAccount(accounts[selectedIndex])
                    }
                    if (notifyOthers) {
                        ExtraCore.setValue(ExtraConstants.REFRESH_ACCOUNT_SPINNER, true)
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(Unit) {
        reloadAccounts(true)
    }

    DisposableEffect(Unit) {
        val refreshListener = ExtraListener<Any> { _, _ ->
            reloadAccounts(false)
            false
        }

        val microsoftLoginListener = ExtraListener<String> { _, value ->
            val backgroundLogin = AuthType.MICROSOFT.createAuth()
            backgroundLogin.createAccount(loginListener, value)
            false
        }

        val elyByLoginListener = ExtraListener<String> { _, value ->
            val backgroundLogin = AuthType.ELY_BY.createAuth()
            backgroundLogin.createAccount(loginListener, value)
            false
        }

        val mojangLoginListener = ExtraListener<Array<String>> { _, value ->
            try {
                val account = Accounts.create { acc: Account -> acc.username = value[0] }
                Accounts.setCurrent(account)
                loginListener.onLoginDone(account)
            } catch (e: IOException) {
                loginListener.onLoginError(e)
            }
            false
        }

        ExtraCore.addExtraListener(ExtraConstants.REFRESH_ACCOUNT_SPINNER, refreshListener)
        ExtraCore.addExtraListener(ExtraConstants.MICROSOFT_LOGIN_TODO, microsoftLoginListener)
        ExtraCore.addExtraListener(ExtraConstants.ELYBY_LOGIN_TODO, elyByLoginListener)
        ExtraCore.addExtraListener(ExtraConstants.MOJANG_LOGIN_TODO, mojangLoginListener)

        onDispose {
            ExtraCore.removeExtraListenerFromValue(ExtraConstants.REFRESH_ACCOUNT_SPINNER, refreshListener)
            ExtraCore.removeExtraListenerFromValue(ExtraConstants.MICROSOFT_LOGIN_TODO, microsoftLoginListener)
            ExtraCore.removeExtraListenerFromValue(ExtraConstants.ELYBY_LOGIN_TODO, elyByLoginListener)
            ExtraCore.removeExtraListenerFromValue(ExtraConstants.MOJANG_LOGIN_TODO, mojangLoginListener)
        }
    }

    val selectedAccount = if (selectedIndex >= 0 && selectedIndex < accounts.size) accounts[selectedIndex] else null

    AccountSpinnerUI(
        selectedAccount = selectedAccount,
        accounts = accounts,
        expanded = expanded,
        onExpandedChange = { expanded = it },
        onAddAccountClick = {
            expanded = false
            ExtraCore.setValue(ExtraConstants.SELECT_AUTH_METHOD, true)
        },
        onAccountSelected = { account ->
            expanded = false
            Accounts.setCurrent(account)
            reloadAccounts(true)
        },
        onAccountDelete = { account ->
            expanded = false
            MaterialAlertDialogBuilder(context)
                .setMessage(R.string.warning_remove_account)
                .setPositiveButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.global_delete) { _, _ ->
                    Accounts.delete(account)
                    reloadAccounts(true)
                }
                .show()
        },
        hideDivider = hideDivider,
        containerColor = containerColor,
        modifier = modifier
    )
}

@Composable
fun <T> HyperSpinner(
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    labelProvider: (T) -> String = { it.toString() },
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(enabled = enabled) { expanded = true },
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            shape = RoundedCornerShape(8.dp),
            border = if (enabled) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = labelProvider(selectedOption),
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    style = MaterialTheme.typography.bodyLarge
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .clip(RoundedCornerShape(12.dp))
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = labelProvider(option),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun AccountSpinnerUI(
    selectedAccount: Account?,
    accounts: List<Account>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onAddAccountClick: () -> Unit,
    onAccountSelected: (Account) -> Unit,
    onAccountDelete: (Account) -> Unit,
    modifier: Modifier = Modifier,
    hideDivider: Boolean = false,
    containerColor: Color = MaterialTheme.colorScheme.surface
) {
    val isMatte = LauncherPreferences.PREF_BLURRED_ELEMENTS_ENABLED

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onExpandedChange(true) },
            color = if (isMatte) containerColor.copy(alpha = 0.4f) else containerColor,
            shape = RoundedCornerShape(0.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (isMatte) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .blur(16.dp)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedAccount != null) {
                        AccountItemContent(
                            account = selectedAccount,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = translatedText(stringResource(R.string.main_add_account)),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp
                        )
                    }
                }

                if (!hideDivider) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier
                .width(300.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                )
                .clip(RoundedCornerShape(12.dp))
        ) {
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = translatedText(stringResource(R.string.main_add_account)),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                onClick = onAddAccountClick
            )

            accounts.forEach { account ->
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            AccountItemContent(
                                account = account,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { onAccountDelete(account) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = translatedText("Delete"),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    },
                    onClick = { onAccountSelected(account) }
                )
            }
        }
    }
}

@Composable
fun AccountItemContent(
    account: Account,
    modifier: Modifier = Modifier
) {
    val skinHead by SkinUtils.rememberSkinHead(account)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(32.dp)) {
            if (skinHead != null) {
                Image(
                    bitmap = skinHead!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.FillBounds
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Color.Gray, RoundedCornerShape(4.dp)))
            }

            if (account.authType != AuthType.LOCAL && account.authType.iconResource != 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                        .padding(2.dp)
                ) {
                    Icon(
                        painter = painterResource(id = account.authType.iconResource),
                        contentDescription = null,
                        modifier = Modifier.size(10.dp),
                        tint = Color.Unspecified
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = account.username,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview
@Composable
fun AccountSpinnerPreview() {
    PojavTheme {
        val account1 = remember {
            Account::class.java.getDeclaredConstructor().apply { isAccessible = true }.newInstance().apply {
                username = "Steve"
                authType = AuthType.LOCAL
            }
        }
        val account2 = remember {
            Account::class.java.getDeclaredConstructor().apply { isAccessible = true }.newInstance().apply {
                username = "MicrosoftUser"
                authType = AuthType.MICROSOFT
            }
        }

        var expanded by remember { mutableStateOf(false) }

        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text("Selected:", color = MaterialTheme.colorScheme.onSurface)
            Box(modifier = Modifier.height(64.dp).fillMaxWidth()) {
                AccountSpinnerUI(
                    selectedAccount = account1,
                    accounts = listOf(account1, account2),
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    onAddAccountClick = {},
                    onAccountSelected = {},
                    onAccountDelete = {}
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Authenticating:", color = MaterialTheme.colorScheme.onSurface)
            Box(modifier = Modifier.height(64.dp).fillMaxWidth()) {
                AccountSpinnerUI(
                    selectedAccount = account2,
                    accounts = listOf(account1, account2),
                    expanded = false,
                    onExpandedChange = {},
                    onAddAccountClick = {},
                    onAccountSelected = {},
                    onAccountDelete = {}
                )
            }
        }
    }
}
