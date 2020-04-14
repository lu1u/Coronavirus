package com.lpi.coronavirus.GPS;

import android.content.Context;
import android.content.Intent;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

/***
 * Fonctions utilitaires pour manipuler les Location du systeme de geolocalisation Android
 * Lucien Pilloni 2020
 */
public class LocationUtil
{
	private static final @NonNull
	String PROVIDER = ".provider";
	private static final @NonNull
	String ALTITUDE = ".altitude";
	private static final @NonNull
	String LATITUDE = ".latitude";
	private static final @NonNull
	String LONGITUDE = ".longitude";

	/***
	 * Ajoute une Location dans les extras d'un Intent
	 * @param loc
	 * @param intent
	 * @param nom Nom de la Location
	 */
	public static void locationToIntent(final @Nullable Location loc, @NonNull Intent intent, final @NonNull String nom)
	{
		if (loc != null)
		{
			intent.putExtra(nom + PROVIDER, loc.getProvider());
			intent.putExtra(nom + ALTITUDE, loc.getAltitude());
			intent.putExtra(nom + LATITUDE, loc.getLatitude());
			intent.putExtra(nom + LONGITUDE, loc.getLongitude());
		}
	}

	/***
	 * Recupere une Location dans un intent
	 * @param intent
	 * @param nom
	 * @return
	 */
	public static @Nullable
	Location intentToLocation(@NonNull Intent intent, final @NonNull String nom)
	{
		try
		{
			String provider = intent.getStringExtra(nom + PROVIDER);
			if (provider == null)
				return null;

			Location loc = new Location(provider);
			loc.setAltitude(intent.getDoubleExtra(nom + ALTITUDE, 0));
			loc.setLatitude(intent.getDoubleExtra(nom + LATITUDE, 0));
			loc.setLongitude(intent.getDoubleExtra(nom + LONGITUDE, 0));

			return loc;
		} catch (Exception e)
		{
			return null;
		}
	}

	/***
	 * Enregistre une Location dans un fichier log
	 * @param l
	 */
	public static void logLocation(@NonNull final Context context, @NonNull final Location l)
	{
		String path = /*(Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED?*/ context.getExternalFilesDir(null) /*:context.getFilesDir())*/
				+ "/gps log.txt";
		FileOutputStream stream = null;
		try
		{
			File file = new File(path);
			if (!file.exists())
			{
				stream = new FileOutputStream(path, false);
				stream.write("PROVIDER,TIME,LATITUDE,LONGITUDE,ALTITUDE,ACCURACY,BEARING,SPEED\n".getBytes());
			}
			else
				stream = new FileOutputStream(path, true);


			Calendar c = Calendar.getInstance();
			c.setTimeInMillis(l.getTime());
			String location = l.getProvider()
					+ "," + c.get(Calendar.HOUR_OF_DAY) + ":" + c.get(Calendar.MINUTE) + ":" + c.get(Calendar.SECOND) + ":" + c.get(Calendar.MILLISECOND)
					+ "," + l.getLatitude()
					+ "," + l.getLongitude()
					+ "," + l.getAltitude()
					+ "," + l.getAccuracy()
					+ "," + l.getBearing()
					+ "," + l.getSpeed()
					+ "\n";

			stream.write(location.getBytes());
		} catch (Exception e)
		{
			e.printStackTrace();
		} finally
		{
			try
			{
				if (stream!=null)
					stream.close();
			} catch (IOException e)
			{
				e.printStackTrace();
			}

		}
	}


}
