package com.example.streamed_string_parser.model;

import androidx.annotation.NonNull;

public class ListItemModel {
    private boolean checked;
    private String text_data;

    public ListItemModel(boolean checked, String text_data) {
        this.checked = checked;
        this.text_data = text_data;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public String getText_data() {
        return text_data;
    }

    public void setText_data(String text_data) {
        this.text_data = text_data;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("{%s -> %s}", isChecked(), getText_data());
    }
}
