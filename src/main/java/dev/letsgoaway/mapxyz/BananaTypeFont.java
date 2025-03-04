package dev.letsgoaway.mapxyz;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.bukkit.map.MapFont;

/**
 * Simple pixel style font for Minecraft
 *
 * @author BananaPuncher714
 */
// thanks!! https://github.com/BananaPuncher714/BananaTypeFont/
public class BananaTypeFont extends MapFont {
    private String name;
    private String version;
    private String description;

    private BananaTypeFont( String name, String description, String version ) {
        this.name = name;
        this.description = description;
        this.version = version;

        malleable = true;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getDescription() {
        return description;
    }

    public static BananaTypeFont from( String name, String description, String version ) {
        return new BananaTypeFont( name, description, version );
    }

    /**
     * Create a new BananaFont from the given input stream.
     * @param stream
     * A stream containing a valid BananaTypeFont.
     * @return
     * @throws IOException
     */
    public static BananaTypeFont from( InputStream stream ) throws IOException {
        // Convert the input stream to a byte array
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[ 16384 ];
        while ( ( nRead = stream.read( data, 0, data.length ) ) != -1 ) buffer.write( data, 0, nRead );
        byte[] arr = buffer.toByteArray();

        // Get the binary reader
        BinaryReader reader = new BinaryReader( arr );

        String id = reader.getString( 2 );
        if ( !id.equals( "BF" ) ) throw new IllegalArgumentException( "Invalid header!" );

        int format = reader.getInt16();
        if ( format == 1 ) return parseFormat1( reader );

        throw new IllegalArgumentException( String.format( "Unsupported BananaTypeFont format %d", format ) );
    }

    private static BananaTypeFont parseFormat1( BinaryReader reader ) {
        int dataOffset = reader.getInt16();
        String name = reader.getString( reader.getInt8() );
        String version = reader.getString( reader.getInt8() );
        String desc = reader.getString( reader.getInt8() );

        BananaTypeFont font = new BananaTypeFont( name, version, desc );

        reader.seek( dataOffset + 2 );
        int maxWidth = 0;
        int maxHeight = 0;
        while ( reader.pos() < reader.length() ) {
            int width = reader.getInt8();
            int height = reader.getInt8();
            maxWidth = Math.max( maxWidth, width );
            maxHeight = Math.max( maxHeight, height );
            reader.seek( reader.pos() + ( int ) Math.ceil( ( width * height ) / 8.0 ) + 2 );
        }

        reader.seek( dataOffset );
        while ( reader.pos() < reader.length() ) {
            int c = reader.getInt16();
            int width = reader.getInt8();
            int height = reader.getInt8();

            int size = width * height;
            if ( size > 0 ) {
                boolean[] values = new boolean[ width * maxHeight ];
                int index = 0;
                while ( index < size ) {
                    int v = reader.getInt8();
                    for ( int i = 7; i >= 0 && index < size; i-- ) {
                        int x = index % width;
                        int y = index / width + 1;
                        values[ x + ( maxHeight - y ) * width ] = ( ( v >> i ) & 1 ) == 1;
                        index++;
                    }
                }

                font.setChar( ( char ) c, new CharacterSprite( width, maxHeight, values ) );
            }
        }
        font.setChar( ' ', new CharacterSprite( maxWidth >> 3, 0, new boolean[ 0 ] ) );

        return font;
    }

    private static class BinaryReader {
        private int pos;
        private byte[] data;

        public BinaryReader( byte[] buffer ) {
            this.pos = 0;
            this.data = buffer;
        }

        public int seek( int newPos ) {
            int oldPos = pos;
            pos = newPos;
            return oldPos;
        }

        public int pos() {
            return pos;
        }

        public int getInt8() {
            if ( pos >= data.length ) return 0;
            byte v = data[ pos++ ];
            if ( v < 0 ) {
                return 1 << 7 | ( v & 0b1111111 );
            }
            return v;
        }

        public int getInt16() {
            return getInt8() | getInt8() << 8;
        }

        public String getString( int len ) {
            StringBuilder b = new StringBuilder();
            for ( int i = 0; i < len; i++ ) {
                b.append( ( char ) getInt8() );
            }
            return b.toString();
        }

        public int length() {
            return data.length;
        }
    }
}