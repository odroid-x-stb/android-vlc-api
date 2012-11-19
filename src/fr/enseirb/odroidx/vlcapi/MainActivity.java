package fr.enseirb.odroidx.vlcapi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);
		((Button) findViewById(R.id.test_button)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(getApplicationContext(), TestPlayerActivity.class);
				i.putExtra("itemLocation", "http://www-itec.uni-klu.ac.at/ftp/datasets/mmsys12/BigBuckBunny/MPDs/BigBuckBunnyNonSeg_2s_isoffmain_DIS_23009_1_v_2_1c2_2011_08_30.mpd");
				i.putExtra("itemTitle", "Test Title");
				i.putExtra("dontParse", false);
				startActivity(i);
			}
		});
	}
}
