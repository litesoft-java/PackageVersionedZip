package org.litesoft.packageversionedzip;

import org.litesoft.commonfoundation.base.*;
import org.litesoft.commonfoundation.typeutils.*;
import org.litesoft.packageversioned.*;
import org.litesoft.server.file.*;

import java.io.*;

/**
 * Parameter Argument: Source ("From"/"Source") - Iterator of RelativeFile: RelativePath, open - Returns an InputStream.
 * <p/>
 * A ".gz" is assumed to be in the following form "Target-Version-....gz" (e.g. "jre-7u60-linux-x64.gz" where
 * "jre" is the Target & "7u60" is the Version).
 */
public class ParameterSource extends AbstractFileParameter {
    public static final String[] NAMES = {"Source", "From"};

    private static final String INVALID = "MUST be an existing local: directory, zip file, or .gz (tar.gz assumed) file";

    private enum SourceType {Dir, Zip, gz}

    private final ParameterTarget mTarget;
    private final ParameterVersion mVersion;
    private SourceType mSourceType;

    public ParameterSource( ParameterTarget pTarget, ParameterVersion pVersion ) {
        super( INVALID, NAMES );
        mTarget = pTarget;
        mVersion = pVersion;
    }

    @Override
    protected File convertValidated( String pValue ) {
        File zFile = super.convertValidated( pValue );
        if ( zFile.isDirectory() ) {
            mSourceType = SourceType.Dir;
            return zFile;
        }
        if ( zFile.isFile() ) {
            String zExtension = ConstrainTo.notNull( FileUtils.getExtension( zFile ) ).toLowerCase();
            if ( "zip".equals( zExtension ) ) {
                mSourceType = SourceType.Zip;
                return zFile;
            }
            if ( "gz".equals( zExtension ) ) {
                mSourceType = SourceType.gz;
                processNameOf_gz( pValue );
                return zFile;
            }
        }
        throw new IllegalArgumentException( "Neither a Dir nor 'zip' or 'gz' file: " + zFile.getAbsolutePath() );
    }

    /**
     * A ".gz" is assumed to be in the following form "Target-Version-....gz" (e.g. "jre-7u60-linux-x64.gz" where
     * "jre" is the Target & "7u60" is the Version).
     */
    private void processNameOf_gz( String pSrcPath ) {
        String gzFileName = Paths.justTheLastName( pSrcPath );
        int z1stDash = gzFileName.indexOf( '-' );
        if ( z1stDash > 0 ) {
            mTarget.set( gzFileName.substring( 0, z1stDash ) );
            int z2ndDash = gzFileName.indexOf( '-', ++z1stDash );
            if ( z2ndDash > z1stDash ) {
                mVersion.set( gzFileName.substring( z1stDash, z2ndDash ) );
            }
        }
    }

    public RelativeFileIterator getSourceFiles()
            throws IOException {
        if ( mSourceType == null ) {
            return null;
        }
        switch ( mSourceType ) {
            case Dir:
                return new RecursiveRelativeFileIterator( mValue );
            case Zip:
                return new ZipRelativeFileIterator( mValue );
            case gz:
                return new TarGZRelativeFileIterator( mValue );
            default:
                throw new IllegalStateException( "Unexpected SourceType: " + mSourceType );
        }
    }
}
