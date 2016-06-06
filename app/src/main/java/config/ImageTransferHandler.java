package config;

import android.util.Log;

import mmi.colorgame.colorgameclient.Client;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignedObject;
import java.util.Arrays;

import mmi.colorgame.colorgameclient.Client;

/**
 * Handles all operations concerning the image transfer between client and server.
 * <p>For more information see the diagrams for a detailed communication between server and client.</p>
 *
 * @author Martin
 *
 */
public class ImageTransferHandler {

    /**
     * The client this ImageTransferHandler is for.
     */
    private final Client client;
    /**
     * The CommunicationHandler of the Client this ImageTransferHandler is for.
     */
    private final CommunicationHandler ch;

    /**
     * Standard Constructor.
     *
     * @param client the client this ImageTransferHandler is for
     * @param ch the ConnectionHandler for the client
     */
    public ImageTransferHandler(Client client, CommunicationHandler ch){
        this.client = client;
        this.ch = ch;
    }

    /**
     * Sends an image to the server. Calls the transferImage() method.
     *
     * @param image the image to transfer
     * @return true if the transfer was successful (with matching hash)
     * @throws SocketTimeoutException if a timeout occurred (leads to sync)
     * @throws NoSuchAlgorithmException if the algorithm specified for the hash calculation is not supported
     * @throws IOException if the stream could not be read
     */
    public boolean sendImage(BufferedInputStream image) throws IOException, NoSuchAlgorithmException {
        try{

            Log.d(Misc.TAG, "Preparing image transfer...");

            //inform user
            //client.setStatus("Image transfer in process...");

            //set the timeout for the socket
            try {
               ch.setTimeout(Misc.TIMEOUT);

            } catch (SocketException eS) {
                Log.e(Misc.TAG, "Failed to set timeout for socket, might run forever!", eS);
            }

            //inform server that an image is about to be sent
            ch.sendLineToServer(".image");

            //listen for response from server
            //check if server confirmed transfer
            String response = ch.readLineFromServer();
            if (response == null){
                response = "";
            }

            switch(response){

                case (".imageTime"):
                    //expected control image from server was not sent in time
                    //client.setStatus("The control image was not sent in time");
                    Log.d(Misc.TAG, "The control image was not sent in time");
                    return false;

                case (".imageConfirm"):
                    break;

                default:
                    //server did not confirm transfer, canceling
                    return false;
            }

            Log.d(Misc.TAG, "Preparations for image transfer done!");

            //execute transfer of image and return resulting integer
            return transferImage(image);

        }finally{

            try {
                //set timeout back to infinity
                ch.setTimeout(0);

                //close inputStream
                if (image != null){
                    image.close();
                }

            }catch(SocketException eS){
                Log.d(Misc.TAG, "Unable to set timeout back to infinite, that should not happen!");
            } catch (IOException eIO) {
                Log.e(Misc.TAG, "SendImage: Error occurred while trying to close inputStream for image-file, check code", eIO);
            }
        }
    }

    /**
     * Transfers the image to the server and compares the hash.
     *
     * @param streamFromFile the stream to the image file
     * @return true if the transfer was successful and the hash is equal
     * @throws SocketTimeoutException if a timeout occurred (leads to sync)
     * @throws NoSuchAlgorithmException if the algorithm specified for the hash calculation is not supported
     * @throws IOException if the stream could not be read
     */
    private boolean transferImage(BufferedInputStream streamFromFile) throws IOException, NoSuchAlgorithmException {

        Log.d(Misc.TAG, "Starting image transfer...");

        /***** prepare image transfer *****/

        //buffer-size for the image-transfer
        byte[] buffer = new byte[Misc.BUFFER_SIZE];

        //initialize messageDigest for hash-calculation
        MessageDigest myMD;
        //create MessageDigest to calculate hash
        try {
            myMD = MessageDigest.getInstance("SHA-256");

        } catch (NoSuchAlgorithmException eNSA) {
            Log.e(Misc.TAG, "Error while trying to create messageDigest, check algorithm", eNSA);
            throw eNSA;
        }

        try {
            //send the size of the image to server (byte)
            ch.sendLineToServer(Integer.toString(streamFromFile.available()));
            //send the size of the buffer (byte) to transfer the image
            ch.sendLineToServer(Integer.toString(buffer.length));

        }catch(IOException eIO){
            Log.e(Misc.TAG, "Failed to send buffer initialization parameters to server", eIO);
            throw eIO;
        }

        /***** execute image transfer *****/

        Log.d(Misc.TAG, "Sending Image...");

        try {
            //read image into the buffer and write it to the outputStream from the socket (in cycles)
            int len;
            while ((len = streamFromFile.read(buffer)) > 0) {
                ch.sendByteToServer(buffer, 0, len);
                //update the message digest for hash calculation
                myMD.update(buffer, 0, len);
            }
        }catch(SocketTimeoutException eST){
            Log.d(Misc.TAG, "Timeout occurred while transferring the image", eST);
            throw eST;

        }catch(IOException eIO){
            Log.e(Misc.TAG, "Failed to send image to server", eIO);
            throw eIO;
        }

        Log.d(Misc.TAG, "Image sent!");

        //wait for answer from server and react depending on response
        try{
            if(!ch.listenForLine(".imageReceived")){
                Log.d(Misc.TAG, "Server responded unexpected while waiting for '.receivedImage'");
                //client.setStatus("Server did not receive image, please try again");
                return false;
            }

        }catch(SocketTimeoutException eST){
            Log.d(Misc.TAG, "Timeout occurred while waiting for '.receivedImage'");
            throw eST;

        }catch(IOException eIO){
            Log.d(Misc.TAG, "Unable to access stream while waiting for '.receivedImage'");
            throw eIO;
        }

        /***** server confirmed the transfer *****/

        Log.d(Misc.TAG, "Checking hash from server...");

        //prepare hash calculation for the image
        byte[] hashOfImage = myMD.digest();

        //try to read size of hash from server
        String response;
        int lengthOfByteArray;

        try {
            //check if a valid string was read from stream
            if ((response = ch.readLineFromServer()) == null){

                Log.d(Misc.TAG, "Invalid response from server while waiting for size of hash!");
                //client.setStatus("Image got corrupted, please try again");
                ch.sendLineToServer(".imageCorrupt");
                return false;
            }

        }catch (SocketTimeoutException eST) {
            Log.d(Misc.TAG, "Timeout occurred while waiting for size of hash!", eST);
            throw eST;

        }catch(IOException eIO){
            Log.e(Misc.TAG, "Unable to access stream while waiting for size of hash", eIO);
            throw eIO;
        }

        //try to parse the received string to integer
        try {
            lengthOfByteArray = Integer.parseInt(response);

        }catch(NumberFormatException eNF){
            Log.e(Misc.TAG, "Failed to read the size of hash", eNF);
            //client.setStatus("Image got corrupted, please try again");
            ch.sendLineToServer(".imageCorrupt");
            return false;
        }

        //in case the size is different, return with an error
        if (lengthOfByteArray != hashOfImage.length) {

            Log.d(Misc.TAG, "Hash size sent from server differs!");
            //client.setStatus("Image got corrupted, please try again");
            ch.sendLineToServer(".imageCorrupt");
            return false;
        }

        //validate the signature from the server and retrieve the hash
        byte[] hashFromServer;

        try {
            // read the signed object from stream
            SignedObject signedHash = (SignedObject)ch.readObjectFromServer();

            //verify the signature
            if (client.verifySignature(signedHash)){

                //if correct, get the object (hash) from server
                hashFromServer = (byte[])signedHash.getObject();

            }else{
                //if not, return with error
                client.setStatus("Server failed to verify, something is fishy...");
                ch.sendLineToServer(".imageCorrupt");
                return false;
            }

        }catch(ClassNotFoundException eCNF){
            Log.e(Misc.TAG, "Unable to read the signed hash", eCNF);
            throw new IOException("Hash verification failed");
        }
        catch(IOException eIO){
            Log.e(Misc.TAG, "Failed to read the calculated hash of the image from the server", eIO);
            throw eIO;
        }

        //if calculated and received hash are equal, send confirmation to server
        if (Arrays.equals(hashOfImage, hashFromServer)) {

            Log.d(Misc.TAG, "Calculated hash from server is identical!");

            //send confirmation to server
            ch.sendLineToServer(".imageSuccess");

            Log.d(Misc.TAG, "Image transfer successful!");

            return true;

        } else {

            //otherwise return with an error
            Log.d(Misc.TAG, "Calculated hash from server differs!");
            client.setStatus("Image got corrupted, please try again");
            ch.sendLineToServer(".imageCorrupt");
            return false;
        }
    }

    /**
     * Requests a token from the server.
     *
     * @throws IOException if an error occurred
     */
    public void requestToken() throws IOException {
        Log.d(Misc.TAG, "Sending a request for a token!");
        ch.sendLineToServer(".tokenForImage");
    }

    /**
     * Listens for a token from the server.
     *
     * @return true if the server sent a token
     * @throws SocketTimeoutException if a timeout or an unexpected response occurred
     * @throws NullPointerException if no string was found ins stream
     * @throws IOException if the stream could not be read
     */
    public String listenForTokenForImage() throws IOException {
        Log.d(Misc.TAG, "Listening for a token...");

        //string to store the response from server
        String response;

        try{
            //try to read a string from stream
            if((response = ch.readLineFromServer()) == null){
                Log.d(Misc.TAG, "No string in stream while trying to read the response for a token request!");
                throw new NullPointerException("No string in stream");
            }

            //depending on answer
            switch (response) {

                case (".noTokenForImage"):
                    //server will not give a token for the image
                    Log.d(Misc.TAG, "Server will not give a token for the image!");
                    return null;

                case (".tokenForImage"):
                    //server will send a token
                    try{
                        String result;

                        //try to read the token
                        if ((result = ch.readLineFromServer()) == null){
                            Log.d(Misc.TAG, "No string in stream while trying to read the token!");
                            throw new NullPointerException("No string in stream");
                        }

                        Log.d(Misc.TAG, "Received token for image!");
                        return result;

                    }catch(SocketTimeoutException eST) {
                        Log.e(Misc.TAG, "Timeout occurred while trying to read the token", eST);
                        throw eST;
                    }catch(IOException eIO) {
                        Log.e(Misc.TAG, "Failed to read the token", eIO);
                        throw eIO;
                    }

                default:
                    Log.d(Misc.TAG, "Server responded unexpected while waiting for response for a token request!");
                    throw new NullPointerException("Server responded unexpected");
            }

        }catch(SocketTimeoutException eST) {
            Log.e(Misc.TAG, "Timeout occurred while trying to read the response for a token request", eST);
            throw eST;
        }catch(IOException eIO){
            Log.e(Misc.TAG, "Failed to read the response for a token request", eIO);
            throw eIO;
        }
    }


}