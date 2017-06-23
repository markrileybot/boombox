package boombox.android;

import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.app.NotificationManagerCompat;
import boombox.android.blespp.Connection;
import boombox.android.blespp.Connection.State;
import boombox.android.blespp.GattInputStream;
import boombox.android.blespp.GattOutputStream;
import boombox.android.blespp.Scanner;
import boombox.proto.Error;
import boombox.proto.GetLaunchTubeState;
import boombox.proto.GetLaunchTubeStateResponse;
import boombox.proto.Launch;
import boombox.proto.LaunchResponse;
import boombox.proto.LaunchTube;
import boombox.proto.Message;
import boombox.proto.MessageType;
import boombox.proto.SetConfigResponse;
import boombox.proto.boomboxConstants;
import org.apache.thrift.TBase;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.transport.TIOStreamTransport;
import org.apache.thrift.transport.TMemoryBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LauncherService extends Service implements Handler.Callback {

	private static final byte[] FOOTER_MAGIC = {
			boomboxConstants.MAGIC0,
			boomboxConstants.MAGIC1,
			boomboxConstants.MAGIC2,
			boomboxConstants.MAGIC3};
	private static final Logger log = LoggerFactory.getLogger(LauncherService.class);

	private static final int MSG_START_SCAN = 0x001;
	private static final int MSG_STOP_SCAN = 0x002;
	private static final int MSG_OPEN_CONNECTION = 0x003;
	private static final int MSG_CLOSE_CONNECTION = 0x004;
	private static final int MSG_CLOSE = 0x005;
	private static final int MSG_REMOVE_CONNECTION = 0x006;
	private static final int MSG_ADD_CONNECTION = 0x007;
	private static final int MSG_INIT_STATE = 0x008;
	private static final int MSG_LAUNCH = 0x009;

	private final LauncherController launcherController = new LauncherController(this);

	private final Set<BluetoothDevice> devices = new HashSet<>(1);
	private BluetoothDevice device;
	private Connection connection;
	private Launcher launcher;

	private final Handler handler = new Handler(this);
	private LauncherListener launcherListener;
	private Notification notification;
	private NotificationManagerCompat notificationManager;
	private Scanner scanner;
	private boolean open;

	@Override
	public void onCreate() {
		log.warn("Launcher service init");
		notificationManager = NotificationManagerCompat.from(this);
		notification = new Builder(this)
				.setAutoCancel(false)
				.setOngoing(true)
				.setContentTitle("Launch Control")
				.setContentText("Idle")
				.build();
		startForeground(0, notification);
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
				log.info("Start scan");
				open = true;
				scanner.scan();
				handler.sendEmptyMessageDelayed(MSG_STOP_SCAN, 60000);
				break;
			case MSG_STOP_SCAN:
				log.info("Stop scan");
				scanner.stop();
				break;
			case MSG_OPEN_CONNECTION:
				log.info("Open connection");
				connection.open();
				break;
			case MSG_CLOSE_CONNECTION:
				log.info("Close connection");
				connection.close();
				break;
			case MSG_ADD_CONNECTION:
				log.info("Add connection");
				device = (BluetoothDevice) msg.obj;
				devices.add(device);
				Connection conn = new Connection.Builder()
						.setContext(LauncherService.this)
						.setDevice(device)
						.setTimeout(10000)
						.setCallback(this::onStateChanged)
						.build();
				connection = conn;
				handler.obtainMessage(MSG_OPEN_CONNECTION, conn).sendToTarget();
				break;
			case MSG_REMOVE_CONNECTION:
				log.info("Remove connection");
				devices.remove(device);
				device = null;
				connection = null;
				launcher = null;
				break;
			case MSG_INIT_STATE:
				try {
					GetLaunchTubeStateResponse response = launcherController.send(connection, new GetLaunchTubeState());
					launcher = new Launcher(connection.getDevice());
					launcher.getLaunchTubes().clear();
					if (response.isSetTubes()) {
						launcher.getLaunchTubes().addAll(response.tubes);
					}
					if (launcherListener != null) {
						launcherListener.onLauncherDiscovered(launcher);
					}
				} catch (Exception e) {
					log.error("Failed to get state", e);
					handler.obtainMessage(MSG_CLOSE_CONNECTION, connection).sendToTarget();
				}
				break;
			case MSG_LAUNCH:
				Launch launch = new Launch().setTubes((List<LaunchTube>) msg.obj);
				try {
					LaunchResponse response = launcherController.send(launcher, launch);
					if (launcherListener != null) {
						launcherListener.onLaunchComplete(launch, response);
					}
				} catch (Exception e) {
					log.error("Failed to launch", e);
					if (launcherListener != null) {
						launcherListener.onLaunchFailed(launch);
					}
				}
				break;
			case MSG_CLOSE:
				log.info("Close connections");
				open = false;
				handler.sendEmptyMessage(MSG_CLOSE_CONNECTION);
				break;
		}
		return true;
	}

	private void onStateChanged(Connection connection, State from, State to) {
		if (from.isOpening() && to == State.OPEN) {
			android.os.Message message = handler.obtainMessage(MSG_INIT_STATE, connection);
			handler.sendMessageDelayed(message, 1000);
		} else if (to == State.CLOSED) {
			if (open) {
				android.os.Message message = handler.obtainMessage(MSG_OPEN_CONNECTION, connection);
				handler.sendMessageDelayed(message, 1000);
			} else {
				handler.obtainMessage(MSG_REMOVE_CONNECTION, connection).sendToTarget();
			}
		}
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

		public void scan() {
			service.handler.sendEmptyMessage(MSG_START_SCAN);
		}

		public boolean isScanning() {
			return service.scanner.isScanning();
		}

		public void setLauncherListener(LauncherListener l) {
			service.launcherListener = l;
		}

		public void launch(List<LaunchTube> tubes) {
			service.handler.obtainMessage(MSG_LAUNCH, tubes).sendToTarget();
		}

		private <REP extends TBase> REP send(Launcher launcher, TBase req) throws Exception {
			return send(service.connection, req);
		}

		@SuppressWarnings("unchecked")
		private <REP extends TBase> REP send(Connection connection, TBase req) throws Exception {
			MessageType messageType = null;
			if (req instanceof Launch) {
				messageType = MessageType.LAUNCH;
			} else if (req instanceof GetLaunchTubeState) {
				messageType = MessageType.GET_STATE;
			} else {
				throw new RuntimeException("Invalid request " + req);
			}

			GattOutputStream outputStream = connection.getOutputStream();
			TMemoryBuffer buffer = new TMemoryBuffer(20);
			TCompactProtocol protocol = new TCompactProtocol(buffer);
			new Message()
					.setId(reqId++)
					.setType(messageType)
					.write(protocol);
			req.write(protocol);
			buffer.write(FOOTER_MAGIC);
			outputStream.write(buffer.getArray(), 0, buffer.length());

			GattInputStream inputStream = connection.getInputStream();
			inputStream.clear();
			protocol = new TCompactProtocol(new TIOStreamTransport(inputStream));
			Message message = new Message();
			message.read(protocol);
			switch (message.type) {
				case ERROR:
					Error error = new Error();
					error.read(protocol);
					throw new RuntimeException("Something went wrong " + error);
				case LAUNCH:
					LaunchResponse launchResponse = new LaunchResponse();
					launchResponse.read(protocol);
					return (REP) launchResponse;
				case GET_STATE:
					GetLaunchTubeStateResponse getLaunchTubeStateResponse = new GetLaunchTubeStateResponse();
					getLaunchTubeStateResponse.read(protocol);
					return (REP) getLaunchTubeStateResponse;
				case SET_CONFIG:
					SetConfigResponse setConfigResponse = new SetConfigResponse();
					setConfigResponse.read(protocol);
					return (REP) setConfigResponse;
				default:
					throw new RuntimeException("Invalid response " + message);
			}
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
}
