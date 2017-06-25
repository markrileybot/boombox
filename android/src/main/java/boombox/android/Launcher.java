package boombox.android;

import android.bluetooth.BluetoothDevice;

public class Launcher {

	public enum State {
		CLOSED, BUSY, IDLE
	}

	private final LaunchTubeGroup launchTubes = new LaunchTubeGroup(8, 2);
	private final BluetoothDevice device;
	private State state = State.IDLE;

	public Launcher(BluetoothDevice device) {
		this.device = device;
	}

	public BluetoothDevice getDevice() {
		return device;
	}

	public String getName() {
		return device.getName();
	}

	public LaunchTubeGroup getLaunchTubes() {
		return launchTubes;
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
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
