package com.stahlnow.android.dislocator;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.common.GoogleApiAvailability;

public class GoogleLicenseDialogFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setPositiveButton(R.string.button_close, null);

        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        String licenseInfo = googleApiAvailability.getOpenSourceSoftwareLicenseInfo(activity);

        if (licenseInfo != null) {
            Context context = getDialogContext(activity);
            RecyclerView recyclerView = createRecyclerView(context);
            recyclerView.setAdapter(new LongMessageAdapter(context, licenseInfo));
            builder.setView(recyclerView);
        }

        return builder.create();
    }

    private Context getDialogContext(Context context) {
        TypedValue outValue = new TypedValue();
        int resId = android.support.v7.appcompat.R.attr.dialogTheme;
        context.getTheme().resolveAttribute(resId, outValue, true);
        int themeId = outValue.resourceId;
        return themeId == 0 ? context : new ContextThemeWrapper(context, themeId);
    }

    private RecyclerView createRecyclerView(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        @SuppressLint("InflateParams")
        RecyclerView recyclerView = (RecyclerView) inflater.inflate(R.layout.recycler_dialog, null);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        return recyclerView;
    }

    private static final class LongMessageAdapter
            extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final Context context;
        private final String[] lines;

        public LongMessageAdapter(Context context, String message) {
            this.context = context;
            this.lines = message.split("\\n");
        }

        @Override
        public int getItemCount() {
            return lines.length;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TextView textView = new TextView(context);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                textView.setTextAppearance(R.style.TextAppearance_AppCompat_Subhead);
            } else {
                textView.setTextAppearance(context, R.style.TextAppearance_AppCompat_Subhead);
            }
            return new RecyclerView.ViewHolder(textView) {
            };
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ((TextView) holder.itemView).setText(lines[position]);
        }
    }
}