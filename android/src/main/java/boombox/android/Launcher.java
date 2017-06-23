package boombox.android;

import android.bluetooth.BluetoothDevice;
import boombox.proto.LaunchTube;

import java.util.ArrayList;
import java.util.List;

public class Launcher {

	private final List<LaunchTube> launchTubes = new ArrayList<>(1);
	private final BluetoothDevice device;

	public Launcher(BluetoothDevice device) {
		this.device = device;
	}

	public BluetoothDevice getDevice() {
		return device;
	}

	public List<LaunchTube> getLaunchTubes() {
		return launchTubes;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Launcher launcher = (Launcher) o;

		return device.equals(launcher.device);
	}

	@Override
	public int hashCode() {
		return device.hashCode();
	}
}
