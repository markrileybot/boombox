package boombox.android;


import boombox.android.proto.Launch;

public interface LauncherListener {
	void onFound(Launcher launcher);
	void onLost(Launcher launcher);
	void onLaunchComplete(Launch request);
	void onLaunchFailed(Launch request);
	void onStateChanged(Launcher launcher);
}
