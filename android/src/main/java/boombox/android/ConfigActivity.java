package boombox.android;

import android.os.Bundle;
import android.app.Activity;

public class ConfigActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_config);
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}
}
