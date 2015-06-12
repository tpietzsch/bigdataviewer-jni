package bdv;

import static bdv.viewer.VisibilityAndGrouping.Event.VISIBILITY_CHANGED;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.InteractiveDisplayCanvasComponent;
import net.imglib2.ui.PainterThread;
import net.imglib2.ui.TransformListener;

import org.jdom2.Element;

import bdv.img.cache.Cache;
import bdv.viewer.DisplayMode;
import bdv.viewer.Interpolation;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.VisibilityAndGrouping;
import bdv.viewer.state.SourceGroup;
import bdv.viewer.state.ViewerState;
import bdv.viewer.state.XmlIoViewerState;


/**
 * A JPanel for viewing multiple of {@link Source}s. The panel contains a
 * {@link InteractiveDisplayCanvasComponent canvas} and a time slider (if there
 * are multiple time-points). Maintains a {@link ViewerState render state}, the
 * renderer, and basic navigation help overlays. It has it's own
 * {@link PainterThread} for painting, which is started on construction (use
 * {@link #stop() to stop the PainterThread}.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public final class HeadlessViewerPanel implements TransformListener< AffineTransform3D >, PainterThread.Paintable, VisibilityAndGrouping.UpdateListener
{
	/**
	 * Currently rendered state (visible sources, transformation, timepoint,
	 * etc.) A copy can be obtained by {@link #getState()}.
	 */
	private final ViewerState state;

	/**
	 * Renders the current state for the {@link #display}.
	 */
	private final HeadlessMultiResolutionRenderer imageRenderer;

	/**
	 * TODO
	 */
	private final HeadlessRenderTarget renderTarget;

	/**
	 * Transformation set by the interactive viewer.
	 */
	private final AffineTransform3D viewerTransform;

	/**
	 * Thread that triggers repainting of the display.
	 */
	private final PainterThread painterThread;

	/**
	 * The {@link ExecutorService} used for rendereing.
	 */
	private final ExecutorService renderingExecutorService;

	/**
	 * Manages visibility and currentness of sources and groups, as well as
	 * grouping of sources, and display mode.
	 */
	private final VisibilityAndGrouping visibilityAndGrouping;

	/**
	 * Optional parameters for {@link HeadlessViewerPanel}.
	 */
	public static class Options
	{
		private int width = 800;

		private int height = 600;

		private double[] screenScales = new double[] { 1, 0.75, 0.5, 0.25, 0.125 };

		private long targetRenderNanos = 30 * 1000000l;

		private boolean doubleBuffered = true;

		private int numRenderingThreads = 3;

		private boolean useVolatileIfAvailable = true;

		public Options width( final int w )
		{
			width = w;
			return this;
		}

		public Options height( final int h )
		{
			height = h;
			return this;
		}

		public Options screenScales( final double[] s )
		{
			screenScales = s;
			return this;
		}

		public Options targetRenderNanos( final long t )
		{
			targetRenderNanos = t;
			return this;
		}

		public Options doubleBuffered( final boolean d )
		{
			doubleBuffered = d;
			return this;
		}

		public Options numRenderingThreads( final int n )
		{
			numRenderingThreads = n;
			return this;
		}

		public Options useVolatileIfAvailable( final boolean v )
		{
			useVolatileIfAvailable = v;
			return this;
		}
	}

	/**
	 * Create default {@link Options}.
	 * @return default {@link Options}.
	 */
	public static Options options()
	{
		return new Options();
	}

	public HeadlessViewerPanel( final List< SourceAndConverter< ? > > sources, final int numTimePoints, final Cache cache )
	{
		this( sources, numTimePoints, cache, options() );
	}

	/**
	 * @param sources
	 *            the {@link SourceAndConverter sources} to display.
	 * @param numTimePoints
	 *            number of available timepoints.
	 * @param cache
	 *            to control IO budgeting and fetcher queue.
	 * @param optional
	 *            optional parameters. See {@link #options()}.
	 */
	public HeadlessViewerPanel( final List< SourceAndConverter< ? > > sources, final int numTimePoints, final Cache cache, final Options optional )
	{
		final int numGroups = 10;
		final ArrayList< SourceGroup > groups = new ArrayList< SourceGroup >( numGroups );
		for ( int i = 0; i < numGroups; ++i )
			groups.add( new SourceGroup( "group " + Integer.toString( i + 1 ), null ) );
		state = new ViewerState( sources, groups, numTimePoints );
		for ( int i = Math.min( numGroups, sources.size() ) - 1; i >= 0; --i )
			state.getSourceGroups().get( i ).addSource( i );

		if ( !sources.isEmpty() )
			state.setCurrentSource( 0 );

		painterThread = new PainterThread( this );
		viewerTransform = new AffineTransform3D();
		renderTarget = new HeadlessRenderTarget( optional.width, optional.height );

		renderingExecutorService = Executors.newFixedThreadPool( optional.numRenderingThreads );
		imageRenderer = new HeadlessMultiResolutionRenderer(
				renderTarget, painterThread,
				optional.screenScales, optional.targetRenderNanos, optional.doubleBuffered,
				optional.numRenderingThreads, renderingExecutorService, optional.useVolatileIfAvailable, cache );

		visibilityAndGrouping = new VisibilityAndGrouping( state );
		visibilityAndGrouping.addUpdateListener( this );

		painterThread.start();
	}

	@Override
	public void paint()
	{
		imageRenderer.paint( state );
	}

	/**
	 * Repaint as soon as possible.
	 */
	public void requestRepaint()
	{
		imageRenderer.requestRepaint();
	}

	@Override
	public synchronized void transformChanged( final AffineTransform3D transform )
	{
		viewerTransform.set( transform );
		state.setViewerTransform( transform );
		requestRepaint();
	}

	@Override
	public void visibilityChanged( final VisibilityAndGrouping.Event e )
	{
		switch ( e.id )
		{
		case VISIBILITY_CHANGED:
			requestRepaint();
			break;
		}
	}

	/**
	 * Switch to next interpolation mode. (Currently, there are two
	 * interpolation modes: nearest-neighbor and N-linear.
	 */
	public synchronized void toggleInterpolation()
	{
		if ( state.getInterpolation() == Interpolation.NEARESTNEIGHBOR )
			state.setInterpolation( Interpolation.NLINEAR );
		else
			state.setInterpolation( Interpolation.NEARESTNEIGHBOR );
		requestRepaint();
	}

	public synchronized void setLinearInterpolation( final boolean enableLinearInterpolation )
	{
		state.setInterpolation( enableLinearInterpolation ? Interpolation.NLINEAR : Interpolation.NEARESTNEIGHBOR );
		requestRepaint();
	}


	/**
	 * Set the {@link DisplayMode}.
	 */
	public synchronized void setDisplayMode( final DisplayMode displayMode )
	{
		visibilityAndGrouping.setDisplayMode( displayMode );
	}

	/**
	 * Set the viewer transform.
	 */
	public synchronized void setCurrentViewerTransform( final AffineTransform3D viewerTransform )
	{
		transformChanged( viewerTransform );
	}

	/**
	 * Show the specified time-point.
	 *
	 * @param timepoint
	 *            time-point index.
	 */
	public synchronized void setTimepoint( final int timepoint )
	{
		if ( state.getCurrentTimepoint() != timepoint )
		{
			state.setCurrentTimepoint( timepoint );
			requestRepaint();
		}
	}

	/**
	 * Show the next time-point.
	 */
	public synchronized void nextTimePoint()
	{
		// TODO
	}

	/**
	 * Show the previous time-point.
	 */
	public synchronized void previousTimePoint()
	{
		// TODO
	}

	/**
	 * Get a copy of the current {@link ViewerState}.
	 *
	 * @return a copy of the current {@link ViewerState}.
	 */
	public synchronized ViewerState getState()
	{
		return state.copy();
	}

	/**
	 * Get the display.
	 *
	 * @return the display.
	 */
	public HeadlessRenderTarget getDisplay()
	{
		return renderTarget;
	}

	/**
	 * Add a {@link TransformListener} to notify about viewer transformation
	 * changes. Listeners will be notified when a new image has been painted
	 * with the viewer transform used to render that image.
	 *
	 * This happens immediately after that image is painted onto the screen,
	 * before any overlays are painted.
	 *
	 * @param listener
	 *            the transform listener to add.
	 */
	public synchronized void addRenderTransformListener( final TransformListener< AffineTransform3D > listener )
	{
		renderTarget.addTransformListener( listener );
	}

	/**
	/**
	 * Add a {@link TransformListener} to notify about viewer transformation
	 * changes. Listeners will be notified when a new image has been painted
	 * with the viewer transform used to render that image.
	 *
	 * This happens immediately after that image is painted onto the screen,
	 * before any overlays are painted.
	 *
	 * @param listener
	 *            the transform listener to add.
	 * @param index
	 *            position in the list of listeners at which to insert this one.
	 */
	public void addRenderTransformListener( final TransformListener< AffineTransform3D > listener, final int index )
	{
		renderTarget.addTransformListener( listener, index );
	}

	public synchronized Element stateToXml()
	{
		return new XmlIoViewerState().toXml( state );
	}

	public synchronized void stateFromXml( final Element parent )
	{
		final XmlIoViewerState io = new XmlIoViewerState();
		io.restoreFromXml( parent.getChild( io.getTagName() ), state );
	}

	/**
	 * Returns the {@link VisibilityAndGrouping} that can be used to modify
	 * visibility and currentness of sources and groups, as well as grouping of
	 * sources, and display mode.
	 */
	public VisibilityAndGrouping getVisibilityAndGrouping()
	{
		return visibilityAndGrouping;
	}

	/**
	 * Stop the {@link #painterThread} and unsubscribe as a cache consumer.
	 */
	public void stop()
	{
		painterThread.interrupt();
		renderingExecutorService.shutdown();
	}

	public ARGBRenderImage getLatestImage()
	{
		return renderTarget.getLatestImage();
	}

}
