package boombox.android;


import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.PagerTitleStrip;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import boombox.android.proto.Launch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.ALogger;

public class MainActivity extends AppCompatActivity implements LauncherListener, OnPageChangeListener {

	static {
		ALogger.setLevel(Log.VERBOSE);
	}

	private static final Logger log = LoggerFactory.getLogger(MainActivity.class);

	private final LauncherServiceConnection connection = new LauncherServiceConnection(this, this);
	private Launcher launcher;
	private Launch launch = new Launch();

	private ProgressBar loading;
	private MenuItem fireButton;
	private MenuItem resetButton;
	private MenuItem progressButton;

	private SectionsPagerAdapter pagerAdapter;
	private ViewPager viewPager;
	private LaunchEditFragment currentFragment;
	private boolean enabled;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);
		setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

		pagerAdapter = new SectionsPagerAdapter(getFragmentManager());
		viewPager = (ViewPager) findViewById(R.id.container);
		viewPager.setAdapter(pagerAdapter);
		viewPager.addOnPageChangeListener(this);

		PagerTitleStrip titleStrip = (PagerTitleStrip) findViewById(R.id.pager_title_strip);
		View prevChild = titleStrip.getChildAt(0);
		View nextChild = titleStrip.getChildAt(titleStrip.getChildCount()-1);
		prevChild.setOnClickListener(v -> viewPager.setCurrentItem(viewPager.getCurrentItem()-1, true));
		nextChild.setOnClickListener(v -> viewPager.setCurrentItem(viewPager.getCurrentItem()+1, true));


		if (PermissionUtils.checkPermissions(this)) {
			connection.connect();
		} else {
			PermissionUtils.requestPermissions(this);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		connection.disconnect();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == PermissionUtils.REQUEST_CODE) {
			if(PermissionUtils.checkPermissions(this)) {
				log.info("Permission granted");
				connection.connect();
			} else {
				log.info("Permission denied");
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main_menu, menu);
		fireButton = menu.findItem(R.id.fire);
		resetButton = menu.findItem(R.id.reset);
		progressButton = menu.findItem(R.id.busy);
		View actionView = progressButton.getActionView();
		loading = (ProgressBar) actionView.findViewById(R.id.loading);
		setEnabled(enabled);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.fire) {
			// fire
			Launch local = launch;
			launch = new Launch();
			if (currentFragment != null) {
				currentFragment.setLaunch(launch);
			}

			if (!local.isEmpty()) {
				connection.launch(local);
			}
		} else if (item.getItemId() == R.id.reset) {
			// reset
			connection.reset();
		}
		return true;
	}

	@Override
	public void onFound(Launcher launcher) {
		runOnUiThread(() -> {
			this.launcher = launcher;
			getSupportActionBar().setSubtitle(launcher.getName());
			updateState();
		});
	}

	@Override
	public void onLost(Launcher launcher) {
		runOnUiThread(() -> {
			this.launcher = null;
			getSupportActionBar().setSubtitle(null);
			updateState();
		});
	}

	@Override
	public void onLaunchComplete(Launch request) {
		runOnUiThread(this::updateState);
	}

	@Override
	public void onLaunchFailed(Launch request) {
		runOnUiThread(this::updateState);
	}

	@Override
	public void onStateChanged(Launcher launcher) {
		runOnUiThread(this::updateState);
	}

	private void updateState() {
		setEnabled(launcher != null && launcher.getState() == Launcher.State.IDLE);
		if (currentFragment != null) {
			currentFragment.setLauncher(launcher);
			currentFragment.updateState();
		}
	}

	private void setEnabled(boolean enabled) {
		this.enabled = enabled;
		if (fireButton != null) {
			fireButton.setVisible(enabled);
			resetButton.setVisible(enabled);
			progressButton.setVisible(!enabled);
			loading.setVisibility(enabled ? View.GONE : View.VISIBLE);
		}
	}

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
	}

	@Override
	public void onPageSelected(int position) {
	}

	@Override
	public void onPageScrollStateChanged(int state) {
		if (state == ViewPager.SCROLL_STATE_IDLE) {
			updateState();
		}
	}

	private final class SectionsPagerAdapter extends FragmentPagerAdapter {

		private SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public void setPrimaryItem(ViewGroup container, int position, Object object) {
			super.setPrimaryItem(container, position, object);
			LaunchEditFragment fragment = (LaunchEditFragment) object;
			if (fragment != currentFragment) {
				currentFragment = fragment;
				currentFragment.setEnabled(enabled);
				currentFragment.setLaunch(launch);
				currentFragment.setLauncher(launcher);
				currentFragment.setConnection(connection);
				currentFragment.updateState();
			}
		}

		@Override
		public Fragment getItem(int position) {
			switch (position) {
				case 0: return new TubeSelectFragment();
				case 1: return new IntervalSetFragment();
				case 2: return new IntervalTapFragment();
			}
			return null;
		}

		@Override
		public int getCount() {
			return 3;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
				case 0:
					return "Select Them Tubes!";
				case 1:
					return "Set Them Intervals!";
				case 2:
					return "Tap Them Intervals!";
			}
			return null;
		}
	}
}
