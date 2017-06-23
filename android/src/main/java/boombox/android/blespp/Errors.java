package boombox.android.blespp;

/**
 * Created by mriley on 11/20/16.
 */
public final class Errors {
	public static class TimeoutException extends RuntimeException {
	}
	public static class ConnectionClosedException extends RuntimeException {
	}

	public static void timeout() {
		throw new TimeoutException();
	}

	public static void closed() {
		throw new ConnectionClosedException();
	}
}
