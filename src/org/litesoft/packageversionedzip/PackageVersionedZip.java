package org.litesoft.packageversionedzip;

import org.litesoft.commonfoundation.exceptions.*;
import org.litesoft.commonfoundation.typeutils.*;
import org.litesoft.packageversioned.*;
import org.litesoft.server.file.*;
import org.litesoft.server.util.*;

import java.io.*;
import java.util.zip.*;

public class PackageVersionedZip extends AbstractApp<Parameters> {
    public static final String VERSION = "0.9";

    public static final String VERSION_FILE = "version.txt";

    public PackageVersionedZip( Parameters pParameters ) {
        super("Package", pParameters);
    }

    public static void main( String[] args ) {
        CONSOLE.printLn( "PackageVersionedZip vs " + VERSION );
        new PackageVersionedZip( new Parameters( new ArgsToMap( args ) ) ).run();
    }

    protected void process() {
        String zTarget = getTarget();
        String zVersion = mParameters.getVersion();
        ZipFileCreator zZipper = new ZipFileCreator( new File( mParameters.getLocalVerDir(),
                                                               Paths.forwardSlashCombine( zTarget, zVersion + ".zip" ) ) );
        RelativeFileIterator zSourceFiles = mParameters.getSourceFiles();
        CONSOLE.indent();
        zZipper.add( new RelativeFileFromContents( VERSION_FILE, zVersion + "\n" ) );
        while ( zSourceFiles.hasNext() ) {
            zZipper.add( zSourceFiles.next() );
        }
        CONSOLE.outdent();
        zSourceFiles.dispose();
        Closeables.close(zZipper);
    }

    private class ZipFileCreator implements Closeable {
        private final File mZipFile;
        private ZipOutputStream mZipOutputStream;

        public ZipFileCreator( File pZipFile ) {
            CONSOLE.printLn( "Producing: ", (mZipFile = pZipFile) );
            OutputStream zOS = FileUtils.asOutputStream( FileUtils.asNewFile( mZipFile ) );
            mZipOutputStream = new ZipOutputStream( IOUtils.createBufferedOutputStream( zOS ) );
        }

        public void add( final RelativeFile pFile ) {
            CONSOLE.printLn( pFile.getRelativeFilePath() );
            ZipEntry zEntry = new ZipEntry( Paths.forwardSlash( pFile.getRelativeFilePath() ) );
            try {
                mZipOutputStream.putNextEntry( zEntry );
            }
            catch ( IOException e ) {
                throw new FileSystemException( e );
            }
            IOCopier.from( new IOSupplier<InputStream>() {
                @Override
                public InputStream get() {
                    return pFile.open();
                }
            } ).append( mZipOutputStream );
            //    } ).to( new IOSupplier<OutputStream>() {
            //        @Override
            //        public OutputStream get()
            //                throws IOException {
            //            File zFile = new File("/tempGZ/" + pFile.getRelativeFilePath());
            //            FileUtils.insureParent( zFile );
            //            return new FileOutputStream( zFile );
            //        }
            //    } );
        }

        @Override
        public void close()
                throws IOException {
            mZipOutputStream.close();
            FileUtils.rollIn( FileUtils.asNewFile( mZipFile ), mZipFile, FileUtils.asBackupFile( mZipFile ) );
        }
    }
}
