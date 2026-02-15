package org.fptn.vpn.views.adapter;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.fptn.vpn.R;
import org.fptn.vpn.database.entity.ServerEntity;
import org.fptn.vpn.utils.CountryFlags;

import java.util.List;

import lombok.Getter;

@Getter
public class ServerEntityAdapter extends BaseAdapter {
    private final int layoutViewResourceId;

    private List<ServerEntity> serverEntityList;

    public ServerEntityAdapter(int layoutViewResourceId) {
        this.layoutViewResourceId = layoutViewResourceId;
    }

    public ServerEntityAdapter(List<ServerEntity> serverEntityList, int layoutViewResourceId) {
        this.layoutViewResourceId = layoutViewResourceId;
        this.serverEntityList = serverEntityList;

        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return serverEntityList != null ? serverEntityList.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return serverEntityList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return serverEntityList.get(position).getId();
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if (view == null) {
            view = LayoutInflater.from(parent.getContext()).inflate(layoutViewResourceId, parent, false);
        }
        ServerEntity server = serverEntityList.get(position);

        TextView host = view.findViewById(R.id.fptn_server_host);
        if (server.getCountryCode() != null) {
            host.setText(CountryFlags.getCountryFlagByCountryCode(server.getCountryCode()));
            host.setVisibility(VISIBLE);

            if (layoutViewResourceId == R.layout.home_list_recycler_server_item) {
                view.findViewById(R.id.ivCountry).setVisibility(GONE);
            }
        } else {
            host.setVisibility(GONE);

            if (layoutViewResourceId == R.layout.home_list_recycler_server_item) {
                view.findViewById(R.id.ivCountry).setVisibility(VISIBLE);
            }
        }

        if (server.isCensured()) {
            view.findViewById(R.id.censoredIcon).setVisibility(VISIBLE);
        } else {
            view.findViewById(R.id.censoredIcon).setVisibility(GONE);
        }

        TextView name = view.findViewById(R.id.fptn_server_name);
        name.setText(server.getName());

        return view;
    }

    public void setServerEntityList(List<ServerEntity> serverEntityList) {
        this.serverEntityList = serverEntityList;

        notifyDataSetChanged();
    }
}
