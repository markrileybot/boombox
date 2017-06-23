package boombox.android;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ToggleButton;
import boombox.android.LauncherService.LauncherController;
import boombox.proto.Launch;
import boombox.proto.LaunchResponse;
import boombox.proto.LaunchTube;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.ALogger;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements LauncherListener {

	static {
		ALogger.setLevel(Log.VERBOSE);
	}

	private static final Logger log = LoggerFactory.getLogger(MainActivity.class);

	private final LauncherServiceConnection connection = new LauncherServiceConnection();

	private GridView tubeGroupView;
	private LaunchTubeGroup tubeGroup;
	private MenuItem fireButton;
	private MenuItem resetButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		tubeGroup = new LaunchTubeGroup(8, 2);
		tubeGroupView = (GridView) findViewById(R.id.tube_group);
		tubeGroupView.setNumColumns(tubeGroup.getCols());
		tubeGroupView.setAdapter(new TubeGroupAdapter(tubeGroup));

		if (PermissionUtils.checkPermissions(this)) {
			connection.connect();
		} else {
			PermissionUtils.requestPermissions(this);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		connection.disconnect();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == PermissionUtils.REQUEST_CODE) {
			if(PermissionUtils.checkPermissions(this)) {
				log.info("Permission granted");
				connection.connect();
			} else {
				log.info("Permission denied");
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main_menu, menu);
		fireButton = menu.findItem(R.id.fire);
		resetButton = menu.findItem(R.id.reset);
		fireButton.setVisible(false);
		resetButton.setVisible(false);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (connection.isConnected()) {
			if (item.getItemId() == R.id.fire) {
				// fire
				List<LaunchTube> tubesToFire = new ArrayList<>(1);
				int childCount = tubeGroupView.getChildCount();
				for (int i = 0; i < childCount; i++) {
					ToggleButton tubeButton = (ToggleButton) tubeGroupView.getChildAt(i);
					tubeButton.setEnabled(false);
					if (tubeButton.isChecked()) {
						tubesToFire.add((LaunchTube) tubeButton.getTag());
					}
				}
				if (!tubesToFire.isEmpty()) {
					fireButton.setVisible(false);
					connection.launcherController.launch(tubesToFire);
				}
			} else if (item.getItemId() == R.id.reset) {
				// reset
				fireButton.setVisible(true);
				resetButton.setVisible(true);
				resetTubeButtonState();
			}
		}
		return true;
	}

	@Override
	public void onLauncherDiscovered(Launcher launcher) {
		fireButton.setVisible(true);
		resetButton.setVisible(true);
	}

	@SuppressWarnings("SuspiciousMethodCalls")
	@Override
	public void onLaunchComplete(Launch request, LaunchResponse response) {
		fireButton.setVisible(true);
		resetButton.setVisible(true);
		resetTubeButtonState();

		if (request.isSetTubes()) {
			List<LaunchTube> tubes = request.getTubes();
			int childCount = tubeGroupView.getChildCount();
			for (int i = 0; i < childCount; i++) {
				ToggleButton tubeButton = (ToggleButton) tubeGroupView.getChildAt(i);
				tubeButton.setEnabled(!tubes.contains(tubeButton.getTag()));
			}
		}
	}

	private void resetTubeButtonState() {
		int childCount = tubeGroupView.getChildCount();
		for (int i = 0; i < childCount; i++) {
			ToggleButton tubeButton = (ToggleButton) tubeGroupView.getChildAt(i);
			tubeButton.setEnabled(true);
			tubeButton.setChecked(false);
		}
	}

	@Override
	public void onLaunchFailed(Launch request) {
		fireButton.setVisible(true);
		resetButton.setVisible(true);
	}

	private final class TubeGroupAdapter extends BaseAdapter {
		private final LaunchTubeGroup group;

		private TubeGroupAdapter(LaunchTubeGroup group) {
			this.group = group;
		}

		@Override
		public int getCount() {
			return group.getSize();
		}

		@Override
		public Object getItem(int position) {
			return group.getAt(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = getLayoutInflater().inflate(R.layout.launch_tube_item, parent,false);
			}
			ToggleButton tubeButton = (ToggleButton) convertView.findViewById(R.id.position);
			LaunchTube tube = group.getAt(position);
			tubeButton.setTextOff(String.valueOf(tube.position));
			tubeButton.setTextOn(String.valueOf(tube.position));
			tubeButton.setText(String.valueOf(tube.position));
			tubeButton.setTag(tube);
			return convertView;
		}
	}

	private class LauncherServiceConnection implements ServiceConnection {

		private LauncherController launcherController;
		private boolean connected;

		public void connect() {
			log.warn("Binding to launcher service....");
			if(bindService(new Intent(getApplicationContext(), LauncherService.class), this, BIND_AUTO_CREATE)) {
			}
		}

		public void disconnect() {
			unbindService(this);
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder binder) {
			log.warn("Service connected.  Scanning....");
			launcherController = (LauncherController) binder;
			launcherController.setLauncherListener(MainActivity.this);
			connected = true;
			launcherController.scan();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			connected = false;
		}

		public boolean isConnected() {
			return connected;
		}
	}
}
