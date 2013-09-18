package com.kedzie.vbox.app;

import android.os.Bundle;
import com.actionbarsherlock.view.Window;
import com.kedzie.vbox.VBoxApplication;
import roboguice.activity.RoboSherlockFragmentActivity;

/**
 * Base Activity for all application activities.  Enables indeterminate progress bar and disables it.
 * @author Marek Kędzierski
 */
public class BaseActivity extends RoboSherlockFragmentActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		requestWindowFeature(Window.FEATURE_PROGRESS);
		setProgressBarIndeterminateVisibility(false);
		setProgressBarVisibility(false);
	}

	@Override
	protected void onStart() {
		super.onStart();
		setProgressBarIndeterminateVisibility(false);
	}
	
	public VBoxApplication getApp() {
	    return (VBoxApplication)getApplication();
	}
}
