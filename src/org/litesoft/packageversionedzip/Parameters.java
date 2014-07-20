package org.litesoft.packageversionedzip;

import org.litesoft.commonfoundation.base.*;
import org.litesoft.commonfoundation.typeutils.*;
import org.litesoft.server.file.*;
import org.litesoft.server.util.*;

import java8.util.function.*;

import java.io.*;
import java.util.*;

/**
 * Four Parameters are needed (Keys for the Arguments):
 * - Source ("From"/"Source") - Iterator of RelativeFile: RelativePath, open - Returns an InputStream
 * - Target ("Target") e.g. "jre"
 * - Version ("Version"), &
 * - DestinationDir ("DestDir") - normally provided by a SystemProperty("DestDir") but defaulting to "..".
 * <p/>
 * As each Argument key starts w/ a unique letter, the 'permutations' option is active.
 * Any non-keyed values are applied in the order above (excess keyed entries are noted, excess non-keyed entries are an Error)
 * <p/>
 * A ".gz" is assumed to be in the following form "Target-Version-....gz" (e.g. "jre-7u60-linux-x64.gz" where
 * "jre" is the Target & "7u60" is the Version).  When this occurs (when combined with the alternate suppliers for DestDir)
 * the parameters can be just the "Source" / ".gz" file.
 */
public class Parameters {
    public static final String SOURCE1 = "From";
    public static final String SOURCE2 = "Source";
    public static final String TARGET = "Target";
    public static final String VERSION = "Version";
    public static final String DEST_DIR = "DestDir";

    private enum SourceType {Dir, Zip, gz}

    private final SourceType mSourceType;
    private final File mSrcPath;
    private String mTarget;
    private String mVersion;
    private File mDestinationDir;

    public Parameters( String pSrcPath ) {
        pSrcPath = Confirm.significant( SOURCE1 + "/" + SOURCE2, pSrcPath );
        mSrcPath = new File( pSrcPath );
        if ( mSrcPath.isDirectory() ) {
            mSourceType = SourceType.Dir;
            return;
        }
        if ( mSrcPath.isFile() ) {
            String zExtension = ConstrainTo.notNull( FileUtils.getExtension( mSrcPath ) ).toLowerCase();
            if ( "zip".equals( zExtension ) ) {
                mSourceType = SourceType.Zip;
                return;
            }
            if ( "gz".equals( zExtension ) ) {
                mSourceType = SourceType.gz;
                processNameOf_gz( pSrcPath );
                return;
            }
        }
        throw new IllegalArgumentException( "Neither a Dir nor 'zip' or 'gz' file: " + mSrcPath.getAbsolutePath() );
    }

    /**
     * A ".gz" is assumed to be in the following form "Target-Version-....gz" (e.g. "jre-7u60-linux-x64.gz" where
     * "jre" is the Target & "7u60" is the Version).
     */
    private void processNameOf_gz( String pSrcPath ) {
        String gzFileName = Paths.justTheLastName( pSrcPath );
        int z1stDash = gzFileName.indexOf( '-' );
        if ( z1stDash > 0 ) {
            setTarget( gzFileName.substring( 0, z1stDash ) );
            int z2ndDash = gzFileName.indexOf( '-', ++z1stDash );
            if ( z2ndDash > z1stDash ) {
                setVersion( gzFileName.substring( z1stDash, z2ndDash ) );
            }
        }
    }

    public boolean validate() {
        return validate( TARGET, mTarget ) &
               validate( VERSION, mVersion ) &
               validate( DEST_DIR, mDestinationDir );
    }

    private boolean validate( String pWhat, Object pToCheck ) {
        if ( pToCheck != null ) {
            return true;
        }
        System.err.println( "*** " + pWhat + " Not Set ***" );
        return false;
    }

    private void setTarget( String pTarget ) {
        mTarget = validateTargetOrVersion( TARGET, pTarget );
    }

    private void setVersion( String pVersion ) {
        mVersion = validateTargetOrVersion( VERSION, pVersion );
    }

    private String validateTargetOrVersion( String pWhat, String pValue ) {
        pValue = Confirm.significant( pWhat, pValue );
        for ( int i = 0; i < pValue.length(); i++ ) {
            if ( !Characters.is7BitAlphaNumeric( pValue.charAt( i ) ) ) {
                throw IllegalArgument.exception( pWhat, "Not all 7 Bit Alpha Numeric" );
            }
        }
        return pValue;
    }

    public Parameters target( String pTarget ) {
        if ( pTarget != null ) {
            setTarget( pTarget );
        }
        return this;
    }

    public Parameters version( String pVersion ) {
        if ( pVersion != null ) {
            setVersion( pVersion );
        }
        return this;
    }

    public Parameters destinationDir( String pDestDir ) {
        pDestDir = Confirm.significant( DEST_DIR, pDestDir );
        File zDestDir = new File( pDestDir );
        DirectoryUtils.ensureExistsAndMutable( DEST_DIR + " - Not Mutable: ", zDestDir );
        mDestinationDir = zDestDir;
        return this;
    }

    public static Parameters from( ArgsToMap pArgs ) {
        Parameters zParameters =
                new Parameters( getFrom( pArgs, SOURCE1, SOURCE2 ) )
                        .target( getFrom( pArgs, TARGET ) )
                        .version( getFrom( pArgs, VERSION ) )
                        .destinationDir( getFrom( pArgs, DEST_DIR, new Supplier<String>() {
                            @Override
                            public String get() {
                                return ConstrainTo.significantOrNull( System.getProperty( DEST_DIR ), ".." );
                            }
                        } ) );
        List<String> zRemaining = pArgs.getRemainingNonKeyed();
        if ( !zRemaining.isEmpty() ) {
            throw new IllegalArgumentException( "Unexpected Arguments: " + zRemaining );
        }
        if ( !(zRemaining = pArgs.getRemainingKeyedKeys()).isEmpty() ) {
            System.out.println( "Ignoring Arguments: " + zRemaining );
        }
        return zParameters;
    }

    private static String getFrom( ArgsToMap pArgs, String pArgKey ) {
        String zValue = pArgs.getWithPermutations( pArgKey );
        return (zValue != null) ? zValue : pArgs.getNonKeyed();
    }

    private static String getFrom( ArgsToMap pArgs, String pArgKey, String pAltKey ) {
        String zValue = pArgs.getWithPermutations( pArgKey );
        return (zValue != null) ? zValue : getFrom( pArgs, pAltKey );
    }

    private static String getFrom( ArgsToMap pArgs, String pArgKey, Supplier<String> pDefaultSupplier ) {
        String zValue = pArgs.getWithPermutations( pArgKey );
        return (zValue != null) ? zValue : pArgs.getNonKeyed( pDefaultSupplier );
    }

    public String getTarget() {
        return mTarget;
    }

    public String getVersion() {
        return mVersion;
    }

    public File getDestinationDir() {
        return mDestinationDir;
    }

    public RelativeFileIterator getSourceFiles()
            throws IOException {
        if ( mSourceType == null ) {
            return null;
        }
        switch ( mSourceType ) {
            case Dir:
                return new RecursiveRelativeFileIterator( mSrcPath );
            case Zip:
                return new ZipRelativeFileIterator( mSrcPath );
            case gz:
                throw new UnsupportedOperationException( "No 'gz' Support Yet" );
            default:
                throw new IllegalStateException( "Unexpected SourceType: " + mSourceType );
        }
    }
}
