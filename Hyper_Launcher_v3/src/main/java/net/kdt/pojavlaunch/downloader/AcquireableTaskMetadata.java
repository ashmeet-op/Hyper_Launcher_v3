package net.kdt.pojavlaunch.downloader;

import java.io.IOException;

public abstract class AcquireableTaskMetadata extends TaskMetadata {
    public AcquireableTaskMetadata(int mirrorType) {
        super(null, null, mirrorType);
    }


    public abstract void acquireMetadata() throws IOException;
}
