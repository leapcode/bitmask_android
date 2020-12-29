package se.leap.bitmaskclient.base.views;

import android.content.Context;
import androidx.annotation.DrawableRes;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

import se.leap.bitmaskclient.R;

import static se.leap.bitmaskclient.base.utils.ViewHelper.convertDimensionToPx;

/**
 * Created by cyberta on 29.06.18.
 */

public class ProviderHeaderView extends RelativeLayout {
    private int stdPadding;
    private int compactPadding;
    private int stdImageSize;
    private int compactImageSize;

    AppCompatImageView providerHeaderLogo;
    AppCompatTextView providerHeaderText;

    public ProviderHeaderView(Context context) {
        super(context);
        initLayout(context);
    }

    public ProviderHeaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initLayout(context);
    }

    public ProviderHeaderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initLayout(context);
    }

    @RequiresApi(21)
    public ProviderHeaderView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initLayout(context);
    }


    void initLayout(Context context) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rootview = inflater.inflate(R.layout.v_provider_header, this, true);
        providerHeaderLogo = rootview.findViewById(R.id.provider_header_logo);
        providerHeaderText = rootview.findViewById(R.id.provider_header_text);

        stdPadding = convertDimensionToPx(context, R.dimen.stdpadding);
        compactPadding = convertDimensionToPx(context, R.dimen.compact_padding);
        stdImageSize = convertDimensionToPx(context, R.dimen.bitmask_logo);
        compactImageSize = convertDimensionToPx(context, R.dimen.bitmask_logo_compact);
    }

    public void setTitle(String title) {
        providerHeaderText.setText(title);
    }

    public void setTitle(@StringRes int stringRes) {
        providerHeaderText.setText(stringRes);
    }

    public void setLogo(@DrawableRes int drawableRes) {
        providerHeaderLogo.setImageResource(drawableRes);
    }

    public void showCompactLayout() {
        LayoutParams logoLayoutParams = (LayoutParams) providerHeaderLogo.getLayoutParams();
        logoLayoutParams.width = compactImageSize;
        logoLayoutParams.height = compactImageSize;
        providerHeaderLogo.setLayoutParams(logoLayoutParams);

        LayoutParams textLayoutParams = (LayoutParams) providerHeaderText.getLayoutParams();
        textLayoutParams.addRule(RIGHT_OF, R.id.provider_header_logo);
        textLayoutParams.addRule(BELOW, 0);
        textLayoutParams.addRule(ALIGN_TOP, R.id.provider_header_logo);
        textLayoutParams.setMargins(compactPadding, compactPadding, compactPadding, compactPadding);

        providerHeaderText.setLayoutParams(textLayoutParams);
        providerHeaderText.setMaxLines(2);
    }

    public void showStandardLayout() {
        LayoutParams logoLayoutParams = (LayoutParams) providerHeaderLogo.getLayoutParams();
        logoLayoutParams.width = stdImageSize;
        logoLayoutParams.height = stdImageSize;
        providerHeaderLogo.setLayoutParams(logoLayoutParams);

        LayoutParams textLayoutParams = (LayoutParams) providerHeaderText.getLayoutParams();
        textLayoutParams.addRule(RIGHT_OF, 0);
        textLayoutParams.addRule(BELOW, R.id.provider_header_logo);
        textLayoutParams.addRule(ALIGN_TOP, 0);
        textLayoutParams.setMargins(stdPadding, stdPadding, stdPadding, stdPadding);
        providerHeaderText.setLayoutParams(textLayoutParams);
        providerHeaderText.setMaxLines(1);
    }

}
