package boombox.android;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.GridView;
import android.widget.ToggleButton;
import boombox.android.proto.LaunchTube;

public class TubeSelectFragment extends LaunchEditFragment implements OnCheckedChangeListener {

	private final LauncherAdapter launcherAdapter = new LauncherAdapter();
	private GridView tubeGroupView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.frag_tube_select, container, false);
		tubeGroupView = (GridView) rootView.findViewById(R.id.tube_group);
		tubeGroupView.setAdapter(launcherAdapter);
		return rootView;
	}

	@Override
	public void updateState() {
		super.updateState();
		launcherAdapter.update();
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		LaunchTube tube = (LaunchTube) buttonView.getTag();
		if (isChecked) {
			getLaunch().addLaunchTube(tube);
		} else {
			getLaunch().removeLaunchTube(tube);
		}
	}

	private final class LauncherAdapter extends BaseAdapter {

		public void update() {
			if (tubeGroupView != null) {
				Launcher launcher = getLauncher();
				tubeGroupView.setNumColumns(launcher == null ? 0 : launcher.getLaunchTubes().getCols());
			}
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			Launcher launcher = getLauncher();
			return launcher == null ? 0 : launcher.getLaunchTubes().getSize();
		}

		@Override
		public Object getItem(int position) {
			Launcher launcher = getLauncher();
			return launcher == null ? null : launcher.getLaunchTubes().getAt(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = getActivity().getLayoutInflater()
						.inflate(R.layout.launch_tube_item, parent,false);
			}
			ToggleButton tubeButton = (ToggleButton) convertView.findViewById(R.id.position);
			LaunchTube tube = getLauncher().getLaunchTubes().getAt(position);
			String label = String.valueOf(tube.getPosition());
			tubeButton.setTextOff(label);
			tubeButton.setTextOn(label);
			tubeButton.setText(label);
			tubeButton.setEnabled(TubeSelectFragment.this.isEnabled()
					&& tube.getState() != LaunchTube.State.FIRED);
			tubeButton.setChecked(getLaunch().get(tube.getPosition()) != null);
			tubeButton.setTag(tube);
			tubeButton.setOnCheckedChangeListener(TubeSelectFragment.this);
			return convertView;
		}
	}
}
