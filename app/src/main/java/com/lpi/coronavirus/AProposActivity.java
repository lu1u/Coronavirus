package com.lpi.coronavirus;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.Calendar;

/***
 * Affiche la boite A propos, avec quelques informations techniques sur l'application
 */
public class AProposActivity
{
	// A utiliser par l'appelant pour etre prevenu quand l'activity est fermee
	interface AProposActivityListener
	{
		void onOk();
	}

	public static void start(@NonNull final Activity context, final AProposActivity.AProposActivityListener listener)
	{

		////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		// Afficher la fenetre
		////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		final AlertDialog dialogBuilder = new AlertDialog.Builder(context).create();

		LayoutInflater inflater = context.getLayoutInflater();
		final View dialogView = inflater.inflate(R.layout.apropos_layout, null);

		final TextView tvDescription = dialogView.findViewById(R.id.textViewDescription);
		final Button btOk = dialogView.findViewById(R.id.buttonOk);

		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(BuildConfig.BUILD_TIME.getTime());

		tvDescription.setText(context.getResources().getString(R.string.about,
				BuildConfig.APPLICATION_ID,
				BuildConfig.VERSION_NAME,
				c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND), c.get(Calendar.YEAR), c.get(Calendar.MONTH)+1, c.get(Calendar.DAY_OF_MONTH),
				BuildConfig.BUILD_TYPE ));

		btOk.setOnClickListener(new View.OnClickListener()
		{
			@Override public void onClick(final View view)
			{
				dialogBuilder.dismiss();
				if (listener!=null)
					listener.onOk();
			}
		});

		////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		// Afficher la fenetre
		////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		dialogBuilder.setView(dialogView);
		dialogBuilder.show();
	}
}
