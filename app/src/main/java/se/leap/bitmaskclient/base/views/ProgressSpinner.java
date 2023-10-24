package se.leap.bitmaskclient.base.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
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
        String text = "";
        if (progress > 0) {
            if ((progress / 10) == 0) {
                text = text + " ";
            }
            if ((progress / 100) == 0) {
                text = text + " ";
            }
            text = text + progress + "%";
        }
        textView.setText(text);
    }

    public void setAnimatedSpinnerDrawable(Drawable drawable) {
        spinnerView.setImageDrawable(drawable);
    }
}
