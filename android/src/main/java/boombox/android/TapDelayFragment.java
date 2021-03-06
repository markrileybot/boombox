package boombox.android;

import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import boombox.android.proto.Launch;
import boombox.android.proto.SequenceItem;

public class TapDelayFragment extends LaunchEditFragment implements OnClickListener {

	private int next = -1;
	private long lastTapTime = 0;
	private TextView tapView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.frag_delay_tap, container, false);
		tapView = (TextView) rootView.findViewById(R.id.tap);
		tapView.setOnClickListener(this);
		return rootView;
	}

	@Override
	public void updateState() {
		lastTapTime = 0;
		next = -1;
		updateTapsRemaining();
		super.updateState();
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		if (tapView != null) {
			tapView.setEnabled(enabled);
		}
	}

	@Override
	public void onClick(View v) {
		Launch launch = getLaunch();
		if (!launch.isEmpty()) {
			long now = SystemClock.elapsedRealtime();
			if (next == -1) {
				next = 1;
			} else if (next < launch.getSize()) {
				SequenceItem sequenceItem = launch.get(next);
				sequenceItem.setDelay((short) Math.min(Short.MAX_VALUE, Math.max(0, now - lastTapTime)));
				next++;
			}
			lastTapTime = now;
			updateTapsRemaining();
		}
	}

	private void updateTapsRemaining() {
		if (tapView != null) {
			Launch launch = getLaunch();
			int size = launch.getSize();
			int tappedTimes = Math.max(next, 0);
			tapView.setText("Tap " + (size - tappedTimes) + " more time(s)!");
		}
	}
}
