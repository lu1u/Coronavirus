package com.lpi.coronavirus;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;

import androidx.annotation.NonNull;

public class AideActivity
{
	public static void start(@NonNull final Activity context)
	{

		////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		// Afficher la fenetre
		////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		final AlertDialog dialogBuilder = new AlertDialog.Builder(context).create();

		LayoutInflater inflater = context.getLayoutInflater();
		final View dialogView = inflater.inflate(R.layout.layout_aide, null);

		final WebView tvMessage = dialogView.findViewById(R.id.webView);

		String bodyData = context.getResources().getString(R.string.aide);
		//if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
		{
			tvMessage.loadData(bodyData, "text/html", "UTF-8");
			//tvMessage.setText(Html.fromHtml(bodyData, Html.FROM_HTML_MODE_LEGACY));
		}
//		else
//		{
//			Spanned h = Html.fromHtml(bodyData, Html.FROM_HTML_MODE_LEGACY);
//			tvMessage.setText(Html.fromHtml(bodyData));
//		}
		////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		// Afficher la fenetre
		////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		dialogBuilder.setView(dialogView);
		dialogBuilder.show();
	}
}
