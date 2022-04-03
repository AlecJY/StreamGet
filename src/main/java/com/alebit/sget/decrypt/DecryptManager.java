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

    public void setSecret(byte[] key) {
        byte[] iv = new byte[16];
        setSecret(key, iv);
    }

    public void setSecret(byte[] key, byte[] iv) {
        Key keySpec = new SecretKeySpec(key, "AES");
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        try {
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivParameterSpec);
        } catch (InvalidKeyException e) {
            System.err.println("Key is invalid.");
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            System.err.println("Wrong IV");
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public void decrypt(InputStream inputStream, OutputStream outputStream) {
        try {
            CipherInputStream cipherInputStream = new CipherInputStream(inputStream, cipher);
            IOUtils.copyLarge(cipherInputStream, outputStream);
        } catch (Exception e) {
            System.err.println("Decryption is failed");
            e.printStackTrace();
            System.exit(-1);
        }
    }

}
