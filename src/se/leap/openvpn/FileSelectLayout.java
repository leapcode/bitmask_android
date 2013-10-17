package se.leap.openvpn;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import se.leap.bitmaskclient.R;


public class FileSelectLayout extends LinearLayout implements OnClickListener {

	private TextView mDataView;
	private String mData;
	private Fragment mFragment;
	private int mTaskId;
	private Button mSelectButton;
	private boolean mBase64Encode;
	private String mTitle;
	private boolean mShowClear;

	public FileSelectLayout( Context context,AttributeSet attrset) {
		super(context,attrset);
		inflate(getContext(), R.layout.file_select, this);
		
		TypedArray ta = context.obtainStyledAttributes(attrset,R.styleable.FileSelectLayout);
		
		mTitle = ta.getString(R.styleable.FileSelectLayout_title);
		
		TextView tview = (TextView) findViewById(R.id.file_title);
		tview.setText(mTitle);
		
		mDataView = (TextView) findViewById(R.id.file_selected_item);
		mSelectButton = (Button) findViewById(R.id.file_select_button);
		mSelectButton.setOnClickListener(this);

	}

	public void setFragment(Fragment fragment, int i)
	{
		mTaskId = i;
		mFragment = fragment;
	}
	
	public void getCertificateFileDialog() {
		Intent startFC = new Intent(getContext(),FileSelect.class);
		startFC.putExtra(FileSelect.START_DATA, mData);
		startFC.putExtra(FileSelect.WINDOW_TITLE,mTitle);
		if(mBase64Encode)
			startFC.putExtra(FileSelect.DO_BASE64_ENCODE, true);
		if(mShowClear)
			startFC.putExtra(FileSelect.SHOW_CLEAR_BUTTON, true);
		mFragment.startActivityForResult(startFC,mTaskId);
	}

	
	public String getData() {
		return mData;
	}

	public void setData(String data) {
		mData = data;
		if(data==null) 
			mDataView.setText(mFragment.getString(R.string.no_data));
		else if(mData.startsWith(VpnProfile.INLINE_TAG))
			mDataView.setText(R.string.inline_file_data);
		else
			mDataView.setText(data);
		
	}

	@Override
	public void onClick(View v) {
		if(v == mSelectButton) {
			getCertificateFileDialog();
		}
	}

	public void setBase64Encode() {
		mBase64Encode =true;
	}

	public void setShowClear() {
		mShowClear=true;
	}
	
}
