package com.lpi.coronavirus;

import android.content.Context;
import android.location.Location;
import android.media.MediaPlayer;

import androidx.annotation.NonNull;
import androidx.annotation.RawRes;

import com.lpi.coronavirus.utils.ResourcesUtils;

import java.util.Calendar;

import TextToSpeech.TTSManager;

/***
 * Verifie la distance au domicile et emet eventuellement une alarme
 */
public class AlarmesManager
{
	/***********************************************************************************************
	 * Verifie la distance au domicile et emet eventuellement une alarme
	 * @param context
	 * @param location
	 */
	public static void nouvellePosition(@NonNull final Context context, @NonNull final Location location)
	{
		Preferences preferences = Preferences.getInstance(context);
		if ( !preferences.getActif())
			// Inactif
			return;

		Location domicile = preferences.getDomicile();
		if (domicile == null)
			// Pas de domicile defini
			return;

		final int distanceEnMetre = (int) location.distanceTo(domicile);
		if (distanceEnMetre < preferences.getDistanceMax())
			// A distance autorisee
			return;

		long derniereSonnerie = preferences.getDerniereSonnerie();
		Calendar now = Calendar.getInstance();
		if ((now.getTimeInMillis() - derniereSonnerie) < (preferences.getTimeOutAlarme() * 1000))
			// Une alarme a deja ete emise il n'y a pas longtemps
			return;

		// Emettre l'alarme
		preferences.setDerniereSonnerie(now.getTimeInMillis());

		final int[] ids = ResourcesUtils.getIntArray(context, R.array.sonneries_id);
		if (ids!=null)
		{
			final int iSonnerie = preferences.getAlarme();
			if (iSonnerie >= 0 && iSonnerie < ids.length)
				AlarmeSonore(context, ids[iSonnerie]);
		}
		TTSManager.speak(context, context.getResources().getString(R.string.distanceDepassee, distanceEnMetre));
	}


	/***********************************************************************************************
	 * Emettre une sonnerie pour indiquer qu'on est trop loin
	 * @param context
	 * @param idSonnerie : id de la ressource raw
	 */
	private static void AlarmeSonore(@NonNull final Context context, @RawRes  int idSonnerie)
	{
		MediaPlayer m = MediaPlayer.create(context, idSonnerie);
		m.start();
	}
}
