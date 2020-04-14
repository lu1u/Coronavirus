package com.lpi.coronavirus.utils;

import android.content.Context;
import android.content.res.TypedArray;

import androidx.annotation.ArrayRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ResourcesUtils
{

	/***
	 * Charge un tableau d'entiers depuis les ressources arrays.xml
	 * @param context
	 * @param arrayId
	 * @return
	 */
	public static @Nullable int[] getIntArray(final @NonNull Context context, final @ArrayRes int arrayId)
	{
		TypedArray ar = context.getResources().obtainTypedArray(arrayId);
		if ( ar==null)
			return null;

		int len = ar.length();

		int[] ints = new int[len];

		for (int i = 0; i < len; i++)
			ints[i] = ar.getResourceId(i, 0);

		ar.recycle();

		return ints;
	}
}
