package com.ashmeet.hyperlauncher.components.spinner

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorPosition
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MenuGroupShapes
import androidx.compose.material3.SelectableDropdownMenuItem
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.window.PopupProperties
import com.ashmeet.hyperlauncher.activity.PojavApplication
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.theme.PojavTheme
import com.ashmeet.hyperlauncher.utils.SkinUtils
import com.ashmeet.hyperlauncher.utils.Tools
import com.ashmeet.hyperlauncher.utils.translation.translatedText
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.authenticator.AuthType
import net.kdt.pojavlaunch.authenticator.accounts.Account
import net.kdt.pojavlaunch.authenticator.accounts.Accounts
import net.kdt.pojavlaunch.authenticator.listener.LoginListener
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore
import net.kdt.pojavlaunch.extra.ExtraListener
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper
import java.io.IOException

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
            val dialog = MaterialAlertDialogBuilder(context)
                .setMessage(R.string.warning_remove_account)
                .setPositiveButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.global_delete) { _, _ ->
                    Accounts.delete(account)
                    reloadAccounts(true)
                }
                .create()
            dialog.show()
            dialog.window?.setGravity(android.view.Gravity.CENTER)
        },
        hideDivider = hideDivider,
        containerColor = containerColor,
        modifier = modifier
    )
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
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onExpandedChange(true) },
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
                            modifier = Modifier.size(32.dp)
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


        Box(
            modifier = Modifier
                .padding(start = 16.dp)
                .align(Alignment.BottomStart)
                .width(200.dp)
                .height(1.dp)
        ) {
            DropdownMenuPopup(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                popupPositionProvider = MenuDefaults.rememberDropdownMenuPopupPositionProvider(
                    MenuAnchorPosition.Below
                ),
                properties = PopupProperties(focusable = true, clippingEnabled = false),
                modifier = Modifier.width(300.dp)
            ) {
                Column {
                    val groupInteractionSource = remember { MutableInteractionSource() }
                    if (accounts.isNotEmpty()) {
                        val stableAccountShapes = remember {
                            MenuGroupShapes(RoundedCornerShape(12.dp), RoundedCornerShape(12.dp))
                        }

                        DropdownMenuGroup(
                            shapes = stableAccountShapes,
                            interactionSource = groupInteractionSource,
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp,
                            shadowElevation = 12.dp,
                            contentPadding = PaddingValues(vertical = 4.dp),
                            modifier = Modifier
                                .width(300.dp)
                                .heightIn(max = 400.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            accounts.fastForEachIndexed { index, account ->
                                val isSelected = account == selectedAccount
                                SelectableDropdownMenuItem(
                                    selected = isSelected,
                                    onClick = { onAccountSelected(account) },
                                    interactionSource = remember { MutableInteractionSource() },
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
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null
                                                    ) { onAccountDelete(account) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = translatedText("Delete"),
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    },
                                    shapes = MenuDefaults.itemShape(index, accounts.size),
                                    colors = MenuDefaults.selectableItemColors(
                                        containerColor = Color.Transparent,
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(MenuDefaults.GroupSpacing))
                    }

                    val addAccountShapes = MenuDefaults.groupShape(0, 1)
                    val stableAddShapes = remember(addAccountShapes) {
                        addAccountShapes.copy(inactiveShape = addAccountShapes.shape)
                    }

                    DropdownMenuGroup(
                        shapes = stableAddShapes,
                        interactionSource = groupInteractionSource,
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp,
                        shadowElevation = 8.dp,
                        contentPadding = PaddingValues(vertical = 4.dp),
                        modifier = Modifier.width(300.dp)
                    ) {
                        SelectableDropdownMenuItem(
                            selected = false,
                            onClick = onAddAccountClick,
                            interactionSource = remember { MutableInteractionSource() },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = translatedText(stringResource(R.string.main_add_account)),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            },
                            shapes = MenuDefaults.itemShape(0, 1),
                            colors = MenuDefaults.selectableItemColors(
                                containerColor = Color.Transparent,
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
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
