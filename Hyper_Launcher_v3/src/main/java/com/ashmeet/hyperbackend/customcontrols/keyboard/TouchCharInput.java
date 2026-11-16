package com.ashmeet.hyperbackend.customcontrols.keyboard;


import static android.content.Context.INPUT_METHOD_SERVICE;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.text.Editable;
import android.text.Selection;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import net.ashmeet.hyperlauncher.R;




@SuppressWarnings("unused")
public class TouchCharInput extends androidx.appcompat.widget.AppCompatEditText {
    public static final String TEXT_FILLER = "                              ";
    public TouchCharInput(@NonNull Context context) {
        this(context, null);
    }
    public TouchCharInput(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.editTextStyle);
    }
    public TouchCharInput(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup();
    }


    private boolean mIsDoingInternalChanges = false;
    private boolean mIsHorizonMode = false;
    private final CharacterSenderStrategy mCharacterSender = new LwjglCharSender();

    public void setHorizonMode(boolean horizonMode) {
        mIsHorizonMode = horizonMode;
    }


    public boolean isHorizonMode() {
        return mIsHorizonMode;
    }


    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        disable();
    }


    @Override
    public boolean onKeyPreIme(final int keyCode, final KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            disable();
        }
        return super.onKeyPreIme(keyCode, event);
    }



    private Activity getActivity() {
        Context context = getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    public void switchKeyboardState(){
        if(isEnabled() && hasFocus()){
            clear();
            disable();
        }else{
            setKeyboardState(true, null);
        }
    }


    public void setKeyboardState(boolean state){
        setKeyboardState(state, null);
    }

    @SuppressWarnings("deprecation")
    public void setKeyboardState(boolean state, String initialText){
        if(!state){
            clear();
            disable();
        }else{
            enable();
            mIsDoingInternalChanges = true;
            Editable editable = getEditableText();
            editable.clear();
            if (mIsHorizonMode && initialText != null && !initialText.isEmpty()) {
                editable.append(initialText);
            } else if (!mIsHorizonMode) {
                editable.append(TEXT_FILLER);
            }
            if (mIsHorizonMode) {
                dev.horizon.android.HorizonInput.setCurrentText(editable.toString());
            }
            int selectionPos = editable.length();
            if (!mIsHorizonMode) {
                selectionPos = TEXT_FILLER.length();
            }
            Selection.setSelection(editable, Math.min(selectionPos, editable.length()));
            mIsDoingInternalChanges = false;
            post(() -> {
                requestFocus();
                Activity activity = getActivity();
                if (activity != null) {
                    try {
                        WindowCompat.getInsetsController(activity.getWindow(), this).show(WindowInsetsCompat.Type.ime());
                    } catch (Throwable t) {
                        // ignore
                    }
                }
                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(this, InputMethodManager.SHOW_FORCED);
                    try {
                        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                    } catch (Throwable ignored) {}
                }
            });
        }
    }



    public void clear(){
        mIsDoingInternalChanges = true;


        Editable editable = getEditableText();
        editable.clear();

        if (!mIsHorizonMode) {
            editable.append(TEXT_FILLER);
            Selection.setSelection(editable, TEXT_FILLER.length());
        } else {
            Selection.setSelection(editable, 0);
            dev.horizon.android.HorizonInput.setCurrentText("");
        }
        mIsDoingInternalChanges = false;
    }


    public void enable(){
        setEnabled(true);
        setFocusable(true);
        setVisibility(VISIBLE);
        requestFocus();
    }


    public void disable(){
        clear();
        setVisibility(GONE);
        clearFocus();
        setEnabled(false);
        try {
            Activity activity = getActivity();
            if (activity != null) {
                WindowCompat.getInsetsController(activity.getWindow(), this).hide(WindowInsetsCompat.Type.ime());
            }
        } catch (Throwable t) {
            try {
                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(getWindowToken(), 0);
                }
            } catch (Throwable ignored) {}
        }
    }


    private void sendEnter(){
        mCharacterSender.sendEnter();
        clear();
    }


    private void setup(){


        addTextChangedListener(new InputTextWatcher());
        setOnEditorActionListener((textView, i, keyEvent) -> {
            if (mIsHorizonMode) {
                mIsDoingInternalChanges = true;
                Editable editable = getEditableText();
                if (!editable.toString().endsWith("\n")) {
                    editable.append("\n");
                }
                dev.horizon.android.HorizonInput.setCurrentText(editable.toString());
                mIsDoingInternalChanges = false;
                try {
                    Activity activity = getActivity();
                    if (activity != null) {
                        WindowCompat.getInsetsController(activity.getWindow(), this).hide(WindowInsetsCompat.Type.ime());
                    }
                } catch (Throwable t) {
                    try {
                        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(getWindowToken(), 0);
                        }
                    } catch (Throwable ignored) {}
                }
                return true;
            }
            sendEnter();
            clear();
            disable();
            return false;
        });
        clear();
        disable();
    }

    public CharacterSenderStrategy getmCharacterSender() {
        return mCharacterSender;
    }

    private class InputTextWatcher implements android.text.TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }


        @Override
        public void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
            if (mIsHorizonMode) {
                dev.horizon.android.HorizonInput.setCurrentText(text.toString());
                return;
            }
            if(mIsDoingInternalChanges) return;

            for (int i = 0; i < lengthBefore; ++i) {
                mCharacterSender.sendBackspace();
            }

            mCharacterSender.sendChars(text.subSequence(start, start + lengthAfter));
        }

        @Override
        public void afterTextChanged(Editable editable) {
            if(mIsDoingInternalChanges) return;
            if(mIsHorizonMode) return;


            if(editable.length() < 1) clear();
        }
    }
}
