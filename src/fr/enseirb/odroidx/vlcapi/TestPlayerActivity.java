package fr.enseirb.odroidx.vlcapi;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

public class TestPlayerActivity extends Activity {

	private static final String TAG = TestPlayerActivity.class.getSimpleName();

	private LibVLC mLibVLC;
	private SurfaceView mSurface;
	private SurfaceHolder mSurfaceHolder;
	private String mLocation;

	private boolean mEndReached;

	private int savedIndexPosition = -1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.test_activity);
		mSurface = (SurfaceView) findViewById(R.id.player_surface);
		mSurfaceHolder = mSurface.getHolder();
		mSurfaceHolder.setFormat(PixelFormat.RGBX_8888);
		mSurfaceHolder.addCallback(mSurfaceCallback);

		try {
			LibVLC.useIOMX(this);
			mLibVLC = LibVLC.getInstance();
		} catch (LibVlcException e) {
			e.printStackTrace();
		}

		EventManager em = EventManager.getInstance();
		em.addHandler(eventHandler);
	}

	@Override
    protected void onResume() {
        super.onResume();
        load();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mLibVLC != null) {
			mLibVLC.stop();
		}
		EventManager em = EventManager.getInstance();
		em.removeHandler(eventHandler);
	}

	public void onPlayPause() {
        if (mLibVLC.isPlaying()) {
            pause();
        } else {
            play();
        }
    }
	
	private void play() {
		mLibVLC.play();
		mSurface.setKeepScreenOn(true);
	}

	private void pause() {
		mLibVLC.pause();
		mSurface.setKeepScreenOn(false);
	}

	private void load() {
		String title = "Title";
		mLocation = null;
		boolean dontParse = false;
		String itemTitle = null;

		if(getIntent().getExtras() != null) {
			/* Started from VideoListActivity */
			mLocation = getIntent().getExtras().getString("itemLocation");
			itemTitle = getIntent().getExtras().getString("itemTitle");
			dontParse = getIntent().getExtras().getBoolean("dontParse");
		}

		mSurface.setKeepScreenOn(true);

		/* Start / resume playback */
		if (savedIndexPosition > -1) {
			mLibVLC.playIndex(savedIndexPosition);
		} else if (mLocation != null && mLocation.length() > 0 && !dontParse) {
			savedIndexPosition = mLibVLC.readMedia(mLocation, false);
		}

		if (mLocation != null && mLocation.length() > 0 && !dontParse) {
			// restore last position
			Media media = DatabaseManager.getInstance(this).getMedia(this, mLocation);
			if (media != null && media.getTime() > 0)
				mLibVLC.setTime(media.getTime());

			try {
				title = URLDecoder.decode(mLocation, "UTF-8");
			} catch (UnsupportedEncodingException e) {
			}
			if (title.startsWith("file:")) {
				title = new File(title).getName();
				int dotIndex = title.lastIndexOf('.');
				if (dotIndex != -1)
					title = title.substring(0, dotIndex);
			}
		} else if(itemTitle != null) {
			title = itemTitle;
		}
	}

	public static void start(Context context, String location, String title, Boolean dontParse) {
        Intent intent = new Intent(context, TestPlayerActivity.class);
        intent.putExtra("itemLocation", location);
        intent.putExtra("itemTitle", title);
        intent.putExtra("dontParse", dontParse);

        if (dontParse)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        else {
        }
        context.startActivity(intent);
    }
	
	private void endReached() {
		/* Exit player when reach the end */
		mEndReached = true;
		finish();
	}

	/**
	 *  Handle libvlc asynchronous events
	 */
	private final Handler eventHandler = new VideoPlayerEventHandler(this);

	private static class VideoPlayerEventHandler extends WeakHandler<TestPlayerActivity> {
		public VideoPlayerEventHandler(TestPlayerActivity owner) {
			super(owner);
		}

		@Override
		public void handleMessage(Message msg) {
			TestPlayerActivity activity = getOwner();
			if(activity == null) {
				return;
			}

			switch (msg.getData().getInt("event")) {
			case EventManager.MediaPlayerPlaying:
				Log.i(TAG, "MediaPlayerPlaying");
				break;
			case EventManager.MediaPlayerPaused:
				Log.i(TAG, "MediaPlayerPaused");
				break;
			case EventManager.MediaPlayerStopped:
				Log.i(TAG, "MediaPlayerStopped");
				break;
			case EventManager.MediaPlayerEndReached:
				Log.i(TAG, "MediaPlayerEndReached");
				activity.endReached();
				break;
			default:
				Log.e(TAG, String.format("Event not handled (0x%x)", msg.getData().getInt("event")));
				break;
			}
		}
	};
	
	 /**
     * attach and disattach surface to the lib
     */
    private final SurfaceHolder.Callback mSurfaceCallback = new Callback() {
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            mLibVLC.attachSurface(holder.getSurface(), TestPlayerActivity.this, width, height);
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            mLibVLC.detachSurface();
        }
    };
}
