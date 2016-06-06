package mmi.colorgame.colorgameclient;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import config.Misc;

public class PlayActivity extends AppCompatActivity {

    private Global global;

    private Intent givenIntent = null;

    private Client activeThread;

    private ColGame activeGame;

    private int gameMode;

    /**
     * The intent that will be returned to the calling activity.
     */
    private Intent returnIntent = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);

        //get the global state of the application
        this.global = ((Global) getApplicationContext());

        //get intent from calling activity
        this.givenIntent = getIntent();

        //int mode = this.givenIntent.getIntExtra("mode", Misc.MODE_SINGLE);

        //if there is no active client thread, the game is in singleplyer-mode
        if(global.getCurrentThread() == null || !global.getCurrentThread().isAlive()) {
            // no connection to server => singleplayer
            this.gameMode = Misc.MODE_SINGLE;

            //check if there is game already running
            if (global.getCurrentgame() == null || !global.getCurrentgame().isActive()){
                //create a new local singleplayer game
                this.activeGame = new ColGame();

            }else{
                //get the already running game
                this.activeGame = global.getCurrentgame();

            }
        }else{
            //connected to server => get the client
            this.activeThread = global.getCurrentThread();
            this.gameMode = Misc.MODE_MULTI;
            //TODO get gamestate from server -> will be done in setUI!!!
        }

        setUI();
    }

    /**
     * Sets the UI according to the state of the game.
     */
    public void setUI(){

        if (this.gameMode == Misc.MODE_SINGLE){
            setFirstColor(this.activeGame.getFirstColor());
            setSecondColor(this.activeGame.getSeceondColor());
            showAnswerButtons();
        }
        //todo set ui according to data from server
    }


    // set the first ImageView (which shows the color of the first "dice") given from the colgame.class or the client
    public void setFirstColor(int color){

        //todo get color from client in multiplayer
        ImageView firstColor = (ImageView) findViewById(R.id.imageView_firstColor);
        firstColor.setImageBitmap(getColorImageBitmap(color));

    }

    // set the second color "dice"
    public void setSecondColor(int color){

        ImageView secondColor = (ImageView) findViewById(R.id.imageView_secondColor);
        secondColor.setImageBitmap(getColorImageBitmap(color));

    }

    // show the buttons with the different colors to touch
    public void showAnswerButtons(){
        ImageButton blue = (ImageButton) findViewById(R.id.imageButton_blue);
        blue.setImageBitmap(getColorImageBitmap(Misc.COLOR_BLUE_ANSWER));
        ImageButton green = (ImageButton) findViewById(R.id.imageButton_green);
        green.setImageBitmap(getColorImageBitmap(Misc.COLOR_GREEN_ANSWER));
        ImageButton red = (ImageButton) findViewById(R.id.imageButton_red);
        red.setImageBitmap(getColorImageBitmap(Misc.COLOR_RED_ANSWER));
        red.setVisibility(View.VISIBLE);
        blue.setVisibility(View.VISIBLE);
        green.setVisibility(View.VISIBLE);
    }

    // get the matching bitmap(file) to the color
    public Bitmap getColorImageBitmap(int color){
        Bitmap bmp = null;

        switch (color) {
            case Misc.COLOR_RED:
                bmp = BitmapFactory.decodeStream(this.getResources().openRawResource(R.raw.col_r));
                break;
            case Misc.COLOR_GREEN:
                bmp = BitmapFactory.decodeStream(this.getResources().openRawResource(R.raw.col_g));
                break;
            case Misc.COLOR_BLUE:
                bmp = BitmapFactory.decodeStream(this.getResources().openRawResource(R.raw.col_b));
                break;
            case Misc.COLOR_BACKGROUND_BLUE_WRITTEN_GREEN:
                bmp = BitmapFactory.decodeStream(this.getResources().openRawResource(R.raw.col_b_green));
                break;
            case Misc.COLOR_BACKGROUND_BLUE_WRITTEN_RED:
                bmp = BitmapFactory.decodeStream(this.getResources().openRawResource(R.raw.col_b_red));
                break;
            case Misc.COLOR_BACKGROUND_GREEN_WRITTEN_BLUE:
                bmp = BitmapFactory.decodeStream(this.getResources().openRawResource(R.raw.col_g_blue));
                break;
            case Misc.COLOR_BACKGROUND_GREEN_WRITTEN_RED:
                bmp = BitmapFactory.decodeStream(this.getResources().openRawResource(R.raw.col_g_red));
                break;
            case Misc.COLOR_BACKGROUND_RED_WRITTEN_BLUE:
                bmp = BitmapFactory.decodeStream(this.getResources().openRawResource(R.raw.col_r_blue));
                break;
            case Misc.COLOR_BACKGROUND_RED_WRITTEN_GREEN:
                bmp = BitmapFactory.decodeStream(this.getResources().openRawResource(R.raw.col_r_green));
                break;
            case Misc.COLOR_BLUE_ANSWER:
                bmp = BitmapFactory.decodeStream(this.getResources().openRawResource(R.raw.col_b_answer));
                break;
            case Misc.COLOR_GREEN_ANSWER:
                bmp = BitmapFactory.decodeStream(this.getResources().openRawResource(R.raw.col_g_answer));
                break;
            case Misc.COLOR_RED_ANSWER:
                bmp = BitmapFactory.decodeStream(this.getResources().openRawResource(R.raw.col_r_answer));
                break;
        }

        return bmp;

    }

    //is called when the blue button is pressed
    public void blueButton(View view) {
        sendAnswer(Misc.COLOR_BLUE);
    }

    //is called when the green button is pressed
    public void greenButton(View view){
        sendAnswer(Misc.COLOR_GREEN);
    }

    //is called when the red button is pressed
    public void redButton(View view){
        sendAnswer(Misc.COLOR_RED);
    }

    //called when the action is performed (currently a button)
    public void actionButton(View view){
        sendActionAnswer();
    }

    //sends the given answer to the colgame.class or the client when multiplayer
    public void sendAnswer(int color) {
        if (gameMode == Misc.MODE_SINGLE) {
            //send answer to colgame.class
            if (this.activeGame.touchAnswer(color)) {
                //if answer was correct increase score
                global.increaseSocre();
            }
            this.activeGame.restart();
        }

        //todo send answer to client in multiplayer


        showScore();
        setUI();

    }

    //sends the action answer to the colgame or the client
    public void sendActionAnswer(){
        //singleplayer
        if (gameMode == Misc.MODE_SINGLE) {
            if(this.activeGame.actionAnswer(0)){
                //if action was correct increase score
                global.increaseSocre();

            }
            //start new game, wwith random colors
            this.activeGame.restart();
        }

        //todo multiplayer

        showScore();
        setUI();
    }

    //show the score textview
    public void showScore(){
        TextView scoreView = (TextView) findViewById(R.id.textView_score);
        scoreView.setVisibility(View.VISIBLE);
        scoreView.setText("Score: " + String.valueOf(global.getCurrentScore()));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_play, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
        MenuItem restart = menu.findItem(R.id.menu_restart);
        if (this.gameMode == Misc.MODE_MULTI){
            restart.setVisible(false);
            restart.setEnabled(false);
        }else{
            restart.setVisible(true);
            restart.setEnabled(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_restart:
                this.activeGame.restart();
                global.resetCurrentScore();
                showScore();
                setUI();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void setScoreText(final String score){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //textView to show current score
                TextView scoreView = (TextView) findViewById(R.id.textView_score);
                scoreView.setText(score);
            }
        });
    }

    /**
     * Sets the intent with a message, so the calling activity can display what happened.
     *
     * @param msg message to pass for the calling activity
     */
    public void returnResult(String msg){
        this.returnIntent.putExtra("msg", msg);
        setResult(RESULT_OK, this.returnIntent);
    }

    /**
     * Closes the activity.
     */
    public void closeActivity(){
        super.finish();
    }


}
