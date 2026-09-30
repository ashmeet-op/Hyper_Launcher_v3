package net.kdt.pojavlaunch.customcontrols.keyboard;


import static android.content.Context.INPUT_METHOD_SERVICE;

import static net.kdt.pojavlaunch.game.platform.Platform.PLATFORM;

import android.content.Context;
import android.text.Editable;
import android.text.Selection;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.ashmeet.hyperlauncher.R;
import net.kdt.pojavlaunch.CallbackBridge;




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
    private CharacterSenderStrategy mCharacterSender = null;


    public void setCharacterSender(CharacterSenderStrategy strategy) {
        mCharacterSender = strategy;
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



    public void switchKeyboardState(){
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(INPUT_METHOD_SERVICE);

        if(hasFocus()){
            clear();
            disable();
        }else{
            enable();
            imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT);
        }
    }


    public void setKeyboardState(boolean state){
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(INPUT_METHOD_SERVICE);
        if(!state){
            clear();
            disable();
        }else{
            enable();
            imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT);
        }
    }



    public void clear(){
        mIsDoingInternalChanges = true;


        Editable editable = getEditableText();
        editable.clear();

        editable.append(TEXT_FILLER);
        Selection.setSelection(editable, TEXT_FILLER.length());
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

    }


    private void sendEnter(){
        if(mCharacterSender != null){
            mCharacterSender.sendEnter();
        }else{
            CallbackBridge.sendKeyPress(KeyEvent.KEYCODE_ENTER);
        }
        clear();
    }


    private void setup(){


        addTextChangedListener(new InputTextWatcher());
        setOnEditorActionListener((textView, i, keyEvent) -> {
            sendEnter();
            clear();
            disable();
            return false;
        });
        clear();
        disable();
    }
    private class InputTextWatcher implements android.text.TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }


        @Override
        public void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
            if(mIsDoingInternalChanges) return;

            if(mCharacterSender != null){
                for(int i=0; i < lengthBefore; ++i){
                    mCharacterSender.sendBackspace();
                }
                mCharacterSender.sendChars(text.subSequence(start, start + lengthAfter));
            }else{
                for(int i=0; i < lengthBefore; ++i){
                    CallbackBridge.sendKeyPress(KeyEvent.KEYCODE_DEL);
                }

                PLATFORM.sendBulkUnicodeEvent(text.subSequence(start, start + lengthAfter).toString(), CallbackBridge.getCurrentMods());
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {
            if(mIsDoingInternalChanges) return;


            if(editable.length() < 1) clear();
        }
    }
}
