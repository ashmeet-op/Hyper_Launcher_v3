package net.kdt.pojavlaunch.customcontrols;
import android.content.*;
import android.view.KeyEvent;

import androidx.annotation.Keep;

import com.ashmeet.hyperlauncher.utils.Tools;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import net.ashmeet.hyperlauncher.R;


@Keep
public class CustomControls {
	public int version = -1;
    public float scaledAt;
	public List<ControlData> mControlDataList;
	public List<ControlDrawerData> mDrawerDataList;
	public List<ControlJoystickData> mJoystickDataList;
	public transient LayoutBitmaps mLayoutBitmaps;
	public CustomControls() {
		this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
	}



	public CustomControls(List<ControlData> mControlDataList, List<ControlDrawerData> mDrawerDataList, List<ControlJoystickData> mJoystickDataList) {
		this.mControlDataList = mControlDataList;
		this.mDrawerDataList = mDrawerDataList;
		this.mJoystickDataList = mJoystickDataList;
		this.scaledAt = 100f;
	}




	@SuppressWarnings("unused")
	public CustomControls(Context ctx) {
		this();
		this.mControlDataList.add(new ControlData(ControlData.getSpecialButtons()[0]));
		this.mControlDataList.add(new ControlData(ControlData.getSpecialButtons()[1]));
		this.mControlDataList.add(new ControlData(ControlData.getSpecialButtons()[2]));
		this.mControlDataList.add(new ControlData(ControlData.getSpecialButtons()[3]));
		this.mControlDataList.add(new ControlData(ControlData.getSpecialButtons()[4]));

		this.mControlDataList.add(new ControlData(ctx, R.string.control_debug, new int[]{KeyEvent.KEYCODE_F3}, "${margin}", "${margin}", false));
		this.mControlDataList.add(new ControlData(ctx, R.string.control_chat, new int[]{KeyEvent.KEYCODE_T}, "${margin} * 2 + ${width}", "${margin}", false));
		this.mControlDataList.add(new ControlData(ctx, R.string.control_listplayers, new int[]{KeyEvent.KEYCODE_TAB}, "${margin} * 4 + ${width} * 3", "${margin}", false));
		this.mControlDataList.add(new ControlData(ctx, R.string.control_thirdperson, new int[]{KeyEvent.KEYCODE_F5}, "${margin}", "${height} + ${margin}", false));

		this.mControlDataList.add(new ControlData(ctx, R.string.control_up, new int[]{KeyEvent.KEYCODE_W}, "${margin} * 2 + ${width}", "${bottom} - ${margin} * 3 - ${height} * 2", true));
		this.mControlDataList.add(new ControlData(ctx, R.string.control_left, new int[]{KeyEvent.KEYCODE_A}, "${margin}", "${bottom} - ${margin} * 2 - ${height}", true));
		this.mControlDataList.add(new ControlData(ctx, R.string.control_down, new int[]{KeyEvent.KEYCODE_S}, "${margin} * 2 + ${width}", "${bottom} - ${margin}", true));
		this.mControlDataList.add(new ControlData(ctx, R.string.control_right, new int[]{KeyEvent.KEYCODE_D}, "${margin} * 3 + ${width} * 2", "${bottom} - ${margin} * 2 - ${height}", true));

		this.mControlDataList.add(new ControlData(ctx, R.string.control_inventory, new int[]{KeyEvent.KEYCODE_E}, "${margin} * 3 + ${width} * 2", "${bottom} - ${margin}", true));

        ControlData shiftData = new ControlData(ctx, R.string.control_shift, new int[]{KeyEvent.KEYCODE_SHIFT_LEFT}, "${margin} * 2 + ${width}", "${screen_height} - ${margin} * 2 - ${height} * 2", true);
		shiftData.isToggle = true;
		this.mControlDataList.add(shiftData);
		this.mControlDataList.add(new ControlData(ctx, R.string.control_jump, new int[]{KeyEvent.KEYCODE_SPACE}, "${right} - ${margin} * 2 - ${width}", "${bottom} - ${margin} * 2 - ${height}", true));


		version = 9;
	}

	public void save(String path) throws IOException {

		version = 9;
		String jsonControls = Tools.GLOBAL_GSON.toJson(this);
		try(FileOutputStream fileOutputStream = new FileOutputStream(path)) {
			LayoutBitmaps.store(fileOutputStream, new LayoutBitmaps.ControlsContainer(
					jsonControls,
					mLayoutBitmaps
			));
		}
	}
}
