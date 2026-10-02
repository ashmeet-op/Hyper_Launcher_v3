package net.kdt.pojavlaunch.customcontrols;

import static android.content.Context.INPUT_METHOD_SERVICE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Insets;
import android.graphics.Point;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;

import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences;
import com.google.gson.JsonSyntaxException;

import net.ashmeet.hyperlauncher.R;
import net.kdt.pojavlaunch.game.GameView;


import com.ashmeet.hyperlauncher.utils.Tools;
import net.kdt.pojavlaunch.customcontrols.buttons.ControlButton;
import net.kdt.pojavlaunch.customcontrols.buttons.ControlDrawer;
import net.kdt.pojavlaunch.customcontrols.buttons.ControlInterface;
import net.kdt.pojavlaunch.customcontrols.buttons.ControlJoystick;
import net.kdt.pojavlaunch.customcontrols.buttons.ControlSubButton;
import net.kdt.pojavlaunch.customcontrols.handleview.ActionRow;
import net.kdt.pojavlaunch.customcontrols.handleview.ControlHandleView;
import com.ashmeet.hyperlauncher.utils.SideDialogUtils;
import com.ashmeet.hyperlauncher.fragments.dialog.EditControlSideDialog;
import net.kdt.pojavlaunch.game.platform.Platform;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ControlLayout extends FrameLayout {
	protected CustomControls mLayout;

	private GameView mGameSurface = null;


	private List<ControlInterface> mButtons;
	private boolean mModifiable = false;
	private boolean mIsModified;
	private boolean mControlVisible = false;

	private float mButtonsOpacity = 1.0f;

	private EditControlSideDialog mControlDialog = null;
	private ControlHandleView mHandleView;
	private ControlButtonMenuListener mMenuListener;
	public ActionRow mActionRow = null;
	public String mLayoutFileName;

	public interface OnControlEditListener {
		void onEditControl(ControlInterface button);
		boolean onDisappearLayer();
	}

	private OnControlEditListener mEditListener;

	public void setOnControlEditListener(OnControlEditListener listener) {
		mEditListener = listener;
		if (mActionRow != null) mActionRow.setVisibility(GONE);
	}

	public ControlLayout(Context ctx) {
		super(ctx);
	}

	public ControlLayout(Context ctx, AttributeSet attrs) {
		super(ctx, attrs);
	}


	public void loadLayout(String jsonPath) throws IOException, JsonSyntaxException {
		Point size = new Point(getWidth(), getHeight());
		try {
			CustomControls layout = LayoutConverter.loadAndConvertIfNecessary(size, jsonPath);
			loadLayout(layout);
			updateLoadedFileName(jsonPath);
		}catch (IOException | JsonSyntaxException e) {

			CustomControls customControls = new CustomControls();
			customControls.mLayoutBitmaps = LayoutBitmaps.createEmpty();
			loadLayout(customControls);
			throw e;
		}
	}

	public void loadLayout(CustomControls controlLayout) {
		this.mButtonsOpacity = LauncherPreferences.PREF_BUTTON_TRANSPARENCY / 100;
		boolean sanitizedModified = false;
		if(controlLayout != null) {
			sanitizedModified = LayoutSanitizer.sanitizeLayout(controlLayout);
		}
		if(mActionRow == null){
			mActionRow = new ActionRow(getContext());
			addView(mActionRow);
		}

		removeAllButtons();
		if(mLayout != null) {
			mLayout.mControlDataList = null;
			mLayout = null;
		}

		System.gc();
		mapTable.clear();


		if (controlLayout == null) return;

		mLayout = controlLayout;



		for(ControlJoystickData joystick : mLayout.mJoystickDataList){
			addJoystickView(joystick);
		}


		for (ControlData button : controlLayout.mControlDataList) {
			addControlView(button);
		}


		for(ControlDrawerData drawerData : controlLayout.mDrawerDataList){
			ControlDrawer drawer = addDrawerView(drawerData);
			if(mModifiable) drawer.areButtonsVisible = true;
		}

		mLayout.scaledAt = LauncherPreferences.PREF_BUTTONSIZE;

		setModified(sanitizedModified);
		mButtons = null;
		getButtonChildren();
	}


	public void addControlButton(ControlData controlButton) {
		mLayout.mControlDataList.add(controlButton);
		addControlView(controlButton);
	}

	private void addControlView(ControlData controlButton) {
		final ControlButton view = new ControlButton(this, controlButton);

		if (!mModifiable) {
			view.setAlpha(view.getProperties().opacity * mButtonsOpacity);
			view.setFocusable(false);
			view.setFocusableInTouchMode(false);
		}
		addView(view);

		setModified(true);
	}


	public void addDrawer(ControlDrawerData drawerData){
		mLayout.mDrawerDataList.add(drawerData);
		addDrawerView();
	}

	private void addDrawerView(){
		addDrawerView(null);
	}

	private ControlDrawer addDrawerView(ControlDrawerData drawerData){

		final ControlDrawer view = new ControlDrawer(this,drawerData == null ? mLayout.mDrawerDataList.get(mLayout.mDrawerDataList.size()-1) : drawerData);

		if (!mModifiable) {
			view.setAlpha(view.getProperties().opacity * mButtonsOpacity);
			view.setFocusable(false);
			view.setFocusableInTouchMode(false);
		}
		addView(view);

		for (ControlData subButton : view.getDrawerData().buttonProperties) {
			addSubView(view, subButton);
		}

		setModified(true);
		return view;
	}


	public void addSubButton(ControlDrawer drawer, ControlData controlButton){

		drawer.getDrawerData().buttonProperties.add(controlButton);
		addSubView(drawer, drawer.getDrawerData().buttonProperties.get(drawer.getDrawerData().buttonProperties.size()-1 ));
	}

	private void addSubView(ControlDrawer drawer, ControlData controlButton){
		final ControlSubButton view = new ControlSubButton(this, controlButton, drawer);

		if (!mModifiable) {
			view.setAlpha(view.getProperties().opacity * mButtonsOpacity);
			view.setFocusable(false);
			view.setFocusableInTouchMode(false);
		}else{
			view.setVisible(true);
		}

		addView(view);
		drawer.addButton(view);


		setModified(true);
	}


	public void addJoystickButton(ControlJoystickData data){
		mLayout.mJoystickDataList.add(data);
		addJoystickView(data);
	}

	private void addJoystickView(ControlJoystickData data){
		ControlJoystick view = new ControlJoystick(this, data);

		if (!mModifiable) {
			view.setAlpha(view.getProperties().opacity * mButtonsOpacity);
			view.setFocusable(false);
			view.setFocusableInTouchMode(false);
		}
		addView(view);

	}


	private void removeAllButtons() {
		for(ControlInterface button : getButtonChildren()){
			removeView(button.getControlView());
		}

		System.gc();


	}

	public void saveLayout(String path) throws Exception {
		mLayout.save(path);
		setModified(false);
	}

	public void toggleControlVisible(){
		mControlVisible = !mControlVisible;
		setControlVisible(mControlVisible);
	}

	public float getLayoutScale(){
		return mLayout.scaledAt;
	}

	public CustomControls getLayout(){
		return mLayout;
	}

	public void setControlVisible(boolean isVisible) {
		if (mModifiable) return;

		mControlVisible = isVisible;
		for(ControlInterface button : getButtonChildren()){

			button.setVisible(((button.getProperties().displayInGame && Platform.isGrabbing()) || (button.getProperties().displayInMenu && !Platform.isGrabbing())) && isVisible);
		}
	}

	public void setModifiable(boolean isModifiable) {
		if(!isModifiable && mModifiable){
			removeEditWindow();
		}
		mModifiable = isModifiable;
		updateButtonOpacity();
	}

	public boolean getModifiable(){
		return mModifiable;
	}

	public void setModified(boolean isModified) {
		mIsModified = isModified;
	}

	public List<ControlInterface> getButtonChildren(){
		if(mModifiable || mButtons == null){
			mButtons = new ArrayList<>();
			for(int i=0; i<getChildCount(); ++i){
				View v = getChildAt(i);
				if(v instanceof ControlInterface)
					mButtons.add(((ControlInterface) v));
			}
		}

		return mButtons;
	}

	public void refreshControlButtonPositions(){
		requestLayout();
	}

	@Override
	public void onViewRemoved(View child) {
		super.onViewRemoved(child);
		if(child instanceof ControlInterface && mControlDialog != null){
			mControlDialog.disappear(false);
		}
	}


	public void editControlButton(ControlInterface button){
		if (mEditListener != null) {
			mEditListener.onEditControl(button);
			if (mActionRow != null) mActionRow.setVisibility(GONE);
			if(mHandleView == null){
				mHandleView = new ControlHandleView(getContext());
				addView(mHandleView);
			}
			mHandleView.setControlButton(button);
			return;
		}

		if(mControlDialog == null){
			mControlDialog = new EditControlSideDialog();
		}

		mControlDialog.setCurrentlyEditedButton(button);

		SideDialogUtils.show(mControlDialog, button.getControlView().getX() + button.getControlView().getWidth()/2f < getWidth()/2f);
		button.loadEditValues(mControlDialog);

		if(mHandleView == null){
			mHandleView = new ControlHandleView(getContext());
			addView(mHandleView);
		}
		mHandleView.setControlButton(button);
	}


	public void adaptPanelPosition(){
		if(mControlDialog != null) mControlDialog.adaptPanelPosition();
	}


	final HashMap<View, ControlInterface> mapTable = new HashMap<>();

	private static boolean eventInViewBounds(MotionEvent event, View view) {
		float x = event.getX();
		float y = event.getY();
		return x > view.getLeft() && x < view.getRight() && y > view.getTop() && y < view.getBottom();
	}


	public void onTouch(View v, MotionEvent ev) {
		int action = ev.getActionMasked();
		ControlInterface lastControlButton = mapTable.get(v);


		ev.offsetLocation(v.getX(), v.getY());



		if (action == MotionEvent.ACTION_UP
				|| action == MotionEvent.ACTION_CANCEL
				|| action == MotionEvent.ACTION_POINTER_UP) {
			if (lastControlButton != null) lastControlButton.handleReleased();
			mapTable.put(v, null);
			return;
		}

		if (action != MotionEvent.ACTION_MOVE && action != MotionEvent.ACTION_DOWN) return;


		if (lastControlButton != null) {
			if (eventInViewBounds(ev, lastControlButton.getControlView())) {
				return;
			}
		}


		if (lastControlButton != null) lastControlButton.handleReleased();
		mapTable.remove(v);


		for (ControlInterface button : getButtonChildren()) {
			if (!button.getProperties().isSwipeable) continue;
			if (eventInViewBounds(ev, button.getControlView())) {

				if (!button.equals(lastControlButton)) {
					button.handlePressed();
					mapTable.put(v, button);
					return;
				}

			}
		}
	}

	@RequiresApi(30)
	private boolean isKeyboardShown() {
		WindowInsets windowInsets = getRootWindowInsets();
		Insets imeInsets = windowInsets.getInsets(WindowInsets.Type.ime());
		return imeInsets.bottom != 0 || imeInsets.left != 0 || imeInsets.top != 0 || imeInsets.right != 0;
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (mModifiable && event.getActionMasked() != MotionEvent.ACTION_UP || mControlDialog == null)
			return true;
		InputMethodManager imm = (InputMethodManager) getContext().getSystemService(INPUT_METHOD_SERVICE);

		boolean isKeyboardHidden;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			boolean keyboardShown = isKeyboardShown();
			isKeyboardHidden = !keyboardShown;
			if(keyboardShown) imm.hideSoftInputFromWindow(getWindowToken(), 0);
		}else {



			isKeyboardHidden = !imm.hideSoftInputFromWindow(getWindowToken(), 0);
		}
		if(isKeyboardHidden){
			if(mControlDialog.disappearLayer()){
				mActionRow.setFollowedButton(null);
				mHandleView.hide();
			}
		}
		return true;
	}

	public void removeEditWindow() {
		if (mEditListener != null && mEditListener.onDisappearLayer()) {
			if(mHandleView != null) mHandleView.hide();
			return;
		}

		InputMethodManager imm = (InputMethodManager) getContext().getSystemService(INPUT_METHOD_SERVICE);


		imm.hideSoftInputFromWindow(getWindowToken(), 0);
		if(mControlDialog != null) {
			mControlDialog.disappear(true);
		}

		if(mActionRow != null) mActionRow.setFollowedButton(null);
		if(mHandleView != null) mHandleView.hide();
	}

	public void save(String path){
		try {
			mLayout.save(path);
		} catch (IOException e) {Log.e("ControlLayout", "Failed to save the layout at:" + path);}
	}


	public boolean hasMenuButton() {
		for(ControlInterface controlInterface : getButtonChildren()){
			for (int keycode : controlInterface.getProperties().keycodes) {
				if (keycode == ControlData.SPECIALBTN_MENU) return true;
			}
		}
		return false;
	}

	public void setMenuListener(ControlButtonMenuListener menuListener) {
		this.mMenuListener = menuListener;
	}

	public void notifyAppMenu() {
		if(mMenuListener != null) mMenuListener.onClickedMenu();
	}


	public GameView getGameSurface(){
		if(mGameSurface == null){
			mGameSurface = findViewById(R.id.main_game_render_view);
		}
		return mGameSurface;
	}

	public void askToExit(EditorExitable editorExitable) {
		if(mIsModified) {
			openSaveDialog(editorExitable);
		}else{
			openExitDialog(editorExitable);
		}
	}

	public void updateLoadedFileName(String path) {
        if (Tools.CTRLMAP_PATH != null) {
            path = path.replace(Tools.CTRLMAP_PATH, ".");
        }
        path = path.substring(0, path.length() - 5);
		mLayoutFileName = path;
	}

	public String saveToDirectory(String name) throws Exception{
		String jsonPath = Tools.CTRLMAP_PATH + "/" + name + ".json";
		saveLayout(jsonPath);
		return jsonPath;
	}

	public void openSaveDialog(EditorExitable editorExitable) {
		Tools.openSaveDialog(this, editorExitable);
	}

	public void openLoadDialog() {
		Tools.openLoadDialog(this);
	}

	public void openSetDefaultDialog() {
		Tools.openSetDefaultDialog(this);
	}

	public void openExitDialog(EditorExitable exitListener) {
		Tools.openExitDialog(getContext(), exitListener);
	}



	@SuppressWarnings("RtlHardcoded")
	private void layoutNonButtonChildren(int left, int top, int right, int bottom) {
		final int count = getChildCount();
		final int parentLeft = getPaddingLeft();
		final int parentRight = right - left - getPaddingRight();
		final int parentTop = getPaddingTop();
		final int parentBottom = bottom - top - getPaddingBottom();
		final int layoutDirection = getLayoutDirection();
		for (int i = 0; i < count; i++) {
			final View child = getChildAt(i);
			if(child instanceof ControlInterface || child.getVisibility() == GONE) continue;
			final LayoutParams lp = (LayoutParams) child.getLayoutParams();
			final int width = child.getMeasuredWidth();
			final int height = child.getMeasuredHeight();
			int childLeft, childTop;
			int gravity = lp.gravity;
			if (gravity == -1) {
				gravity = Gravity.START | Gravity.TOP;
			}
			final int absoluteGravity = Gravity.getAbsoluteGravity(gravity, layoutDirection);
			switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
				case Gravity.CENTER_HORIZONTAL:
					childLeft = parentLeft + (parentRight - parentLeft - width) / 2 +
							lp.leftMargin - lp.rightMargin;
					break;
				case Gravity.RIGHT:
					childLeft = parentRight - width - lp.rightMargin;
					break;
				case Gravity.LEFT:
				default:
					childLeft = parentLeft + lp.leftMargin;
			}
			switch (gravity & Gravity.VERTICAL_GRAVITY_MASK) {
				case Gravity.TOP:
				default:
					childTop = parentTop + lp.topMargin;
					break;
				case Gravity.CENTER_VERTICAL:
					childTop = parentTop + (parentBottom - parentTop - height) / 2 +
							lp.topMargin - lp.bottomMargin;
					break;
				case Gravity.BOTTOM:
					childTop = parentBottom - height - lp.bottomMargin;
					break;
			}
			child.layout(childLeft, childTop, childLeft + width, childTop + height);
		}
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		layoutNonButtonChildren(left, top, right, bottom);
		int w = right - left;
		int h = bottom - top;

		for(ControlInterface controlInterface : getButtonChildren()) {
			ControlData properties = controlInterface.getProperties();
			View interfaceView = controlInterface.getControlView();

			int width = (int) properties.getWidth();
			int height = (int) properties.getHeight();

			interfaceView.measure(
					MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
					MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
			);

			if(!changed && !interfaceView.isLayoutRequested()) {
				interfaceView.layout(
						interfaceView.getLeft(), interfaceView.getTop(),
						interfaceView.getRight(), interfaceView.getBottom()
				);
			} else {
				int l = (int) (properties.insertDynamicPos(properties.dynamicX, w, h) + left);
				int t = (int) (properties.insertDynamicPos(properties.dynamicY, w, h) + top);

				int r = l + width;
				int b = t + height;
				interfaceView.layout(l, t, r, b);
			}
		}
	}

	public boolean areControlVisible(){
		return mControlVisible;
	}

	public LayoutBitmaps getBitmaps() {
		return mLayout.mLayoutBitmaps;
	}

	public void updateButtonOpacity() {
		mButtonsOpacity = Math.clamp(LauncherPreferences.PREF_BUTTON_TRANSPARENCY / 100, 0, 1);
		for(ControlInterface button : getButtonChildren()) {

			if(mModifiable) button.setVisible(true);
			button.getControlView().setAlpha(mModifiable ? button.getProperties().opacity : mButtonsOpacity * button.getProperties().opacity);
		}
	}
}