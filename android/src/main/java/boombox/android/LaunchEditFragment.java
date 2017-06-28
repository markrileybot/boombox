package boombox.android;

import android.app.Fragment;
import boombox.android.proto.Launch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LaunchEditFragment extends Fragment {

	protected static final Logger log = LoggerFactory.getLogger(LaunchEditFragment.class);

	private Launch launch;
	private Launcher launcher;
	private LauncherServiceConnection connection;
	private boolean enabled;

	public void setLaunch(Launch launch) {
		this.launch = launch;
	}

	public Launch getLaunch() {
		if (launch == null) {
			launch = new Launch();
		}
		return launch;
	}

	public void setLauncher(Launcher launcher) {
		this.launcher = launcher;
	}

	public Launcher getLauncher() {
		return launcher;
	}

	public void setConnection(LauncherServiceConnection connection) {
		this.connection = connection;
	}

	public LauncherServiceConnection getConnection() {
		return connection;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void updateState() {
		setEnabled(launcher != null
				&& connection.isConnected()
				&& launcher.getState() == Launcher.State.IDLE);
	}

	@Override
	public void onResume() {
		super.onResume();
		updateState();
	}
}
