package net.kdt.pojavlaunch.instances;

import android.util.Log;
import net.kdt.pojavlaunch.utils.JSONUtils;

import java.io.File;
import java.io.IOException;

public class DisplayInstance {
    public transient File mInstanceRoot;
    public String name;
    public String versionId;
    public String icon;
    public boolean isPinned;

    protected void sanitize() {
        sanitizeIcon();
    }

    protected DisplayInstance() {
    }

    protected File getInstanceIconLocation() {
        return new File(mInstanceRoot, "icon.webp");
    }

    private void sanitizeIcon() {
        if(!InstanceIconProvider.hasStaticIcon(icon)) {
            icon = InstanceIconProvider.FALLBACK_ICON_NAME;
        }
    }

    public void write() throws IOException {
        JSONUtils.writeToFile(Instances.metadataLocation(mInstanceRoot), this);
    }

    public void maybeWrite() {
        try {
            write();
        } catch (IOException e) {
            Log.e("DisplayInstance", "Failed to write", e);
        }
    }
}
