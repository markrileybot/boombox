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

public class SetDelayFragment extends LaunchEditFragment implements OnSeekBarChangeListener {

	private final DelayAdapter delayAdapter = new DelayAdapter();
	private SeekBar totalTime;
	private ListView delayList;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.frag_delay_set, container, false);
		totalTime = (SeekBar) rootView.findViewById(R.id.total_time);
		totalTime.setOnSeekBarChangeListener(this);
		delayList = (ListView) rootView.findViewById(R.id.delay_list);
		delayList.setAdapter(delayAdapter);
		return rootView;
	}

	@Override
	public void updateState() {
		super.updateState();
		delayAdapter.update();
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if (fromUser) {
			TextView valueText = (TextView) ((View) seekBar.getParent()).findViewById(R.id.delay_value);
			if (seekBar == totalTime) {
				valueText.setText(formatDelay(progress));
				updateTotalTime(progress);
			} else {
				SequenceItem item = (SequenceItem) seekBar.getTag();
				int delayBefore = getLaunch().getDelayBefore(item);
				if (progress < delayBefore) progress = delayBefore;
				valueText.setText(formatDelay(item, progress));
				item.setDelay((short) (progress - delayBefore));
				seekBar.setProgress(progress);
				updateFollowingDelay(item);
			}
		}
	}

	private void updateTotalTime(int time) {
		int childCount = delayList.getChildCount();
		for (int i = 0; i < childCount; i++) {
			View childAt = delayList.getChildAt(i);
			SeekBar bar = (SeekBar) childAt.findViewById(R.id.delay);
			if (bar != null) {
				bar.setMax(time);
			}
		}
	}

	private void updateFollowingDelay(SequenceItem item) {
		Launch launch = getLaunch();
		boolean found = false;
		int childCount = delayList.getChildCount();
		for (int i = 0; i < childCount; i++) {
			View childAt = delayList.getChildAt(i);
			SeekBar bar = (SeekBar) childAt.findViewById(R.id.delay);
			if (bar != null) {
				SequenceItem next = (SequenceItem) bar.getTag();
				if (!found) {
					found = next == item;
				} else if (next != null) {
					int l = launch.getDelayBefore(next) + next.getDelay();
					bar.setProgress(l);
					((TextView) childAt.findViewById(R.id.delay_value))
							.setText(formatDelay(next, l));
				}
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
			return getLaunch().getSize();
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
			}

			int progress = getLaunch().getDelayBefore(sequenceItem) + sequenceItem.getDelay();
			((TextView) convertView.findViewById(R.id.delay_value)).setText(formatDelay(sequenceItem, progress));

			SeekBar seekBar = (SeekBar) convertView.findViewById(R.id.delay);
			seekBar.setTag(sequenceItem);
			seekBar.setMax(totalTime.getProgress());
			seekBar.setProgress(progress);
			seekBar.setOnSeekBarChangeListener(SetDelayFragment.this);
			seekBar.setEnabled(SetDelayFragment.this.isEnabled());
			return convertView;
		}
	}

	private static String formatDelay(SequenceItem item, long progress) {
		return "#" + item.getTube() + " after " + formatDelay(progress);
	}

	private static String formatDelay(long progress) {
		return String.valueOf(progress / 1000f) + "s";
	}
}
