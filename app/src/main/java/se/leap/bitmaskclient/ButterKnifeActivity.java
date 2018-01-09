package se.leap.bitmaskclient;

import android.support.v7.app.AppCompatActivity;
import android.view.View;

import butterknife.ButterKnife;

/**
 * Automatically inject with ButterKnife after calling setContentView
 */

public abstract class ButterKnifeActivity extends AppCompatActivity {

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        ButterKnife.inject(this);
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        ButterKnife.inject(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
