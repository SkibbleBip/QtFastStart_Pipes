/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 SkibbleBip
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 *
 * @author SkibbleBip
 * Represents a "fake" FileChannel to read and write to, where all data is contained in a byte array
 * instead of an actual file device
 */
public class ArtificialFileStream {
    
        private int position;
        private byte[] array;
        /**
         * 
         * @return Position currently processing from
         */
        public int position(){return this.position;}
        /**
         * 
         * @return the total size of the internal byte array contained in this ArtificialFileStream
         */
        public int size(){return this.array.length;}
        /**
         * 
         * @return internal byte array contained in the ArtificialFileStream
         */
        public byte[] getByteArray(){return this.array;}
        
        /**
         * 
         * @param newPosition sets the current processing position
         * @return this ArtificialFileStream
         */
        public ArtificialFileStream position(int newPosition){
            this.position = newPosition;
            return this;
        }
        

        /**
         * 
         * @param is inputstream to create the ArtificialFileStream from
         * @throws IOException
         * @throws QtFastStart_Pipes.ArtificialFileStream.BadFileSizeException 
         */
        public ArtificialFileStream(InputStream is) throws IOException, BadFileSizeException{
            this.array = is.readAllBytes();
            this.position = 0;
            
            if(this.array.length < 0 )
                throw new BadFileSizeException("File size is negative");
            if(this.array.length > Integer.MAX_VALUE)
                throw new BadFileSizeException("File is bigger than the supported size " + Integer.MAX_VALUE);
        
        }
        /**
         * 
         * @param b byte array
         */
        public ArtificialFileStream(byte[] b){
            this.array = b;
            this.position = 0;
        }
        /**
         * 
         */
        public ArtificialFileStream(){
            this.array = new byte[0];
            this.position = 0;
        
        }
        
        /*Methods*/
        
        
        
     /**
     * @param buffer  Byte Buffer.
     * @return Number of bytes read, possibly less than size of byte buffer
     */
        public ByteBuffer read(ByteBuffer buffer){
            
            //buffer = buffer.clear();
            
            int q;
            
            if(this.array.length - this.position < buffer.capacity())
                q = this.array.length - this.position;
            else
                q = buffer.remaining();
            
            byte[] tmp = new byte[q];
            
            
            //                   src           srcPos   dest   destPos    length
            System.arraycopy(this.array, this.position, tmp,     0,         q);
            
            buffer = buffer.put(tmp);
            this.position += tmp.length;
            return buffer;
        
        }
        
        /**
         * 
         * @param buffer byte buffer
         * @param position position to read from
         * @return resulting byte buffer that was read
         * @throws QtFastStart_Pipes.ArtificialFileStream.BadFilePositionException 
         */
        public ByteBuffer read(ByteBuffer buffer, int position) throws BadFilePositionException{
            
            //buffer = buffer.clear();
            int q;
            
            if(this.array.length < position)
                throw new BadFilePositionException("Position "+ position + " is bigger than the size "+ this.array.length);
            
            if(position < 0)
                throw new BadFilePositionException("Position "+ position + " is negative");
            
            if(this.array.length - position < buffer.capacity())
                q = this.array.length - position;
            else
                q = buffer.remaining();
            
            byte[] tmp = new byte[/*buffer.remaining()*/q];
            
            
            System.arraycopy(this.array, position, tmp,     0,         q);
            
            buffer = buffer.put(tmp);
            
            this.position+=q;
            
            return buffer;
        
        }
        
        /**
         * 
         * @param buffer input byte buffer
         * @return number of bytes read
         */
        public int write(ByteBuffer buffer){
            byte[] tmp = new byte[this.array.length + buffer.capacity()];
            byte[] buff = buffer.array();
            
            
            System.arraycopy(this.array, 0, tmp, 0, this.array.length);
            System.arraycopy(buff, 0, tmp, this.array.length, buff.length);
            
            this.array = tmp;
            
            return buff.length;
        
        }
        
        
        /**
         * 
         * @param buffer byte buffer to read to
         * @param position position to write
         * @return number of bytes read
         * @throws QtFastStart_Pipes.ArtificialFileStream.BadFilePositionException 
         */
        public int write(ByteBuffer buffer, int position) throws BadFilePositionException{
            
            
            if(position < 0)
                throw new BadFilePositionException("Position " + position + " is negative");
            
            if(position > this.array.length){
                byte[] tmp = new byte[position];
                System.arraycopy(this.array, 0, tmp, 0, this.array.length);
                System.arraycopy(buffer.array(), 0, tmp, position - buffer.capacity(), buffer.capacity());
                
                this.array = tmp;
                return buffer.capacity();
                
            }
            
            if(position + buffer.capacity() > this.array.length){
                byte[] tmp = new byte[position+buffer.capacity()];
                
                System.arraycopy(this.array, 0, tmp, 0, this.array.length);
                System.arraycopy(buffer.array(), 0, tmp, position, buffer.capacity());
                
                this.array = tmp;
                
                return buffer.capacity();
                
            }
            
            System.arraycopy(buffer.array(), 0, this.array, position, buffer.capacity());
            
            return buffer.capacity();
            
        
        }
        
        /**
         * 
         * @param position position to write to
         * @param count number of bytes to transfer
         * @param target the ArtificialFileStream to write to
         * @return number of bytes transferred
         * @throws QtFastStart_Pipes.ArtificialFileStream.BadFilePositionException 
         */
        public int transferTo(int position, int count, ArtificialFileStream target) throws BadFilePositionException{
            if(count < 0)
                throw new BadFilePositionException("count "+count+" is negative");
            
            ByteBuffer tmp = ByteBuffer.allocate(count);
            
            ByteBuffer ret = this.read(tmp, position);
            
            
            target.write(ret);
            
            return ret.capacity();
        
        }
        
        
        
        
        /*Artificial file exceptions*/
    
    public static class BadFileSizeException extends Exception{
        public BadFileSizeException(String err){
            super(err);
        }
    
    }
    
    public static class BadFilePositionException extends Exception{
        public BadFilePositionException(String err){
            super(err);
        }
    
    }
    
    
    
}
