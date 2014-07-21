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
        unix { // "old-unix" format - magic tag == ""

            @Override
            public boolean isAcceptableMagicString( String pMagic ) {
                return pMagic.length() == 0;
            }
        },
        ustar { // 'ustar' format - magic tag representing a POSIX tar archive.

            @Override
            public boolean isAcceptableMagicString( String pMagic ) {
                return pMagic.equals( "ustar" );
            }
        },
        gnu { // GNU 'ustar' format - magic tag representing a GNU tar archive: "ustar  " spaces???

            @Override
            public boolean isAcceptableMagicString( String pMagic ) {
                return pMagic.startsWith( "ustar" ) && (pMagic.length() > 5); // && headerBuf[262] != 0 && headerBuf[263] != 0
            }
        };

        abstract public boolean isAcceptableMagicString( String pMagic );

        public static Type getType( String pMagic ) {
            for ( Type zType : values() ) {
                if ( zType.isAcceptableMagicString( pMagic ) ) {
                    return zType;
                }
            }
            return null;
        }
    }

    public final Type format;

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

    public enum Action {
        Normal,
        Directory,
        Ignore,
        ReportProceed,
        ReportIgnore,
        ReportExtended,
        Extended,
        Error;

        public boolean isDirectory() {
            return name().equals( "Directory" );
        }

        public boolean error() {
            return name().contains( "Error" );
        }

        public boolean report() {
            return name().contains( "Report" );
        }

        public boolean ignore() {
            return name().contains( "Ignore" );
        }

        public boolean extended() {
            return name().contains( "Extended" );
        }
    }

    public enum TypeFlag {
        Normal( Action.Normal, (char) 0, '0' ), // All formats
        HardLink( '1' ), // All formats
        SymLink( Action.Error, '2' ), // All formats BUT old GNU = reserved
        CharacterSpecial( '3' ), // All but unix
        BlockSpecial( '4' ), // All but unix
        Directory( Action.Directory, '5' ), // All but unix
        FIFO( '6' ), // All but unix
        Contiguous( '7' ), // All but unix, & old GNU = reserved
        GlobalExtendedHeader( Action.ReportExtended, 'g' ), // All but unix
        ExtendedHeader( Action.ReportExtended, 'x' ), // All but unix
        // A-Z vendor specific extensions
        SolarisACL( 'A' ),
        SolarisExtendedAttributeFile( Action.ReportExtended, 'E' ),
        InodeOnly( 'I' ), // as in'star'
        ObsoleteGNUfileNameTooLong( Action.ReportExtended, 'N' ), // for file names that do not fit into the main header.
        POSIXeXtended( Action.ReportExtended, 'X' ), // POSIX 1003.1-2001 eXtended (VU version) AND Solaris extended Header
        GNU_DumpDir( Action.Ignore, 'D' ), // This is a dir entry that contains the names of files that were in the dir at the time the dump was made.
        GNU_LongLink( Action.ReportExtended, 'K' ), // Identifies the *next* file on the tape as having a long linkname.
        GNU_LongName( Action.Extended, 'L' ), // Identifies the *next* file on the tape as having a long name.
        GNU_MultiVol( Action.Error, 'M' ), // This is the continuation of a file that began on another volume.
        GNU_Sparse( Action.Error, 'S' ), // This is for sparse files.
        GNU_VolDeader( Action.Ignore, 'V' ); // This file is a tape/volume header.  Ignore it on extraction.

        private final Action mAction;
        private final byte[] mIdentifiers;

        TypeFlag( Action pAction, char... pIdentifiers ) {
            mAction = pAction;
            mIdentifiers = new byte[pIdentifiers.length];
            for ( int i = 0; i < pIdentifiers.length; i++ ) {
                mIdentifiers[i] = (byte) pIdentifiers[i];
            }
        }

        TypeFlag( char pIdentifier ) {
            this( Action.ReportProceed, pIdentifier );
        }

        public Action getAction() {
            return mAction;
        }

        private boolean isIdentifier( byte pIdentifier ) {
            for ( byte zIdentifier : mIdentifiers ) {
                if ( zIdentifier == pIdentifier ) {
                    return true;
                }
            }
            return false;
        }

        public static TypeFlag find( byte pIdentifier ) {
            for ( TypeFlag zTypeFlag : values() ) {
                if ( zTypeFlag.isIdentifier( pIdentifier ) ) {
                    return zTypeFlag;
                }
            }
            return null;
        }
    }

    private String name = "";
    @SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"}) private int permissionMode; // permission mode.
    private int userId;
    private int groupId;
    private long size; // in bytes
    private long modificationTime;
    @SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"}) private int checkSum;
    private TypeFlag typeFlag;
    @SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"}) private String linkName = "";
    private String userName = "";
    private String groupName = "";
    @SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"}) private int devMajor; // major device number.
    @SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"}) private int devMinor; // minor device number.

    @SuppressWarnings("UnusedDeclaration")
    public boolean isUnixTarFormat() {
        return format == Type.unix;
    }

    public boolean isUSTarFormat() {
        return format == Type.ustar;
    }

    @SuppressWarnings("UnusedDeclaration")
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

        if ( null == (format = Type.getType( _magic.asString() )) ) {
            throw new InvalidHeaderException( "Unrecognized: " + _magic.toString() );
        }

        name = parseFileName( headerBuf );
        permissionMode = _mode.asIntOctal();
        userId = _uid.asIntOctal();
        groupId = _gid.asIntOctal();
        size = _size.asLongOctal();
        modificationTime = _mtime.asLongOctal();
        checkSum = _chksum.asIntOctal();
        linkName = _linkname.asString();

        if ( isUSTarFormat() ) {
            userName = _uname.asString();
            groupName = _gname.asString();
            devMajor = _devmajor.asIntOctal();
            devMinor = _devminor.asIntOctal();
        }

        if ( null == (typeFlag = TypeFlag.find( _typeflag.asByte() )) ) {
            throw new InvalidHeaderException( "Unrecognized: " + _typeflag );
        }
        Action zAction = typeFlag.getAction();
        if ( zAction.error() ) {
            throw new RuntimeException( "Unable to process: " + this );
        }
        if ( zAction.report() ) {
            System.out.println( "*** Report: " + this );
        }
    }

    public TypeFlag getTypeFlag() {
        return typeFlag;
    }

    public Action getAction() {
        return (typeFlag != null) ? typeFlag.getAction() : Action.Error;
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
        return getAction().isDirectory() || getName().endsWith( "/" );
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
    public Date getModificationTime() {
        return new Date( modificationTime * 1000 );
    }

    public String toString() {
        return "[" + format + "-" + typeFlag
               + ", name=" + getName()
               + ", isDir=" + isDirectory()
               + ", size=" + getSize()
               + ", userId=" + getUserId()
               + ", user=" + getUserName()
               + ", groupId=" + getGroupId()
               + ", group=" + getGroupName()
               + "]";
    }
}
 
