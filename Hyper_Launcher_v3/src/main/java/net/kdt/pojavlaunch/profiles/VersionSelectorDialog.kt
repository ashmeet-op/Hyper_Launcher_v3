package net.kdt.pojavlaunch.profiles;

import static net.kdt.pojavlaunch.extra.ExtraCore.getValue;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;
import androidx.compose.ui.platform.ComposeView;
import androidx.compose.ui.platform.ViewCompositionStrategy;


import com.ashmeet.hyperlauncher.components.text.LegacyMigratedComponentsBridge;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import net.kdt.pojavlaunch.JVersionList;
import net.ashmeet.hyperlauncher.R;
import net.kdt.pojavlaunch.extra.ExtraConstants;

import java.util.ArrayList;
import java.util.List;

public class VersionSelectorDialog {
    public static void open(Context context, boolean hideCustomVersions, VersionSelectorListener listener) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(R.string.version_select_hint);

        JVersionList jVersionList = (JVersionList) getValue(ExtraConstants.RELEASE_TABLE);
        JVersionList.Version[] versionArray;
        if(jVersionList == null || jVersionList.versions == null) versionArray = new JVersionList.Version[0];
        else versionArray = jVersionList.versions;
        VersionListAdapter adapter = new VersionListAdapter(versionArray, hideCustomVersions, context);

        List<String> groups = new ArrayList<>();
        List<List<String>> groupData = new ArrayList<>();
        for (int i = 0; i < adapter.getGroupCount(); i++) {
            groups.add(adapter.getGroupName(i));
            groupData.add(adapter.getGroupChildren(i));
        }

        ComposeView composeView = new ComposeView(context);
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed.INSTANCE);

        builder.setView(composeView);
        AlertDialog dialog = builder.create();

        LegacyMigratedComponentsBridge.setVersionSelectorContent(
                composeView,
                groups,
                groupData,
                (groupIdx, childIdx) -> {
                    String version = adapter.getChild(groupIdx, childIdx);
                    listener.onVersionSelected(version, adapter.isSnapshotSelected(groupIdx));
                    dialog.dismiss();
                    return kotlin.Unit.INSTANCE;
                }
        );

        dialog.show();
    }
}
