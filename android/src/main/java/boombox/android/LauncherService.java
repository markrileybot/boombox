package boombox.android;

import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.app.NotificationManagerCompat;
import boombox.android.blespp.Connection;
import boombox.android.blespp.Connection.State;
import boombox.android.blespp.GattInputStream;
import boombox.android.blespp.GattOutputStream;
import boombox.android.blespp.Scanner;
import boombox.android.proto.Launch;
import boombox.android.proto.LaunchTube;
import boombox.android.proto.Message;
import boombox.android.proto.Message.Type;
import boombox.android.proto.SequenceItem;
import com.google.common.io.LittleEndianDataInputStream;
import com.google.common.io.LittleEndianDataOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.Set;

public class LauncherService extends Service implements Handler.Callback {

	private static final Logger log = LoggerFactory.getLogger(LauncherService.class);

	private static final int MSG_START_SCAN = 0x001;
	private static final int MSG_STOP_SCAN = 0x002;
	private static final int MSG_OPEN_CONNECTION = 0x003;
	private static final int MSG_CLOSE_CONNECTION = 0x004;
	private static final int MSG_CLOSE = 0x005;
	private static final int MSG_REMOVE_CONNECTION = 0x006;
	private static final int MSG_ADD_CONNECTION = 0x007;
	private static final int MSG_INIT = 0x008;
	private static final int MSG_LAUNCH = 0x009;
	private static final int MSG_RESET = 0x0A;

	private final LauncherController launcherController = new LauncherController(this);
	private final LauncherStateController launcherState = new LauncherStateController();
	private final Set<BluetoothDevice> devices = new HashSet<>(1);
	private final Handler handler;

	private BluetoothDevice device;
	private Connection connection;
	private Launcher launcher;
	private State connectionState;

	private NotificationManagerCompat notificationManager;
	private Notification notification;
	private Scanner scanner;
	private boolean open;

	public LauncherService() {
		HandlerThread handlerThread = new HandlerThread("LauncherWorker");
		handlerThread.start();
		handler = new Handler(handlerThread.getLooper(),this);
	}

	@Override
	public void onCreate() {
		notificationManager = NotificationManagerCompat.from(this);
		notification = new Builder(this)
				.setAutoCancel(false)
				.setOngoing(true)
				.setContentTitle("Launch Control")
				.setContentText("Idle")
				.setSmallIcon(R.drawable.icon)
				.build();
		startForeground(1, notification);
		notificationManager.notify(1, notification);
		scanner = new Scanner(this, new ScannerCallback());
	}

	@Override
	public void onDestroy() {
	}

	@Override
	public IBinder onBind(Intent intent) {
		return launcherController;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		launcherController.close();
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean handleMessage(android.os.Message msg) {
		switch (msg.what) {
			case MSG_START_SCAN:
				handleStartScan();
				break;
			case MSG_STOP_SCAN:
				handleStopScan();
				break;
			case MSG_OPEN_CONNECTION:
				handleOpenConnection();
				break;
			case MSG_CLOSE_CONNECTION:
				handleCloseConnection();
				break;
			case MSG_ADD_CONNECTION:
				handleCreateConnection(msg);
				break;
			case MSG_REMOVE_CONNECTION:
				handleDestroyConnection();
				break;
			case MSG_INIT:
				handleLauncherInit();
				break;
			case MSG_LAUNCH:
				handleLauncherLaunch(msg);
				break;
			case MSG_RESET:
				handleLauncherReset();
				break;
			case MSG_CLOSE:
				handleClose();
				break;
		}
		return true;
	}

	private void handleClose() {
		log.info("Close connections");
		open = false;
		handler.sendEmptyMessage(MSG_CLOSE_CONNECTION);
	}

	private void handleLauncherReset() {
		launcherState.updateLauncherState(Launcher.State.BUSY);
		try {
			launcherController.send(launcher, new Message().setType(Type.RESET));
			for (LaunchTube launchTube : launcher.getLaunchTubes()) {
				launchTube.setState(LaunchTube.State.ARMED);
			}
		} catch (Exception e) {
			log.error("Failed to reset", e);
		} finally {
			launcherState.updateLauncherState(Launcher.State.IDLE);
		}
	}

	private void handleLauncherLaunch(android.os.Message msg) {
		launcherState.updateLauncherState(Launcher.State.BUSY);
		Launch launch = (Launch) msg.obj;
		try {
			launcherController.send(launcher, new Message().setPayload(launch));
			LaunchTubeGroup launchTubes = launcher.getLaunchTubes();
			for (SequenceItem i : launch.getSequence()) {
				launchTubes.getAt(i.getTube()).setState(LaunchTube.State.FIRED);
			}
			launcherState.onLaunchComplete(launch);
		} catch (Exception e) {
			log.error("Failed to launch", e);
			launcherState.onLaunchFailed(launch);
		} finally {
			launcherState.updateLauncherState(Launcher.State.IDLE);
		}
	}

	private void handleLauncherInit() {
		if (launcher == null || !launcher.getDevice().equals(connection.getDevice())) {
			launcher = new Launcher(connection.getDevice());
		}
		launcher.setState(Launcher.State.BUSY);
		try {
			launcherState.onFound(launcher);
			launcherController.send(connection, new Message().setType(Type.PING));
		} catch (Exception e) {
			log.error("Failed to get state", e);
			handler.obtainMessage(MSG_CLOSE_CONNECTION, connection).sendToTarget();
		} finally {
			launcherState.updateLauncherState(Launcher.State.IDLE);
		}
	}

	private void handleDestroyConnection() {
		log.info("Remove connection");
		Launcher old = launcher;
		devices.remove(device);
		device = null;
		connection = null;
		launcher = null;
		if (old != null) {
			old.setState(Launcher.State.CLOSED);
			launcherState.onLost(old);
		}
	}

	private void handleCreateConnection(android.os.Message msg) {
		log.info("Add connection");
		device = (BluetoothDevice) msg.obj;
		devices.add(device);
		connection = new Connection.Builder()
				.setContext(LauncherService.this)
				.setDevice(device)
				.setTimeout(5000)
				.setCallback(this::onStateChanged)
				.build();
		handler.obtainMessage(MSG_OPEN_CONNECTION, connection).sendToTarget();
	}

	private void handleCloseConnection() {
		if (connection != null) {
			connection.close();
		}
		launcherState.updateLauncherState(Launcher.State.CLOSED);
	}

	private void handleOpenConnection() {
		if (connection != null) {
			connection.open();
		}
		launcherState.updateLauncherState(Launcher.State.BUSY);
	}

	private void handleStopScan() {
		scanner.stop();
	}

	private void handleStartScan() {
		open = true;
		scanner.scan();
		handler.sendEmptyMessageDelayed(MSG_STOP_SCAN, 60000);
	}

	private void onStateChanged(Connection connection, State from, State to) {
		connectionState = to;
		if (from.isOpening() && to == State.OPEN) {
			android.os.Message message = handler.obtainMessage(MSG_INIT, connection);
			handler.sendMessageDelayed(message, 1000);
		} else if (to == State.CLOSED) {
			if (open) {
				android.os.Message message = handler.obtainMessage(MSG_OPEN_CONNECTION, connection);
				handler.sendMessageDelayed(message, 1000);
			} else {
				handler.obtainMessage(MSG_REMOVE_CONNECTION, connection).sendToTarget();
			}
		}
		launcherState.onStateChanged(launcher);
	}

	public static class LauncherController extends Binder {
		private final LauncherService service;
		private int reqId;

		private LauncherController(LauncherService service) {
			this.service = service;
		}

		public void close() {
			service.handler.sendEmptyMessage(MSG_STOP_SCAN);
			service.handler.sendEmptyMessage(MSG_CLOSE);
		}

		public void reset() {
			service.handler.sendEmptyMessage(MSG_RESET);
		}

		public State getState() {
			return service.connectionState == null ? State.CLOSED : service.connectionState;
		}

		public void scan() {
			service.handler.sendEmptyMessage(MSG_START_SCAN);
		}

		public boolean isScanning() {
			return service.scanner.isScanning();
		}

		public void setLauncherListener(LauncherListener l) {
			service.launcherState.setListener(l);
		}

		public void launch(Launch launch) {
			service.handler.obtainMessage(MSG_LAUNCH, launch).sendToTarget();
		}

		private Message send(Launcher launcher, Message req) throws Exception {
			return send(service.connection, req);
		}

		@SuppressWarnings("unchecked")
		private Message send(Connection connection, Message req) throws Exception {
			req.setSequence(reqId++);
			ByteArrayOutputStream out = new ByteArrayOutputStream(256);
			req.write(new LittleEndianDataOutputStream(out));
			GattOutputStream outputStream = connection.getOutputStream();
			outputStream.write(out.toByteArray(), 0, out.size());

			GattInputStream inputStream = connection.getInputStream();
			inputStream.clear();
			req.read(new LittleEndianDataInputStream(inputStream));

			if (req.getType() == Type.ERROR) {
				throw new RuntimeException("Something went wrong!");
			}
			return req;
		}
	}

	private class ScannerCallback implements Scanner.Callback {
		@Override
		public void onScanResult(Scanner scanner, int callbackType, ScanResult result) {
			BluetoothDevice device = result.getDevice();
			if (device.getType() == BluetoothDevice.DEVICE_TYPE_LE && !devices.contains(device)) {
				log.info("Scan discovered {}", device);
				handler.obtainMessage(MSG_ADD_CONNECTION, device).sendToTarget();
			}
		}

		@Override
		public void onScanFailed(Scanner scanner, int errorCode) {
		}
	}

	private final class LauncherStateController implements LauncherListener {

		private LauncherListener listener;

		public void setListener(LauncherListener listener) {
			this.listener = listener;
		}

		@Override
		public void onFound(Launcher launcher) {
			if (listener != null) {
				listener.onFound(launcher);
			}
		}

		@Override
		public void onLost(Launcher launcher) {
			if (listener != null) {
				listener.onLost(launcher);
			}
		}

		@Override
		public void onLaunchComplete(Launch request) {
			if (listener != null) {
				listener.onLaunchComplete(request);
			}
		}

		@Override
		public void onLaunchFailed(Launch request) {
			if (listener != null) {
				listener.onLaunchFailed(request);
			}
		}

		@Override
		public void onStateChanged(Launcher launcher) {
			if (listener != null) {
				listener.onStateChanged(launcher);
			}
		}

		public void updateLauncherState(Launcher.State newState) {
			if (launcher != null) {
				if (newState != null) {
					launcher.setState(newState);
				}
				if (listener != null) {
					listener.onStateChanged(launcher);
				}
			}
		}
	}
}
