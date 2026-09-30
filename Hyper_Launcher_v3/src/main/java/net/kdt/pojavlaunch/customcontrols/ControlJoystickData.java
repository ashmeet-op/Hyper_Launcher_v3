package net.kdt.pojavlaunch.customcontrols;

public class ControlJoystickData extends ControlData {


    public boolean forwardLock = false;

    public boolean absolute = false;

    public ControlJoystickData(){
        super();
    }

    public ControlJoystickData(ControlJoystickData properties) {
        super(properties);
        forwardLock = properties.forwardLock;
        absolute = properties.absolute;
    }
}
