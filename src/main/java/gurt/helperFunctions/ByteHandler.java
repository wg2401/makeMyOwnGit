package gurt.helperFunctions;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ByteHandler 
{
    public static byte[] hexStringToBytes(String hexString)
    {
        if (hexString.length() % 2 != 0) 
        {
            throw new IllegalArgumentException("Hex string length must be even");
        }

        byte[] rawBytes = new byte[hexString.length()/2];

        for (int i = 0; i < hexString.length()/2; i++)
        {
            int index = i*2;
            String curByte = hexString.substring(index, index+2);
            rawBytes[i] = (byte) Integer.parseInt(curByte, 16);
        }

        return rawBytes;
    }

    public static byte[] combineTwoByteArrays(byte[] a, byte [] b)
    {
        byte[] c = new byte[a.length + b.length];

        for (int i = 0; i < a.length; i++)
        {
            c[i] = a[i];
        }

        for (int i = 0; i < b.length; i++)
        {
            c[i + a.length] = b[i];
        }

        return c;
    }

    public static byte[] bytesToHashedBytes(byte[] a)
    {
        try
        {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(a);
        }
        catch(NoSuchAlgorithmException e)
        {
            System.out.println(e.getMessage());
            return null;
        }
    }
    
    //get hashed bytes into hex format, and append to StringBuilder()
    public static StringBuilder bytesToHashedSB(byte[] a)
    {
        try
        {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashed = md.digest(a);
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hashed)
            {
                sb.append(String.format("%02x", b));
            }

            return sb;
        }
        catch(NoSuchAlgorithmException e)
        {
            System.out.println(e.getMessage());
            return null;
        }
    }
}
