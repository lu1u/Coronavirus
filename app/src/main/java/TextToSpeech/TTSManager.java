package TextToSpeech;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import androidx.annotation.NonNull;

/***
 * Utilitaire pour faciliter l'utilisation du TextToSpeech Android
 */
public class TTSManager implements TextToSpeech.OnInitListener
{

	private TextToSpeech _tts= null;
	private String _message ;

	/***
	 * Methode a appeler pour faire une annonce vocale
	 * @param context
	 * @param message
	 */
	public static void speak(@NonNull final Context context, @NonNull final String message)
	{
		// TODO: restriction de TTS si on est dans un service
		if (context.isRestricted())
		{
			return;
		}
		new TTSManager(context, message);
	}


	private TTSManager(@NonNull final Context context, @NonNull final String message)
	{
		_message = message;
		try
		{
			_tts = new TextToSpeech(context, this);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override public void onInit(int status)
	{
		try
		{
			if (_tts!=null)
			{
				if (status == TextToSpeech.SUCCESS)
					_tts.speak(_message, TextToSpeech.QUEUE_ADD, null, null);

				_tts.shutdown();
			}
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
