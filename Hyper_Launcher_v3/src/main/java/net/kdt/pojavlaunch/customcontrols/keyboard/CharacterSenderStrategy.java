package net.kdt.pojavlaunch.customcontrols.keyboard;


public interface CharacterSenderStrategy {

    void sendBackspace();


    void sendEnter();

    void sendChars(CharSequence chars);

}
