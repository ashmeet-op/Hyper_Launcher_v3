package net.kdt.pojavlaunch.progresskeeper;

import static com.ashmeet.hyperlauncher.utils.Tools.BYTE_TO_MB;

import com.ashmeet.hyperlauncher.utils.Tools;

public class DownloaderProgressWrapper implements Tools.DownloaderFeedback {

    private final int mProgressString;
    private final String mProgressRecord;
    public String extraString = null;


    public DownloaderProgressWrapper(int progressString, String progressRecord) {
        this.mProgressString = progressString;
        this.mProgressRecord = progressRecord;
    }

    @Override
    public void updateProgress(int curr, int max) {
        Object[] va;
        if(extraString != null)  {
            va = new Object[3];
            va[0] = extraString;
            va[1] = curr/BYTE_TO_MB;
            va[2] = max/BYTE_TO_MB;
        }
        else {
            va = new Object[2];
            va[0] = curr/BYTE_TO_MB;
            va[1] = max/BYTE_TO_MB;
        }

        ProgressKeeper.submitProgress(mProgressRecord, (int) Math.max((float)curr/max*100,0), mProgressString, va);
    }
}
