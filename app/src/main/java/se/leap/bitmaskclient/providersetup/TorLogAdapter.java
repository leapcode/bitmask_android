package se.leap.bitmaskclient.providersetup;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import se.leap.bitmaskclient.R;

public class TorLogAdapter extends RecyclerView.Adapter<TorLogAdapter.ViewHolder> {
    private List<String> values;
    public boolean postponeUpdate;

    static class ViewHolder extends RecyclerView.ViewHolder {
        public AppCompatTextView logTextLabel;
        public View layout;

        public ViewHolder(View v) {
            super(v);
            layout = v;
            logTextLabel = v.findViewById(android.R.id.text1);
        }
    }

    public void updateData(List<String> data) {
        values = data;
        if (!postponeUpdate) {
            notifyDataSetChanged();
        }
    }

    public TorLogAdapter(List<String> data) {
        values = data;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(
                parent.getContext());
        View v = inflater.inflate(R.layout.v_log_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        final String log = values.get(position);
        holder.logTextLabel.setText(log);
    }

    @Override
    public int getItemCount() {
        return values.size();
    }
}
