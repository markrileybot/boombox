package boombox.android;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import boombox.android.LauncherService.LauncherController;
import boombox.android.blespp.Connection.State;
import boombox.android.proto.Launch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LauncherServiceConnection implements ServiceConnection {

	private static final Logger log = LoggerFactory.getLogger(LauncherServiceConnection.class);

	private final Context context;
	private final LauncherListener listener;
	private LauncherController launcherController;
	private boolean connected;

	public LauncherServiceConnection(Context context, LauncherListener listener) {
		this.context = context;
		this.listener = listener;
	}

	public void connect() {
		log.warn("Binding to launcher service....");
		if(context.bindService(new Intent(context.getApplicationContext(), LauncherService.class), this, Context.BIND_AUTO_CREATE)) {
		}
	}

	public void disconnect() {
		context.unbindService(this);
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder binder) {
		log.warn("Service connected.  Scanning....");
		connected = true;
		launcherController = (LauncherController) binder;
		launcherController.setLauncherListener(listener);
		launcherController.scan();
	}

	public State getState() {
		return connected ? launcherController.getState() : null;
	}

	public void launch(Launch launch) {
		if (connected) {
			launcherController.launch(launch);
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
