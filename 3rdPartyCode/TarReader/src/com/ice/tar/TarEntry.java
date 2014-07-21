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

/**
 * This class represents an entry in a Tar archive. It consists
 * of the entry's header.
 * <p/>
 * TarEntries that are created from the header bytes read from
 * an archive are instantiated with the TarEntry( byte[] )
 * constructor. These entries will be used when extracting from
 * or listing the contents of an archive. These entries have their
 * header filled in using the header bytes.
 * <p/>
 * <pre>
 *
 * Original Unix Tar Header:
 *
 * Field  Field     Field
 * Width  Name      Meaning
 * -----  --------- ---------------------------
 *   100  name      name of file
 *     8  mode      file mode
 *     8  uid       owner user ID
 *     8  gid       owner group ID
 *    12  size      length of file in bytes
 *    12  mtime     modify time of file
 *     8  chksum    checksum for header
 *     1  link      indicator for links
 *   100  linkname  name of linked file
 *
 * </pre>
 * <p/>
 * <pre>
 *
 * POSIX "ustar" Style Tar Header:
 *
 * Field  Field     Field
 * Width  Name      Meaning
 * -----  --------- ---------------------------
 *   100  name      name of file
 *     8  mode      file mode
 *     8  uid       owner user ID
 *     8  gid       owner group ID
 *    12  size      length of file in bytes
 *    12  mtime     modify time of file
 *     8  chksum    checksum for header
 *     1  typeflag  type of file
 *   100  linkname  name of linked file
 *     6  magic     USTAR indicator
 *     2  version   USTAR version
 *    32  uname     owner user name
 *    32  gname     owner group name
 *     8  devmajor  device major number
 *     8  devminor  device minor number
 *   155  prefix    prefix for file name
 *
 * struct posix_header
 *   {                     byte offset
 *   char name[100];            0
 *   char mode[8];            100
 *   char uid[8];             108
 *   char gid[8];             116
 *   char size[12];           124
 *   char mtime[12];          136
 *   char chksum[8];          148
 *   char typeflag;           156
 *   char linkname[100];      157
 *   char magic[6];           257
 *   char version[2];         263
 *   char uname[32];          265
 *   char gname[32];          297
 *   char devmajor[8];        329
 *   char devminor[8];        337
 *   char prefix[155];        345
 *   };                       500
 *
 * </pre>
 * <p/>
 * Note that while the class does recognize GNU formatted headers,
 * it does not perform proper processing of GNU archives. I hope
 * to add the GNU support someday.
 * <p/>
 * Directory "size" fix contributed by:
 * Bert Becker <becker@informatik.hu-berlin.de>
 *
 * @author Timothy Gerard Endres, <time@gjt.org>
 * @see TarHeader
 */

public class TarEntry {
    /**
     * This is the entry's header information.
     */
    protected TarHeader header;

    /**
     * The default constructor is protected for use only by subclasses.
     */
    protected TarEntry() {
    }

    /**
     * Construct an entry from an archive's header bytes.
     *
     * @param headerBuf The header bytes from a tar archive entry.
     */
    public TarEntry( byte[] headerBuf )
            throws InvalidHeaderException {
        this.header = new TarHeader( headerBuf );
    }

    /**
     * Get this entry's header.
     *
     * @return This entry's TarHeader.
     */
    public TarHeader getHeader() {
        return this.header;
    }

    /**
     * Get this entry's name.
     *
     * @return This entry's name.
     */
    public String getName() {
        return header.getName();
    }

    /**
     * Get this entry's file size.
     */
    public long getSize() {
        return header.getSize();
    }

    /**
     * Return whether or not this entry represents a directory.
     *
     * @return True if this entry is a directory.
     */
    public boolean isDirectory() {
        return header.isDirectory();
    }

    //    /**
    //     * Compute the checksum of a tar entry header.
    //     *
    //     * @param buf The tar entry's header buffer.
    //     *
    //     * @return The computed checksum.
    //     */
    //    public long computeCheckSum( byte[] buf ) {
    //        long sum = 0;
    //
    //        for ( byte aByte : buf ) {
    //            sum += 255 & aByte;
    //        }
    //
    //        return sum;
    //    }

    public String toString() {
        return "TarEntry" + header;
    }
}

