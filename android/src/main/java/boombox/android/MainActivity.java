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
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.GridView;
import android.widget.ToggleButton;
import boombox.android.LauncherService.LauncherController;
import boombox.android.blespp.Connection.State;
import boombox.proto.Launch;
import boombox.proto.LaunchResponse;
import boombox.proto.LaunchTube;
import boombox.proto.LaunchTubeState;
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
	private MenuItem fireButton;
	private MenuItem resetButton;
	private MenuItem progressButton;
	private LauncherAdapter launcherAdapter;
	private Launcher launcher;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		launcherAdapter = new LauncherAdapter();
		tubeGroupView = (GridView) findViewById(R.id.tube_group);
		tubeGroupView.setAdapter(launcherAdapter);

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
		progressButton = menu.findItem(R.id.busy);
		setEnabled(false);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.fire) {
			// fire
			List<LaunchTube> tubesToFire = new ArrayList<>(launcherAdapter.getSelection());
			if (!tubesToFire.isEmpty()) {
				connection.launch(tubesToFire);
				launcherAdapter.clearSelection();
				int childCount = tubeGroupView.getChildCount();
				for (int i = 0; i < childCount; i++) {
					((ToggleButton)tubeGroupView.getChildAt(i)).setChecked(false);
				}
			}
		} else if (item.getItemId() == R.id.reset) {
			// reset
			connection.reset();
		}
		launcherAdapter.update();
		return true;
	}

	@Override
	public void onFound(Launcher launcher) {
		this.launcher = launcher;
		updateState();
	}

	@Override
	public void onLost(Launcher launcher) {
		this.launcher = null;
		updateState();
	}

	@Override
	public void onLaunchComplete(Launch request, LaunchResponse response) {
		updateState();
	}

	@Override
	public void onLaunchFailed(Launch request) {
		updateState();
	}

	@Override
	public void onStateChanged(Launcher launcher) {
		updateState();
	}

	private void updateState() {
		runOnUiThread(() -> {
			setEnabled(launcher != null && launcher.getState() == Launcher.State.IDLE);
			launcherAdapter.update();
		});
	}

	private void setEnabled(boolean enabled) {
		fireButton.setVisible(enabled);
		resetButton.setVisible(enabled);
		progressButton.setVisible(!enabled);
	}

	private final class LauncherAdapter extends BaseAdapter implements OnCheckedChangeListener {
		private final List<LaunchTube> selection = new ArrayList<>(1);
		private boolean ready;

		public List<LaunchTube> getSelection() {
			return selection;
		}

		public void update() {
			ready = false;
			if (launcher != null) {
				tubeGroupView.setNumColumns(launcher.getLaunchTubes().getCols());
				ready = connection.isConnected() && launcher.getState() == Launcher.State.IDLE;
			}
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return launcher == null ? 0 : launcher.getLaunchTubes().getSize();
		}

		@Override
		public Object getItem(int position) {
			return launcher == null ? null : launcher.getLaunchTubes().getAt(position);
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
			LaunchTube tube = launcher.getLaunchTubes().getAt(position);
			tubeButton.setTextOff(String.valueOf(tube.position));
			tubeButton.setTextOn(String.valueOf(tube.position));
			tubeButton.setText(String.valueOf(tube.position));
			tubeButton.setEnabled(ready && tube.getState() != LaunchTubeState.FIRED);
			tubeButton.setTag(tube);
			tubeButton.setOnCheckedChangeListener(this);
			return convertView;
		}

		@SuppressWarnings("SuspiciousMethodCalls")
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			if (isChecked) {
				selection.add((LaunchTube) buttonView.getTag());
			} else {
				selection.remove(buttonView.getTag());
			}
		}

		public void clearSelection() {
			selection.clear();
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
			connected = true;
			launcherController = (LauncherController) binder;
			launcherController.setLauncherListener(MainActivity.this);
			launcherController.scan();
		}

		public State getState() {
			return connected ? launcherController.getState() : null;
		}

		public void launch(List<LaunchTube> tubes) {
			if (connected) {
				launcherController.launch(tubes);
			}
		}

		public void reset() {
			if (connected) {
				launcherController.reset();
			}
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
