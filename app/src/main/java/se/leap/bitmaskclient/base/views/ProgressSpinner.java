package se.leap.bitmaskclient.base.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;

import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.databinding.VProgressSpinnerBinding;

public class ProgressSpinner extends RelativeLayout {

    private static final String TAG = ProgressSpinner.class.getSimpleName();

    AppCompatImageView spinnerView;
    AppCompatTextView textView;

    public ProgressSpinner(Context context) {
        super(context);
        initLayout(context);
    }

    public ProgressSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        initLayout(context);
    }

    public ProgressSpinner(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initLayout(context);
    }


    public ProgressSpinner(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initLayout(context);
    }

    private void initLayout(Context context) {
        VProgressSpinnerBinding binding = VProgressSpinnerBinding.inflate(LayoutInflater.from(context), this, true);
        spinnerView = binding.spinnerView;
        textView = binding.tvProgress;
    }

    public void update(int progress) {
        textView.setText(textView.getContext().getString(R.string.percentage, progress));
    }
}
