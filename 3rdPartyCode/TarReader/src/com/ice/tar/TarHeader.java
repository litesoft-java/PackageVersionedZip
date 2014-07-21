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

import java.util.*;

/**
 * This class encapsulates the Tar Entry Header used in Tar Archives.
 * The class also holds a number of tar constants, used mostly in headers.
 *
 * @author Timothy Gerard Endres, <time@gjt.org>
 */
public class TarHeader {
    public enum Type {
        unix, // "old-unix" format - magic tag == ""
        ustar, // 'ustar' format - magic tag representing a POSIX tar archive.
        gnu // GNU 'ustar' format - magic tag representing a GNU tar archive: "ustar  " spaces???
    }

    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . POSIX "ustar" Style Tar Header:
    //
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . Field . . . Field
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . Width . . . Meaning
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . -----  --------- ---------------------------
    public final THF _name = new THF( "name", /* . . . . . . . */ 100 ); // name of file
    public final THF _mode = new THF( "mode", /* . . . . . . . . */ 8 ); // file mode
    public final THF _uid = new THF( "uid", /* . . . . . . . . . */ 8 ); // owner user ID
    public final THF _gid = new THF( "gid", /* . . . . . . . . . */ 8 ); // owner group ID
    public final THF _size = new THF( "size", /* . . . . . . . .*/ 12 ); // length of file in bytes
    public final THF _mtime = new THF( "mtime", /* . . . . . . .*/ 12 ); // modify time of file
    public final THF _chksum = new THF( "chksum", /* . . . . . . */ 8 ); // checksum for header
    public final THF _typeflag = new THF( "typeflag", /* . . . . */ 1 ); // type of file
    public final THF _linkname = new THF( "linkname", /* . . . */ 100 ); // name of linked file *** LAST FIELD of Original Unix Tar Header ***
    public final THF _magic = new THF( "magic", /* . . . . . . . */ 6 ); // USTAR indicator
    public final THF _version = new THF( "version", /* . . . . . */ 2 ); // USTAR version
    public final THF _uname = new THF( "uname", /* . . . . . . .*/ 32 ); // owner user name
    public final THF _gname = new THF( "gname", /* . . . . . . .*/ 32 ); // owner group name
    public final THF _devmajor = new THF( "devmajor", /* . . . . */ 8 ); // device major number
    public final THF _devminor = new THF( "devminor", /* . . . . */ 8 ); // device minor number
    public final THF _prefix = new THF( "prefix", /* . . . . . */ 155 ); // prefix for file name
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . =====
    public final THF[] THFS = { // . . . . . . . . . . . . . . .. 500 - length
                                _name,
                                _mode,
                                _uid,
                                _gid,
                                _size,
                                _mtime,
                                _chksum,
                                _typeflag,
                                _linkname,
                                _magic,
                                _version,
                                _uname,
                                _gname,
                                _devmajor,
                                _devminor,
                                _prefix
    };

    public final Type format;

    /**
     * The entry's name.
     */
    public String name = "";
    /**
     * The entry's permission mode.
     */
    public int mode;
    /**
     * The entry's user id.
     */
    public int userId;
    /**
     * The entry's group id.
     */
    public int groupId;
    /**
     * The entry's size.
     */
    public long size;
    /**
     * The entry's modification time.
     */
    public long modTime;
    /**
     * The entry's checksum.
     */
    public int checkSum;
    /**
     * The entry's link flag.
     */
    public byte linkFlag;
    /**
     * The entry's link name.
     */
    public String linkName = "";
    /**
     * The entry's magic tag.
     */
    public String magic = "";
    /**
     * The entry's user name.
     */
    public String userName = "";
    /**
     * The entry's group name.
     */
    public String groupName = "";
    /**
     * The entry's major device number.
     */
    public int devMajor;
    /**
     * The entry's minor device number.
     */
    public int devMinor;

    public boolean isUnixTarFormat() {
        return format == Type.unix;
    }

    /**
     * Returns true if this entry's header is in "ustar" format.
     */
    public boolean isUSTarFormat() {
        return format == Type.ustar;
    }

    /**
     * Returns true if this entry's header is in the GNU 'ustar' format.
     */
    public boolean isGNUTarFormat() {
        return format == Type.gnu;
    }

    /**
     * Parse an entry's TarHeader information from a header buffer.
     * <p/>
     * Old unix-style code contributed by David Mehringer <dmehring@astro.uiuc.edu>.
     *
     * @param headerBuf The tar entry header buffer to get information from.
     */
    public TarHeader( byte[] headerBuf )
            throws InvalidHeaderException {

        // parseTarHeader...
        int offset = 0;
        for ( THF zTHF : THFS ) {
            offset = zTHF.populate( offset, headerBuf );
        }

        magic = _magic.asString();
        if ( magic.equals( "" ) ) {
            format = Type.unix;
        } else if ( magic.equals( "ustar" ) ) {
            format = Type.ustar;
        } else if ( magic.startsWith( "ustar" ) ) { // && headerBuf[262] != 0 && headerBuf[263] != 0
            format = Type.gnu;
        } else {
            throw new InvalidHeaderException( _magic.toString() );
        }

        name = parseFileName( headerBuf );
        mode = _mode.asIntOctal();
        userId = _uid.asIntOctal();
        groupId = _gid.asIntOctal();
        size = _size.asLongOctal();
        modTime = _mtime.asLongOctal();
        checkSum = _chksum.asIntOctal();
        linkFlag = _typeflag.asByte();
        linkName = _linkname.asString();

        if ( isUSTarFormat() ) {
            userName = _uname.asString();
            groupName = _gname.asString();
            devMajor = _devmajor.asIntOctal();
            devMinor = _devminor.asIntOctal();
        }
    }

    /**
     * Determine if the given entry is a descendant of this entry.
     * Descendancy is determined by the name of the descendant
     * starting with this entry's name.
     *
     * @param desc Entry to be checked as a descendent of this.
     *
     * @return True if entry is a descendant of this.
     */
    public boolean isDescendent( TarHeader desc ) {
        return desc.name.startsWith( this.name );
    }

    /**
     * Get the name of this entry.
     *
     * @return Teh entry's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Parse an octal string from a header buffer. This is used for the
     * file permission mode value.
     *
     * @param header The header buffer from which to parse.
     * @param offset The offset into the buffer from which to parse.
     * @param length The number of header bytes to parse.
     *
     * @return The long value of the octal string.
     */
    public static long parseOctal( byte[] header, int offset, int length )
            throws InvalidHeaderException {
        long result = 0;
        boolean stillPadding = true;

        int end = offset + length;
        for ( int i = offset; i < end; ++i ) {
            if ( header[i] == 0 ) {
                break;
            }

            if ( header[i] == (byte) ' ' || header[i] == '0' ) {
                if ( stillPadding ) {
                    continue;
                }

                if ( header[i] == (byte) ' ' ) {
                    break;
                }
            }

            stillPadding = false;

            result = (result << 3) + (header[i] - '0');
        }

        return result;
    }

    /**
     * Parse a file name from a header buffer. This is different from
     * parseName() in that is recognizes 'ustar' names and will handle
     * adding on the "prefix" field to the name.
     * <p/>
     * Contributed by Dmitri Tikhonov <dxt2431@yahoo.com>
     *
     * @param header The header buffer from which to parse.
     *
     * @return The header's entry name.
     */
    public static String parseFileName( byte[] header ) {
        StringBuilder result = new StringBuilder( 256 );

        // If header[345] is not equal to zero, then it is the "prefix"
        // that 'ustar' defines. It must be prepended to the "normal"
        // name field. We are responsible for the separating '/'.
        //
        if ( header[345] != 0 ) {
            for ( int i = 345; i < 500 && header[i] != 0; ++i ) {
                result.append( (char) header[i] );
            }

            result.append( "/" );
        }

        for ( int i = 0; i < 100 && header[i] != 0; ++i ) {
            result.append( (char) header[i] );
        }

        return result.toString();
    }

    /**
     * Parse an entry name from a header buffer.
     *
     * @param header The header buffer from which to parse.
     * @param offset The offset into the buffer from which to parse.
     * @param length The number of header bytes to parse.
     *
     * @return The header's entry name.
     */
    public static StringBuffer parseName( byte[] header, int offset, int length )
            throws InvalidHeaderException {
        StringBuffer result = new StringBuffer( length );

        int end = offset + length;
        for ( int i = offset; i < end; ++i ) {
            if ( header[i] == 0 ) {
                break;
            }
            result.append( (char) header[i] );
        }

        return result;
    }

    /**
     * This method, like getNameBytes(), is intended to place a name
     * into a TarHeader's buffer. However, this method is sophisticated
     * enough to recognize long names (name.length() > NAMELEN). In these
     * cases, the method will break the name into a prefix and suffix and
     * place the name in the header in 'ustar' format. It is up to the
     * TarEntry to manage the "entry header format". This method assumes
     * the name is valid for the type of archive being generated.
     *
     * @param outbuf  The buffer containing the entry header to modify.
     * @param newName The new name to place into the header buffer.
     *
     * @return The current offset in the tar header (always TarHeader.NAMELEN).
     *
     * @throws InvalidHeaderException If the name will not fit in the header.
     */
    public static int getFileNameBytes( String newName, byte[] outbuf )
            throws InvalidHeaderException {
        if ( newName.length() > 100 ) {
            // Locate a pathname "break" prior to the maximum name length...
            int index = newName.indexOf( "/", newName.length() - 100 );
            if ( index == -1 ) {
                throw new InvalidHeaderException( "file name is greater than 100 characters, " + newName );
            }

            // Get the "suffix subpath" of the name.
            String name = newName.substring( index + 1 );

            // Get the "prefix subpath", or "prefix", of the name.
            String prefix = newName.substring( 0, index );
            if ( prefix.length() > TarHeader.PREFIXLEN ) {
                throw new InvalidHeaderException( "file prefix is greater than 155 characters" );
            }

            TarHeader.getNameBytes( new StringBuffer( name ), outbuf,
                                    TarHeader.NAMEOFFSET, TarHeader.NAMELEN );

            TarHeader.getNameBytes( new StringBuffer( prefix ), outbuf,
                                    TarHeader.PREFIXOFFSET, TarHeader.PREFIXLEN );
        } else {
            TarHeader.getNameBytes( new StringBuffer( newName ), outbuf,
                                    TarHeader.NAMEOFFSET, TarHeader.NAMELEN );
        }

        // The offset, regardless of the format, is now the end of the
        // original name field.
        //
        return TarHeader.NAMELEN;
    }

    /**
     * Move the bytes from the name StringBuffer into the header's buffer.
     *
     * @param offset The offset into the buffer at which to store.
     * @param length The number of header bytes to store.
     *
     * @return The new offset (offset + length).
     */
    public static int getNameBytes( StringBuffer name, byte[] buf, int offset, int length ) {
        int i;

        for ( i = 0; i < length && i < name.length(); ++i ) {
            buf[offset + i] = (byte) name.charAt( i );
        }

        for (; i < length; ++i ) {
            buf[offset + i] = 0;
        }

        return offset + length;
    }

    /**
     * Parse an octal integer from a header buffer.
     *
     * @param offset The offset into the buffer from which to parse.
     * @param length The number of header bytes to parse.
     *
     * @return The integer value of the octal bytes.
     */
    public static int getOctalBytes( long value, byte[] buf, int offset, int length ) {
        int idx = length - 1;

        buf[offset + idx] = 0;
        --idx;
        buf[offset + idx] = (byte) ' ';
        --idx;

        if ( value == 0 ) {
            buf[offset + idx] = (byte) '0';
            --idx;
        } else {
            for ( long val = value; idx >= 0 && val > 0; --idx ) {
                buf[offset + idx] = (byte) ((byte) '0' + (byte) (val & 7));
                val = val >> 3;
            }
        }

        for (; idx >= 0; --idx ) {
            buf[offset + idx] = (byte) ' ';
        }

        return offset + length;
    }

    /**
     * Parse an octal long integer from a header buffer.
     *
     * @param offset The offset into the buffer from which to parse.
     * @param length The number of header bytes to parse.
     *
     * @return The long value of the octal bytes.
     */
    public static int getLongOctalBytes( long value, byte[] buf, int offset, int length ) {
        byte[] temp = new byte[length + 1];
        TarHeader.getOctalBytes( value, temp, 0, length + 1 );
        System.arraycopy( temp, 0, buf, offset, length );
        return offset + length;
    }

    /**
     * Parse the checksum octal integer from a header buffer.
     *
     * @param offset The offset into the buffer from which to parse.
     * @param length The number of header bytes to parse.
     *
     * @return The integer value of the entry's checksum.
     */
    public static int getCheckSumOctalBytes( long value, byte[] buf, int offset, int length ) {
        getOctalBytes( value, buf, offset, length );
        buf[offset + length - 1] = (byte) ' ';
        buf[offset + length - 2] = 0;
        return offset + length;
    }

    /**
     * 'name' field in a header buffer.
     */
    private static final int NAMEOFFSET = 0;
    private static final int NAMELEN = 100;

    /**
     * The 'name prefix' field in a header buffer.
     */
    private static final int PREFIXOFFSET = 345;
    private static final int PREFIXLEN = 155;

    /**
     * LF_ constants represent the "link flag" of an entry, or more commonly,
     * the "entry type". This is the "old way" of indicating a normal file.
     */
    public static final byte LF_OLDNORM = 0;
    /**
     * Normal file type.
     */
    public static final byte LF_NORMAL = (byte) '0';
    /**
     * Link file type.
     */
    public static final byte LF_LINK = (byte) '1';
    /**
     * Symbolic link file type.
     */
    public static final byte LF_SYMLINK = (byte) '2';
    /**
     * Character device file type.
     */
    public static final byte LF_CHR = (byte) '3';
    /**
     * Block device file type.
     */
    public static final byte LF_BLK = (byte) '4';
    /**
     * Directory file type.
     */
    public static final byte LF_DIR = (byte) '5';
    /**
     * FIFO (pipe) file type.
     */
    public static final byte LF_FIFO = (byte) '6';
    /**
     * Contiguous file type.
     */
    public static final byte LF_CONTIG = (byte) '7';

    /**
     * Get this entry's file size.
     */
    public long getSize() {
        return size;
    }

    /**
     * Return whether or not this entry represents a directory.
     *
     * @return True if this entry is a directory.
     */
    public boolean isDirectory() {
        return (linkFlag == TarHeader.LF_DIR) || getName().endsWith( "/" );
    }

    /**
     * Get this entry's user id.
     *
     * @return This entry's user id.
     */
    public int getUserId() {
        return userId;
    }

    /**
     * Get this entry's group id.
     *
     * @return This entry's group id.
     */
    public int getGroupId() {
        return groupId;
    }

    /**
     * Get this entry's user name.
     *
     * @return This entry's user name.
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Get this entry's group name.
     *
     * @return This entry's group name.
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * Set this entry's modification time.
     */
    public Date getModTime() {
        return new Date( modTime * 1000 );
    }

    public String toString() {
        return "[name=" + getName() +
               ", isDir=" + isDirectory() +
               ", size=" + getSize() +
               ", userId=" + getUserId() +
               ", user=" + getUserName() +
               ", groupId=" + getGroupId() +
               ", group=" + getGroupName() + "]";
    }
}
 
