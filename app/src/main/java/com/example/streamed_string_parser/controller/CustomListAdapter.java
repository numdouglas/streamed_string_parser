package com.example.streamed_string_parser.controller;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.streamed_string_parser.R;
import com.example.streamed_string_parser.model.ListItemModel;

import java.util.List;

public class CustomListAdapter extends BaseAdapter {
    private final List<ListItemModel> listItemModels;
    private final Context mContext;
    private int itemModelSize;

    public static class ViewHolder {
        CheckBox checkBox;
        TextView textView;
    }

    public CustomListAdapter(List<ListItemModel> listItemModels, Context context) {
        this.listItemModels = listItemModels;
        this.mContext = context;
        itemModelSize = listItemModels.size();
    }

    @Override
    public int getCount() {
        return itemModelSize;
    }

    @Override
    public ListItemModel getItem(int id) {
        return listItemModels.get(id);
    }

    @Override
    public long getItemId(int id) {
        return id;
    }

    @Override
    public void notifyDataSetChanged() {
        itemModelSize = listItemModels.size();
        super.notifyDataSetChanged();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder viewHolder;
        ListItemModel listItemModel = getItem(position);

        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.list_item_row, parent, false);
            viewHolder.checkBox = convertView.findViewById(R.id.item_checkbox);
            viewHolder.textView = convertView.findViewById(R.id.item_textview);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.checkBox.setChecked(listItemModel.isChecked());
        viewHolder.textView.setText(listItemModel.getText_data());

        viewHolder.checkBox.setTag(position);
        viewHolder.checkBox.setOnClickListener(view -> {
            final int checkbox_position = (int) view.getTag();
            boolean isChecked = false;
            if (listItemModels.get(checkbox_position).isChecked() == false) {
                isChecked = true;
            }
            listItemModels.get(checkbox_position).setChecked(isChecked);
            notifyDataSetChanged();
            // Timber.e("%s", listItemModel);
        });

        return convertView;
    }
}