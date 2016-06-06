package mmi.colorgame.colorgameclient;

import android.app.Activity;
import android.content.Intent;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import config.Misc;

public class ConnectionActivity extends AppCompatActivity {

    private EditText ipEditText;

    private Intent returnIntent;

    private Global global;

    private Client activeThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        this.global = ((Global) getApplicationContext());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);

        this.ipEditText = (EditText) findViewById(R.id.editText_ip);

        this.returnIntent = getIntent();
    }

    public void connectToServer(View view){

        if(global.getCurrentThread() == null || !global.getCurrentThread().isAlive()) {
            //no client exists or the last client stopped
            //check the given ip
            String ip = this.ipEditText.getText().toString();

            if (ip != null) {
                Toast.makeText(this, "Attempting to connect to Server...", Toast.LENGTH_SHORT).show();
                //valid ip
                this.activeThread = new Client(ip);
                this.activeThread.start();
                global.setCurrentThread(this.activeThread);
            } else {
                //invalid ip
                Toast.makeText(this, "Please enter a valid IP-address", Toast.LENGTH_SHORT).show();
                //this.returnIntent.putExtra("msg", "please enter a valid IP-address");
                //setResult(RESULT_OK, this.returnIntent);
            }
        }
        //a client is already running, retrieves it from the Global.class
        else{
            this.activeThread = global.getCurrentThread();
            //this.activeThread.setUIActivity(this);
        }

        Intent intent = new Intent(this, PlayActivity.class);
        //intent.putExtra("mode", Misc.MODE_MULTI);
        startActivityForResult(intent, 1);
        //intent.putExtra("ServerIP", this.ipEditText.getText().toString());
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1){
            if(resultCode == Activity.RESULT_OK) {
                returnResult(data.getStringExtra("msg"));
            }
        }
    }

    public void returnResult(String msg){
        this.returnIntent.putExtra("msg", msg);
        setResult(RESULT_OK, this.returnIntent);
    }

}
