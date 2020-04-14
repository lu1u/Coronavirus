package com.lpi.coronavirus;

import android.app.Activity;
import android.app.AlertDialog;
import android.media.MediaPlayer;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lpi.coronavirus.utils.ResourcesUtils;

public class ParametresActivity
{

	interface ParametresActivityListener
	{
		void onOk();
		void onCancel();
	}

	public static void start(@NonNull final Activity context, final @Nullable ParametresActivityListener listener)
	{
		final int[] ids = ResourcesUtils.getIntArray(context, R.array.sonneries_id);
		final AlertDialog dialogBuilder = new AlertDialog.Builder(context).create();


		//AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context, Utils.getTheme(context));
		LayoutInflater inflater = context.getLayoutInflater();
		final View dialogView = inflater.inflate(R.layout.activity_parametres, null);

		final EditText etDistance = dialogView.findViewById(R.id.editTextDistanceMax);
		final EditText etDelaiSonnerie = dialogView.findViewById(R.id.editTextDelaiSonnerie);
		final SeekBar sbPrecision = dialogView.findViewById(R.id.seekBarGeolocalisation);
		final Button btAnnuler = dialogView.findViewById(R.id.buttonAnnuler);
		final Button btOk = dialogView.findViewById(R.id.buttonOk);
		final Spinner spinSonneries = dialogView.findViewById(R.id.spinnerAlarme);
		final CheckBox cbAvance = dialogView.findViewById(R.id.checkBoxAvance);
		final View vLayoutAvance = dialogView.findViewById(R.id.layoutAvance);

		final Preferences preferences = Preferences.getInstance(context);
		etDistance.setText(Integer.toString((int) preferences.getDistanceMax()));
		etDelaiSonnerie.setText(Long.toString(preferences.getTimeOutAlarme()));

		// Precision de mesure
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
			sbPrecision.setMin(0);

		sbPrecision.setMax(2);
		sbPrecision.setProgress(preferences.getPrecision());

		// Son de l'alarme
		final int sel = preferences.getAlarme();
		if (ids != null)
			if (sel >= 0 && sel < ids.length)
				spinSonneries.setSelection(preferences.getAlarme());
		spinSonneries.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		{
			MediaPlayer _mp;

			@Override
			public void onItemSelected(final AdapterView<?> adapterView, final View view, final int i, final long l)
			{
				// Jouer la nouvelle sonnerie
				if (_mp != null)
				{
					_mp.stop();
					_mp.release();
				}

				if (ids != null)
				{
					_mp = MediaPlayer.create(context, ids[i]);
					_mp.start();
				}
			}

			@Override public void onNothingSelected(final AdapterView<?> adapterView)
			{

			}
		});

		// Bouton Annuler
		btAnnuler.setOnClickListener(new View.OnClickListener()
		{
			@Override public void onClick(final View view)
			{
				dialogBuilder.dismiss();
				if (listener != null)
					listener.onCancel();
			}
		});

		// Bouton OK
		btOk.setOnClickListener(new View.OnClickListener()
		{
			@Override public void onClick(final View view)
			{
				preferences.setDistanceMax(Float.parseFloat(etDistance.getText().toString()));
				preferences.setTimeoutAlarme(Long.parseLong(etDelaiSonnerie.getText().toString()));
				preferences.setPrecision(sbPrecision.getProgress());
				preferences.setAlarme(spinSonneries.getSelectedItemPosition());
				dialogBuilder.dismiss();
				if (listener != null)
					listener.onOk();
			}
		});

		// Vue simplifiee/avancee
		cbAvance.setChecked(false);
		vLayoutAvance.setVisibility(View.GONE);
		cbAvance.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(final CompoundButton compoundButton, final boolean b)
			{
				if  (b)
				{
					vLayoutAvance.setAnimation(AnimationUtils.loadAnimation(context, R.anim.enter_top));
					vLayoutAvance.setVisibility(View.VISIBLE);
				}
				else
				{
					vLayoutAvance.setAnimation(AnimationUtils.loadAnimation(context, R.anim.exit_top));
					vLayoutAvance.setVisibility(View.GONE);
				}
			}
		});

		////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		// Afficher la fenetre
		////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		dialogBuilder.setView(dialogView);
		dialogBuilder.show();
	}


}
