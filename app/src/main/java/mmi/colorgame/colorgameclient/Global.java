package mmi.colorgame.colorgameclient;

import android.app.Application;

public class Global extends Application {

    /**
     * The last client that existed or still exists.
     */
    private Client currentClient = null;

    private ColGame currentgame = null;

    private int currentScore = 0;



    /**
     * Sets the current Client.
     * @param client the new client
     */
    public void setCurrentThread(Client client){
        this.currentClient = client;
    }

    /**
     * Returns the current client.
     * @return the current client
     */
    public Client getCurrentThread(){
        return this.currentClient;
    }

    public ColGame getCurrentgame() {
        return currentgame;
    }

    public void setCurrentgame(ColGame currentgame) {
        this.currentgame = currentgame;
    }

    public void increaseSocre(){
        this.currentScore = this.currentScore+1;
    }

    public int getCurrentScore(){
        return this.currentScore;
    }

    public void resetCurrentScore(){
        this.currentScore = 0;
    }
}
