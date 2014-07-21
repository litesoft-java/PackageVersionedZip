package org.litesoft.server.file;

import org.litesoft.commonfoundation.exceptions.*;
import org.litesoft.commonfoundation.typeutils.*;
import org.litesoft.server.util.*;

import com.ice.tar.*;

import java.io.*;
import java.util.*;
import java.util.zip.*;

public class TarGZRelativeFileIterator extends RelativeFileIterator {
    public static final int MAX_MEMORY_FILE_SIZE = 1024 * 1024 * 1; // 1 MB

    private TarInputStream mTarInputStream;
    private TarEntry mTarEntry;

    public TarGZRelativeFileIterator( File pTarGZFile )
            throws IOException {
        mTarInputStream = new TarInputStream( new GZIPInputStream( new FileInputStream( pTarGZFile ) ) );
        mTarEntry = nextFile();
    }

    private TarEntry nextFile() {
        try {
            for ( TarEntry zTarEntry; null != (zTarEntry = mTarInputStream.getNextEntry()); ) {
                if ( !zTarEntry.isDirectory() ) {
                    return zTarEntry;
                }
            }
            return null;
        }
        catch ( IOException e ) {
            throw new FileSystemException( e );
        }
    }

    @Override
    public boolean hasNext() {
        return (mTarEntry != null);
    }

    @Override
    public RelativeFile next() {
        if ( !hasNext() ) {
            return super.next();
        }
        try {
            int zAvailable = mTarInputStream.available();
            RelativeFile zRelativeFile = (zAvailable <= MAX_MEMORY_FILE_SIZE) ?
                                         new MemoryTarRelativeFile( mTarEntry.getName() ) :
                                         new TempFileTarRelativeFile( mTarEntry.getName() );
            mTarEntry = nextFile();
            return zRelativeFile;
        }
        catch ( IOException e ) {
            throw new FileSystemException( e );
        }
    }

    @Override
    public void dispose() {
        Closeables.dispose( mTarInputStream );
        mTarInputStream = null;
    }

    private class TempFileTarRelativeFile extends RelativeFile {
        private File mTempFile;

        private TempFileTarRelativeFile( String pRelativeFilePath )
                throws IOException {
            super( pRelativeFilePath );
            mTempFile = File.createTempFile( "temp-" + pRelativeFilePath.replace( '/', '_' ), ".tmp" );
            FileOutputStream zOutputStream = new FileOutputStream( mTempFile );
            mTarInputStream.copyEntryContents( zOutputStream );
            zOutputStream.close();
        }

        @Override
        public InputStream open()
                throws FileSystemException {
            try {
                return new FileInputStream( mTempFile );
            }
            catch ( IOException e ) {
                throw new FileSystemException( e );
            }
        }
    }

    private class MemoryTarRelativeFile extends RelativeFile {

        private List<IOBlock> mBlocks = Lists.newLinkedList();

        public MemoryTarRelativeFile( String pRelativeFilePath )
                throws IOException {
            super( pRelativeFilePath );
            for ( IOBlock zBlock; null != (zBlock = IOBlock.from( mTarInputStream )); ) {
                mBlocks.add( zBlock );
            }
        }

        @Override
        public InputStream open()
                throws FileSystemException {
            return new BlockInputStream( mBlocks );
        }
    }
}

