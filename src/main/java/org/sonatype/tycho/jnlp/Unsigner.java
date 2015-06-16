package org.sonatype.tycho.jnlp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class Unsigner
{

    private final Logger log = Logger.getLogger( getClass().getName() );

    public File unsign( final File src )
        throws RuntimeException
    {
        final File bak = new File( src.getParentFile(), src.getName() + ".bak" );

        if ( bak.exists() )
        {
            bak.delete();
        }

        log.info( "Backing up original jar: " + src + "\n    to: " + bak );
        src.renameTo( bak );

        ZipFile in = null;
        ZipOutputStream out = null;
        try
        {
            in = new ZipFile( bak );
            out = new ZipOutputStream( new FileOutputStream( src ) );

            for ( final Enumeration<? extends ZipEntry> entries = in.entries(); entries.hasMoreElements(); )
            {
                final ZipEntry entry = entries.nextElement();
                if ( entry.getName().endsWith( ".SF" ) || entry.getName().endsWith( ".RSA" )
                    || entry.getName().endsWith( ".LIST" ) || entry.getName().endsWith( ".DSA" ) )
                {
                    continue;
                }

                out.putNextEntry( entry );
                if ( !entry.isDirectory() )
                {
                    final InputStream stream = in.getInputStream( entry );
                    final byte[] buf = new byte[16384];
                    int read = -1;
                    while ( ( read = stream.read( buf ) ) > -1 )
                    {
                        out.write( buf, 0, read );
                    }
                }
                out.closeEntry();
            }
        }
        catch ( final IOException e )
        {
            throw new RuntimeException( "Cannot read: " + bak, e );
        }
        finally
        {
            if ( in != null )
            {
                try
                {
                    in.close();
                }
                catch ( final IOException e )
                {
                }
            }

            if ( out != null )
            {
                try
                {
                    out.close();
                }
                catch ( final IOException e )
                {
                    log.log( Level.WARNING, "Failed to close output jar: " + src + ". Error: " + e.getMessage(), e );
                }
            }
        }

        return bak;
    }
}
