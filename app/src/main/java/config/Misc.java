package config;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import java.io.File;

/**
 * Contains all constants and miscellaneous methods.
 *
 * @author Martin
 *
 */
public class Misc {

    /**
     * Set to true when using a real phone and not an emulator!
     */
    public static final boolean REAL_PHONE = true;
    /**
     * Port from server.
     */
    public static final int SERVER_PORT = 9999;
    /**
     * ID from the client.
     */
    public static final int CLIENT_ID = 97357315;
    /**
     * Used for debugging.
     */
    public static final String TAG = "Client";
    /**
     * Encryption algorithm used to create the keyPair with keytool.
     */
    public static final String ENCRYPTION_ALGORITHM = "RSA";
    /**
     * Defines the protocol used by the sockets.
     */
    public static final String TLS_PROTOCOL = "TLSv1.2";
    /**
     * Defines the cipher suite/s used by the sockets.
     */
    public static final String CIPHER_SUITES = "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256";
    /**
     * Type of the KeyStore.
     */
    public static final String KEYSTORE_TYPE = "BKS";
    /**
     * Password for the KeyStore (and authentication of user) is currently hardcoded.
     */
    public static final String KEYSTORE_PASSWORD = "123qwe";
    /**
     * Alias of the certificate for the server.
     */
    public static final String KEYSTORE_SERVER_ALIAS = "server";
    /**
     * Filename of image to send.
     */
    public static final String IMAGE_STRING = "trees.JPG";
    /**
     * Size of the buffer used in transferImage().
     */
    public static final int BUFFER_SIZE = 1024;
    /**
     * Request code for the image capture method.
     */
    public static final int REQUEST_IMAGE_CAPTURE = 1;
    /**
     * Time until timeout in milliseconds (when calling read from stream).
     */
    public static final int TIMEOUT = 10000;
    /**
     * String to display how much time the user has to take the control image.
     */
    public static final String TIME_FOR_CONTROL_IMAGE = "30";

    public static final int MODE_SINGLE = 0;
    public static final int MODE_MULTI = 1;

    public static final int GAME_STATE_INIT = 0;
    public static final int GAME_STATE_BUSY = 1;

    public static final int COLOR_RED = 0;
    public static final int COLOR_GREEN = 1;
    public static final int COLOR_BLUE = 2;
    public static final int COLOR_BACKGROUND_BLUE_WRITTEN_RED = 3;
    public static final int COLOR_BACKGROUND_BLUE_WRITTEN_GREEN = 4;
    public static final int COLOR_BACKGROUND_GREEN_WRITTEN_RED = 5;
    public static final int COLOR_BACKGROUND_GREEN_WRITTEN_BLUE = 6;
    public static final int COLOR_BACKGROUND_RED_WRITTEN_BLUE = 7;
    public static final int COLOR_BACKGROUND_RED_WRITTEN_GREEN = 8;
    public static final int COLOR_BLUE_ANSWER = 9;
    public static final int COLOR_GREEN_ANSWER = 10;
    public static final int COLOR_RED_ANSWER = 11;

    /**
     * Returns a file pointing to the directory where the images taken with this app will be stored.
     * <p>Currently: ExternalStoragePublicDirectory/DIRECTORY_PICTURES/AppForLumberjack/</p>
     *
     * @return the file pointing to the directory
     */
    public static File getDirectory(){

        File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "AppForLumberjack");
        Log.d(Misc.TAG, directory.getName());
        if (!directory.mkdirs()){
            Log.d(Misc.TAG, "Failed to create a new directory in pictures");
        }

        return directory;
    }

}
