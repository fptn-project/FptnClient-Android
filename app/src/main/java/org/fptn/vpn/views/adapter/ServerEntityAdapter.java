package org.fptn.vpn.views.adapter;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.graphics.Color;
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
            host.setVisibility(VISIBLE);

            TextView tvCountryFlagEmoji = view.findViewById(R.id.tv_country_flag_emoji);
            if (tvCountryFlagEmoji != null) {
                tvCountryFlagEmoji.setText(CountryFlags.getCountryFlagByCountryCode(server.getCountryCode()));
                tvCountryFlagEmoji.setVisibility(VISIBLE);
            }
            if (layoutViewResourceId == R.layout.home_list_recycler_server_item) {
                view.findViewById(R.id.iv_country).setVisibility(GONE);
            }
        } else {
            host.setVisibility(GONE);

            if (layoutViewResourceId == R.layout.home_list_recycler_server_item) {
                view.findViewById(R.id.iv_country).setVisibility(VISIBLE);
            }
        }

        if (server.isCensured()) {
            view.findViewById(R.id.censoredIcon).setVisibility(VISIBLE);
        } else {
            view.findViewById(R.id.censoredIcon).setVisibility(GONE);
        }

        TextView name = view.findViewById(R.id.fptn_server_name);
        name.setText(server.getName());

        // show ping
        TextView pingView = view.findViewById(R.id.server_ping);
        if (pingView != null) {
            long ping = server.getPingMs();
            if (ping > 0) {
                pingView.setText(ping + " ms");
                pingView.setVisibility(VISIBLE);
            } else if (ping == -1) {
                pingView.setText("  --  ");
                pingView.setVisibility(VISIBLE);
            } else {
                pingView.setVisibility(GONE);
            }
            // change color
            int color = getPingColor(ping);
            pingView.setBackgroundColor(color);
        }

        return view;
    }

    private int getPingColor(long ping) {
        if (ping < 0) {
            return Color.WHITE;
        } else if (ping < 150) {
            return 0xFF00FF00;
        } else if (ping < 200) {
            return 0xFFFFFF00;
        } else if (ping < 300) {
            return 0xFFFFA500;
        }
        return 0xFFFF0000;
    }

    public void setServerEntityList(List<ServerEntity> serverEntityList) {
        this.serverEntityList = serverEntityList;
        notifyDataSetChanged();
    }
}
