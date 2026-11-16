package com.ashmeet.hyperbackend.customcontrols.keyboard;


public interface CharacterSenderStrategy {

    void sendBackspace();


    void sendEnter();

    void sendChars(CharSequence chars);

}
