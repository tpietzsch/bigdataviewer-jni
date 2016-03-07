package bdv;

import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.array.IntArray;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.util.Fraction;

public final class ARGBRenderImage extends ArrayImg< ARGBType, IntArray >
{
	final private int[] data;

	boolean isComplete;

	public ARGBRenderImage( final int width, final int height )
	{
		this( width, height, new int[ width * height ] );
	}

	public ARGBRenderImage( final int width, final int height, final IntArray data )
	{
		this( width, height, data.getCurrentStorageArray() );
	}

	public ARGBRenderImage( final int width, final int height, final int[] data )
	{
		super( new IntArray( data ), new long[]{ width, height }, new Fraction() );
		setLinkedType( new ARGBType( this ) );
		this.data = data;
	}

	public int[] getData()
	{
		return data;
	}

	public int getWidth()
	{
		return ( int ) dimension( 0 );
	}

	public int getHeight()
	{
		return ( int ) dimension( 1 );
	}

	public boolean isComplete()
	{
		return isComplete;
	}
}
