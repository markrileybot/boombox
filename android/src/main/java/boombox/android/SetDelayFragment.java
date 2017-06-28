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

public class SetDelayFragment extends LaunchEditFragment implements OnSeekBarChangeListener {

	private final DelayAdapter delayAdapter = new DelayAdapter();
	private final List<SeekBar> seekBars = new ArrayList<>(1);
	private SeekBar totalTime;
	private int maxProgress;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.frag_delay_set, container, false);
		totalTime = (SeekBar) rootView.findViewById(R.id.total_time);
		totalTime.setOnSeekBarChangeListener(this);
		ListView delayList = (ListView) rootView.findViewById(R.id.delay_list);
		delayList.setAdapter(delayAdapter);
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
		delayAdapter.update();
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if (fromUser) {
			if (seekBar == totalTime) {
				updateTotalTime(progress);
			} else {
				SequenceItem item = (SequenceItem) seekBar.getTag();
				long delayBefore = getLaunch().getDelayBefore(item);
				item.setDelay((short) (progress - delayBefore));
				maxProgress = Math.max(totalTime.getProgress(), Math.max(progress, maxProgress));
				updateFollowingDelay(item);
			}
		}
	}

	private void updateTotalTime(int time) {
		for (SeekBar bar : seekBars) {
			bar.setMax(time);
		}
	}

	private void updateFollowingDelay(SequenceItem item) {
		Launch launch = getLaunch();
		boolean found = false;
		for (SeekBar bar : seekBars) {
			SequenceItem next = (SequenceItem) bar.getTag();
			if (!found) {
				found = next == item;
			} else if (next != null) {
				bar.setProgress((int) (launch.getDelayBefore(next) + next.getDelay()));
			}
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {

	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {

	}

	private final class DelayAdapter extends BaseAdapter {

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
						.inflate(R.layout.delay_set_item, parent,false);
				seekBars.add((SeekBar) convertView.findViewById(R.id.delay));
			}

			((TextView) convertView.findViewById(R.id.tube)).setText(
					String.valueOf(sequenceItem.getTube()));

			int progress = (int) (getLaunch().getDelayBefore(sequenceItem) + sequenceItem.getDelay());
			SeekBar seekBar = (SeekBar) convertView.findViewById(R.id.delay);
			seekBar.setTag(sequenceItem);
			seekBar.setMax(Math.max(progress + 1000, totalTime.getProgress()));
			seekBar.setProgress(progress);
			seekBar.setOnSeekBarChangeListener(SetDelayFragment.this);
			seekBar.setEnabled(SetDelayFragment.this.isEnabled());
			return convertView;
		}
	}
}
