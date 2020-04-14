package com.lpi.coronavirus;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Preferences
{
	private static final String PREFERENCES = Preferences.class.getName();
	private static final String PREF_DOMICILE = "domicile";
	private static final String PREF_ACTIF = "actif";
	private static final String PREF_PRECISION = "precision";
	private static final String PREF_TIMEOUT_ALARME = "timeoutAlarm";
	private static final String PREF_DISTANCE_MAX = "distanceMax";
	private static final String PREF_ALARME = "alarme";
	private static final String PREF_DERNIERE_SONNERIE = "derniereSonnerie";
	private static final String PREF_ALARME_POSITION = "reveillerPourPosition";
	@NonNull
	final SharedPreferences settings;
	@NonNull
	final SharedPreferences.Editor editor;

	private Location _domicile;
	private boolean _actif;
	private static Preferences _instance;
	private int _precision;
	private long _timeoutAlarm;
	private float _distanceMax;
	private int _alarme;
	private long _derniereSonnerie;
	private int _delaiAlarme;

	public static synchronized Preferences getInstance(@NonNull final Context context)
	{
		if ( _instance == null)
			_instance = new Preferences(context);

		return _instance;

	}

	private Preferences(@NonNull final Context context)
	{
		settings = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
		editor = settings.edit();

		_domicile = locationFromString(settings.getString(PREF_DOMICILE, ""));
		_actif = settings.getBoolean(PREF_ACTIF, true);
		_precision = settings.getInt(PREF_PRECISION, 0);
		_timeoutAlarm = settings.getLong(PREF_TIMEOUT_ALARME, 30);
		_distanceMax = settings.getFloat(PREF_DISTANCE_MAX, 1000);
		_alarme = settings.getInt(PREF_ALARME, 0);
		_derniereSonnerie = settings.getLong(PREF_DERNIERE_SONNERIE, 0);
		_delaiAlarme = settings.getInt(PREF_ALARME_POSITION, 1);
	}

	public synchronized void setDomicile(@NonNull final Location l)
	{
		_domicile = l;
		String value = toString(l);
		editor.putString(PREF_DOMICILE, value);
		editor.apply();
	}
	public @Nullable Location getDomicile()
	{
		return _domicile;
	}




	public boolean getActif()
	{
		return _actif;
	}

	public synchronized void setActif(boolean v)
	{
		_actif = v;
		editor.putBoolean(PREF_ACTIF, v);
		editor.apply();
	}

	public int getPrecision()
	{
		return _precision;
	}


	public synchronized void setPrecision(int v)
	{
		_precision = v;
		editor.putInt(PREF_PRECISION, v);
		editor.apply();
	}

	public long getTimeOutAlarme()
	{
		return _timeoutAlarm;
	}


	public synchronized void setTimeoutAlarme(long v)
	{
		_timeoutAlarm = v;
		editor.putLong(PREF_TIMEOUT_ALARME, v);
		editor.apply();
	}

	public float getDistanceMax() { return _distanceMax ;}

	public synchronized void setDistanceMax(float v)
	{
		_distanceMax = v;
		editor.putFloat(PREF_DISTANCE_MAX, v);
		editor.apply();
	}

	public int getAlarme()
	{
		return _alarme;
	}


	public synchronized void setAlarme(int v)
	{
		_alarme = v;
		editor.putInt(PREF_ALARME, v);
		editor.apply();
	}

	public long getDerniereSonnerie()
	{
		return _derniereSonnerie;
	}

	public synchronized void setDerniereSonnerie(long v)
	{
		_derniereSonnerie = v;
		editor.putLong(PREF_DERNIERE_SONNERIE, v);
		editor.apply();
	}

	public int getDelaiAlarmes()
	{
		return _delaiAlarme;
	}

	public synchronized void setDelaiAlarmes(int v)
	{
		_delaiAlarme = v;
		editor.putInt(PREF_ALARME_POSITION, v);
		editor.apply();
	}

	@NonNull private String toString(@NonNull final Location l)
	{
		return l.getAltitude() + "/" + l.getLatitude() + "/" + l.getLongitude();
	}

	private @Nullable Location locationFromString(@Nullable String val)
	{
		if (val == null)
			return null;

		Location l = new Location(LocationManager.GPS_PROVIDER);
		String[] split = val.split("/");
		if (split.length < 3)
			return null;

		try
		{
			l.setAltitude(Double.parseDouble(split[0]));
			l.setLatitude(Double.parseDouble(split[1]));
			l.setLongitude(Double.parseDouble(split[2]));
		} catch (NumberFormatException e)
		{
			e.printStackTrace();
		}
		return l;
	}
}
