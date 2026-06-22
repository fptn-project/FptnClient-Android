/*
 * FPTN Android Client
 * Copyright (C) 2026  Skokov Stanislav, Enin Sergey
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Website: https://fptn.org
 */

package org.fptn.vpn.views.adapter;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
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

        View ivCountry = view.findViewById(R.id.iv_country); // in one layout it is ImageView, in another - TextView
        TextView tvCountryFlagEmoji = view.findViewById(R.id.tv_country_flag_emoji);
        LinearLayout topRow = view.findViewById(R.id.top_row);
        LinearLayout bottomRow = view.findViewById(R.id.bottom_row);
        TextView serverName = view.findViewById(R.id.fptn_server_name);
        ImageView censoredIcon = view.findViewById(R.id.censoredIcon);
        TextView pingEmoji = view.findViewById(R.id.tv_ping_emoji);
        TextView pingView = view.findViewById(R.id.server_ping);
        TextView hostView = view.findViewById(R.id.fptn_server_host);

        if (topRow != null) {
            RelativeLayout.LayoutParams topRowParams = (RelativeLayout.LayoutParams) topRow.getLayoutParams();
            topRowParams.addRule(RelativeLayout.CENTER_VERTICAL, 0);
            topRowParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
            topRowParams.setMarginStart((int) (38 * parent.getContext().getResources().getDisplayMetrics().density));
            topRow.setLayoutParams(topRowParams);
        }

        if (server.IsAuto()) {
            ivCountry.setVisibility(VISIBLE);
            tvCountryFlagEmoji.setVisibility(GONE);
            censoredIcon.setVisibility(GONE);
            pingEmoji.setVisibility(GONE);
            pingView.setVisibility(GONE);
            hostView.setVisibility(GONE);

            if (bottomRow != null) {
                bottomRow.setVisibility(GONE);
            }

            // center Auto
            if (topRow != null) {
                RelativeLayout.LayoutParams topRowParams = (RelativeLayout.LayoutParams) topRow.getLayoutParams();
                topRowParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
                topRowParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);

                int marginStartInPx = (int) (4 * parent.getContext().getResources().getDisplayMetrics().density);
                topRowParams.setMarginStart(marginStartInPx);
                topRow.setLayoutParams(topRowParams);

                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) serverName.getLayoutParams();
                params.width = LinearLayout.LayoutParams.MATCH_PARENT;
                params.weight = 0;
                params.gravity = android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.START;
                serverName.setLayoutParams(params);

                for (int i = 0; i < topRow.getChildCount(); i++) {
                    View child = topRow.getChildAt(i);
                    if (child.getId() == R.id.censoredIcon) {
                        child.setVisibility(GONE);
                    }
                }
            }
            serverName.setVisibility(VISIBLE);
            serverName.setText(server.getName());
            serverName.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            serverName.setTypeface(serverName.getTypeface(), android.graphics.Typeface.NORMAL);

        } else {
            ivCountry.setVisibility(GONE);

            if (server.getCountryCode() != null && !server.getCountryCode().isEmpty()) {
                tvCountryFlagEmoji.setVisibility(VISIBLE);
                tvCountryFlagEmoji.setText(CountryFlags.getCountryFlagByCountryCode(server.getCountryCode()));
            } else {
                tvCountryFlagEmoji.setVisibility(GONE);
            }

            serverName.setVisibility(VISIBLE);
            serverName.setText(server.getName());
            serverName.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            serverName.setTypeface(serverName.getTypeface(), android.graphics.Typeface.NORMAL);

            if (topRow != null) {
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) serverName.getLayoutParams();
                params.width = 0;
                params.weight = 1;
                params.gravity = android.view.Gravity.START;
                serverName.setLayoutParams(params);

                if (server.isCensured()) {
                    censoredIcon.setVisibility(VISIBLE);
                } else {
                    censoredIcon.setVisibility(GONE);
                }
            }

            if (bottomRow != null) {
                bottomRow.setVisibility(VISIBLE);
                long ping = server.getPingMs();
                if (ping > 0) {
                    pingView.setVisibility(VISIBLE);
                    pingView.setText(ping + "ms");
                    pingEmoji.setVisibility(VISIBLE);
                    pingEmoji.setText(getPingEmoji(ping));
                } else if (ping < 0) {
                    pingView.setVisibility(VISIBLE);
                    pingView.setText("---  ---  ---");
                    pingEmoji.setVisibility(GONE);
                } else {
                    pingView.setVisibility(GONE);
                    pingEmoji.setVisibility(GONE);
                }
            }
            hostView.setVisibility(GONE);
        }
        return view;
    }

    private String getPingEmoji(long ping) {
        if (ping < 150) {
            return "🟢";
        } else if (ping < 200) {
            return "🟡";
        } else if (ping < 300) {
            return "🟠";
        }
        return "🔴";
    }

    public void setServerEntityList(List<ServerEntity> serverEntityList) {
        this.serverEntityList = serverEntityList;
        notifyDataSetChanged();
    }
}
