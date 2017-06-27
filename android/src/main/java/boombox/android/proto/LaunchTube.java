package boombox.android.proto;

public class LaunchTube {

	public enum State {
		ARMED(0),
		FIRED(1);

		private final int value;

		private State(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}

		public static State findByValue(int value) {
			switch (value) {
				case 0:
					return ARMED;
				case 1:
					return FIRED;
				default:
					return null;
			}
		}
	}

	private byte position;
	private State state;

	public LaunchTube() {
	}

	public LaunchTube(byte position, State state) {
		this.position = position;
		this.state = state;
	}

	public byte getPosition() {
		return position;
	}

	public LaunchTube setPosition(byte position) {
		this.position = position;
		return this;
	}

	public State getState() {
		return state;
	}

	public LaunchTube setState(State state) {
		this.state = state;
		return this;
	}
}
