package config;

import android.util.Log;

import mmi.colorgame.colorgameclient.Client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import javax.crypto.SealedObject;
import javax.net.ssl.SSLSocket;


/**
 * Handles all operations on the input- and outputStream from the server.
 * <p>The method setupConnection() is used to initialize the streams and
 * set the protocols and cipher suites of the connection.</p>
 * <p>Also implements the methods for the synchronization between client and server.</p>
 * <p>Lastly you can set a timeout for the socket or close the connection.</p>
 *
 * @author Martin
 *
 */
public class CommunicationHandler {

    /**
     * Client this CommunicationHandler is for.
     */
    private final Client client;
    /**
     * Socket of the client.
     */
    private final SSLSocket socketForClient;

    /**
     * outputStream of the client.
     */
    private ObjectOutputStream out;
    /**
     * InputStream of the client.
     */
    private ObjectInputStream in;

    /**
     * Standard constructor.
     *
     * @param client the client this CommunicationHandler is for
     * @param socketForClient the SSLSocket of the client
     */
    public CommunicationHandler(Client client, SSLSocket socketForClient){
        this.client = client;
        this.socketForClient = socketForClient;
    }

    /**
     * Sets up the socket for a TLS 1.2 connection and tries to connect to the server.
     * <p> Calls setStreams() to initialize the streams.
     *
     * @throws IOException if the connection could not be set up
     */
    public void setupConnection() throws IOException {

        Log.d(Misc.TAG, "Setting up connection to server...");

        try {
            Log.d(Misc.TAG, "Setting protocols and cipher suites...");

            setTimeout(Misc.TIMEOUT);

            //set protocols and cipher suites
            //if not a real phone, take suites specified in Misc class
            if (!Misc.REAL_PHONE){
                socketForClient.setEnabledProtocols(new String[]{Misc.TLS_PROTOCOL});
                socketForClient.setEnabledCipherSuites(new String[]{Misc.CIPHER_SUITES});
            }
            //if a real phone
            else{
                //TODO define what cipher suites are allowed
            }


            Log.d(Misc.TAG, "Protocols and cipher suites set!");

            initializeStreams(this.socketForClient);

        }finally{
            setTimeout(0);
        }

        Log.d(Misc.TAG, "Connection to server set up!");
    }

    /**
     * Initializes the streams.
     *
     * @param socket the connected socket
     * @throws IOException if the streams can not be accessed
     */
    private void initializeStreams(SSLSocket socket) throws IOException {

        Log.d(Misc.TAG, "Initializing streams...");

        try {

            //initializing streams
            this.out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            this.out.flush();
            this.in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));

            Log.d(Misc.TAG, "Streams initialized!");

        } catch (SocketTimeoutException eST) {
            Log.e(Misc.TAG, "Timeout while trying to initialize streams", eST);
            throw new IOException("Timeout while initializing streams");
        }
    }

    /**
     * Sends an array of bytes to the server.
     *
     * @param b the array of bytes to send
     * @param off offset in the data
     * @param len number of bytes to write
     * @throws IOException if the transmission failed
     */
    public void sendByteToServer(byte[] b, int off, int len) throws IOException {
        try{
            this.out.write(b, off, len);
            this.out.flush();
        }catch(IOException eIO){
            Log.e(Misc.TAG, "Error while trying to write a byte array to server", eIO);
            throw eIO;
        }
    }

    /**
     * Sends an object to the server.
     *
     * @param obj the object to send
     * @throws IOException if the transmission failed
     */
    public void sendObjectToServer(Object obj) throws IOException {
        try{
            this.out.writeObject(obj);
            this.out.flush();

        }catch(IOException eIO){
            Log.e(Misc.TAG, "IOException while trying to send an object to server", eIO);
            throw eIO;
        }
    }

    /**
     * Reads an object from the server.
     *
     * @return the read object
     * @throws SocketTimeoutException if a timeout occurred
     * @throws IOException if the reading failed
     */
    public Object readObjectFromServer() throws IOException {
        try{
            return this.in.readObject();

        }catch(SocketTimeoutException eST){
            Log.e(Misc.TAG, "Timeout while trying to read an object from server", eST);
            throw eST;

        }catch(Exception e){
            Log.e(Misc.TAG, "Exception while trying to read an object from server", e);
            throw new IOException(e);
        }
    }

    /**
     * Sends a string to the server.
     *
     * @param lineToSend the string to send
     * @throws IOException if the transmission failed
     */
    public void sendLineToServer(String lineToSend) throws IOException {
        try{

            this.out.writeObject(lineToSend);
            this.out.flush();
            //Log.d(Misc.TAG, "Client sent: '" + lineToSend + "'.");

        }catch(IOException eIO){
            Log.e(Misc.TAG, "Failed to send: " + lineToSend, eIO);
            throw eIO;
        }
    }

    /**
     * Reads a string from the server.
     *
     * @return the read string
     * @throws SocketTimeoutException if a timeout occurred
     * @throws IOException if the reading failed
     */
    public String readLineFromServer() throws IOException {
        try{

            //read the object from the stream
            Object result = this.in.readObject();

            //when the object is string, return the string
            if(result instanceof String){
                //Log.d(Misc.TAG, "Server sent: '" + result + "'.");
                return (String) result;

                //otherwise clear the stream and return null
            }else{
                Log.e(Misc.TAG, "Trying to read a string but no string was present in stream, purging stream");
                int remainder;
                if((remainder = this.in.available()) > 0){
                    this.in.skipBytes(remainder);
                }
                return null;
            }

        }catch(OptionalDataException eOD){
            Log.e(Misc.TAG, "No object in stream when trying to read a string, just primitive data", eOD);
            int remainder;
            if((remainder = this.in.available()) > 0){
                this.in.skipBytes(remainder);
            }
            return null;

        }catch(SocketTimeoutException eST){
            Log.e(Misc.TAG, "Timeout while trying to read string from stream", eST);
            throw eST;

        }catch(ClassNotFoundException eCNF){
            Log.e(Misc.TAG, "Class of the object to read from stream was not found", eCNF);
            throw new IOException(eCNF);

        }catch(IOException eIO){
            Log.e(Misc.TAG, "IOException when reading the stream for an object", eIO);
            throw eIO;
        }
    }

    /**
     * Listens for a certain string on stream (will read the stream).
     *
     * @param lineToListenFor the string for the comparison
     * @return true if the read string and the specified string are equal
     * @throws SocketTimeoutException if a timeout occurred
     * @throws IOException if the reading failed
     */
    public boolean listenForLine(String lineToListenFor) throws IOException {

        String response;

        try{

            //read an object from the stream
            Object result = this.in.readObject();

            //try to convert the object to a string
            if (result instanceof String){

                response = (String) result;

                if (response.equals(lineToListenFor)) {
                    //when strings match
                    //Log.d(Misc.TAG, "Server sent: '" + response + "'.");
                    return true;

                }else{
                    //mismatch
                    Log.d(Misc.TAG, "Server sent: '" + response + "', while listening for '" + lineToListenFor + "'.");
                    return false;
                }

            }else{

                //no string in stream
                Log.d(Misc.TAG, "Trying to read a string from stream but no string was found in stream, clearing stream");
                int remainder;
                if((remainder = this.in.available()) > 0){
                    this.in.skipBytes(remainder);
                }
                return false;
            }
        }catch(OptionalDataException eOD){
            Log.e(Misc.TAG, "No object in stream when trying to read a string, just primitive data", eOD);
            int remainder;
            if((remainder = this.in.available()) > 0){
                this.in.skipBytes(remainder);
            }
            return false;

        }catch(SocketTimeoutException eST){
            Log.e(Misc.TAG, "Timeout while waiting for: " + lineToListenFor, eST);
            throw eST;

        }catch(ClassNotFoundException eCNF){
            Log.e(Misc.TAG, "Class not found to read object(string) from stream", eCNF);
            throw new IOException(eCNF);

        }catch(IOException eIO){
            Log.e(Misc.TAG, "Failed to read from the stream", eIO);
            throw eIO;
        }
    }

    /**
     * Attempts to establish synchronization with the server.
     *
     * @return true if the synchronization should be established
     * @throws IOException if the synchronization failed
     */
    public boolean synchronization() throws IOException {

        Log.d(Misc.TAG, "Attempting synchronization...");

        //number of attempts to synchronize
        final int ATTEMPTS = 10;
        //time to wait for answer per attempt (seconds)
        final int TIME_PER_ATTEMPT = 5;

        //set timeout for socket
        try {
            socketForClient.setSoTimeout(TIME_PER_ATTEMPT * 1000);
        } catch (SocketException eS) {
            Log.e(Misc.TAG, "Unable to set timeout for socket", eS);
        }

        try {
            for (int i = 0; i < ATTEMPTS; i++) {

                //initiate synchronization with server
                sendLineToServer(".sync");

                try {
                    //and listen for response
                    if (listenForLine(".confirmSync")) {

                        //server confirmed for the first time
                        //there may still be several instances of ".confirmSync" in the stream
                        //so all remaining instances need to be cleared from the stream!
                        if (finishSynchronization(ATTEMPTS)) {

                            //stream is clear, synchronization should be established
                            Log.d(Misc.TAG, "Synchronization with server successful!");
                            return true;

                        } else {
                            Log.d(Misc.TAG, "Synchronization with server failed!");
                            return false;
                        }
                    }

                } catch (SocketTimeoutException eST) {
                    Log.e(Misc.TAG, "Timeout while waiting for '.confirmSync'", eST);
                    return false;
                } catch (IOException eIO) {
                    Log.d(Misc.TAG, "Failed to read stream, synchronization failed", eIO);
                    throw eIO;
                }
            }

            Log.d(Misc.TAG, "Synchronization with server failed!");
            return false;

        }finally{
            try {
                socketForClient.setSoTimeout(0);
            } catch (SocketException eS) {
                Log.e(Misc.TAG, "Unable to set timeout for socket", eS);
            }
        }
    }

    /**
     * Wraps up synchronization and clears the stream.
     *
     * @param maxAttempts the limit of attempts to try to establish synchronization
     * @return true if the server is idle after clearing the stream (hopefully in sync)
     * @throws IOException if the stream could not be read
     */
    private boolean finishSynchronization(int maxAttempts) throws IOException {

        Log.d(Misc.TAG, "Attempting finish synchronization...");

        int count = 0;

        while(true){
            try {
                if (listenForLine(".confirmSync")) {
                    //received an additional confirmation, continue reading the stream until server is idle

                    Log.d(Misc.TAG, "Received an additional '.confirmSync' waiting for server to be idle!");

                    count++;

                    if (count > maxAttempts) {
                        Log.e(Misc.TAG, "Server is sending too many 'confirmSync'!");
                        return false;
                    }
                } else {
                    //server should be idle after synchronization but more data was found in stream
                    // => close connection
                    Log.d(Misc.TAG, "Received data after '.confirmSync' but server should be idle!");
                    return false;
                }
            }catch(SocketTimeoutException eST){
                Log.d(Misc.TAG, "Timeout while waiting for additional '.confirmSync', server should be idle!");
                return true;
            }
        }

    }

    /**
     * Sends some data (pure testing).
     */
    public void sendSomeSimpleStrings() {

        Log.d(Misc.TAG, "Sending some text to server...");

        try {
            sendLineToServer("Dear Server, some philosophy for you:");

            //next part is sealed with public key from server, because why not
            //sealing the string
            SealedObject so = client.sealObject("Duke, Duke, Duke. (was sealed with server public key)");
            //if not successful inform user and send as cleartext
            if (so == null){
                Log.d(Misc.TAG, "Failed to seal a string, will be sent in cleartext");
                sendLineToServer("Duke, Duke, Duke.");
            }else {
                //command to prepare the server for a sealed string
                sendLineToServer(".sealedString");
                //send the string as object
                sendObjectToServer(so);
            }
            sendLineToServer("Duke of Earl.");
            sendLineToServer("Duke, Duke, Duke of Earl.");
            sendLineToServer("Duke, Duke.");
            sendLineToServer("When I hold you in my arms, you are my duchess of Earl.");
            sendLineToServer("And when I walk through my Dukedom, Paradise we will share-air-air!");
            sendLineToServer("Philosophy over");

            Log.d(Misc.TAG, "Sent some text!");

        } catch (Exception e) {
            Log.e(Misc.TAG, "Failed to send some text!", e);
        }
    }

    /**
     * Closes the connection to the server.
     */
    public void closeConnection() {

        Log.d(Misc.TAG, "Attempting to close connection...");

        try {
            if (socketForClient != null) {
                //if socket already closed
                if (socketForClient.isClosed()) {
                    Log.d(Misc.TAG, "Connection already closed!");
                    return;
                }

                //send command to server that connection will be closed
                sendLineToServer(".close");
                // attempt to close everything
                socketForClient.close();
                Log.d(Misc.TAG, "Closed connection successfully!");

            } else {
                Log.d(Misc.TAG, "No socket exists, that can be closed!");
            }
        } catch (IOException eIO) {
            Log.e(Misc.TAG, "Failed to close connection!", eIO);
        }
    }

    /**
     * Sets the timeout of the ServerSocket to the specified value.
     *
     * @param millis the duration until a timeout occurs (in milliseconds)
     * @throws IOException if an error occurred
     */
    public void setTimeout(int millis) throws IOException {
        this.socketForClient.setSoTimeout(millis);
        Log.d(Misc.TAG, "Timeout for socket set to " + millis + " ms.");
    }
}
