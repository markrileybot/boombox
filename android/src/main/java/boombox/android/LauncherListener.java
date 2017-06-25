package boombox.android;

import boombox.proto.Launch;
import boombox.proto.LaunchResponse;

public interface LauncherListener {
	void onFound(Launcher launcher);
	void onLost(Launcher launcher);
	void onLaunchComplete(Launch request, LaunchResponse response);
	void onLaunchFailed(Launch request);
	void onStateChanged(Launcher launcher);
}
