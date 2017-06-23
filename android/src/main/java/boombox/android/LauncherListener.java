package boombox.android;

import boombox.proto.Launch;
import boombox.proto.LaunchResponse;

public interface LauncherListener {
	void onLauncherDiscovered(Launcher launcher);
	void onLaunchComplete(Launch request, LaunchResponse response);
	void onLaunchFailed(Launch request);
}
