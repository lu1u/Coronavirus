package com.lpi.coronavirus.GPS;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.lpi.coronavirus.AlarmesManager;
import com.lpi.coronavirus.MainActivity;
import com.lpi.coronavirus.Preferences;
import com.lpi.coronavirus.R;

/***
 * Service de geolocalisation GPS en tache de fond
 * Pour tenter de répondre au mieux aux limitations de geolocalisation Android
 * https://developer.android.com/about/versions/oreo/background-location-limits
 *
 * Lucien Pilloni 2020
 *
 * Demarrage: GPSLoggerService.startLocalisation(context)
 * Arret: GPSLoggerService.stopLocalisation(context)
 *
 *
 */
public class GPSLoggerService extends Service implements LocationListener
{

	private static final int TWO_MINUTES = 1000 * 60 * 2;
	private static final @NonNull
	String CHANNEL_ID = "1";

	public static final @NonNull
	String ACTION_NOUVELLE_POSITION = "action_nouvelle_position";
	private static final @NonNull
	String ACTION_START = "action_start";
	private static final @NonNull
	String ACTION_STOP = "action_stop";
	private static final @NonNull
	String ACTION_DOMICILE = "action_domicile";
	private static final @NonNull
	String ACTION_CONFIG = "configuration";

	public static final @NonNull
	String EXTRA_DOMICILE = "domicile";
	private static final String PROX_ALERT_INTENT = "proximityAlert";

	private LocationManager _locationManager;
	private Preferences _preferences;
	private Location _derniereLocation;
	@Nullable
	ProximityReceiver _proximityReceiver;
	AlarmeReceiver _alarmReceiver;
	boolean _dedans = true;

	/***********************************************************************************************
	 * Demarrage du service
	 * @param context
	 */
	public static void startLocalisation(@NonNull final Context context)
	{
		final Intent intent = new Intent(context, GPSLoggerService.class);
		intent.setAction(ACTION_START);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
			context.startForegroundService(intent);
		else
			context.startService(intent);
	}

	/***********************************************************************************************
	 * Arret du service
	 * @param context
	 */
	public static void stopLocalisation(@NonNull final Context context)
	{
		final Intent intent = new Intent(context, GPSLoggerService.class);
		intent.setAction(ACTION_STOP);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
			context.startForegroundService(intent);
		else
			context.startService(intent);
	}

	/***********************************************************************************************
	 * Initialisation de la position du domicile
	 * @param context
	 */
	public static void setDomicile(@NonNull final Context context, @Nullable final Location location)
	{
		final Intent intent = new Intent(context, GPSLoggerService.class);
		intent.setAction(ACTION_DOMICILE);
		LocationUtil.locationToIntent(location, intent, EXTRA_DOMICILE);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
			context.startForegroundService(intent);
		else
			context.startService(intent);
	}

	/***********************************************************************************************
	 * Relire la configuration
	 * @param context
	 */
	public static void reInitConfiguration(@NonNull final Context context)
	{
		final Intent intent = new Intent(context, GPSLoggerService.class);
		intent.setAction(ACTION_CONFIG);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
			context.startForegroundService(intent);
		else
			context.startService(intent);
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}

	/***********************************************************************************************
	 * Le service demarrage, avec une intent qui defini quelle action on veut effectuer:
	 * - ACTION_START
	 * - ACTION_STOP
	 * @param intent
	 * @param flags
	 * @param startId
	 * @return
	 */
	@Override
	public int onStartCommand(@NonNull Intent intent, int flags, int startId)
	{
		super.onStartCommand(intent, flags, startId);
		Notification n = createNotification();

		startForeground(1, n );

		if ( intent==null)
			return START_NOT_STICKY;

		String action = intent.getAction();
		try
		{
			if (ACTION_START.equals(action))
				onStartGPSCommand();
			else if (ACTION_STOP.equals(action))
				onStopGPSCommand();
			else if (ACTION_DOMICILE.equals(action))
				onSetHomeCommand(LocationUtil.intentToLocation(intent, EXTRA_DOMICILE));
			else if (ACTION_CONFIG.equals(action))
				onReadConfigCommand();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return START_STICKY;
	}

	/***
	 * Creer la notification exigee par Android pour demarrer un ForegroundService
	 * @return
	 */
	private Notification createNotification()
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
		{
			NotificationChannel serviceChannel = new NotificationChannel(CHANNEL_ID, "Foreground Service Channel", NotificationManager.IMPORTANCE_LOW);
			serviceChannel.setShowBadge(false);
			NotificationManager manager = getSystemService(NotificationManager.class);
			manager.createNotificationChannel(serviceChannel);


			Intent notificationIntent = new Intent(this, MainActivity.class);
			PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
			return new NotificationCompat.Builder(this, CHANNEL_ID)
					.setContentTitle(getResources().getString(R.string.app_name))
					.setPriority(NotificationManager.IMPORTANCE_LOW)
					.setDefaults(Notification.FLAG_FOREGROUND_SERVICE)
					.setContentText("géolocalisation")
					.setContentIntent(pendingIntent)
					.build();
		}
		else
			return null;
	}

	/***********************************************************************************************
	 * Relire la configuration
	 */
	private void onReadConfigCommand()
	{
		onStopGPSCommand();
		_preferences = Preferences.getInstance(this);
		onStartGPSCommand();
	}

	private void onSetHomeCommand(@Nullable final Location location)
	{
		_preferences.setDomicile( location );
	}

	/***********************************************************************************************
	 * Arret du GPS
	 */
	private void onStopGPSCommand()
	{
		//if (_locationManager != null)
		//_locationManager.removeUpdates(this);
		arreteProximityAlert();
	}

	/***********************************************************************************************
	 * Demarrage de la localisation GPS
	 */
	private void onStartGPSCommand()
	{
		if (_locationManager == null)
			_locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		lanceProximityAlert();
		lanceAlarmePosition();

	}

	/***
	 * Met une alarme toutes les minutes pour detecter la position
	 */
	private void lanceAlarmePosition()
	{
		BroadcastReceiver receiver = new BroadcastReceiver() {
			@Override public void onReceive( Context context, Intent intent )
			{
				context.unregisterReceiver( this ); // this == BroadcastReceiver, not Activity
				if ( _preferences.getActif())
				{
					PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
					PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Corona:wake");
					wl.acquire();

					Location ici = getLastKnownLocation(context);
					if (ici != null)
						AlarmesManager.nouvellePosition(context, ici);

					// Relancer l'alarme
					lanceAlarmePosition();

					wl.release();
				}
			}
		};

		this.registerReceiver( receiver, new IntentFilter("com.blah.blah.somemessage") );

		PendingIntent pintent = PendingIntent.getBroadcast( this, 0, new Intent("com.blah.blah.somemessage"), 0 );
		AlarmManager manager = (AlarmManager)(this.getSystemService( Context.ALARM_SERVICE ));

		// set alarm to fire 5 sec (1000*5) from now (SystemClock.elapsedRealtime())
		manager.set( AlarmManager.RTC_WAKEUP, 1000*60 * _preferences.getDelaiAlarmes(), pintent);
	}

	/***
	 * Mettre en place une alerte de proximité Android
	 * ATTENTION c'est très limité:
	 * https://developer.android.com/reference/android/location/LocationManager#addProximityAlert(double,%20double,%20float,%20long,%20android.app.PendingIntent)
	 */
	private void lanceProximityAlert()
	{
		Location domicile = _preferences.getDomicile();
		if (domicile != null && _locationManager != null)
			if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
					&& ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
			{
				_dedans = true;
				Intent intent = new Intent(PROX_ALERT_INTENT);
				PendingIntent proximityIntent = PendingIntent.getBroadcast(this, 0, intent, 0);

				_locationManager.addProximityAlert(
						domicile.getLatitude(), // the latitude of the central point of the alert region
						domicile.getLongitude(), // the longitude of the central point of the alert region
						_preferences.getDistanceMax(), // the radius of the central point of the alert region, in meters
						-1, // time for this proximity alert, in milliseconds, or -1 to indicate no expiration
						proximityIntent // will be used to generate an Intent to fire when entry to or exit from the alert region is detected
				                                  );

				IntentFilter filter = new IntentFilter(PROX_ALERT_INTENT);
				registerReceiver(_proximityReceiver, filter);
			}
	}

	private void arreteProximityAlert()
	{
		Location domicile = _preferences.getDomicile();
		if (domicile != null && _locationManager != null)
			if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
					&& ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
			{
				Intent intent = new Intent(PROX_ALERT_INTENT);
				PendingIntent proximityIntent = PendingIntent.getBroadcast(this, 0, intent, 0);

				_locationManager.removeProximityAlert(proximityIntent);
				//IntentFilter filter = new IntentFilter(PROX_ALERT_INTENT);
				unregisterReceiver(_proximityReceiver);
			}
	}

	/***
	 * Determine la distance minimum entre deux mesures GPS en fonction de la configuration
	 * @return
	 */
//	public static float getMinDistance(@NonNull final Preferences preferences)
//	{
//		switch (preferences.getPrecision())
//		{
//			case 0:
//				return 1;
//			case 1:
//				return 5;
//			default:
//				return 10;
//		}
//	}
//
//	/***
//	 * Determine le temps minimum entre deux mesures GPS en fonction de la configuration
//	 * @return
//	 */
//	public static int getMinTime(@NonNull final Preferences preferences)
//	{
//		switch (preferences.getPrecision())
//		{
//			case 0:
//				return 1;
//			case 1:
//				return 5;
//			default:
//				return 10;
//		}
//	}
	@RequiresApi(api = Build.VERSION_CODES.O)
	@Override
	public void onCreate()
	{
		super.onCreate();
		_preferences = Preferences.getInstance(this);

		_proximityReceiver = new ProximityReceiver();
		_alarmReceiver = new AlarmeReceiver();
	}

	/***********************************************************************************************
	 * Destruction du service
	 */
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		if (_proximityReceiver != null)
			unregisterReceiver(_proximityReceiver);

		if ( _alarmReceiver !=null)
			unregisterReceiver(_alarmReceiver);
	}


	/***********************************************************************************************
	 * Reception d'une nouvelle coordonnee GPS
	 * @param location
	 */
	@Override public void onLocationChanged(@NonNull final Location location)
	{
		LocationUtil.logLocation(this, location);

		if (!_preferences.getActif())
			return;

		if (_preferences.getDomicile()==null)
		{
			_preferences.setDomicile(location);
		}

		if (!isBetterLocation(location, _derniereLocation))
			return;

		_derniereLocation = location;
		AlarmesManager.nouvellePosition(this, location);

		// Avertir l'interface utilisateur de la nouvelle position
		try
		{
			Intent broadCastIntent = new Intent();
			broadCastIntent.setAction(ACTION_NOUVELLE_POSITION);
			LocationUtil.locationToIntent(location, broadCastIntent, EXTRA_DOMICILE);
			sendBroadcast(broadCastIntent);

		} catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}


	/***********************************************************************************************
	 * Emettre une sonnerie pour indiquer qu'on est trop loin
	 */
//	private void Bip()
//	{
//
//		MediaPlayer m = MediaPlayer.create(getApplicationContext(), R.raw.bip);
//		m.start();
//	}
	@Override public void onStatusChanged(final String s, final int i, final Bundle bundle)
	{

	}

	@Override public void onProviderEnabled(final String s)
	{

	}

	@Override public void onProviderDisabled(final String s)
	{

	}

	/**
	 * Determines whether one Location reading is better than the current Location fix
	 *
	 * @param location            The new Location that you want to evaluate
	 * @param currentBestLocation The current Location fix, to which you want to compare the new one
	 */
	protected boolean isBetterLocation(@NonNull final Location location, @Nullable final Location currentBestLocation)
	{
		if (currentBestLocation == null)
		{
			// A new location is always better than no location
			return true;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
		boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use the new location
		// because the user has likely moved
		if (isSignificantlyNewer)
		{
			return true;
			// If the new location is more than two minutes older, it must be worse
		}
		else if (isSignificantlyOlder)
		{
			return false;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(location.getProvider(),
				currentBestLocation.getProvider());

		// Determine location quality using a combination of timeliness and accuracy
		if (isMoreAccurate)
		{
			return true;
		}
		else if (isNewer && !isLessAccurate)
		{
			return true;
		}
		else return isNewer && !isSignificantlyLessAccurate && isFromSameProvider;
	}


	/**
	 * Checks whether two providers are the same
	 */
	private boolean isSameProvider(@Nullable String provider1, @Nullable String provider2)
	{
		if (provider1 == null)
		{
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}

	public class AlarmeReceiver extends BroadcastReceiver
	{
		/***
		 * Reception d'une alerte de proximite
		 * @param context
		 * @param intent
		 */
		@Override
		public void onReceive(Context context, Intent intent)
		{
			// Position actuelle
			Location ici = getLastKnownLocation(context);
			if (ici != null)
				AlarmesManager.nouvellePosition(context, ici);

			// Relancer une alarme
			if ( _preferences.getActif())
				lanceAlarmePosition();
		}
	}

	public class ProximityReceiver extends BroadcastReceiver
	{
		/***
		 * Reception d'une alerte de proximite
		 * @param context
		 * @param intent
		 */
		@Override
		public void onReceive(Context context, Intent intent)
		{
			final Boolean entering = intent.getBooleanExtra(LocationManager.KEY_PROXIMITY_ENTERING, false);

			if (entering)
			{   // On entre
//				if (!_dedans)
//				{
//					// On etait dehors, on vient de rentrer
//					// Rien
//				}
			}
			else
			{   // On sort
				if (_dedans)// On etait dedans, on vient de sortir

				{
					Location ici = getLastKnownLocation(context);
					if (ici != null)
						AlarmesManager.nouvellePosition(context, ici);
				}
			}

			_dedans = entering;

			// Relancer l'alerte de proximite apres un delai raisonnable
			Handler handler = new Handler();
			handler.postDelayed(new Runnable()
			{
				@Override
				public void run()
				{
					if ( _preferences.getActif())
						lanceProximityAlert();
				}
			}, MainActivity.getMinTime(_preferences) * 500);
		}
	}


	/***
	 * Retrouve la derniere position memorisee par le GPS, comme on peut obtenir une distance
	 * par deux fournisseurs (GPS et NETWORK), on prend la plus eloignee du domicile
	 * @param context
	 * @return
	 */
	private @Nullable Location getLastKnownLocation(final @NonNull Context context)
	{
		Location domicile = _preferences.getDomicile();
		if (domicile == null)
			return null;

		if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
				&& ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
		{
			Location iciNETWORK = _locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			Location iciGPS = _locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

			if (iciGPS == null)
				return iciNETWORK;

			if (iciNETWORK == null)
				return iciGPS;

			final float distanceNETWORK = domicile.distanceTo(iciNETWORK);
			final float distanceGPS = domicile.distanceTo(iciGPS);
			if (distanceGPS > distanceNETWORK)
				return iciGPS;
			else
				return iciNETWORK;
		}
		else
			return null;
	}
}
