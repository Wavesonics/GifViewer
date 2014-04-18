package com.darkrockstudios.apps.gifviewer;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;

import com.darkrockstudios.apps.gifviewer.util.SystemUiHider;
import com.darkrockstudios.uriimageview.UriImageHandler;
import com.darkrockstudios.uriimageview.UriImageView;

import butterknife.ButterKnife;
import butterknife.InjectView;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class GifViewerActivity extends Activity
{
	/**
	 * Whether or not the system UI should be auto-hidden after
	 * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
	 */
	private static final boolean AUTO_HIDE = true;

	/**
	 * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
	 * user interaction before hiding the system UI.
	 */
	private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

	/**
	 * If set, will toggle the system UI visibility upon interaction. Otherwise,
	 * will show the system UI visibility upon interaction.
	 */
	private static final boolean TOGGLE_ON_CLICK = false;

	/**
	 * The flags to pass to {@link SystemUiHider#getInstance}.
	 */
	private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

	/**
	 * The instance of the {@link SystemUiHider} for this activity.
	 */
	private SystemUiHider mSystemUiHider;

	@InjectView(R.id.fullscreen_content)
	UriImageView m_imageView;

	@InjectView(R.id.fullscreen_content_controls)
	View m_controlsView;

	// Must move this to a retained fragment
	private UriImageHandler m_imageHandler;

	@Override
	protected void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );

		setContentView( R.layout.activity_gif_viewer );
		ButterKnife.inject( this );

		m_imageHandler = new UriImageHandler();
		m_imageView.setErrorImage( R.drawable.image_error );

		// Set up an instance of SystemUiHider to control the system UI for
		// this activity.
		mSystemUiHider = SystemUiHider.getInstance( this, m_imageView, HIDER_FLAGS );
		mSystemUiHider.setup();
		mSystemUiHider
				.setOnVisibilityChangeListener( new SystemUiHider.OnVisibilityChangeListener()
				{
					// Cached values.
					int mControlsHeight;
					int mShortAnimTime;

					@Override
					@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
					public void onVisibilityChange( boolean visible )
					{
						if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2 )
						{
							// If the ViewPropertyAnimator API is available
							// (Honeycomb MR2 and later), use it to animate the
							// in-layout UI controls at the bottom of the
							// screen.
							if( mControlsHeight == 0 )
							{
								mControlsHeight = m_controlsView.getHeight();
							}
							if( mShortAnimTime == 0 )
							{
								mShortAnimTime = getResources().getInteger(
										                                          android.R.integer.config_shortAnimTime );
							}
							m_controlsView.animate()
							              .translationY( visible ? 0 : mControlsHeight )
							              .setDuration( mShortAnimTime );
						}
						else
						{
							// If the ViewPropertyAnimator APIs aren't
							// available, simply show or hide the in-layout UI
							// controls.
							m_controlsView.setVisibility( visible ? View.VISIBLE : View.GONE );
						}

						if( visible && AUTO_HIDE )
						{
							// Schedule a hide().
							delayedHide( AUTO_HIDE_DELAY_MILLIS );
						}
					}
				} );

		// Set up the user interaction to manually show or hide the system UI.
		m_imageView.setOnClickListener( new View.OnClickListener()
		{
			@Override
			public void onClick( View view )
			{
				if( TOGGLE_ON_CLICK )
				{
					mSystemUiHider.toggle();
				}
				else
				{
					mSystemUiHider.show();
				}
			}
		} );

		// Upon interacting with UI controls, delay any scheduled hide()
		// operations to prevent the jarring behavior of controls going away
		// while interacting with the UI.
		findViewById( R.id.dummy_button ).setOnTouchListener( mDelayHideTouchListener );
	}

	@Override
	protected void onPostCreate( Bundle savedInstanceState )
	{
		super.onPostCreate( savedInstanceState );

		// Trigger the initial hide() shortly after the activity has been
		// created, to briefly hint to the user that UI controls
		// are available.
		delayedHide( 100 );

		loadImageFromIntent();
	}

	@Override
	public void onResume()
	{
		super.onResume();

		m_imageHandler.onResume( m_imageView );
	}

	@Override
	public void onPause()
	{
		super.onPause();

		m_imageHandler.onPause();
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		ButterKnife.reset( this );
	}

	private void loadImageFromIntent()
	{
		final Intent intent = getIntent();
		if( intent != null )
		{
			final Uri data = intent.getData();
			if( data != null )
			{
				m_imageHandler.loadImage( data, m_imageView, GifViewerApplication.getImageCache() );
			}
		}
	}

	/**
	 * Touch listener to use for in-layout UI controls to delay hiding the
	 * system UI. This is to prevent the jarring behavior of controls going away
	 * while interacting with activity UI.
	 */
	View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener()
	{
		@Override
		public boolean onTouch( View view, MotionEvent motionEvent )
		{
			if( AUTO_HIDE )
			{
				delayedHide( AUTO_HIDE_DELAY_MILLIS );
			}
			return false;
		}
	};

	Handler  mHideHandler  = new Handler();
	Runnable mHideRunnable = new Runnable()
	{
		@Override
		public void run()
		{
			mSystemUiHider.hide();
		}
	};

	/**
	 * Schedules a call to hide() in [delay] milliseconds, canceling any
	 * previously scheduled calls.
	 */
	private void delayedHide( int delayMillis )
	{
		mHideHandler.removeCallbacks( mHideRunnable );
		mHideHandler.postDelayed( mHideRunnable, delayMillis );
	}
}
