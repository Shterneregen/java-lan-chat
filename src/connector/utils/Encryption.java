package connector.utils;

import java.io.UnsupportedEncodingException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * Класс отвечающий за шифрование
 *
 * @author Yura
 */
public class Encryption {

    public Encryption() {
        prepare();
    }

    //<editor-fold defaultstate="collapsed" desc="Симметричное шифрование">
    /**
     * Шифрует строку методом xor
     *
     * @param pText шифруемый текст
     * @param pKey ключ шифрования
     * @return
     * @throws UnsupportedEncodingException
     */
    public static String encode(String pText, String pKey) throws UnsupportedEncodingException {
        byte[] iMsg = pText.getBytes("Cp1251");
        byte[] iPsw = pKey.getBytes("Cp1251");
        for (int i = 0, j = 0; i < iMsg.length; i++, j++) {
            if (j == iPsw.length - 1) {
                j = 0;
            }
            iMsg[i] ^= (byte) (iPsw[j] & 0xf);
        }
        return new String(iMsg, "Cp1251");
    }

    /**
     * Расшифровывает строку методом xor
     *
     * @param pText зашифрованная строка
     * @param pKey ключ шифрования
     * @return
     * @throws UnsupportedEncodingException
     */
    public static String decode(String pText, String pKey) throws UnsupportedEncodingException {
        byte[] iMsg = pText.getBytes("Cp1251");
        byte[] iPsw = pKey.getBytes("Cp1251");
        for (int i = 0, j = 0; i < iMsg.length; i++, j++) {
            if (j == iPsw.length - 1) {
                j = 0;
            }
            iMsg[i] ^= (byte) (iPsw[j] & 0xf);
        }
        return new String(iMsg, "Cp1251");
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Ассиметричное шифрование">
    private KeyPair keypair;
    private Cipher cipher;

    /**
     * Возвращает открытый ключ из существующей пары открытый/закрытый ключ
     *
     * @return
     */
    public PublicKey getPublicKeyFromKeypair() {
        return keypair.getPublic();
    }

    /**
     * Формирует пару открытый/закрытый ключ по заданному открытому ключу
     *
     * @param publicKey открытый ключ
     */
    public void createPair(PublicKey publicKey) {
        try {
            //throws NoSuchAlgorithmException, NoSuchPaddingException

            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(1024);
            KeyPair tempKeypair = kpg.generateKeyPair();

            this.keypair = new KeyPair(publicKey, tempKeypair.getPrivate());
            this.cipher = Cipher.getInstance("RSA");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException ex) {
            Logger.getLogger(Encryption.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /**
     * Формирует пару открытый/закрытый ключ
     */
    public final void prepare() {
        try {
            //throws NoSuchAlgorithmException, NoSuchPaddingException
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(1024);
            this.keypair = kpg.generateKeyPair();

            this.cipher = Cipher.getInstance("RSA");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException ex) {
            Logger.getLogger(Encryption.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Шифрует сообщение открытым ключом
     *
     * @param plaintext
     * @return
     */
    public String encrypt(String plaintext) {
        byte[] bytes;
        byte[] encrypted;
        String encryptedStrTranspherable = "";//
        try {
            this.cipher.init(Cipher.ENCRYPT_MODE, this.keypair.getPublic());
            bytes = plaintext.getBytes("UTF-8");
            encrypted = blockCipher(bytes, Cipher.ENCRYPT_MODE);
//	encryptedTranspherable = Hex.encodeHex(encrypted);
            encryptedStrTranspherable = byte2Hex(encrypted);//
        } catch (Exception ex) {
            Logger.getLogger(Encryption.class.getName()).log(Level.SEVERE, null, ex);
        }
        return encryptedStrTranspherable;//
    }

    /**
     * Расшифровывает строку закрытым ключом
     *
     * @param encryptedStr зашифрованая строка
     * @return
     */
    public String decrypt(String encryptedStr) {
        byte[] bts;
        byte[] decrypted;
        String resStr = "";
        try {
            this.cipher.init(Cipher.DECRYPT_MODE, this.keypair.getPrivate());
            bts = hex2Byte(encryptedStr);
            decrypted = blockCipher(bts, Cipher.DECRYPT_MODE);
            resStr = new String(decrypted, "UTF-8");
        } catch (Exception ex) {
            Logger.getLogger(Encryption.class.getName()).log(Level.SEVERE, null, ex);
        }
        return resStr;
    }

    private byte[] blockCipher(byte[] bytes, int mode) throws IllegalBlockSizeException, BadPaddingException {
        // string initialize 2 buffers.
        // scrambled will hold intermediate results
        byte[] scrambled = new byte[0];

        // toReturn will hold the total result
        byte[] toReturn = new byte[0];
        // if we encrypt we use 100 byte long blocks. Decryption requires 128 byte long blocks (because of RSA)
        int length = (mode == Cipher.ENCRYPT_MODE) ? 100 : 128;

        // another buffer. this one will hold the bytes that have to be modified in this step
        byte[] buffer = new byte[length];

        for (int i = 0; i < bytes.length; i++) {

            // if we filled our buffer array we have our block ready for de- or encryption
            if ((i > 0) && (i % length == 0)) {
                //execute the operation
                scrambled = cipher.doFinal(buffer);
                // add the result to our total result.
                toReturn = append(toReturn, scrambled);
                // here we calculate the length of the next buffer required
                int newlength = length;

                // if newlength would be longer than remaining bytes in the bytes array we shorten it.
                if (i + length > bytes.length) {
                    newlength = bytes.length - i;
                }
                // clean the buffer array
                buffer = new byte[newlength];
            }
            // copy byte into our buffer.
            buffer[i % length] = bytes[i];
        }

        // this step is needed if we had a trailing buffer. should only happen when encrypting.
        // example: we encrypt 110 bytes. 100 bytes per run means we "forgot" the last 10 bytes. they are in the buffer array
        scrambled = cipher.doFinal(buffer);

        // final step before we can return the modified data.
        toReturn = append(toReturn, scrambled);

        return toReturn;
    }

    private byte[] append(byte[] prefix, byte[] suffix) {
        byte[] toReturn = new byte[prefix.length + suffix.length];
        for (int i = 0; i < prefix.length; i++) {
            toReturn[i] = prefix[i];
        }
        for (int i = 0; i < suffix.length; i++) {
            toReturn[i + prefix.length] = suffix[i];
        }
        return toReturn;
    }

    public static String byte2Hex(byte b[]) {
        java.lang.String hs = "";
        java.lang.String stmp = "";
        for (int n = 0; n < b.length; n++) {
            stmp = java.lang.Integer.toHexString(b[n] & 0xff);
            if (stmp.length() == 1) {
                hs = hs + "0" + stmp;
            } else {
                hs = hs + stmp;
            }
        }
        return hs.toLowerCase();
    }

    public static byte hex2Byte(char a1, char a2) {
        int k;
        if (a1 >= '0' && a1 <= '9') {
            k = a1 - 48;
        } else if (a1 >= 'a' && a1 <= 'f') {
            k = (a1 - 97) + 10;
        } else if (a1 >= 'A' && a1 <= 'F') {
            k = (a1 - 65) + 10;
        } else {
            k = 0;
        }
        k <<= 4;
        if (a2 >= '0' && a2 <= '9') {
            k += a2 - 48;
        } else if (a2 >= 'a' && a2 <= 'f') {
            k += (a2 - 97) + 10;
        } else if (a2 >= 'A' && a2 <= 'F') {
            k += (a2 - 65) + 10;
        } else {
            k += 0;
        }
        return (byte) (k & 0xff);
    }

    public static byte[] hex2Byte(String str) {
        int len = str.length();
        if (len % 2 != 0) {
            return null;
        }
        byte r[] = new byte[len / 2];
        int k = 0;
        for (int i = 0; i < str.length() - 1; i += 2) {
            r[k] = hex2Byte(str.charAt(i), str.charAt(i + 1));
            k++;
        }
        return r;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Не используется">
    /**
     * Преобразует строку в целочисленный массив
     *
     * @param message
     * @return
     * @throws UnsupportedEncodingException
     */
    public static int[] msgToInt(String message) throws UnsupportedEncodingException {
        byte[] msgToByte = message.getBytes("UTF-8");
        int[] imsg = new int[msgToByte.length];
        for (int i = 0; i < msgToByte.length; i++) {
            imsg[i] = (0xfff & msgToByte[i]);
        }
        return imsg;
    }

    /**
     * Преобразует целочисленный массив в байтовый
     *
     * @param imsg
     * @return
     */
    public static byte[] intToMsg(int[] imsg) {
        byte[] intToByte = new byte[imsg.length];
        for (int i = 0; i < imsg.length; i++) {
            intToByte[i] = (byte) (imsg[i]);
        }
        return intToByte;
    }
    //</editor-fold>

}
