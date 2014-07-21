/*
** Authored by Timothy Gerard Endres
** <mailto:time@gjt.org>  <http://www.trustice.com>
** 
** This work has been placed into the public domain.
** You may use this work in any way and for any purpose you wish.
**
** THIS SOFTWARE IS PROVIDED AS-IS WITHOUT WARRANTY OF ANY KIND,
** NOT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY. THE AUTHOR
** OF THIS SOFTWARE, ASSUMES _NO_ RESPONSIBILITY FOR ANY
** CONSEQUENCE RESULTING FROM THE USE, MODIFICATION, OR
** REDISTRIBUTION OF THIS SOFTWARE. 
** 
*/

package com.ice.tar;

import java.io.*;

/**
 * The TarBuffer class implements the tar archive concept
 * of a buffered input stream. This concept goes back to the
 * days of blocked tape drives and special io devices. In the
 * Java universe, the only real function that this class
 * performs is to ensure that files have the correct "block"
 * size, or other tars will complain.
 * <p/>
 * You should never have a need to access this class directly.
 * TarBuffers are created by Tar IO Streams.
 *
 * @author Timothy Gerard Endres,
 *         <a href="mailto:time@gjt.org">time@trustice.com</a>.
 * @version $Revision: 1.10 $
 */

public class TarBuffer {
    public static final int DEFAULT_RCDSIZE = (512);
    public static final int DEFAULT_RECS_PER_BLOCK = 20;
    public static final int DEFAULT_BLKSIZE = (DEFAULT_RCDSIZE * DEFAULT_RECS_PER_BLOCK);

    private final int recordSize = DEFAULT_RCDSIZE;
    private final int blockSize = DEFAULT_BLKSIZE;
    private final int recsPerBlock = DEFAULT_RECS_PER_BLOCK;

    private byte[] blockBuffer;
    private int currBlkIdx = -1;
    private int currRecIdx = recsPerBlock;

    private InputStream inStream;


    private boolean debug;

    public TarBuffer( InputStream inStream ) {
        this.inStream = inStream;
    }

    /**
     * Set the debugging flag for the buffer.
     *
     * @param debug If true, print debugging output.
     */
    public void setDebug( boolean debug ) {
        this.debug = debug;
    }

    /**
     * Get the TAR Buffer's block size. Blocks consist of multiple records.
     */
    public int getBlockSize() {
        return blockSize;
    }

    /**
     * Get the TAR Buffer's record size.
     */
    public int getRecordSize() {
        return recordSize;
    }

    /**
     * Determine if an archive record indicate End of Archive. End of
     * archive is indicated by a record that consists entirely of null bytes.
     *
     * @param record The record data to check.
     */
    public boolean isEOFRecord( byte[] record ) {
        for ( int i = 0, sz = getRecordSize(); i < sz; ++i ) {
            if ( record[i] != 0 ) {
                return false;
            }
        }

        return true;
    }

    /**
     * Read a record from the input stream and return the data.
     *
     * @return The record data.
     */
    public byte[] readRecord()
            throws IOException {
        if ( debug ) {
            System.err.println(
                    "ReadRecord: recIdx = " + currRecIdx
                    + " blkIdx = " + currBlkIdx );
        }

        if ( (blockBuffer == null) || (recsPerBlock <= currRecIdx) ) {
            if ( !readBlock() ) {
                return null;
            }
        }

        byte[] result = new byte[recordSize];

        System.arraycopy(
                blockBuffer, (currRecIdx * recordSize),
                result, 0, recordSize );

        currRecIdx++;

        return result;
    }

    /**
     * @return false if End-Of-File, else true
     */
    private boolean readBlock()
            throws IOException {
        if ( debug ) {
            System.err.println( "ReadBlock: blkIdx = " + currBlkIdx );
        }

        blockBuffer = new byte[blockSize]; // to Force all zeros!
        int numBytes, offset = 0;

        for (int bytesNeeded = blockSize; bytesNeeded > 0; bytesNeeded -= numBytes) {
            if ( -1 == (numBytes = inStream.read( blockBuffer, offset, bytesNeeded )) ) {
                break;
            }
            offset += numBytes;

            // NOTE
            // We have fit EOF, and the block is not full!
            //
            // This is a broken archive. It does not follow the standard
            // blocking algorithm. However, because we are generous, and
            // it requires little effort, we will simply ignore the error
            // and continue as if the entire block were read. This does
            // not appear to break anything upstream. We used to return
            // false in this case.
            //
            // Thanks to 'Yohann.Roussel@alcatel.fr' for this fix.
        }

        currBlkIdx++;
        currRecIdx = 0;

        return (offset != 0);
    }

    /**
     * Get the current block number, zero based.
     *
     * @return The current zero based block number.
     */
    public int getCurrentBlockNum() {
        return currBlkIdx;
    }

    /**
     * Get the current record number, within the current block, zero based.
     * Thus, current offset = (currentBlockNum * recsPerBlk) + currentRecNum.
     *
     * @return The current zero based record number.
     */
    public int getCurrentRecordNum() {
        return currRecIdx - 1;
    }

    /**
     * Close the TarBuffer. If this is an output buffer, also flush the
     * current block before closing.
     */
    public void close()
            throws IOException {
        if ( debug ) {
            System.err.println( "TarBuffer.closeBuffer()." );
        }

        if ( inStream != null ) {
            Closeable zCloseable = inStream;
            inStream = null;
            zCloseable.close();
        }
    }
}

