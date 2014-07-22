package org.litesoft.packageversionedzip;

import org.litesoft.commonfoundation.base.*;
import org.litesoft.commonfoundation.typeutils.*;
import org.litesoft.packageversioned.*;
import org.litesoft.server.file.*;
import org.litesoft.server.util.*;

import java.io.*;

/**
 * Four Parameters are needed (Keys for the Arguments):
 * - Source ("From"/"Source") - Iterator of RelativeFile: RelativePath, open - Returns an InputStream
 * - Target ("Target") e.g. "jre"
 * - Version ("Version"), &
 * - LocalVerDir ("LocalVerDir") - See AbstractParameters for details.
 * <p/>
 * As each Argument key starts w/ a unique letter, the 'permutations' option is active.
 * Any non-keyed values are applied in the order above (excess keyed entries are noted, excess non-keyed entries are an Error)
 * <p/>
 * A ".gz" is assumed to be in the following form "Target-Version-....gz" (e.g. "jre-7u60-linux-x64.gz" where
 * "jre" is the Target & "7u60" is the Version).  When this occurs (when combined with the alternate suppliers for DestDir)
 * the parameters can be just the "Source" / ".gz" file.
 */
public class Parameters extends AbstractParameters {
    public static final String SOURCE1 = "From";
    public static final String SOURCE2 = "Source";

    private enum SourceType {Dir, Zip, gz}

    private final SourceType mSourceType;
    private final File mSrcPath;

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

    @Override
    public File getLocalVerDir() {
        return super.getLocalVerDir();
    }

    @Override
    public boolean validate() {
        return validateTarget() &
               validateVersion() &
               validateLocalVerDir();
    }

    public Parameters target( String pTarget ) {
        return setTargetOptionally( pTarget );
    }

    public Parameters version( String pVersion ) {
        return setVersionOptionally( pVersion );
    }

    public Parameters localVerDir( String pLocalVerDir ) {
        return setLocalVerDir( pLocalVerDir );
    }

    @Override
    protected File validateLocalVerDir( File pLocalVerDir ) {
        return DirectoryUtils.ensureExistsAndMutable( LOCAL_VER_DIR + " - Not Mutable: ", pLocalVerDir );
    }

    public static Parameters from( ArgsToMap pArgs ) {
        return finish( pArgs,
                       new Parameters( getFrom( pArgs, SOURCE1, SOURCE2 ) )
                               .target( getTargetFrom( pArgs ) )
                               .version( getVersionFrom( pArgs ) )
                               .localVerDir( getLocalVerDirFrom( pArgs ) ) );
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
                return new TarGZRelativeFileIterator( mSrcPath );
            default:
                throw new IllegalStateException( "Unexpected SourceType: " + mSourceType );
        }
    }
}
