package gurt.helperFunctions;

public class ObjectParser 
{
    //remove header from object bytes
    public static byte[] removeHeader(byte[] obj)
    {
        int nullTermInd = 0;

        while (obj[nullTermInd] != 0)
        {
            nullTermInd++;
        }

        byte[] withoutHeader = new byte[obj.length - 1 - nullTermInd];
        
        for (int i = nullTermInd + 1; i < obj.length; i++)
        {
            withoutHeader[i - 1 - nullTermInd] = obj[i];
        }
        
        return withoutHeader;
    }    
}
