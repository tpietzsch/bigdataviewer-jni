package bdv;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import bdv.viewer.state.ViewerState;
import mpicbg.spim.data.SpimDataException;
import net.imglib2.realtransform.AffineTransform3D;

public final class BigDataViewerJni
{
	private static final AtomicInteger idGenerator = new AtomicInteger();

	private static final ConcurrentHashMap< Integer, HeadlessBigDataViewer > bdvs = new ConcurrentHashMap< Integer, HeadlessBigDataViewer >();

	public static int construct(
			final String fn,
			final int width,
			final int height,
			final double[] screenscales,
			final int numRenderingThreads )
	{
		final int id = idGenerator.incrementAndGet();
		try
		{
			bdvs.put( id, HeadlessBigDataViewer.open( fn, width, height, screenscales, numRenderingThreads ) );
		}
		catch ( final SpimDataException e )
		{
			e.printStackTrace();
			return -1;
		}
		return id;
	}

	public static int construct(
			final int shareCacheWithId,
			final int width,
			final int height,
			final double[] screenscales,
			final int numRenderingThreads )
	{
		final int id = idGenerator.incrementAndGet();
		try
		{
			bdvs.put( id, HeadlessBigDataViewer.open( bdvs.get( shareCacheWithId ), width, height, screenscales, numRenderingThreads ) );
		}
		catch ( final SpimDataException e )
		{
			e.printStackTrace();
			return -1;
		}
		return id;
	}

	public static void destruct( final int id )
	{
		final HeadlessBigDataViewer bdv = bdvs.remove( id );
		if ( bdv != null )
			bdv.getViewer().stop();
	}

	public static void setTransform( final int id, final double[] m3x4 )
	{
		final HeadlessBigDataViewer bdv = bdvs.get( id );
		if ( bdv != null )
		{
			final AffineTransform3D t = new AffineTransform3D();
			t.set( m3x4 );
			bdv.getViewer().setCurrentViewerTransform( t );
		}
	}

	public static void getTransform( final int id, final double[] m3x4 )
	{
		final HeadlessBigDataViewer bdv = bdvs.get( id );
		if ( bdv != null )
		{
			final ViewerState state = bdv.getViewer().getState();
			final AffineTransform3D t = new AffineTransform3D();
 			state.getViewerTransform( t );
 			System.arraycopy( t.getRowPackedCopy(), 0, m3x4, 0, 12 );
		}
	}

	public static int getNumTimepoints( final int id )
	{
		final HeadlessBigDataViewer bdv = bdvs.get( id );
		if ( bdv != null )
		{
			return bdv.getViewer().getState().getNumTimePoints();
		}
		return 0;
	}

	public static void setTimepoint( final int id, final int timepoint )
	{
		final HeadlessBigDataViewer bdv = bdvs.get( id );
		if ( bdv != null )
		{
			bdv.getViewer().setTimepoint( timepoint );
		}
	}

	public static void setLinearInterpolation( final int id, final boolean enableLinearInterpolation )
	{
		final HeadlessBigDataViewer bdv = bdvs.get( id );
		if ( bdv != null )
		{
			bdv.getViewer().setLinearInterpolation( enableLinearInterpolation );
		}
	}

	public static int getNumSources( final int id )
	{
		final HeadlessBigDataViewer bdv = bdvs.get( id );
		if ( bdv != null )
		{
			return bdv.getViewer().getState().numSources();
		}
		return 0;
	}

	public static void setSourceVisible( final int id, final int source, final boolean visible )
	{
		final HeadlessBigDataViewer bdv = bdvs.get( id );
		if ( bdv != null )
		{
			bdv.getViewer().getVisibilityAndGrouping().setSourceActive( source, visible );
		}
	}

	public static boolean isSourceVisible( final int id, final int source )
	{
		final HeadlessBigDataViewer bdv = bdvs.get( id );
		if ( bdv != null )
		{
			return bdv.getViewer().getVisibilityAndGrouping().isSourceVisible( source );
		}
		return false;
	}

	public static void setSourceParams( final int id, final int source, final int min, final int max, final int argb )
	{
		final HeadlessBigDataViewer bdv = bdvs.get( id );
		if ( bdv != null )
		{
			bdv.setSourceParams( source, min, max, argb );
		}
	}

	public static int getSourceParamsMin( final int id, final int source )
	{
		final HeadlessBigDataViewer bdv = bdvs.get( id );
		if ( bdv != null )
		{
			return bdv.getSourceParamsMin( source );
		}
		return 0;
	}

	public static int getSourceParamsMax( final int id, final int source )
	{
		final HeadlessBigDataViewer bdv = bdvs.get( id );
		if ( bdv != null )
		{
			return bdv.getSourceParamsMax( source );
		}
		return 0;
	}

	public static int getSourceParamsColor( final int id, final int source )
	{
		final HeadlessBigDataViewer bdv = bdvs.get( id );
		if ( bdv != null )
		{
			return bdv.getSourceParamsColor( source );
		}
		return 0;
	}

	public static ARGBRenderImage getRenderedBitmap( final int id )
	{
		final HeadlessBigDataViewer bdv = bdvs.get( id );
		if ( bdv != null )
		{
			return bdv.getViewer().getLatestImage();
		}
		return null;
	}

	public static void main( final String[] args ) throws SpimDataException
	{
		final String fn = "/Users/pietzsch/workspace/data/111010_weber_full.xml";
		final int id = construct( fn, 8000, 6000, new double[] { 1, 0.75, 0.5, 0.25, 0.125 }, 3 );
		for ( int i = 0; i < 10; ++i )
			System.out.println( getRenderedBitmap( id ) );
		destruct( id );
	}
}
