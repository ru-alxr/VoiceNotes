package mx.alxr.voicenotes.utils.extensions;

import android.util.Base64;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileUtils {

    private static String hashFile(File file, String algorithm) {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance(algorithm);

            byte[] bytesBuffer = new byte[1024];
            int bytesRead = -1;

            while ((bytesRead = inputStream.read(bytesBuffer)) != -1) {
                digest.update(bytesBuffer, 0, bytesRead);
            }

            byte[] hashedBytes = digest.digest();
            String base64 = Base64.encodeToString(hashedBytes, Base64.DEFAULT);
            return base64.trim();
        } catch (NoSuchAlgorithmException | IOException ex) {
            throw new RuntimeException("Could not generate hash from file", ex);
        }
    }

    public static String generateMD5(File file) throws RuntimeException {
        return hashFile(file, "MD5");
    }

    public static String generateSHA1(File file) throws RuntimeException {
        return hashFile(file, "SHA-1");
    }

    public static String generateSHA256(File file) throws RuntimeException {
        return hashFile(file, "SHA-256");
    }

}