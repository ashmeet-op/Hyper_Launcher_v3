package com.ashmeet.hyperlauncher.fragments.controls

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.utils.drawable.rememberDrawablePainter
import com.ashmeet.hyperlauncher.utils.translation.translatedText
import com.ashmeet.hyperlauncher.screens.settings.preferences.SingleChoiceDialog
import com.ashmeet.hyperlauncher.theme.PojavTheme
import fr.spse.gamepad_remapper.RemapperManager
import fr.spse.gamepad_remapper.RemapperView
import net.ashmeet.hyperlauncher.R
import com.ashmeet.hyperbackend.customcontrols.gamepad.Gamepad
import com.ashmeet.hyperbackend.customcontrols.gamepad.GamepadMapperAdapter

class GamepadMapperFragment : Fragment(), View.OnKeyListener, View.OnGenericMotionListener {
    companion object {
        const val TAG = "GamepadMapperFragment"
    }

    private val remapperViewBuilder = RemapperView.Builder(null)
        .remapA(true)
        .remapB(true)
        .remapX(true)
        .remapY(true)
        .remapLeftJoystick(true)
        .remapRightJoystick(true)
        .remapStart(true)
        .remapSelect(true)
        .remapLeftShoulder(true)
        .remapRightShoulder(true)
        .remapLeftTrigger(true)
        .remapRightTrigger(true)
        .remapDpad(true)

    private val exitHandler = Handler(Looper.getMainLooper())
    private val exitRunnable = Runnable {
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    private var inputManager: RemapperManager? = null
    private var mapperAdapter: GamepadMapperAdapter? = null
    private var gamepad: Gamepad? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                PojavTheme {
                    GamepadMapperContent(
                        onAdapterCreated = { mapperAdapter = it },
                        onInputManagerCreated = { inputManager = it },
                        onKeyListenerSet = { this@GamepadMapperFragment.view?.setOnKeyListener(this@GamepadMapperFragment) },
                        onGenericMotionListenerSet = {
                            this@GamepadMapperFragment.view?.setOnGenericMotionListener(
                                this@GamepadMapperFragment
                            )
                        }
                    )
                }
            }
        }
    }

    @Composable
    fun GamepadMapperContent(
        onAdapterCreated: (GamepadMapperAdapter) -> Unit,
        onInputManagerCreated: (RemapperManager) -> Unit,
        onKeyListenerSet: () -> Unit,
        onGenericMotionListenerSet: () -> Unit
    ) {
        var grabState by remember { mutableIntStateOf(0) }
        val grabOptions = listOf(
            translatedText(stringResource(R.string.customctrl_visibility_in_menus)),
            translatedText(stringResource(R.string.customctrl_visibility_ingame))
        )

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = if (LauncherPreferences.PREF_LAUNCHER_BACKGROUND_PATH != null) Color.Transparent else MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = translatedText(stringResource(R.string.controller_remapper_exit_part1)),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Image(
                            painter = rememberDrawablePainter(
                                ContextCompat.getDrawable(
                                    LocalContext.current,
                                    R.drawable.button_select
                                )
                            ),
                            contentDescription = translatedText(stringResource(R.string.controller_button_select)),
                            modifier = Modifier.size(30.dp)
                        )
                        Text(
                            text = translatedText(stringResource(R.string.controller_remapper_exit_part2)),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = translatedText(stringResource(R.string.controller_remapper_operating_mode)),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp
                        )
                        var showGrabDialog by remember { mutableStateOf(false) }
                        Text(
                            text = grabOptions[grabState],
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { showGrabDialog = true }
                                .padding(8.dp)
                        )

                        if (showGrabDialog) {
                            SingleChoiceDialog(
                                title = translatedText(stringResource(R.string.controller_remapper_operating_mode)),
                                options = grabOptions,
                                optionValues = listOf("0", "1"),
                                selectedValue = grabState.toString(),
                                onValueChange = {
                                    val newState = it.toInt()
                                    grabState = newState
                                    mapperAdapter?.setGrabState(newState == 1)
                                },
                                onDismiss = { showGrabDialog = false }
                            )
                        }
                    }
                }

                AndroidView(
                    factory = { ctx ->
                        RecyclerView(ctx).apply {
                            layoutManager = LinearLayoutManager(ctx)
                            val adapter = GamepadMapperAdapter(ctx)
                            this.adapter = adapter
                            onAdapterCreated(adapter)
                            onInputManagerCreated(RemapperManager(ctx, remapperViewBuilder))

                            isFocusable = true
                            isFocusableInTouchMode = true
                            requestFocus()
                        }
                    },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    update = {
                        onKeyListenerSet()
                        onGenericMotionListenerSet()
                    }
                )
            }
        }
    }

    private fun createGamepad(inputDevice: InputDevice) {
        gamepad = object : Gamepad(inputDevice, mapperAdapter, null) {
            override fun handleGamepadInput(keycode: Int, value: Float) {
                if (keycode == KeyEvent.KEYCODE_BUTTON_SELECT) {
                    handleExitButton(value > 0.5)
                }
                super.handleGamepadInput(keycode, value)
            }
        }
    }

    private fun handleExitButton(isPressed: Boolean) {
        if (isPressed) exitHandler.postDelayed(exitRunnable, 400)
        else exitHandler.removeCallbacks(exitRunnable)
    }

    override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
        if (!Gamepad.isGamepadEvent(event)) return false
        if (gamepad == null) createGamepad(event.device)
        inputManager?.handleKeyEventInput(requireContext(), event, gamepad)
        return true
    }

    override fun onGenericMotion(v: View, event: MotionEvent): Boolean {
        if (!Gamepad.isGamepadEvent(event)) return false
        if (gamepad == null) createGamepad(event.device)
        inputManager?.handleMotionEventInput(requireContext(), event, gamepad)
        return true
    }
}