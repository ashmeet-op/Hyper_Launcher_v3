package net.kdt.pojavlaunch.customcontrols.gamepad

import android.content.Context
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.RecyclerView
import com.ashmeet.hyperlauncher.components.gamepad.ControllerMappingItem
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.game.platform.input.PlatformGrabListener
import net.kdt.pojavlaunch.utils.KeycodeUtils

class GamepadMapperAdapter(context: Context) : RecyclerView.Adapter<GamepadMapperAdapter.ViewHolder>(), GamepadDataProvider {
    private val BUTTON_COUNT = 20
    private lateinit var mSimulatedGamepadMap: GamepadMap
    private lateinit var mRebinderButtons: Array<RebinderButton>
    private lateinit var mRealButtons: Array<GamepadEmulatedButton>
    private val mKeyOptions: List<String>
    private val mSpecialKeycodeCount: Int
    private var mGamepadGrabListener: PlatformGrabListener? = null
    private var mGrabState = false
    private var mOldState = false

    init {
        GamepadMapStore.load()
        val specialKeycodeNames = GamepadMap.getSpecialKeycodeNames()
        mSpecialKeycodeCount = specialKeycodeNames.size
        val allKeys = mutableListOf<String>()
        allKeys.addAll(specialKeycodeNames)
        allKeys.addAll(KeycodeUtils.generateKeyName())
        mKeyOptions = allKeys
        createRebinderMap()
        updateRealButtons()
    }

    private fun createRebinderMap() {
        mSimulatedGamepadMap = GamepadMap()
        val list = mutableListOf<RebinderButton>()

        mSimulatedGamepadMap.BUTTON_A = RebinderButton(R.drawable.button_a, R.string.controller_button_a).also { list.add(it) }
        mSimulatedGamepadMap.BUTTON_B = RebinderButton(R.drawable.button_b, R.string.controller_button_b).also { list.add(it) }
        mSimulatedGamepadMap.BUTTON_X = RebinderButton(R.drawable.button_x, R.string.controller_button_x).also { list.add(it) }
        mSimulatedGamepadMap.BUTTON_Y = RebinderButton(R.drawable.button_y, R.string.controller_button_y).also { list.add(it) }
        mSimulatedGamepadMap.BUTTON_START = RebinderButton(R.drawable.button_start, R.string.controller_button_start).also { list.add(it) }
        mSimulatedGamepadMap.BUTTON_SELECT = RebinderButton(R.drawable.button_select, R.string.controller_button_select).also { list.add(it) }
        mSimulatedGamepadMap.TRIGGER_RIGHT = RebinderButton(R.drawable.trigger_right, R.string.controller_button_trigger_right).also { list.add(it) }
        mSimulatedGamepadMap.TRIGGER_LEFT = RebinderButton(R.drawable.trigger_left, R.string.controller_button_trigger_left).also { list.add(it) }
        mSimulatedGamepadMap.SHOULDER_RIGHT = RebinderButton(R.drawable.shoulder_right, R.string.controller_button_shoulder_right).also { list.add(it) }
        mSimulatedGamepadMap.SHOULDER_LEFT = RebinderButton(R.drawable.shoulder_left, R.string.controller_button_shoulder_left).also { list.add(it) }
        mSimulatedGamepadMap.DIRECTION_FORWARD = RebinderButton(R.drawable.stick_right, R.string.controller_direction_forward).also { list.add(it) }
        mSimulatedGamepadMap.DIRECTION_RIGHT = RebinderButton(R.drawable.stick_right, R.string.controller_direction_right).also { list.add(it) }
        mSimulatedGamepadMap.DIRECTION_LEFT = RebinderButton(R.drawable.stick_right, R.string.controller_direction_left).also { list.add(it) }
        mSimulatedGamepadMap.DIRECTION_BACKWARD = RebinderButton(R.drawable.stick_right, R.string.controller_direction_backward).also { list.add(it) }
        mSimulatedGamepadMap.THUMBSTICK_RIGHT = RebinderButton(R.drawable.stick_right_click, R.string.controller_stick_press_r).also { list.add(it) }
        mSimulatedGamepadMap.THUMBSTICK_LEFT = RebinderButton(R.drawable.stick_left_click, R.string.controller_stick_press_l).also { list.add(it) }
        mSimulatedGamepadMap.DPAD_UP = RebinderButton(R.drawable.dpad_up, R.string.controller_dpad_up).also { list.add(it) }
        mSimulatedGamepadMap.DPAD_DOWN = RebinderButton(R.drawable.dpad_down, R.string.controller_dpad_down).also { list.add(it) }
        mSimulatedGamepadMap.DPAD_RIGHT = RebinderButton(R.drawable.dpad_right, R.string.controller_dpad_right).also { list.add(it) }
        mSimulatedGamepadMap.DPAD_LEFT = RebinderButton(R.drawable.dpad_left, R.string.controller_dpad_left).also { list.add(it) }

        mRebinderButtons = list.toTypedArray()
    }

    private fun updateRealButtons() {
        val currentRealMap = if (mGrabState) GamepadMapStore.getGameMap() else GamepadMapStore.getMenuMap()
        mRealButtons = arrayOf(
            currentRealMap.BUTTON_A, currentRealMap.BUTTON_B, currentRealMap.BUTTON_X, currentRealMap.BUTTON_Y,
            currentRealMap.BUTTON_START, currentRealMap.BUTTON_SELECT, currentRealMap.TRIGGER_RIGHT, currentRealMap.TRIGGER_LEFT,
            currentRealMap.SHOULDER_RIGHT, currentRealMap.SHOULDER_LEFT, currentRealMap.DIRECTION_FORWARD, currentRealMap.DIRECTION_RIGHT,
            currentRealMap.DIRECTION_LEFT, currentRealMap.DIRECTION_BACKWARD, currentRealMap.THUMBSTICK_RIGHT, currentRealMap.THUMBSTICK_LEFT,
            currentRealMap.DPAD_UP, currentRealMap.DPAD_DOWN, currentRealMap.DPAD_RIGHT, currentRealMap.DPAD_LEFT
        )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ComposeView(parent.context))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = mRebinderButtons.size

    private fun updateStickIcons() {
        val stickIcon = if (mGrabState) R.drawable.stick_left else R.drawable.stick_right
        (mSimulatedGamepadMap.DIRECTION_FORWARD as RebinderButton).iconResourceId = stickIcon
        (mSimulatedGamepadMap.DIRECTION_BACKWARD as RebinderButton).iconResourceId = stickIcon
        (mSimulatedGamepadMap.DIRECTION_RIGHT as RebinderButton).iconResourceId = stickIcon
        (mSimulatedGamepadMap.DIRECTION_LEFT as RebinderButton).iconResourceId = stickIcon
    }

    private class RebinderButton(var iconResourceId: Int, val localeResourceId: Int) : GamepadButton()

    inner class ViewHolder(val composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {
        fun bind(index: Int) {
            val rebinderButton = mRebinderButtons[index]
            val realButton = mRealButtons[index]
            val context = composeView.context

            val keyCodeNames = realButton.keycodes.map { keyCode ->
                val selected = if (keyCode < 0) keyCode + mSpecialKeycodeCount else KeycodeUtils.getIndexByValue(keyCode.toInt()) + mSpecialKeycodeCount
                mKeyOptions.getOrNull(selected) ?: "UNKNOWN"
            }

            composeView.setContent {
                ControllerMappingItem(
                    iconRes = rebinderButton.iconResourceId,
                    keyCodeLabel = context.getString(rebinderButton.localeResourceId),
                    currentKeys = keyCodeNames,
                    isToggle = if (realButton is GamepadButton) realButton.isToggleable else false,
                    onKeySelected = { keyIndex, newKeyName ->
                        val selectionIndex = mKeyOptions.indexOf(newKeyName)
                        val offset = selectionIndex - mSpecialKeycodeCount
                        val newValue = if (selectionIndex <= mSpecialKeycodeCount) offset else KeycodeUtils.getValueByIndex(offset)
                        realButton.keycodes[keyIndex] = newValue
                        GamepadMapStore.save()
                        notifyItemChanged(index)
                    },
                    onToggleChanged = { checked ->
                        if (realButton is GamepadButton) {
                            realButton.isToggleable = checked
                            GamepadMapStore.save()
                        }
                    },
                    keyOptions = mKeyOptions
                )
            }
        }
    }

    override fun getMenuMap(): GamepadMap = mSimulatedGamepadMap
    override fun getGameMap(): GamepadMap = mSimulatedGamepadMap
    override fun isGrabbing(): Boolean = mGrabState
    override fun attachGrabListener(grabListener: PlatformGrabListener) {
        mGamepadGrabListener = grabListener
        grabListener.onGrabState(mGrabState)
    }

    fun setGrabState(newState: Boolean) {
        mGrabState = newState
        mGamepadGrabListener?.onGrabState(newState)
        if (mGrabState == mOldState) return
        updateRealButtons()
        updateStickIcons()
        notifyItemRangeChanged(0, mRebinderButtons.size)
        mOldState = mGrabState
    }
}
