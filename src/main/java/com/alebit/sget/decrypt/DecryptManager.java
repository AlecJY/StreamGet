package com.alebit.sget.decrypt;

import org.apache.commons.io.IOUtils;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.*;

/**
 * Created by Alec on 2016/6/27.
 */
public class DecryptManager {
    private Cipher cipher;

    public DecryptManager() {
        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Unknown encrypt type.");
            System.exit(-1);
        } catch (NoSuchPaddingException e) {
            System.err.println("Unknown error.");
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public void setSecret(File file) {
        byte[] iv = new byte[16];
        setSecret(file, iv);
    }

    public void setSecret(File file, byte[] iv) {
        byte[] keyData = null;
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            keyData = new byte[(int) file.length()];
            fileInputStream.read(keyData);
            fileInputStream.close();
        } catch (Exception e) {
            System.err.println("Cannot get key.");
            System.exit(-1);
        }
        Key key = new SecretKeySpec(keyData, "AES");
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        try {
            cipher.init(Cipher.DECRYPT_MODE, key, ivParameterSpec);
        } catch (InvalidKeyException e) {
            System.err.println("Key is invalid.");
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            System.err.println("Wrong IV");
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public void decrypt(File file, File output, boolean append) {
        try (FileInputStream fileInputStream = new FileInputStream(file);
             CipherInputStream cipherInputStream = new CipherInputStream(fileInputStream, cipher);
             FileOutputStream fileOutputStream = new FileOutputStream(output, append)) {
            IOUtils.copyLarge(cipherInputStream, fileOutputStream);
        } catch (Exception e) {
            System.err.println("File IO errors or decrypt failed");
            e.printStackTrace();
            System.exit(-1);
        }
    }

}
