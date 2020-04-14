package com.lpi.coronavirus;

import android.animation.Animator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


public class CircularProgressBar extends View
{
	private static final String PROPERTY_MAX = "max";
	private static final String PROPERTY_VALEUR = "valeur";
	private static final float ANGLE_DEPART = 270.0f;
	boolean _afficheMax;
	float _minimum,   // Valeur min de la jauge
			_maximum,   // Valeur max de la jauge
			_valeur,    // Valeur reelle de la jauge
			_valeurCible, // Valeur cible pendant les animations
			_valeurADessiner, // Valeur actuellement dessinée pendant les animations
			_valeurMax; // Valeur du marqueur max
	ValueAnimator _animator;
	@Nullable
	private String _message;
	int _couleurTex;
	float _textSize;
	private Paint _paintJauge, _paintFond;
	private Paint _paintMax;
	TextPaint _textPaint;
	private float _valeurMaxADessiner = 0;
	RectF r = new RectF();          // Pour eviter d'allouer un objet dans onDraw


	public CircularProgressBar(Context context)
	{
		super(context);
		init(null, 0);
	}

	public CircularProgressBar(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init(attrs, 0);
	}

	public CircularProgressBar(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		init(attrs, defStyle);
	}

	/***********************************************************************************************
	 * Initialisation du controle, lecture des attributs Styleable
	 * @param attrs
	 * @param defStyle
	 */
	private void init(AttributeSet attrs, int defStyle)
	{
		// Load attributes
		final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.CircularProgressBar, defStyle, 0);

		//mExampleString = a.getString(
		//		R.styleable.CircularProgressBar_exampleString);

//
		//if (a.hasValue(R.styleable.CircularProgressBar_exampleDrawable))
		//{
		//	mExampleDrawable = a.getDrawable(
		//			R.styleable.CircularProgressBar_exampleDrawable);
		//	mExampleDrawable.setCallback(this);
		//}

		_message = a.getString(R.styleable.CircularProgressBar_CircTexte);
		_minimum = a.getFloat(R.styleable.CircularProgressBar_CircMinimum, 0);
		_maximum = a.getFloat(R.styleable.CircularProgressBar_CircMaximum, 100);
		_valeur = a.getFloat(R.styleable.CircularProgressBar_CircValeur, 60);
		_afficheMax = a.getBoolean(R.styleable.CircularProgressBar_CircAfficheMax, true);
		_valeurADessiner = _valeur;
		int _couleurBarre = a.getColor(R.styleable.CircularProgressBar_CircCouleurBarre, Color.RED);
		float _largeurBarre = a.getDimension(R.styleable.CircularProgressBar_CircLargeurBarre, 4);
		int style = a.getInt(R.styleable.CircularProgressBar_CircBoutsBarre, 0);
		Paint.Cap bouts;
		switch (style)
		{
			case 0:
				bouts = Paint.Cap.ROUND;
				break;
			case 1:
				bouts = Paint.Cap.SQUARE;
				break;
			default:
				bouts = Paint.Cap.BUTT;
		}

		// Style graphique de la jauge
		{

			_paintJauge = new Paint();
			_paintJauge.setColor(_couleurBarre);
			_paintJauge.setStrokeWidth(_largeurBarre);
			_paintJauge.setStrokeCap(bouts);
			_paintJauge.setStyle(Paint.Style.STROKE);
			_paintJauge.setShadowLayer(12, 2, 2, Color.argb(128, 0, 0, 0));
		}

		// Style graphique de la marque valeur max
		{
			int _couleurMax = a.getColor(R.styleable.CircularProgressBar_CircCouleurMax, Color.RED);
			float _largeurMax = a.getDimension(R.styleable.CircularProgressBar_CircLargeurBarre, _largeurBarre);
			_paintMax = new Paint();
			_paintMax.setColor(_couleurMax);
			_paintMax.setStrokeWidth(_largeurMax);
			_paintMax.setStrokeCap(bouts);
			_paintMax.setStyle(Paint.Style.STROKE);
			_paintMax.setShadowLayer(12, 2, 2, Color.argb(128, 0, 0, 0));
		}

		// Style graphique du fond de la jauge
		{
			int couleurFond = a.getColor(R.styleable.CircularProgressBar_CircCouleurFond, Color.RED);
			float largeurBarre = a.getDimension(R.styleable.CircularProgressBar_CircLargeurFond, _largeurBarre * 1.5f);

			_paintFond = new Paint();
			_paintFond.setColor(couleurFond);
			_paintFond.setStrokeWidth(largeurBarre);
			_paintFond.setStyle(Paint.Style.STROKE);
		}

		// Style graphique du texte, completé dans calculeTextPaint
		{
			_couleurTex = a.getColor(R.styleable.CircularProgressBar_CircCouleurTexte, _couleurBarre);
			_textSize = a.getDimension(R.styleable.CircularProgressBar_CircLargeurBarre, 4);
			calculeTextPaint();
		}
		a.recycle();
	}

	/***********************************************************************************************
	 * Calcule un textPaint avec une taille de caractere qui permet d'afficher le texte entierement
	 */
	private void calculeTextPaint()
	{
		if (_message == null)
			return;

		_textPaint = new TextPaint();
		_textPaint.setColor(_couleurTex);
		_textPaint.setTextSize(_textSize);
		int largeurBarre = (int) Math.max(_paintJauge.getStrokeWidth(), _paintFond.getStrokeWidth());
		int paddingLeft = getPaddingLeft() + largeurBarre;
		int paddingTop = getPaddingTop() + largeurBarre;
		int paddingRight = getPaddingRight() + largeurBarre;
		int paddingBottom = getPaddingBottom() + largeurBarre;

		int contentWidth = getWidth() - paddingLeft - paddingRight;
		int contentHeight = getHeight() - paddingTop - paddingBottom;

		Rect rText = new Rect();
		_textPaint.getTextBounds(_message, 0, _message.length(), rText);
		float tailleTexte = _textPaint.getTextSize();
		while ((tailleTexte > 1) && ((rText.width() > contentWidth) || (rText.height() > contentHeight)))
		{
			tailleTexte -= 0.5f;
			_textPaint.setTextSize(tailleTexte);
			_textPaint.getTextBounds(_message, 0, _message.length(), rText);
		}

	}

	/***********************************************************************************************
	 * Dessiner le controle
	 * @param canvas
	 */
	@Override
	protected void onDraw(@NonNull Canvas canvas)
	{
		super.onDraw(canvas);
		int largeurBarre = (int) Math.max(_paintJauge.getStrokeWidth(), _paintFond.getStrokeWidth());
		int paddingLeft = getPaddingLeft() + largeurBarre;
		int paddingTop = getPaddingTop() + largeurBarre;
		int paddingRight = getPaddingRight() + largeurBarre;
		int paddingBottom = getPaddingBottom() + largeurBarre;

		int contentWidth = getWidth() - paddingLeft - paddingRight;
		int contentHeight = getHeight() - paddingTop - paddingBottom;

		float taille = (Math.min(contentHeight, contentWidth) * 0.5f);
		float cx = paddingLeft + (contentWidth * 0.5f);
		float cy = paddingTop + (contentHeight * 0.5f);

		r.set(cx - taille, cy - taille, cx + taille, cy + taille);

		// Fond de la jauge
		canvas.drawArc(r, 0, 360, false, _paintFond);

		// Jauge
		final float arc = _valeurADessiner /  (_maximum - _minimum) * 360.0f;
		canvas.drawArc(r, ANGLE_DEPART, arc, false, _paintJauge);

		// Valeur max
		if (_afficheMax)
		{
			final float arcMax = _valeurMaxADessiner / (_maximum - _minimum) * 360.0f;
			canvas.drawArc(r, arcMax + ANGLE_DEPART - 1, 0.1f, false, _paintMax);
		}

		// Message
		if (_message != null)
		{
			if (_textPaint == null)
				calculeTextPaint();
			afficheTextCentre(canvas, _textPaint, _message);
		}
	}

	/***********************************************************************************************
	 * Afficher un texte centré
	 * @param canvas
	 * @param paint
	 * @param text
	 */
	private static void afficheTextCentre(@NonNull Canvas canvas, @NonNull Paint paint, @NonNull String text)
	{
		Rect r = new Rect();
		canvas.getClipBounds(r);
		int cHeight = r.height();
		int cWidth = r.width();
		paint.setTextAlign(Paint.Align.LEFT);
		paint.getTextBounds(text, 0, text.length(), r);
		float x = cWidth / 2f - r.width() / 2f - r.left;
		float y = cHeight / 2f + r.height() / 2f - r.bottom;
		canvas.drawText(text, x, y, paint);
	}

	/***********************************************************************************************
	 * Modifier la valeur minimum de la jauge
	 * @param value
	 */
	public void setMinimum(float value)
	{
		_minimum = value;
		_valeurMax = _minimum;
		animer();
	}


	/***********************************************************************************************
	 * Modifier la valeur maximum de la jauge
	 * @param value
	 */
	public void setMaximum(float value)
	{
		_maximum = value;
		_valeurMax = _minimum;
		animer();
	}


	/***********************************************************************************************
	 * Modifie la valeur affichee, avec une jolie animation
	 * @param value
	 */
	public void setValeur(float value)
	{
		if (value < _minimum)
			value = _minimum;
		if (value > _maximum)
			value = _maximum;
		if (_valeurMax < value)
			_valeurMax = value;

		_valeurCible = value;
		animer();
	}

	/***
	 * Fait une animation a chaque changement des valeurs
	 */
	private void animer()
	{
		if (_animator != null)
			// Animation deja en cours
			return;

		_animator = ValueAnimator.ofFloat(_valeur, _valeurCible);
		_animator.setDuration(Math.abs(_valeurCible - _valeur) > (_maximum / 2) ? 2000 : 1000);
		PropertyValuesHolder propertyRadius = PropertyValuesHolder.ofFloat(PROPERTY_VALEUR, _valeur, _valeurCible);
		PropertyValuesHolder propertyMax = PropertyValuesHolder.ofFloat(PROPERTY_MAX, _valeurMaxADessiner, _valeurMax);
		_animator.setValues(propertyRadius, propertyMax);
		_animator.setInterpolator(new AccelerateDecelerateInterpolator());
		_animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
		{
			@Override
			public void onAnimationUpdate(@NonNull ValueAnimator animation)
			{
				try
				{
					_valeurADessiner = (float) animation.getAnimatedValue(PROPERTY_VALEUR);
					_valeurMaxADessiner = (float) animation.getAnimatedValue(PROPERTY_MAX);
				} catch (Exception e)
				{
					e.printStackTrace();
				}
				invalidate();
			}
		});

		_animator.addListener(new Animator.AnimatorListener()
		{
			@Override public void onAnimationStart(final Animator animator)
			{

			}

			@Override public void onAnimationEnd(final Animator animator)
			{
				_valeur = _valeurCible;
				_valeurADessiner = _valeurCible;
				_valeurMaxADessiner = _valeurMax;
				_animator = null;
				invalidate();
			}

			@Override public void onAnimationCancel(final Animator animator)
			{

			}

			@Override public void onAnimationRepeat(final Animator animator)
			{

			}
		});
		_animator.start();
	}

	/***********************************************************************************************
	 * Change le message affiché au centre
	 * @param message
	 */
	public void setText(@NonNull final String message)
	{
		_message = message;
		_textPaint = null; // Recalculer la taille du texte au prochain affichage
		invalidate();
	}

	/***
	 * Changer la marque de valeur max atteinte
	 * @param value
	 */
	public void setValeurMax(float value)
	{
		if (value < _minimum)
			value = _minimum;
		if (value > _maximum)
			value = _maximum;

		_valeurMax = value;
		_valeurMaxADessiner = value;
		animer();
	}
}
