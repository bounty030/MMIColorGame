package mmi.colorgame.colorgameclient;

import java.util.Random;

import config.Misc;

/**
 * Created by martin on 13.05.16.
 *  This class implements a game state with the different colors that are shown to the user.
 *  The class evaluates the answers from the user
 */
public class ColGame {

    // the activity which implements the user-interface seen on the phone
    private PlayActivity ui;

    // stores the state of the game, this variable will be called when the activity is started
    private int currentState;

    // is true when acurrent game is running
    boolean active;

    //the three colors which will be shown to the user
    //the pure color without text
    private int firstColor;
    //the word that is shown on the second color
    private int writtenColor;
    //the background color of the written color
    private int writtenBackgroundColor;

    //creator
    public ColGame(){

        restart();

        this.currentState = Misc.GAME_STATE_INIT;

    }

    //choose new random colors
    public void restart(){

        Random rnd = new Random();

        // get a radnom integer which will be randomly assigned to one of the three colors
        int randomColor = rnd.nextInt(3);

        // assign random color
        switch(randomColor){
            case 0:
                this.firstColor = Misc.COLOR_BLUE;
                break;
            case 1:
                this.firstColor = Misc.COLOR_GREEN;
                break;
            case 2:
                this.firstColor = Misc.COLOR_RED;
                break;
        }

        // same for second color
        randomColor = rnd.nextInt(3);

        // same again
        switch(randomColor){
            case 0:
                this.writtenColor = Misc.COLOR_BLUE;
                break;
            case 1:
                this.writtenColor = Misc.COLOR_GREEN;
                break;
            case 2:
                this.writtenColor = Misc.COLOR_RED;
                break;
        }

        // Choose a random background color for the written color, which is different
        boolean rndBool = rnd.nextBoolean();
        switch (writtenColor) {
            case Misc.COLOR_BLUE:
                if (rndBool){writtenBackgroundColor = Misc.COLOR_GREEN;} else {writtenBackgroundColor = Misc.COLOR_RED;}
                break;
            case Misc.COLOR_GREEN:
                if (rndBool){writtenBackgroundColor = Misc.COLOR_BLUE;} else {writtenBackgroundColor = Misc.COLOR_RED;}
                break;
            case Misc.COLOR_RED:
                if (rndBool){writtenBackgroundColor = Misc.COLOR_BLUE;} else {writtenBackgroundColor = Misc.COLOR_GREEN;}
                break;
        }
    }

    //this function evaluates the answer the user has given by touching one of the color-buttons
    public boolean touchAnswer(int colorChoice){
        boolean result;
        // the first and written color are different
        if (firstColor != writtenColor){
            //result will be true if the asnwer is different to the first and written color
            result = (colorChoice != firstColor && colorChoice != writtenColor);
        }else{
            //the user touched a color, but an action was expected
            result = false;
        }
        return result;
    }

    //this shoud be called when the user gave a speech answer
    public boolean actionAnswer(int answer){
        boolean result;
        // the first and written color are different
        if (firstColor == writtenColor){
            //action was expected, correct answer
            //todo check if the action answer was correct
            result = true;
        }else{
            //the user touched a color, but an action was expected
            result = false;
        }
        return result;
    }

    //return the value of the first color
    public int getFirstColor() {
        return this.firstColor;
    }

    //returns the value of the second color image which incorporates the backgroundcolor and written color
    public int getSeceondColor() {

        switch(writtenBackgroundColor){

            case Misc.COLOR_BLUE:
                if (writtenColor == Misc.COLOR_GREEN){
                    return Misc.COLOR_BACKGROUND_BLUE_WRITTEN_GREEN;
                }else{
                    return Misc.COLOR_BACKGROUND_BLUE_WRITTEN_RED;
                }

            case Misc.COLOR_GREEN:
                if (writtenColor == Misc.COLOR_BLUE){
                    return Misc.COLOR_BACKGROUND_GREEN_WRITTEN_BLUE;
                }else{
                    return Misc.COLOR_BACKGROUND_GREEN_WRITTEN_RED;
                }

            case Misc.COLOR_RED:
                if (writtenColor == Misc.COLOR_BLUE){
                    return Misc.COLOR_BACKGROUND_RED_WRITTEN_BLUE;
                }else{
                    return Misc.COLOR_BACKGROUND_RED_WRITTEN_GREEN;
                }

            default:
                return -1;
        }
    }

    // assigns the given activity to the current activity of the game
    public void setUi(PlayActivity ui) {
        this.ui = ui;
    }

    //returns the current activity for the game
    public PlayActivity getUi() {
        return this.ui;
    }

    //returns true if a game is currently running
    public boolean isActive(){
        return this.active;
    }

    // returns thte current state of the game
    public int getGameState(){
        return this.currentState;
    }

}
