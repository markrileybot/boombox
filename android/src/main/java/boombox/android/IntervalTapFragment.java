package boombox.android;

import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import boombox.android.proto.Launch;
import boombox.android.proto.SequenceItem;

import java.util.List;

public class IntervalTapFragment extends LaunchEditFragment implements OnClickListener {

	private int next = -1;
	private long lastTapTime = 0;
	private View tapView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.frag_interval_tap, container, false);
		tapView = rootView.findViewById(R.id.tap);
		tapView.setOnClickListener(this);
		return rootView;
	}

	@Override
	public void updateState() {
		lastTapTime = 0;
		next = -1;
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
			} else if (next < launch.size()) {
				SequenceItem sequenceItem = launch.get(next);
				sequenceItem.setInterval((short) Math.min(Short.MAX_VALUE, Math.max(0, now - lastTapTime)));
				next++;
			}
			lastTapTime = now;
		}
	}
}
