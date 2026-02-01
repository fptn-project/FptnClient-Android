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

import java.util.List;

public class AppInfoListAdapter extends RecyclerView.Adapter<AppInfoListAdapter.ViewHolder> {
    private final List<AppInfo> apps;

    private PerAppVpnMode perAppVpnMode;

    public AppInfoListAdapter(List<AppInfo> apps, PerAppVpnMode perAppVpnMode) {
        this.apps = apps;
        this.perAppVpnMode = perAppVpnMode;
    }

    public void setPerAppVpnMode(PerAppVpnMode perAppVpnMode){
        this.perAppVpnMode = perAppVpnMode;

        notifyItemRangeChanged(0, apps.size());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app_selection, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppInfo app = apps.get(position);

        holder.checkBox.setText(app.getLabel());
        holder.appIcon.setImageDrawable(app.getIcon());

        if (perAppVpnMode == PerAppVpnMode.ONLY_ALLOWED){
            holder.checkBox.setChecked(app.isAllowed());
        } else if (perAppVpnMode == PerAppVpnMode.EXCEPT_DISALLOWED){
            holder.checkBox.setChecked(app.isDisallowed());
        }

        holder.itemView.setOnClickListener(v -> {
            if (perAppVpnMode == PerAppVpnMode.ONLY_ALLOWED){
                app.setAllowed(!app.isAllowed());
            } else if (perAppVpnMode == PerAppVpnMode.EXCEPT_DISALLOWED){
                app.setDisallowed(!app.isDisallowed());
            }
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() {
        return apps.size();
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