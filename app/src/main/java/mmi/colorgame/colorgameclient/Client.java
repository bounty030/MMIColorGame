package mmi.colorgame.colorgameclient;

import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.KeyStoreException;
import java.security.SignedObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.crypto.SealedObject;
import javax.net.ssl.SSLSocket;

import config.CommunicationHandler;
import config.ImageTransferHandler;
import config.KeyStoreHandler;
import config.Misc;

/**
 * The client thread.
 * <p>Once started will automatically try to connect to the given ip address
 * by calling evaluateConnectionInitialization(). After successful connection
 * the client is in loop and waits for the user issue commands. The flags represent
 * those commands. They can be set with the public set methods.</p>
 * <p>Every client has a Communication-, ImageTransfer- and KeyStoreHandler,
 * which implement all methods regarding their assignments.</p>
 * <p>A client also updates his own state when a command was received so that other processes
 * can get the information in what state the thread is in.</p>
 * <p>There is currently the option to:</p>
 * <p>1. Send an image to the server (evaluateSendImage())</p>
 * <p>2. Request and listen for a token (evaluateListenForToken())</p>
 * <p>3. Send some strings to the server (implemented in CommunicationHandler)</p>
 * <p>4. Close the connection to the server and stop the thread (simply exits the loop)</p>
 */
public class Client extends Thread {

    /**
     * Defines the state a client can be in.
     */
    public static final int
            STATE_SART = 0,
            STATE_PLAY = 1;

    /**
     * Activity acting as user interface with all the Buttons and TextViews.
     */
    private PlayActivity userInterface;

    /**
     * IP address of the server.
     */
    private final String serverIP;

    /**
     * Socket of the client.
     */
    private SSLSocket socketForClient;

    /**
     * Handler for communication.
     */
    private CommunicationHandler ch;
    /**
     * Handler for image operations.
     */
    private ImageTransferHandler ih;
    /**
     * Handler for KeyStore.
     */
    private KeyStoreHandler kh;

    /**
     * Current state of the client.
     */
    private int state;

    /**
     * Flag for executing a command.
     */
    private boolean
            closeConnection,
            sendImage,
            sendNonsense,
            requestToken,
            imageConfirmed;

    /**
     * Used to store temporary data.
     */
    private String
            token,
            currentStatus;


    public Client(String serverIP_string){
        if(serverIP_string != null){ this.serverIP = serverIP_string; }
        else{ this.serverIP = null; }
        this.socketForClient = null;
        this.closeConnection = false;
        this.sendImage = false;
        this.sendNonsense = false;
        this.token = null;
        this.state = -1;
    }

    /**
     * Constructor that will set the ip of of the server.
     *
     * @param serverIP_string ip from the server
     * @param ui the activity that handles the user interface
     */
    public Client(String serverIP_string, PlayActivity ui){
        if(serverIP_string != null){ this.serverIP = serverIP_string; }
        else{ this.serverIP = null; }
        this.userInterface = ui;
        this.socketForClient = null;
        this.closeConnection = false;
        this.sendImage = false;
        this.sendNonsense = false;
        this.token = null;
        this.state = -1;
    }

    /**
     * Initializes connection to server and waits for user input.
     */
    public void run() {

        try {

            //try to establish connection to the server and evaluate the result
            evaluateConnectionInitialization();

            //loop: waiting for input from user until close button is pushed or thread gets interrupted
            while (!isInterrupted() && !closeConnection) {

                //image button
                //send an image to the server (for emulator) or take one with the camera
                if (sendImage) {
                    Log.d(Misc.TAG, "Image button pushed!");

                    //try to send an image and evaluate the result
                    evaluateSendImage(Misc.IMAGE_STRING);

                    //unset flag
                    sendImage = false;

                }

                //token button
                if (requestToken){
                    Log.d(Misc.TAG, "Token button pushed!");

                    //send command to server and evaluate response
                    evaluateListenForToken();

                    //unset flag
                    requestToken = false;

                }

                //poetry button: send some simple strings
                if (sendNonsense) {
                    Log.d(Misc.TAG, "Poetry button pushed!");

                    //send some simple strings to the server
                    ch.sendSomeSimpleStrings();

                    //unset flag
                    sendNonsense = false;
                }
            }

            if (closeConnection && !isInterrupted()) {
                this.userInterface.returnResult("Communication with server was successful, connection closed.");
            }

        } finally {
            //try to close the connection
            try {
                ch.closeConnection();
            }catch(Exception e){
                Log.e(Misc.TAG, "Connection can not be closed", e);
            }
            //close the user interface for the client
            this.userInterface.closeActivity();
        }

    }

    /**************************************************************************************************
     creates socket and connects to server
     **************************************************************************************************/

    /**
     * Initializes all the handlers for the client and attempts to establish a connection to the server.
     * <p>1. Loads the KeyStore from a file (BKS) and initializes KeyStoreHandler</p>
     * <p>2. Attempts to connect the socket to the server</p>
     * <p>3. Initializes CommunicationHandler and and configures the connection (streams, cipher suites, etc.)</p>
     * <p>4. Initializes ImageTransferHandler</p>
     * <p>5. Sends the ID of the client to the server</p>
     */
    private void evaluateConnectionInitialization() {

        //if an exception occurs the connection will be canceled and the client will be stopped
        try {

            //load keystore from file
            InputStream is = this.userInterface.getResources().openRawResource(R.raw.client);

            //initialize KeyStoreHandler
            this.kh = new KeyStoreHandler(is);

            //initialize socket
            this.socketForClient = kh.connectToSocket(this.serverIP, Misc.SERVER_PORT);

            //initialize CommunicationHandler
            this.ch = new CommunicationHandler(this, this.socketForClient);

            //configure connection
            this.ch.setupConnection();

            //initialize ImageTransferHandler
            //this.ih = new ImageTransferHandler(this, ch);

            //send id from client to server
            this.ch.sendLineToServer(Integer.toString(Misc.CLIENT_ID));

            //inform user and make buttons for interactions visible
            setStatus("Connection to server successful");

            //set the state of the client to idle (waiting for user input)
            //setState(STATE_IDLE);

            return;

        } catch (KeyStoreException eKS) {
            this.userInterface.returnResult("Failed to load the keystore, check file");

        } catch (UnknownHostException eUH) {
            if (serverIP != null) {
                this.userInterface.returnResult("Unable to find specified server-IP and/or name: " + serverIP);
            } else {
                this.userInterface.returnResult("No IP entered, please enter a valid IP-Address");
            }

        } catch (Exception e) {
            this.userInterface.returnResult("Failed to establish connection to server, check server status");
        }

        //interrupt thread and return
        interrupt();
    }

    /**************************************************************************************************
     requests a token for the last image
     **************************************************************************************************/

    /**
     * Requests a token from the server and evaluates the response.
     * The token will be stored in a private field that can be accessed with getToken().
     * <p>If a SocketTimeoutException occurs, a button will appear, allowing the user to send another request to the server.
     * All other exceptions will cancel the communication and stop the thread.</p>
     */
    private void evaluateListenForToken(){

        //execute token request and evaluate response
        try{
            ih.requestToken();

            if ((this.token = ih.listenForTokenForImage()) != null) {

                //token received
                setStatus("Received token from server");

            }else{

                //server will not send a token for the last image
                setStatus("Server will not send a token for the last image");
            }

            //return to being idle
            //setState(STATE_IDLE);

        }catch(SocketTimeoutException |NullPointerException e) {

            //timeout or unexpected response => synchronization and show token request button
            Log.e(Misc.TAG, "Token transfer failed, synchronization necessary");
            evaluateSynchronization();
            setStatus("Error while trying to read the token, if this error continues contact local overlord");
            //setState(STATE_TOKEN_REQUEST);

        }catch(Exception e) {
            this.userInterface.returnResult("Error occurred during token transfer, please contact your local overlord");
            interrupt();
        }
    }

    /**************************************************************************************************
     synchronizes with the server
     **************************************************************************************************/

    /**
     * Synchronizes with the server and evaluates the result. If the synchronization failed (independent on the reason),
     * the thread will be stopped.
     */
    private void evaluateSynchronization(){
        try{

            //execute synchronization
            if (ch.synchronization()){
                Log.d(Misc.TAG, "Synchronization successful!");
                return;
            }

            //if synchronization failed interrupt thread
            this.userInterface.returnResult("Synchronization with server failed, try reconnect");
            interrupt();

        }catch(Exception e){
            //set return intent and interrupt thread
            this.userInterface.returnResult("Synchronization with server failed, try reconnect");
            interrupt();
        }
    }

    /**************************************************************************************************
     image transfer
     **************************************************************************************************/

    /**
     * Evaluates the result from "sendImage(String fileName)".
     * <p>The different exceptions that can occur will be evaluated
     * and information for the user will be displayed.
     * In addition the method will evaluate the response from the server
     * and act accordingly.</p>
     *
     * @param file the filename of the image (if no image will be taken with the camera)
     */
    private void evaluateSendImage(String file){

        Log.d(Misc.TAG, "Initiating image transfer...");

        //prevent the user from hammering the buttons until transfer is completed
        //setState(STATE_IMAGE_TRANSFER);

        File image = null;
        BufferedInputStream bufferedInputStreamFromFile = null;


        /*
        the only difference between a new image and a control image is - for the client -
        that the the control images are not stored on the sd card (only the last one is stored and will be overwritten)
         */
        try{

            //check whether to take an actual photo
            if(Misc.REAL_PHONE){

                //execute image capture with camera
                if ((image = evaluateTakeImageWithCamera()) == null){
                    setStatus("Failed to take an image with the camera, contact overlord.");
                    return;
                }

                //initialize inputStream on file
                try{
                    bufferedInputStreamFromFile = new BufferedInputStream(new FileInputStream(image));
                }catch(FileNotFoundException eFNF){
                    Log.e(Misc.TAG, "Image could not be located", eFNF);
                    setStatus("Failed to load the stored image form sd card, transmission canceled.");
                    return;
                }

            }
            //load a stored image
            else {
                try {
                    //source for the image (currently in "res/raw" called with getAssets())
                    bufferedInputStreamFromFile = new BufferedInputStream(this.userInterface.getAssets().open(file));

                } catch (IOException eIO) {
                    //when no file could be found
                    Log.e(Misc.TAG, "No image could be found in the specified path", eIO);
                    interrupt();
                }
            }

            //execute image transfer with the inputStream pointing on the file
            if(ih.sendImage(bufferedInputStreamFromFile)) {

                Log.d(Misc.TAG, "Image transfer done!");

                Log.d(Misc.TAG, "Waiting for server to respond...");

                //check for response from server
                String response;
                if ((response = ch.readLineFromServer()) != null){

                    //transfer successful and valid response
                    switch (response) {

                        //server received first image and requests a control image
                        case (".imageRequest"):

                            Log.d(Misc.TAG, "Server requests control image!");

                            //evaluate the instructions from the server and display them on screen
                            prepareControlImage();
                            return;

                        //server is satisfied with the control image
                        case (".imageDone"):

                            Log.d(Misc.TAG, "Control image validated the previous image!");

                            setStatus("Second image validated first image, server accepted first image as measurement");

                            //if the control image was taken with a camera it can be deleted
                            if(Misc.REAL_PHONE) {
                                if (!image.delete()) {
                                    Log.d(Misc.TAG, "Failed to delete the control image");
                                }
                            }

                            //request a token for the first image
                            evaluateListenForToken();
                            return;

                        //the control image did not validate the first image
                        case (".imageReject"):

                            Log.d(Misc.TAG, "Control image failed to validate the previous image!!");

                            //image can be deleted
                            if(Misc.REAL_PHONE) {
                                if (!image.delete()) {
                                    Log.d(Misc.TAG, "Failed to delete the control image");
                                }
                            }

                            setStatus("Server found inconsistency in images, please try again.");
                            return;
                    }
                }
            }

            //communication error, try synchronization
            evaluateSynchronization();

        }catch(FileNotFoundException eFNF){

            //specified file could not be loaded as an image
            setStatus("No image found, check name or path");

        } catch (SocketTimeoutException eST){

            evaluateSynchronization();

            //synchronization successful
            setStatus("Image transfer failed due to communication error. Reestablished synchronization to server.\nTry again or contact local overlord");

        }catch(Exception e){

            //error occurred, connection will be closed
            this.userInterface.returnResult("Error occurred, please contact your local overlord");
            interrupt();

        }finally{
            //setState(STATE_IDLE);
        }
    }

    /**************************************************************************************************
     takes an image with the camera
     **************************************************************************************************/

    /**
     * Attempts to take an image with the camera of the phone.
     * <p>The image is stored on the sd card in directory defined in the Misc class.
     * The name of the iamge is based on the current time of the phone.</p>
     *
     * @return a file pointing to the captured image
     */
    private File evaluateTakeImageWithCamera(){

        //get path to picture from Misc.class
        File directory = Misc.getDirectory();
        File fileImage;

        //string to define the name of the image file
        String info;
        info = new SimpleDateFormat("yyyyMMdd_HH_mm_ss", Locale.GERMANY).format(new Date());

        //create file in the directory with a filename depending on the time
        fileImage = new File(directory, "wood" + info + ".jpg");

        //for the activity that takes the picture
        Uri fileUri = Uri.fromFile(fileImage);

        //create intent and take picture
        Intent takeImageIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takeImageIntent.resolveActivity(this.userInterface.getPackageManager()) == null) {
            return null;
        }

        //start activity to take picture (from user interface activity)
        takeImageIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
        this.userInterface.startActivityForResult(takeImageIntent, Misc.REQUEST_IMAGE_CAPTURE);

        //wait for user to confirm that the image was taken
        this.imageConfirmed = false;
        //setState(STATE_IMAGE_CONFIRM);

        setStatus("Please confirm the image.");

        while (true){
            if (imageConfirmed) break;
        }

        //user confirmed, proceed with image transfer
        //setState(STATE_IMAGE_TRANSFER);

        return fileImage;
    }

    /**************************************************************************************************
     miscellaneous
     **************************************************************************************************/

    /**
     * Reads and displays instructions from server.
     *
     * @throws IOException if server responded unexpected and the consequential synchronization attempt failed
     */
    private void prepareControlImage() throws IOException {

        if (!(ch.listenForLine(".instructions"))) {

            //communication error, try synchronization
            evaluateSynchronization();

            setStatus("Server did not send instructions for a second image, reestablished synchronization. \n If this error continues contact your local Overlord");
            return;
        }

        //received instructions
        String instructions = ch.readLineFromServer();
        setStatus("Image transfer successful, please take a control image within " + Misc.TIME_FOR_CONTROL_IMAGE + " seconds with the following instructions:\n" + instructions);
    }

    /**
     * Sets the state of the client. Will call the activity.setUI() function to update the user interface according to the state.
     * Possible states are:
     * <p>STATE_IMAGE_IDLE: idle (waiting for user input)</p>
     * <p>STATE_IMAGE_TRANSFER: image transfer in progress</p>
     * <p>STATE_IMAGE_CONFIRM:  waiting for user to confirm image capture</p>
     * <p>STATE_TOKEN_REQUEST:  allowing user to send a token request for the last image</p>
     *
     * @param state the state of the client
     */
    private void setState(int state){
        //Log.d(Misc.TAG, "State set to " + state);
        this.state = state;
        this.userInterface.setUI();
    }

    /**
     * Returns the state in which the client currently is in.
     *
     * @return the current state of the client
     */
    public int getSate(){
        return this.state;
    }

    /**
     * Tells the client what activity acts as the user interface.
     *
     * @param ui the activity acting as user interface
     */
    public void setUIActivity(PlayActivity ui){
        this.userInterface = ui;
    }

    /**
     * Returns the token of the last received image.
     *
     * @return token of the last image, null if no token has been received yet
     */
    public String getToken(){
        return this.token;
    }

    /**
     * Calls the verifySignedObject() method of the KeyStoreHandler to verify a signature from the server.
     *
     * @param signed the object to verify
     * @return true if the signature is valid
     */
    public boolean verifySignature(SignedObject signed){
        return kh.verifySignedObject(signed);
    }

    /**
     * Calls the sealObject() method of the KeyStoreHandler to seal an object with the certificate of the server.
     *
     * @param obj the object to seal
     * @return the sealed object, null if unsuccessful
     */
    public SealedObject sealObject(Serializable obj){
        return kh.sealObject(obj);
    }

    /**
     * Sets the flag to send an image.
     */
    public void setSendImage(){
        this.sendImage = true;
    }

    /**
     * Sets the flag to send simple strings.
     */
    public void setSendNonsense(){
        this.sendNonsense = true;
    }

    /**
     * Sets the flag to close the connection.
     */
    public void setCloseConnection(){
        this.closeConnection = true;
    }

    /**
     * Sets the flag to request a token.
     */
    public void setRequestToken(){
        this.requestToken = true;
    }

    /**
     * Sets the flag that the user confirmed the taken picture.
     */
    public void setImageConfirmed() { this.imageConfirmed = true; }

    /**
     * Sets the status of the client to given text. Will also update the user interface!
     *
     * @param status the status to display
     */
    public void setStatus(String status) {
        //this.userInterface.setStatusText(status);
        //this.currentStatus = status;
    }

    /**
     * Returns the current status of the client.
     *
     * @return status of the client
     */
    public String getCurrentStatusText(){
        return this.currentStatus;
    }
}
