package config;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.SignedObject;
import java.security.cert.Certificate;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

/**
 *  Handles all operations concerning the KeyStore from the client.
 */
public class KeyStoreHandler {

    /**
     * The KeyStore of the client.
     */
    private KeyStore keyStoreClient;

    /**
     * The SSLSocketFactory used to create a socket that connects to the server.
     */
    private SSLSocketFactory socketFactory;

    /**
     * Loads the specified KeyStore from a file and initializes the SocketFactory.
     *
     * @param isKeyStore stream to the file containing the KeyStore
     * @throws KeyStoreException if the initialization of the SocketFactory failed
     */
    public KeyStoreHandler(InputStream isKeyStore) throws KeyStoreException {

        Log.d(Misc.TAG, "Initializing KeyStoreHandler...");

        /*** initialization of KeyStore ***/

        try {

            Log.d(Misc.TAG, "Loading KeyStore...");

            //get an instance of the KeyStore
            this.keyStoreClient = KeyStore.getInstance(Misc.KEYSTORE_TYPE);

            //load the KeyStore from file (password required)
            this.keyStoreClient.load(isKeyStore, Misc.KEYSTORE_PASSWORD.toCharArray());

            Log.d(Misc.TAG, "KeyStore loaded!");

        } catch (Exception e) {
            Log.e(Misc.TAG, "Failed to load KeyStore!", e);
            throw new KeyStoreException(e);
        }

        /*** initialization of TrustManager ***/

        TrustManagerFactory myTrustManagerFactory;
        TrustManager[] myTrustManager;

        try {
            myTrustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            myTrustManagerFactory.init(this.keyStoreClient);
            myTrustManager = myTrustManagerFactory.getTrustManagers();
        } catch (Exception e) {
            Log.e(Misc.TAG, "Failed to initialize TrustManager!", e);
            throw new KeyStoreException(e);
        }

        /*** initialization of KeyManager ***/

        KeyManagerFactory myKeyManagerFactory;
        KeyManager[] myKeyManager;

        try {
            myKeyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            myKeyManagerFactory.init(this.keyStoreClient, Misc.KEYSTORE_PASSWORD.toCharArray());
            myKeyManager = myKeyManagerFactory.getKeyManagers();
        } catch (Exception e) {
            Log.e(Misc.TAG, "Failed to initialize KeyManager!", e);
            throw new KeyStoreException(e);
        }

        /*** initialization of SSLContext (with Key- and TrustManager) ***/

        SSLContext mySSLContext;

        try {
            mySSLContext = SSLContext.getInstance(Misc.TLS_PROTOCOL);
            mySSLContext.init(myKeyManager, myTrustManager, null);
        } catch (Exception e) {
            Log.e(Misc.TAG, "Failed to initialize SSLContext!", e);
            throw new KeyStoreException(e);
        }

        /*** initialization of SSLSocketFactory ***/

        try {
            this.socketFactory = mySSLContext.getSocketFactory();
        } catch (Exception e) {
            Log.e(Misc.TAG, "Failed to initialize SocketFactory!", e);
            throw new KeyStoreException(e);
        }

        Log.d(Misc.TAG, "KeyStoreHandler initialized!");

        //print all available cipher suites on screen if a real smart phone is used
        if (Misc.REAL_PHONE){
            Log.d(Misc.TAG, "Available cipher suites: " + Arrays.toString(this.socketFactory.getSupportedCipherSuites()));
        }

    }

    /**
     * Connects to the server and initializes the socket.
     *
     * @param ip the ip of the server
     * @param port the port of the server
     * @return a SSLSocket for the client that is connected to the socket of the server
     * @throws UnknownHostException if no host could be found under specified ip/port
     * @throws IOException if the stream could not be read
     */
    public SSLSocket connectToSocket(String ip, int port) throws IOException {

        Log.d(Misc.TAG, "Connecting SSLSocket to " + ip + "...");

        //connect with socket (serverIP as string)
        SSLSocket socketForClient = (SSLSocket) this.socketFactory.createSocket(ip, port);

        Log.d(Misc.TAG, "SSLSocket connected to " + ip + "!");

        return socketForClient;
    }

    /**
     * Verifies the signature from an object.
     *
     * @param signed the signed object to verify
     * @return true if the verification was successful
     */
    public boolean verifySignedObject(SignedObject signed){

        Log.d(Misc.TAG, "Verifying the server signature...");

        try {
            //load certificate from server
            Certificate serverCertificate = this.keyStoreClient.getCertificate(Misc.KEYSTORE_SERVER_ALIAS);

            //initialize signature engine for verification process
            Signature signer = Signature.getInstance("SHA256withRSA");

            //verify signed object with public key from sever
            boolean result = signed.verify(serverCertificate.getPublicKey(), signer);

            if (result){
                Log.d(Misc.TAG, "Server signature verified!");
            }else{
                Log.d(Misc.TAG, "Server signature failed to verify!");
            }

            return result;

        }catch(KeyStoreException eKS){
            Log.e(Misc.TAG, "Unable to access keyStore to verify a signed object", eKS);
            return false;
        }catch(NoSuchAlgorithmException eNSA){
            Log.e(Misc.TAG, "Encryption algorithm is not supported", eNSA);
            return false;
        }catch(InvalidKeyException eIK){
            Log.e(Misc.TAG, "The verification key (server certificate) is invalid", eIK);
            return false;
        }catch(SignatureException eS){
            Log.e(Misc.TAG, "Signature verification failed", eS);
            return false;
        }
    }

    /**
     * Seals an object with a public key in the keystore.
     *
     * @param objectToSeal the object that shall be sealed
     * @return the sealed object, null if sealing failed
     */
    public SealedObject sealObject(Serializable objectToSeal){

        Log.d(Misc.TAG, "Sealing an object with server public key...");

        try{

            //get the server certificate from keyStore
            Certificate serverCertificate = this.keyStoreClient.getCertificate(Misc.KEYSTORE_SERVER_ALIAS);

            //initialize the cipher
            Cipher enCipher = Cipher.getInstance(Misc.ENCRYPTION_ALGORITHM + "/ECB/PKCS1Padding");
            enCipher.init(Cipher.ENCRYPT_MODE, serverCertificate);

            //return the sealed object
            SealedObject result = new SealedObject(objectToSeal, enCipher);

            Log.d(Misc.TAG, "Object sealed with server public key!");

            return result;

        }catch(KeyStoreException eKS){
            Log.e(Misc.TAG, "Unable to seal an object with certificate from client", eKS);
            return null;
        } catch (NoSuchAlgorithmException eNSA) {
            Log.e(Misc.TAG, "Algorithm ist not supported", eNSA);
            return null;
        } catch (NoSuchPaddingException eNSP) {
            Log.e(Misc.TAG, "Padding ist not supported", eNSP);
            return null;
        } catch (InvalidKeyException eIK) {
            Log.e(Misc.TAG, "Invalid key to sign the object", eIK);
            return null;
        } catch (IllegalBlockSizeException eIBS) {
            Log.e(Misc.TAG, "Cipher is unable to seal the object due to mismatching block size", eIBS);
            return null;
        } catch (IOException eIO) {
            Log.e(Misc.TAG, "Error occurred during serialization", eIO);
            return null;
        }
    }
}
