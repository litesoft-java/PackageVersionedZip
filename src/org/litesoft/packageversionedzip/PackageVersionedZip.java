package org.litesoft.packageversionedzip;

import org.litesoft.commonfoundation.typeutils.*;
import org.litesoft.server.file.*;
import org.litesoft.server.util.*;

import java.io.*;

public class PackageVersionedZip {
    public static final String VERSION = "0.9";

    public static final String VERSIONED_PATH = "versioned";
    public static final String VERSION_FILE = "version.txt";

    private Parameters mParameters;

    public PackageVersionedZip( Parameters pParameters ) {
        if ( !(mParameters = pParameters).validate() ) {
            System.exit( 1 );
        }
    }

    public static void main( String[] args )
            throws Exception {
        System.out.println( "PackageVersionedZip vs " + VERSION );
        new PackageVersionedZip( Parameters.from( new ArgsToMap( args ) ) ).process();
        System.out.println( "Done!" );
    }

    private void process()
            throws IOException {
        String zTarget = mParameters.getTarget();
        String zVersion = mParameters.getVersion();
        ZipFileCreator zZipper = new ZipFileCreator( new File( mParameters.getDestinationDir(),
                                                               Paths.forwardSlashCombine( VERSIONED_PATH, zTarget, zVersion + ".zip" ) ) );
        zZipper.add( new RelativeFileFromContents( VERSION_FILE, zVersion + "\n" ) );
        RelativeFileIterator zSourceFiles = mParameters.getSourceFiles();
        while ( zSourceFiles.hasNext() ) {
            zZipper.add( zSourceFiles.next() );
        }
        zSourceFiles.dispose();
        zZipper.close();
    }

    private static class ZipFileCreator implements Closeable {
        private final File mZipFile;
        // private final File mZipFile;

        public ZipFileCreator( File pZipFile ) {
            System.out.println( "Producing: " + (mZipFile = pZipFile) );
            // TODO: Open New File...
        }

        public void add( RelativeFile pFile ) {
            System.out.println( "    " + pFile.getRelativeFilePath() );
            // TODO: xxx Stream it in!
        }

        @Override
        public void close()
                throws IOException {
            // TODO: xxx
        }
    }
}
