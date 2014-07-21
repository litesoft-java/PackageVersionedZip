package com.ice.tar;

/**
 * THF = Tar Header Field
 */
public class THF {
    public static final String UTF_8 = "UTF-8";

    private final String mName;
    private final byte[] mBytes;
    private String mAsString;
    private Integer mIntFromOctal;
    private Long mLongFromOctal;

    public THF( String pName, int pLength ) {
        mName = pName;
        mBytes = new byte[pLength];
    }

    public int populate( int pFrom, byte[] pHeader ) {
        for ( int i = 0; i < mBytes.length; i++ ) {
            if ( pFrom < pHeader.length ) {
                mBytes[i] = pHeader[pFrom++];
            }
        }
        return pFrom;
    }

    public byte asByte() {
        return mBytes[0];
    }

    public int asIntOctal() {
        if ( mIntFromOctal != null ) {
            return mIntFromOctal;
        }
        return mIntFromOctal = (int) octal();
    }

    public long asLongOctal() {
        if ( mLongFromOctal != null ) {
            return mLongFromOctal;
        }
        return mLongFromOctal = octal();
    }

    private long octal() {
        long result = 0;
        boolean stillPadding = true;
        for ( byte zByte : mBytes ) {
            if ( stillPadding && ((zByte == ' ') || (zByte == '0')) ) {
                continue;
            }
            if ( zByte == 0 ) {
                break;
            }
            if ( (zByte < '0') || ('7' < zByte) ) {
                throw new RuntimeException( "Octal? " + this );
            }
            stillPadding = false;
            result = (result << 3) + (zByte - '0');
        }
        return result;
    }

    public String asString() {
        if ( mAsString != null ) {
            return mAsString;
        }
        if ( mBytes[0] == 0 ) {
            return mAsString = "";
        }
        StringBuilder sb = new StringBuilder();
        for ( byte zByte : mBytes ) {
            if ( zByte == 0 ) {
                break;
            }
            sb.append( (char) zByte );
        }
        return mAsString = sb.toString();
//        int zLength = 0;
//        for ( byte zByte : mBytes ) {
//            if ( zByte == 0 ) {
//                break;
//            }
//            zLength++;
//        }
//        try {
//            return mAsString = new String( mBytes, 0, zLength, UTF_8 );
//        }
//        catch ( UnsupportedEncodingException e ) {
//            return null;
//        }
    }

    public String details() {
        return appendDetails( new StringBuilder(), -1 );
    }

    private String appendDetails( StringBuilder pSB, int pMaxStringLength ) {
        String zAsString = asString();
        if ( zAsString == null ) {
            pSB.append( "null" );
        } else if ( (pMaxStringLength == -1) || (zAsString.length() <= pMaxStringLength) ) {
            pSB.append( "'" ).append( zAsString ).append( "'" );
        } else {
            pSB.append( "'" ).append( zAsString.substring( 0, pMaxStringLength ) ).append( "'..." );
        }
        pSB.append( ", or (dec)" );
        for ( byte zByte : mBytes ) {
            pSB.append( ' ' ).append( 255 & (int) zByte ).append( ',' );
        }
        return pSB.toString().substring( 0, pSB.length() - 1 );
    }

    @Override
    public String toString() {
        return appendDetails( new StringBuilder( mName ).append( ": " ), 60 );
    }
}
