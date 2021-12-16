/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Yuya Tanaka
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package QtFastStart_Pipes;


import QtFastStart_Pipes.ArtificialFileStream.BadFilePositionException;
import QtFastStart_Pipes.ArtificialFileStream.BadFileSizeException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Level;
import java.util.logging.Logger;

// Ported from qt-faststart.c, released in public domain.
// I'll make this open source. :)
// blob: d2a06242966d7a640d32d304a5653f4e1545f259
// commit: 0ea54d698be613465d92a82495001ddabae128b0
// author: ypresto
public class QtFastStart {
    public static boolean sDEBUG = false;

    

    /* package */
    static long uint32ToLong(int int32) {
        return int32 & 0x00000000ffffffffL;
    }

    /**
     * Ensures passed uint32 value in long can be represented as Java int.
     */
    /* package */
    static int uint32ToInt(int uint32) throws UnsupportedFileException {
        if (uint32 < 0) {
            throw new UnsupportedFileException("uint32 value is too large");
        }
        return uint32;
    }

    /**
     * Ensures passed uint32 value in long can be represented as Java int.
     */
    /* package */
    static int uint32ToInt(long uint32) throws UnsupportedFileException {
        if (uint32 > Integer.MAX_VALUE || uint32 < 0) {
            throw new UnsupportedFileException("uint32 value is too large");
        }
        return (int) uint32;
    }

    /**
     * Ensures passed uint64 value can be represented as Java long.
     */
    /* package */
    static long uint64ToLong(long uint64) throws UnsupportedFileException {
        if (uint64 < 0) throw new UnsupportedFileException("uint64 value is too large");
        return uint64;
    }

    private static int fourCcToInt(byte[] byteArray) {
        return ByteBuffer.wrap(byteArray).order(ByteOrder.BIG_ENDIAN).getInt();
    }

    private static void printf(String format, Object... args) {
        if (sDEBUG) System.err.println("QtFastStart: " + String.format(format, args));
    }


    private static ByteBuffer readAndFill(ArtificialFileStream infile, ByteBuffer buffer)/* throws IOException*/ {
        buffer = buffer.clear();

        buffer = infile.read(buffer);
        buffer.flip();
        return  buffer;
    }

    private static ByteBuffer readAndFill(ArtificialFileStream infile, ByteBuffer buffer, long position) throws BadFilePositionException{
        buffer = buffer.clear();
        
        buffer = infile.read(buffer, (int)position);
        
        buffer.flip();
        return buffer;//size == buffer.capacity();
    }

    /* top level atoms */
    private static final int FREE_ATOM = fourCcToInt(new byte[]{'f', 'r', 'e', 'e'});
    private static final int JUNK_ATOM = fourCcToInt(new byte[]{'j', 'u', 'n', 'k'});
    private static final int MDAT_ATOM = fourCcToInt(new byte[]{'m', 'd', 'a', 't'});
    private static final int MOOV_ATOM = fourCcToInt(new byte[]{'m', 'o', 'o', 'v'});
    private static final int PNOT_ATOM = fourCcToInt(new byte[]{'p', 'n', 'o', 't'});
    private static final int SKIP_ATOM = fourCcToInt(new byte[]{'s', 'k', 'i', 'p'});
    private static final int WIDE_ATOM = fourCcToInt(new byte[]{'w', 'i', 'd', 'e'});
    private static final int PICT_ATOM = fourCcToInt(new byte[]{'P', 'I', 'C', 'T'});
    private static final int FTYP_ATOM = fourCcToInt(new byte[]{'f', 't', 'y', 'p'});
    private static final int UUID_ATOM = fourCcToInt(new byte[]{'u', 'u', 'i', 'd'});

    private static final int CMOV_ATOM = fourCcToInt(new byte[]{'c', 'm', 'o', 'v'});
    private static final int STCO_ATOM = fourCcToInt(new byte[]{'s', 't', 'c', 'o'});
    private static final int CO64_ATOM = fourCcToInt(new byte[]{'c', 'o', '6', '4'});

    private static final int ATOM_PREAMBLE_SIZE = 8;

    /**
     * @param in  Input Stream.
     * @return input stream if input file is already fast start, or byte array of the resulting output
     * @throws IOException
     */
    public static byte[] fastStart(InputStream in) throws IOException{
        
        byte[] ret = null;
        ArtificialFileStream aStream;
        
        try {
            
            aStream = new ArtificialFileStream(in);
            ret = fastStartImpl(aStream);
        } catch (BadFileSizeException | MalformedFileException | UnsupportedFileException | BadFilePositionException ex) {
            Logger.getLogger(QtFastStart.class.getName()).log(Level.SEVERE, null, ex);
        }
            
        in.close();
        return ret;

    }
    /**
     * @param in byte array.
     * @return input bytes if input file is already fast start, or byte array of the resulting output
     * @throws IOException
     */
    public static byte[] fastStart(byte[] in) throws IOException{
        
        byte[] ret = null;
        ArtificialFileStream aStream;
        
        aStream = new ArtificialFileStream(in);
        try {
            ret = fastStartImpl(aStream);
        } catch (MalformedFileException | UnsupportedFileException | BadFilePositionException ex) {
            Logger.getLogger(QtFastStart.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return ret;
    
    }    

    private static byte[] fastStartImpl(ArtificialFileStream in) throws MalformedFileException, UnsupportedFileException, BadFilePositionException {
        ByteBuffer atomBytes = ByteBuffer.allocate(ATOM_PREAMBLE_SIZE).order(ByteOrder.BIG_ENDIAN);
        int atomType = 0;
        long atomSize = 0; // uint64_t
        long lastOffset;
        ByteBuffer moovAtom;
        ByteBuffer ftypAtom = null;
        // uint64_t, but assuming it is in int32 range. It is reasonable as int max is around 2GB. Such large moov is unlikely, yet unallocatable :).
        int moovAtomSize;
        int startOffset = 0;
        
        ArtificialFileStream outStream = new ArtificialFileStream();
        

        // traverse through the atoms in the file to make sure that 'moov' is at the end
        int orig = atomBytes.capacity();
        atomBytes = readAndFill(in, atomBytes);
        while (orig == atomBytes.limit()) {
            atomSize = uint32ToLong(atomBytes.getInt()); // uint32
            atomType = atomBytes.getInt(); // representing uint32_t in signed int

            // keep ftyp atom
            if (atomType == FTYP_ATOM) {
                int ftypAtomSize = uint32ToInt(atomSize); // XXX: assume in range of int32_t
                ftypAtom = ByteBuffer.allocate(ftypAtomSize).order(ByteOrder.BIG_ENDIAN);
                atomBytes.rewind();
                ftypAtom.put(atomBytes);
                
                ftypAtom = in.read(ftypAtom);
                if (ftypAtom.capacity() < ftypAtomSize - ATOM_PREAMBLE_SIZE) 
                    break;
                ftypAtom.flip();
                startOffset = in.position(); // after ftyp atom
            } else {
                if (atomSize == 1) {
                    /* 64-bit special case */
                    atomBytes.clear();
                    
                    atomBytes = readAndFill(in, atomBytes);
                    if (orig != atomBytes.capacity()) 
                        break;
                    atomSize = uint64ToLong(atomBytes.getLong()); // XXX: assume in range of int64_t
                    in.position(in.position() + (int)atomSize - ATOM_PREAMBLE_SIZE * 2); // seek
                } else {
                    in.position(in.position() + (int)atomSize - ATOM_PREAMBLE_SIZE); // seek
                }
            }
            if (sDEBUG) printf("%c%c%c%c %10d %d",
                    (atomType >> 24) & 255,
                    (atomType >> 16) & 255,
                    (atomType >> 8) & 255,
                    (atomType >> 0) & 255,
                    in.position() - atomSize,
                    atomSize);
            if ((atomType != FREE_ATOM)
                    && (atomType != JUNK_ATOM)
                    && (atomType != MDAT_ATOM)
                    && (atomType != MOOV_ATOM)
                    && (atomType != PNOT_ATOM)
                    && (atomType != SKIP_ATOM)
                    && (atomType != WIDE_ATOM)
                    && (atomType != PICT_ATOM)
                    && (atomType != UUID_ATOM)
                    && (atomType != FTYP_ATOM)) {
                if(sDEBUG)
                    printf("encountered non-QT top-level atom (is this a QuickTime file?)");
                break;
            }

        /* The atom header is 8 (or 16 bytes), if the atom size (which
         * includes these 8 or 16 bytes) is less than that, we won't be
         * able to continue scanning sensibly after this atom, so break. */
            if (atomSize < 8)
                break;
        
        atomBytes = readAndFill(in, atomBytes);
        
        }

        if (atomType != MOOV_ATOM) {
            if(sDEBUG) 
                printf("last atom in file was not a moov atom");
            return in.getByteArray();
        }

        // moov atom was, in fact, the last atom in the chunk; load the whole moov atom

        // atomSize is uint64, but for moov uint32 should be stored.
        // XXX: assuming moov atomSize <= max vaue of int32
        moovAtomSize = uint32ToInt(atomSize);
        lastOffset = in.size() - moovAtomSize; // NOTE: assuming no extra data after moov, as qt-faststart.c
        moovAtom = ByteBuffer.allocate(moovAtomSize).order(ByteOrder.BIG_ENDIAN);
        
        orig = moovAtom.capacity();
        moovAtom = readAndFill(in, moovAtom, lastOffset);
        if (orig != moovAtom.capacity()) {
            throw new MalformedFileException("failed to read moov atom");
        }

        // this utility does not support compressed atoms yet, so disqualify files with compressed QT atoms
        if (moovAtom.getInt(12) == CMOV_ATOM) {
            throw new UnsupportedFileException("this utility does not support compressed moov atoms yet");
        }

        // crawl through the moov chunk in search of stco or co64 atoms
        while (moovAtom.remaining() >= 8) {
            int atomHead = moovAtom.position();
            atomType = moovAtom.getInt(atomHead + 4); // representing uint32_t in signed int
            if (!(atomType == STCO_ATOM || atomType == CO64_ATOM)) {
                moovAtom.position(moovAtom.position() + 1);
                continue;
            }
            atomSize = uint32ToLong(moovAtom.getInt(atomHead)); // uint32
            if (atomSize > moovAtom.remaining()) {
                throw new MalformedFileException("bad atom size");
            }
            moovAtom.position(atomHead + 12); // skip size (4 bytes), type (4 bytes), version (1 byte) and flags (3 bytes)
            if (moovAtom.remaining() < 4) {
                throw new MalformedFileException("malformed atom");
            }
            // uint32_t, but assuming moovAtomSize is in int32 range, so this will be in int32 range
            int offsetCount = uint32ToInt(moovAtom.getInt());
            if (atomType == STCO_ATOM) {
                
                if(sDEBUG)
                    printf("patching stco atom...");
                
                if (moovAtom.remaining() < offsetCount * 4) {
                    throw new MalformedFileException("bad atom size/element count");
                }
                for (int i = 0; i < offsetCount; i++) {
                    int currentOffset = moovAtom.getInt(moovAtom.position());
                    int newOffset = currentOffset + moovAtomSize; // calculate uint32 in int, bitwise addition
                    // current 0xffffffff => new 0x00000000 (actual >= 0x0000000100000000L)
                    if (currentOffset < 0 && newOffset >= 0) {
                        throw new UnsupportedFileException("This is bug in original qt-faststart.c: "
                                + "stco atom should be extended to co64 atom as new offset value overflows uint32, "
                                + "but is not implemented.");
                    }
                    moovAtom.putInt(newOffset);
                }
            } else if (atomType == CO64_ATOM) {
                
                if(sDEBUG)
                    printf("patching co64 atom...");
                if (moovAtom.remaining() < offsetCount * 8) {
                    throw new MalformedFileException("bad atom size/element count");
                }
                for (int i = 0; i < offsetCount; i++) {
                    long currentOffset = moovAtom.getLong(moovAtom.position());
                    moovAtom.putLong(currentOffset + moovAtomSize); // calculate uint64 in long, bitwise addition
                }
            }
        }

        in.position(startOffset); // seek after ftyp atom

        if (ftypAtom != null) {
            // dump the same ftyp atom
            if(sDEBUG)
                printf("writing ftyp atom...");
            ftypAtom.rewind();
            outStream.write(ftypAtom);
        }

        // dump the new moov atom
        if(sDEBUG)
            printf("writing moov atom...");
        moovAtom.rewind();
        outStream.write(moovAtom);

        // copy the remainder of the infile, from offset 0 -> (lastOffset - startOffset) - 1
        if(sDEBUG)
            printf("copying rest of file...");
        in.transferTo(startOffset, (int)(lastOffset - startOffset), outStream);

        return outStream.getByteArray();
    }

    public static class QtFastStartException extends Exception {
        private QtFastStartException(String detailMessage) {
            super(detailMessage);
        }
    }

    public static class MalformedFileException extends QtFastStartException {
        private MalformedFileException(String detailMessage) {
            super(detailMessage);
        }
    }

    public static class UnsupportedFileException extends QtFastStartException {
        private UnsupportedFileException(String detailMessage) {
            super(detailMessage);
        }
    }
    

    
    
}
