package org.litesoft.packageversionedzip;

import org.litesoft.packageversioned.*;
import org.litesoft.server.file.*;
import org.litesoft.server.util.*;

import java.io.*;

/**
 * Four Parameters are needed (Keys for the Arguments):
 * - Source ("From"/"Source") - Iterator of RelativeFile: RelativePath, open - Returns an InputStream.
 * - Target ("Target") e.g. "jre"
 * - Version ("Version") - if the version is "!" (an Exclamation Point) then use "now" as defined by Timestamps.
 * - LocalVerDir ("LocalVerDir") - See ParameterLocalVerDir for details.
 * <p/>
 * As each Argument key starts w/ a unique letter, the 'permutations' option is active.
 * Any non-keyed values are applied in the order above (excess keyed entries are noted, excess non-keyed entries are an Error)
 * <p/>
 * When a ".gz" of the normal format (See ParameterSource) is supplied to the "Source", when combined with the alternate suppliers for LocalVerDir,
 * the parameters can be just the "Source" ".gz" file.
 */
public class Parameters extends AbstractParameters {
    private ParameterSource mSource = new ParameterSource( mTarget, mVersion );
    private ParameterLocalVerDir mLocalVerDir = ParameterLocalVerDir.existingOrCreatable();

    private Parameter<?>[] mParameters = {mSource, mTarget, mVersion, mLocalVerDir};

    public static final String SOURCE1 = "From";
    public static final String SOURCE2 = "Source";

    public Parameters( ArgsToMap pArgs ) {
        prepToString( mTarget, mVersion );
        populate( mParameters, pArgs );
    }

    public final String getVersion() {
        return mVersion.get();
    }

    public File getLocalVerDir() {
        return mLocalVerDir.get();
    }

    public RelativeFileIterator getSourceFiles() {
        return mSource.getSourceFiles();
    }

    @Override
    public boolean validate() {
        return validate( mParameters );
    }
}
