package bdv;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import bdv.img.cache.Cache;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.WrapBasicImgLoader;
import bdv.spimdata.XmlIoSpimDataMinimal;
import bdv.tools.InitializeViewerState;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.MinMaxGroup;
import bdv.tools.brightness.SetupAssignments;
import bdv.viewer.DisplayMode;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.VisibilityAndGrouping;

public final class HeadlessBigDataViewer
{
	private final SetupAssignments setupAssignments;

	private final HeadlessViewerPanel viewer;

	private File proposedSettingsFile;

	/**
	 *
	 * @param converterSetups
	 *            list of {@link ConverterSetup} that control min/max and color
	 *            of sources.
	 * @param sources
	 *            list of pairs of source of some type and converter from that
	 *            type to ARGB.
	 * @param spimData
	 *            may be null. The {@link AbstractSpimData} of the dataset (if
	 *            there is one). If it exists, it is used to set up a "Crop"
	 *            dialog.
	 * @param numTimepoints
	 *            the number of timepoints in the dataset.
	 * @param cache
	 *            handle to cache. This is used to control io timing.
	 * @param width
	 *            width of the viewer window.
	 * @param height
	 *            height of the viewer window.
	 * @param screenscales
	 *            Scale factors from the viewer canvas to screen images of
	 *            different resolutions. A scale factor of 1 means 1 pixel in
	 *            the screen image is displayed as 1 pixel on the canvas, a
	 *            scale factor of 0.5 means 1 pixel in the screen image is
	 *            displayed as 2 pixel on the canvas, etc.
	 */
	public HeadlessBigDataViewer(
			final ArrayList< ConverterSetup > converterSetups,
			final ArrayList< SourceAndConverter< ? > > sources,
			final AbstractSpimData< ? > spimData,
			final int numTimepoints,
			final Cache cache,
			final int width,
			final int height,
			final double[] screenscales )
	{
		setupAssignments = new SetupAssignments( converterSetups, 0, 65535 );
		if ( setupAssignments.getMinMaxGroups().size() > 0 )
		{
			final MinMaxGroup group = setupAssignments.getMinMaxGroups().get( 0 );
			for ( final ConverterSetup setup : setupAssignments.getConverterSetups() )
				setupAssignments.moveSetupToGroup( setup, group );
		}
		viewer = new HeadlessViewerPanel( sources, numTimepoints, cache, HeadlessViewerPanel.options().width( width ).height( height ).screenScales( screenscales ) );
	}

	public static HeadlessBigDataViewer open(
			final AbstractSpimData< ? > spimData,
			final int width,
			final int height,
			final double[] screenscales )
	{
		if ( WrapBasicImgLoader.wrapImgLoaderIfNecessary( spimData ) )
		{
			System.err.println( "WARNING:\nOpening <SpimData> dataset that is not suited for interactive browsing.\nConsider resaving as HDF5 for better performance." );
		}

		final ArrayList< ConverterSetup > converterSetups = new ArrayList< ConverterSetup >();
		final ArrayList< SourceAndConverter< ? > > sources = new ArrayList< SourceAndConverter< ? > >();
		BigDataViewer.initSetups( spimData, converterSetups, sources );

		final AbstractSequenceDescription< ?, ?, ? > seq = spimData.getSequenceDescription();
		final int numTimepoints = seq.getTimePoints().size();
		final Cache cache = ( ( ViewerImgLoader< ?, ? > ) seq.getImgLoader() ).getCache();

		final HeadlessBigDataViewer bdv = new HeadlessBigDataViewer( converterSetups, sources, spimData, numTimepoints, cache, width, height, screenscales );

		WrapBasicImgLoader.removeWrapperIfPresent( spimData );
		final AffineTransform3D initTransform = InitializeViewerState.initTransform( width, height, false, bdv.viewer.getState() );
		bdv.viewer.setCurrentViewerTransform( initTransform );
		return bdv;
	}

	public static HeadlessBigDataViewer open(
			final String xmlFilename,
			final int width,
			final int height,
			final double[] screenscales )
		throws SpimDataException
	{
		final SpimDataMinimal spimData = new XmlIoSpimDataMinimal().load( xmlFilename );
		final HeadlessBigDataViewer bdv = open( spimData, width, height, screenscales );
		if ( !bdv.tryLoadSettings( xmlFilename ) )
			InitializeViewerState.initBrightness( 0.001, 0.999, bdv.viewer.getState(), bdv.setupAssignments );
		final VisibilityAndGrouping vg = bdv.getViewer().getVisibilityAndGrouping();
		vg.setDisplayMode( DisplayMode.FUSED );
		final int numSources = bdv.getViewer().getState().numSources();
		for ( int i = 0; i < numSources; ++i )
			vg.setSourceActive( i, i == 0 );
		return bdv;
	}

	public HeadlessViewerPanel getViewer()
	{
		return viewer;
	}

	public void setSourceParams( final int sourceIndex, final int min, final int max, final int argb )
	{
		final ConverterSetup converterSetup = setupAssignments.getConverterSetups().get( sourceIndex );
		converterSetup.setDisplayRange( min, max );
		converterSetup.setColor( new ARGBType( argb ) );
		viewer.requestRepaint();
	}

	public int getSourceParamsMin( final int sourceIndex )
	{
		final ConverterSetup converterSetup = setupAssignments.getConverterSetups().get( sourceIndex );
		return ( int ) converterSetup.getDisplayRangeMin();
	}

	public int getSourceParamsMax( final int sourceIndex )
	{
		final ConverterSetup converterSetup = setupAssignments.getConverterSetups().get( sourceIndex );
		return ( int ) converterSetup.getDisplayRangeMax();
	}

	public int getSourceParamsColor( final int sourceIndex )
	{
		final ConverterSetup converterSetup = setupAssignments.getConverterSetups().get( sourceIndex );
		return converterSetup.getColor().get();
	}

	protected void loadSettings( final String xmlFilename ) throws IOException, JDOMException
	{
		final SAXBuilder sax = new SAXBuilder();
		final Document doc = sax.build( xmlFilename );
		final Element root = doc.getRootElement();
		viewer.stateFromXml( root );
		setupAssignments.restoreFromXml( root );
		viewer.requestRepaint();
	}

	protected boolean tryLoadSettings( final String xmlFilename )
	{
		proposedSettingsFile = null;
		if( xmlFilename.startsWith( "http://" ) )
		{
			// load settings.xml from the BigDataServer
			final String settings = xmlFilename + "settings";
			{
				try
				{
					loadSettings( settings );
					return true;
				}
				catch ( final FileNotFoundException e )
				{}
				catch ( final Exception e )
				{
					e.printStackTrace();
				}
			}
		}
		else if ( xmlFilename.endsWith( ".xml" ) )
		{
			final String settings = xmlFilename.substring( 0, xmlFilename.length() - ".xml".length() ) + ".settings" + ".xml";
			proposedSettingsFile = new File( settings );
			if ( proposedSettingsFile.isFile() )
			{
				try
				{
					loadSettings( settings );
					return true;
				}
				catch ( final Exception e )
				{
					e.printStackTrace();
				}
			}
		}
		return false;
	}
}
