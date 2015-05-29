package se.leap.bitmaskclient.userstatus;


import android.content.Context;
import android.util.AttributeSet;

import mbanje.kurt.fabbutton.CircleImageView;
import se.leap.bitmaskclient.R;

public class FabButton extends mbanje.kurt.fabbutton.FabButton {


    public FabButton(Context context) {
        super(context);
    }

    public FabButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FabButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void init(Context context, AttributeSet attrs, int defStyle) {
        super.init(context, attrs, defStyle);
        super.init(context, attrs, defStyle);
    }

    private CircleImageView getImage() {
        return (CircleImageView) findViewById(R.id.fabbutton_circle);
    }
}
