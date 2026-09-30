package net.kdt.pojavlaunch.tasks;


public class SpeedCalculator {
    private long mLastMillis;
    private long mLastBytes;
    private int mIndex;
    private final double[] mPreviousInputs;
    private double mSum;

    public SpeedCalculator() {
        this(64);
    }

    public SpeedCalculator(int averageDepth) {
        mPreviousInputs = new double[averageDepth];
    }

    private double addToAverage(double speed) {
        mSum -= mPreviousInputs[mIndex];
        mSum += speed;
        mPreviousInputs[mIndex] = speed;
        if(++mIndex == mPreviousInputs.length) mIndex = 0;
        double dLength = mPreviousInputs.length;
        return (mSum + (dLength / 2d)) / dLength;
    }


    public double feed(long bytes) {
        long millis = System.currentTimeMillis();
        long deltaBytes = bytes - mLastBytes;
        long deltaMillis = millis - mLastMillis;
        mLastBytes = bytes;
        mLastMillis = millis;
        double speed = (double)deltaBytes / ((double)deltaMillis / 1000d);
        return addToAverage(speed);
    }
}
