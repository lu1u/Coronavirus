package com.lpi.coronavirus;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.lpi.coronavirus.GPS.GPSLoggerService;
import com.lpi.coronavirus.GPS.LocationUtil;
import com.lpi.coronavirus.utils.GPSUtils;
import com.lpi.coronavirus.utils.MessageBoxUtils;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements LocationListener
{
	static final String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE};

	@Nullable
	CircularProgressBar _progressBar;
	Preferences _preferences;
	TextView _tvInfos, _tvDistanceMax;
	private LocationManager _locationManager;
	private Location _lastLocation;


	/***********************************************************************************************
	 * Creation de l'activity
	 * @param savedInstanceState
	 */
	@Override protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		// Obtenir les objets de l'interface utilisateur
		_progressBar = findViewById(R.id.circularProgressBar);
		_tvInfos = findViewById(R.id.textViewInfos);
		_tvDistanceMax = findViewById(R.id.textViewDistanceMax);

		// Preferences
		_preferences = Preferences.getInstance(this);

		_progressBar.setMinimum(0);
		_progressBar.setMaximum((int) _preferences.getDistanceMax());
		_progressBar.setValeur(0);
		_progressBar.setText(getString(R.string.message_gps_en_attente));

		requestPermissions(permissions, 0);

//		if (_preferences.getActif())
//			GPSLoggerService.startLocalisation(this);
	}

	/***********************************************************************************************
	 * Resultat de la demande de permissions Android
	 * @param requestCode
	 * @param permissions
	 * @param grantResults
	 */
	@Override
	public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults)
	{
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		updateGPS();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);

		return true;
	}

	@Override public boolean onPrepareOptionsMenu(final @NonNull Menu menu)
	{
		MenuItem m = menu.findItem(R.id.action_active);
		if (m != null)
			m.setChecked(_preferences.getActif());

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item)
	{
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		switch (item.getItemId())
		{
			case R.id.action_settings:
			{
				ParametresActivity.start(this, new ParametresActivity.ParametresActivityListener()
				{
					@Override public void onOk()
					{
						_tvDistanceMax.setText(getResources().getString(R.string.format_distance_max, _preferences.getDistanceMax() ));
						_progressBar.setMaximum(_preferences.getDistanceMax());
						GPSLoggerService.reInitConfiguration(MainActivity.this);
					}

					@Override public void onCancel()
					{

					}
				});
				return true;
			}

			case R.id.action_aide:
			{
				AideActivity.start(this);
				return true;
			}
			case R.id.action_apropos:
			{
				AProposActivity.start(this, new AProposActivity.AProposActivityListener()
				{
					@Override public void onOk()
					{
						//GPSLoggerService.reInitConfiguration(MainActivity.this);
					}
				});
				return true;
			}

			case R.id.action_active:
			{
				boolean active = !_preferences.getActif();
				_preferences.setActif(active);

				if (active)
				{
					if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
							&& ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
					{
						if (_locationManager == null)
							_locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

						long minTime = getMinTime(_preferences);
						float minDistance = getMinDistance(_preferences);

						_locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, this);
						_locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTime, minDistance, this);

						GPSLoggerService.startLocalisation(this);
					}
				}
				else
				{
					if (_locationManager != null)
						_locationManager.removeUpdates(this);

					GPSLoggerService.stopLocalisation(this);
				}
				return true;
			}
		}


		return super.onOptionsItemSelected(item);
	}

	@Override protected void onPause()
	{
		super.onPause();
		if (_locationManager != null)
			_locationManager.removeUpdates(this);
	}

	@Override protected void onResume()
	{
		super.onResume();
		_tvDistanceMax.setText(getResources().getString(R.string.format_distance_max, _preferences.getDistanceMax() ));

		if (_preferences.getActif())
			if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
					&& ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
			{
				if (_locationManager == null)
					_locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

				long minTime = getMinTime(_preferences);
				float minDistance = getMinDistance(_preferences);

				_locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, this);
				_locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTime, minDistance, this);

				if (_lastLocation != null)
					onLocationChanged(_lastLocation);
			}
	}

	/***
	 * Determine la distance minimum entre deux mesures GPS en fonction de la configuration
	 * @return
	 */
	public static float getMinDistance(@NonNull final Preferences preferences)
	{
		switch (preferences.getPrecision())
		{
			case 0:
				return 1;
			case 1:
				return 5;
			default:
				return 10;
		}
	}

	/***
	 * Determine le temps minimum entre deux mesures GPS en fonction de la configuration
	 * @return
	 */
	public static int getMinTime(@NonNull final Preferences preferences)
	{
		switch (preferences.getPrecision())
		{
			case 0:
				return 1;
			case 1:
				return 5;
			default:
				return 10;
		}
	}

	/***********************************************************************************************
	 * Reception d'une nouvelle position, callback du LocationManager
	 * @param location
	 */
	@Override public void onLocationChanged(final @NonNull Location location)
	{
		LocationUtil.logLocation(this, location);
		if (_preferences.getDomicile() == null)
		{
			_preferences.setDomicile(location);
		}

		if (GPSUtils.isBetterLocation(location, _lastLocation))
		{
			float distanceEnMetres = location.distanceTo(_preferences.getDomicile());
			_progressBar.setValeur(distanceEnMetres);
			_progressBar.setText(getResources().getString(R.string.format_distance, distanceEnMetres));

			Calendar c = Calendar.getInstance();
			c.setTimeInMillis(location.getTime());
			_tvInfos.setText(getResources().getString(R.string.format_infos,
					location.getProvider(),
					(int) location.getAccuracy(),
					c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND), c.get(Calendar.MILLISECOND)));
			_lastLocation = location;

			AlarmesManager.nouvellePosition(this, location);
		}

	}

	@Override public void onStatusChanged(final String s, final int i, final Bundle bundle)
	{

	}

	@Override public void onProviderEnabled(final String s)
	{

	}

	@Override public void onProviderDisabled(final String s)
	{

	}

	/***
	 * Reinitialiser la position du domicile
	 * @param v
	 */
	public void onClickSetHome(View v)
	{
		MessageBoxUtils.messageBox(this, getString(R.string.titre_definir_domicile), getString(R.string.question_definir_domicile), MessageBoxUtils.BOUTON_CANCEL | MessageBoxUtils.BOUTON_OK, new MessageBoxUtils.Listener()
		{
			@Override public void onOk()
			{
				_preferences.setDomicile(null); // Sera reinitialisé au prochain top GPS
				_progressBar.setValeurMax(0);
				_progressBar.setValeur(0);
				_progressBar.setText(getString(R.string.message_gps_en_attente));
				GPSLoggerService.setDomicile(MainActivity.this, null);
			}

			@Override public void onCancel()
			{

			}
		});
	}

	/**
	 * Actif/desactive le suivi GPS
	 */
	private void updateGPS()
	{
		boolean actif = _preferences.getActif();
		if (actif)
			GPSLoggerService.startLocalisation(this);
		else
			GPSLoggerService.stopLocalisation(this);
	}
}
