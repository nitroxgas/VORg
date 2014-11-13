package br.com.casadalagoa.vorf;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;


public class about_activity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_activity);
        ImageButton mImageButton;
        mImageButton = (ImageButton) findViewById(R.id.btn_google_play);
        mImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse("http://goo.gl/fUpUhi");
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            }
        });

        TextView mTextView;
        mTextView = (TextView) findViewById(R.id.mAbout);
        mTextView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                int leg = Utility.getCurrentLeg(getBaseContext());
                String strLeg;
                if (leg == 1) strLeg = "2";
                else strLeg = "1";
                Utility.setCurrentLeg(getBaseContext(), strLeg);
                Utility.setNextUpdate(getBaseContext(),"2014-01-01 00:00:00");
                Log.v("About Leg:", strLeg);
                return true;
            }
        });
    }


    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.about_activity, menu);
        return true;
    }*/

   /* @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }*/
}
