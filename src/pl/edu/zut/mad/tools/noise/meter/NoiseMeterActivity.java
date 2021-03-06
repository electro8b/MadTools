package pl.edu.zut.mad.tools.noise.meter;

import java.text.DecimalFormat;

import org.achartengine.GraphicalView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.app.ActionBar.Tab;

import pl.edu.zut.mad.tools.MainActivity;
import pl.edu.zut.mad.tools.R;
import pl.edu.zut.mad.tools.compass.Compass;
import pl.edu.zut.mad.tools.converter.Converter;
import pl.edu.zut.mad.tools.inclinometer.Inclinometer;
import pl.edu.zut.mad.tools.lightmeter.LightMeter;
import pl.edu.zut.mad.tools.utils.GraphPoint;
import pl.edu.zut.mad.tools.utils.LinearGraph;
import pl.edu.zut.mad.tools.utils.TabCreator;
import pl.edu.zut.mad.tools.whereIsCar.WhereIsCar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

public class NoiseMeterActivity extends SherlockActivity implements
		ActionBar.TabListener, MicrophoneInputListener {
	private String[] navi_items;
	private boolean tabActive = false;

	private static final String TAG = "NoiseMeterActivity";

	private MicrophoneInput micInput; // The micInput object provides real time
										// audio.
	private TextView noiseLevel;
	private BarLevelDrawable mBarLevel;

	private int count = 0;
	private static GraphicalView view;
	private LinearGraph lineGraph;

	private final double mOffsetdB = 10; // Offset for bar, i.e. 0 lit LEDs at
											// 10 dB.
	// The Google ASR input requirements state that audio input sensitivity
	// should be set such that 90 dB SPL at 1000 Hz yields RMS of 2500 for
	// 16-bit samples, i.e. 20 * log_10(2500 / mGain) = 90.
	private final double mGain = 2500.0 / Math.pow(10.0, 90.0 / 20.0);
	private double mRmsSmoothed; // Temporally filtered version of RMS.
	private final double mAlpha = 0.9; // Coefficient of IIR smoothing filter
										// for RMS.

	// Variables to monitor UI update and check for slow updates.
	private volatile boolean mDrawing;
	private volatile int mDrawingCollided;

	// Wy��czenie przechodzenia telefonu w stan u�pienia
	// WakeLock
	private WakeLock mWakeLock = null;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setTheme(R.style.Theme_Sherlock);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_noise_meter);
		navi_items = getResources().getStringArray(R.array.navi_items);
		// Here the micInput object is created for audio capture.
		// It is set up to call this object to handle real time audio frames of
		// PCM samples. The incoming frames will be handled by the
		// processAudioFrame method below.
		micInput = new MicrophoneInput(this);

		lineGraph = new LinearGraph(getString(R.string.noise_bar_title), "",
				"dB", 120.0f, 0.0f, 60.0f, 0.0f);
		noiseLevel = (TextView) findViewById(R.id.noiseLevel);
		mBarLevel = (BarLevelDrawable) findViewById(R.id.bar_level_drawable_view);

		getSupportActionBar().setDisplayShowTitleEnabled(true);
		getSupportActionBar().setTitle("Mad Tools");
		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		TabCreator tc = new TabCreator(this);
		tc.createTab(2);

		tabActive = true;

		// WakeLock
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK,
				"noiseMeterActivity");

	}

	@Override
	protected void onStart() {
		super.onStart();
		view = lineGraph.getView(this);
		LinearLayout layout = (LinearLayout) findViewById(R.id.graphLayout);
		layout.addView(view);

	}

	@Override
	protected void onPause() {
		micInput.stop();
		super.onPause();

		// WakeLock
		mWakeLock.release();
	}

	@Override
	protected void onResume() {

		micInput.setSampleRate(8000);
		micInput.setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION);
		micInput.start();
		super.onResume();

		// WakeLock
		mWakeLock.acquire();
	}

	/**
	 * This method gets called by the micInput object owned by this activity. It
	 * first computes the RMS value and then it sets up a bit of code/closure
	 * that runs on the UI thread that does the actual drawing.
	 */
	@Override
	public void processAudioFrame(short[] audioFrame) {
		if (!mDrawing) {
			mDrawing = true;
			// Compute the RMS value. (Note that this does not remove DC).
			double rms = 0;
			for (int i = 0; i < audioFrame.length; i++) {
				rms += audioFrame[i] * audioFrame[i];
			}
			rms = Math.sqrt(rms / audioFrame.length);

			// Compute a smoothed version for less flickering of the display.
			mRmsSmoothed = mRmsSmoothed * mAlpha + (1 - mAlpha) * rms;
			final double rmsdB = 20.0 * Math.log10(mGain * mRmsSmoothed);

			// Set up a method that runs on the UI thread to update of the LED
			// bar
			// and numerical display.
			mBarLevel.post(new Runnable() {
				@Override
				public void run() {
					// The bar has an input range of [0.0 ; 1.0] and 10
					// segments.
					// Each LED corresponds to 8 dB.
					mBarLevel.setLevel((mOffsetdB + rmsdB) / 80);

					DecimalFormat df = new DecimalFormat("##");
					noiseLevel.setText(df.format(20 + rmsdB) + " dB");

					count++;
					GraphPoint p = new GraphPoint(count, 20 + rmsdB);
					lineGraph.addNewPoints(p);

					if (p.getX() > 60) {
						lineGraph.setXAxisMin(p.getX() - 60);
						lineGraph.setXAxisMax(p.getX());
					}

					view.repaint();

					mDrawing = false;
				}
			});
		} else {
			mDrawingCollided++;
			Log.v(TAG,
					"Level bar update collision, i.e. update took longer "
							+ "than 20ms. Collision count"
							+ Double.toString(mDrawingCollided));
		}
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		Log.e("TABNoise", tab.getText().toString());
		if (tabActive) {
			if (tab.getText().toString().equals(navi_items[0])) {
				Intent main = new Intent(this, MainActivity.class);
				startActivity(main);
				finish();
			} else if (tab.getText().toString().equals(navi_items[1])) {
				Intent compssIntent = new Intent(this, Compass.class);
				startActivity(compssIntent);
				finish();
			} else if (tab.getText().toString().equals(navi_items[2])) {

			} else if (tab.getText().toString().equals(navi_items[3])) {
				Intent inclinometerIntent = new Intent(this, Inclinometer.class);
				startActivity(inclinometerIntent);
				finish();
			} else if (tab.getText().toString().equals(navi_items[4])) {
				Intent lightMeterIntent = new Intent(this, LightMeter.class);
				startActivity(lightMeterIntent);
				finish();
			} else if (tab.getText().toString().equals(navi_items[5])) {
				Intent converterIntent = new Intent(this, Converter.class);
				startActivity(converterIntent);
				finish();
			} else if (tab.getText().toString().equals(navi_items[6])) {
				Intent whereIsCarIntent = new Intent(this, WhereIsCar.class);
				startActivity(whereIsCarIntent);
				finish();
			}
		}

	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {
		// TODO Auto-generated method stub

	}
}
