package gurt.helperFunctions;

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
}
