package boombox.android;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import boombox.android.proto.Launch;
import boombox.android.proto.SequenceItem;

import java.util.ArrayList;
import java.util.List;

public class IntervalSetFragment extends LaunchEditFragment implements OnSeekBarChangeListener {

	private final IntervalAdapter intervalAdapter = new IntervalAdapter();
	private final List<SeekBar> seekBars = new ArrayList<>(1);
	private SeekBar totalTime;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.frag_interval_set, container, false);
		totalTime = (SeekBar) rootView.findViewById(R.id.total_time);
		totalTime.setOnSeekBarChangeListener(this);
		ListView intervalList = (ListView) rootView.findViewById(R.id.interval_list);
		intervalList.setAdapter(intervalAdapter);
		return rootView;
	}

	@Override
	public void onPause() {
		super.onPause();
		seekBars.clear();
	}

	@Override
	public void updateState() {
		super.updateState();
		intervalAdapter.update();
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if (fromUser) {
			if (seekBar == totalTime) {
				updateTotalTime(progress);
			} else {
				SequenceItem item = (SequenceItem) seekBar.getTag();
				long intervalBefore = getLaunch().getIntervalBefore(item);
				item.setInterval((short) (progress - intervalBefore));
				updateFollowingIntervals(item);
			}
		}
	}

	private void updateTotalTime(int time) {
		for (SeekBar bar : seekBars) {
			bar.setMax(time);
		}
	}

	private void updateFollowingIntervals(SequenceItem item) {
		Launch launch = getLaunch();
		boolean found = false;
		for (SeekBar bar : seekBars) {
			SequenceItem next = (SequenceItem) bar.getTag();
			if (!found) {
				found = next == item;
			} else if (next != null) {
				bar.setProgress((int) (launch.getIntervalBefore(next) + next.getInterval()));
			}
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {

	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {

	}

	private final class IntervalAdapter extends BaseAdapter {

		public void update() {
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return getLaunch().size();
		}

		@Override
		public Object getItem(int position) {
			return getLaunch().get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			SequenceItem sequenceItem = (SequenceItem) getItem(position);

			if (convertView == null) {
				convertView = getActivity().getLayoutInflater()
						.inflate(R.layout.interval_set_item, parent,false);
				seekBars.add((SeekBar) convertView.findViewById(R.id.interval));
			}

			((TextView) convertView.findViewById(R.id.tube)).setText(
					String.valueOf(sequenceItem.getTube()));

			SeekBar seekBar = (SeekBar) convertView.findViewById(R.id.interval);
			seekBar.setTag(sequenceItem);
			seekBar.setProgress((int) (getLaunch().getIntervalBefore(sequenceItem) + sequenceItem.getInterval()));
			seekBar.setOnSeekBarChangeListener(IntervalSetFragment.this);
			seekBar.setEnabled(IntervalSetFragment.this.isEnabled());
			return convertView;
		}
	}
}
