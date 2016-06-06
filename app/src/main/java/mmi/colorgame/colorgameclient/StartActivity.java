package mmi.colorgame.colorgameclient;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import config.Misc;

public class StartActivity extends AppCompatActivity {

    private TextView textView_information;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        this.textView_information = (TextView) findViewById(R.id.textView_welcome);
    }

    /**
     * Starts the game in single-player
     */
    public void startSingleplayer(View view){

        Intent intent = new Intent(this, PlayActivity.class);

        //intent.putExtra("mode", Misc.MODE_SINGLE);

        startActivityForResult(intent, 1);
    }

    /**
     * Starts a ConnectionUserInterfaceActivity to allow communication with the server.
     * @param view has to be here (just Android stuff...)
     */
    public void startMultiplayer(View view){
        //intent to start other activities
        Intent intent = new Intent(this, ConnectionActivity.class);

        //start activity
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1){
            if (resultCode == Activity.RESULT_OK){
                this.textView_information.setText(data.getStringExtra("msg"));
            }
        }
    }

}
