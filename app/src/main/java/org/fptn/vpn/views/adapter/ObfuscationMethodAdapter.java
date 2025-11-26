package org.fptn.vpn.views.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.fptn.vpn.R;
import org.fptn.vpn.enums.TLSHandshakeObfuscation;

import lombok.Getter;

@Getter
public class ObfuscationMethodAdapter extends BaseAdapter {
    private final TLSHandshakeObfuscation[] values = TLSHandshakeObfuscation.values();

    public ObfuscationMethodAdapter() {
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return values.length;
    }

    @Override
    public Object getItem(int position) {
        return values[position];
    }

    @Override
    public long getItemId(int position) {
        return values[position].getId();
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if (view == null) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.obfuscation_method_spinner_item, parent, false);
        }

        TextView label = view.findViewById(R.id.obfuscation_method_spinner_item_label);
        label.setText(values[position].toString());
        return view;
    }
}
