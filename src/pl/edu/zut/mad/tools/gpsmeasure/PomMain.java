package pl.edu.zut.mad.tools.gpsmeasure;

import pl.edu.zut.mad.tools.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

public class PomMain extends Activity implements OnClickListener {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main_pom);
		findViewById(R.id.button1).setOnClickListener(this);
		findViewById(R.id.button2).setOnClickListener(this);
		findViewById(R.id.button3).setOnClickListener(this);
	}


	public void onClick(View v) {
		Intent intent;
		switch (v.getId()) {
		case R.id.button1:
			intent = new Intent(PomMain.this, PomDlActivity.class);
			startActivity(intent);
			break;

		case R.id.button3:
			startActivity(new Intent(
					android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));

			break;
	

		case R.id.button2:
			AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
			dialogBuilder.setTitle("Informacje");
			dialogBuilder.setMessage("Autor: Rafa� Cicho�" + "\n"
					+ "Licencja: GNU GPL");
			dialogBuilder.setIcon(R.drawable.off);
			dialogBuilder.show();
			break;

		}
	}
}
