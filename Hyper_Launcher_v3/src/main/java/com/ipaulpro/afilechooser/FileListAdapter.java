

package com.ipaulpro.afilechooser;

import android.content.*;
import android.graphics.drawable.Drawable;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

import com.ashmeet.hyperlauncher.utils.drawable.MaterialIconUtil;


public class FileListAdapter extends BaseAdapter {

    private final LayoutInflater mInflater;

    private List<File> mData = new ArrayList<File>();

    public FileListAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
    }

    public void add(File file) {
        mData.add(file);
        notifyDataSetChanged();
    }

    public void remove(File file) {
        mData.remove(file);
        notifyDataSetChanged();
    }

    public void insert(File file, int index) {
        mData.add(index, file);
        notifyDataSetChanged();
    }

    public void clear() {
        mData.clear();
        notifyDataSetChanged();
    }

    @Override
    public File getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;

        if (row == null)
            row = mInflater.inflate(android.R.layout.simple_list_item_1, parent, false);

        TextView view = (TextView) row;


        final File file = getItem(position);


        view.setText(file.getName());


        Drawable icon = file.isDirectory() ? MaterialIconUtil.getFolderDrawable(view.getContext()) : MaterialIconUtil.getFileDrawable(view.getContext());
        view.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
        view.setCompoundDrawablePadding(20);
        return row;
    }

}
