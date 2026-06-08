package org.fptn.vpn.views.perappvpn;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import org.fptn.vpn.R;
import org.fptn.vpn.enums.PerAppVpnMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class AppInfoListAdapter extends RecyclerView.Adapter<AppInfoListAdapter.ViewHolder> {
    private final List<AppInfo> allApps;
    private List<AppInfo> displayApps = new ArrayList<>();
    private PerAppVpnMode perAppVpnMode;
    private String searchQuery = "";

    public AppInfoListAdapter(List<AppInfo> apps, PerAppVpnMode perAppVpnMode) {
        this.allApps = apps;
        this.perAppVpnMode = perAppVpnMode;
        refreshDisplay();
    }

    public void setPerAppVpnMode(PerAppVpnMode perAppVpnMode) {
        this.perAppVpnMode = perAppVpnMode;
        refreshDisplay();
    }

    public void filter(String query) {
        this.searchQuery = query.toLowerCase(Locale.getDefault());
        refreshDisplay();
    }

    private void refreshDisplay() {
        displayApps = allApps.stream()
                .filter(app -> searchQuery.isEmpty() ||
                        app.getLabel().toLowerCase(Locale.getDefault()).contains(searchQuery))
                .sorted((a, b) -> {
                    boolean aOn = isActive(a);
                    boolean bOn = isActive(b);
                    if (aOn != bOn) return aOn ? -1 : 1;
                    return a.getLabel().compareToIgnoreCase(b.getLabel());
                })
                .collect(Collectors.toList());
        notifyDataSetChanged();
    }

    private boolean isActive(AppInfo app) {
        if (perAppVpnMode == PerAppVpnMode.ONLY_ALLOWED) return app.isAllowed();
        if (perAppVpnMode == PerAppVpnMode.EXCEPT_DISALLOWED) return app.isDisallowed();
        return false;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.settings_per_app_vpnmode_layout_item_app_selection, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppInfo app = displayApps.get(position);

        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setText(app.getLabel());
        holder.appIcon.setImageDrawable(app.getIcon());

        if (perAppVpnMode == PerAppVpnMode.ONLY_ALLOWED) {
            holder.checkBox.setChecked(app.isAllowed());
        } else if (perAppVpnMode == PerAppVpnMode.EXCEPT_DISALLOWED) {
            holder.checkBox.setChecked(app.isDisallowed());
        } else {
            holder.checkBox.setChecked(false);
        }

        holder.checkBox.setOnClickListener(v -> {
            if (perAppVpnMode == PerAppVpnMode.ONLY_ALLOWED) {
                app.setAllowed(!app.isAllowed());
            } else if (perAppVpnMode == PerAppVpnMode.EXCEPT_DISALLOWED) {
                app.setDisallowed(!app.isDisallowed());
            }
            refreshDisplay();
        });
    }

    @Override
    public int getItemCount() {
        return displayApps.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView appIcon;
        SwitchCompat checkBox;

        public ViewHolder(View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.app_icon);
            checkBox = itemView.findViewById(R.id.app_switch);
        }
    }
}
