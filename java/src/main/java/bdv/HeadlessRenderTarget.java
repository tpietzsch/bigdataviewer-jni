/*
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2015 Tobias Pietzsch, Stephan Preibisch, Barry DeZonia,
 * Stephan Saalfeld, Curtis Rueden, Albert Cardona, Christian Dietz, Jean-Yves
 * Tinevez, Johannes Schindelin, Jonathan Hale, Lee Kamentsky, Larry Lindsey, Mark
 * Hiner, Michael Zinsmaier, Martin Horn, Grant Harris, Aivar Grislis, John
 * Bogovic, Steffen Jaensch, Stefan Helfrich, Jan Funke, Nick Perry, Mark Longair,
 * Melissa Linkert and Dimiter Prodanov.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package bdv;

import java.util.concurrent.CopyOnWriteArrayList;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.OverlayRenderer;
import net.imglib2.ui.RenderTarget;
import net.imglib2.ui.TransformListener;

/**
 * {@link OverlayRenderer} drawing a {@link ARGBRenderImage}, scaled to fill the
 * canvas. It can be used as a {@link RenderTarget}, such that the
 * {@link ARGBRenderImage} to draw is set by a {@link Renderer}.
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public final class HeadlessRenderTarget
{
	/**
	 * The {@link ARGBRenderImage} that is actually drawn on the canvas. Depending
	 * on {@link #discardAlpha} this is either the {@link ARGBRenderImage}
	 * obtained from {@link #screenImage}, or {@link #screenImage}s buffer
	 * re-wrapped using a RGB color model.
	 */
	private ARGBRenderImage paintedImage;

	/**
	 * A {@link ARGBRenderImage} that has been previously
	 * {@link #setARGBRenderImage(ARGBRenderImage) set} for painting. Whenever a new
	 * image is set, this is stored here and marked {@link #pending}. Whenever
	 * an image is painted and a new image is pending, the new image is painted
	 * to the screen. Before doing this, the image previously used for painting
	 * is swapped into {@link #pendingImage}. This is used for double-buffering.
	 */
	private ARGBRenderImage pendingImage;

	/**
	 * Viewer transform that was used to render {@link #paintedImage}.
	 */
	private final AffineTransform3D paintedTransform;

	/**
	 * Viewer transform that was used to render {@link #pendingImage}.
	 */
	private final AffineTransform3D pendingTransform;

	/**
	 * These listeners will be notified about the transform that is associated
	 * to the currently rendered image. This is intended for example for
	 * {@link OverlayRenderer}s that need to exactly match the transform of
	 * their overlaid content to the transform of the image.
	 */
	private final CopyOnWriteArrayList< TransformListener< AffineTransform3D > > paintedTransformListeners;

	/**
	 * Whether an image is pending.
	 */
	private boolean pending;

	/**
	 * Whether the current image has been updated since it was last read.
	 */
	private boolean currentImageUpdated;

	/**
	 * The current canvas width.
	 */
	private final int width;

	/**
	 * The current canvas height.
	 */
	private final int height;

	public HeadlessRenderTarget( final int width, final int height )
	{
		paintedImage = null;
		pendingImage = null;
		pending = false;
		currentImageUpdated = false;
		this.width = width;
		this.height = height;
		pendingTransform = new AffineTransform3D();
		paintedTransform = new AffineTransform3D();
		paintedTransformListeners = new CopyOnWriteArrayList< TransformListener< AffineTransform3D > >();
	}

	/**
	 * Set the {@link ARGBRenderImage} that is to be drawn on the canvas, and the
	 * transform with which this image was created.
	 *
	 * @param img
	 *            image to draw (may be null).
	 * @return a previously set {@link ARGBRenderImage} that is currently not
	 *         being painted or null. Used for double-buffering.
	 */
	synchronized ARGBRenderImage setRenderedImageAndTransform( final ARGBRenderImage img, final AffineTransform3D transform )
	{
		pendingTransform.set( transform );
		final ARGBRenderImage tmp = pendingImage;
		pendingImage = img;
		pending = true;
		return tmp;
	}

	/**
	 * Get the current canvas width.
	 *
	 * @return canvas width.
	 */
	public int getWidth()
	{
		return width;
	}

	/**
	 * Get the current canvas height.
	 *
	 * @return canvas height.
	 */
	public int getHeight()
	{
		return height;
	}

	/**
	 * Add a {@link TransformListener} to notify about viewer transformation
	 * changes. Listeners will be notified when a new image has been rendered
	 * (immediately before that image is displayed) with the viewer transform
	 * used to render that image.
	 *
	 * @param listener
	 *            the transform listener to add.
	 */
	public void addTransformListener( final TransformListener< AffineTransform3D > listener )
	{
		addTransformListener( listener, Integer.MAX_VALUE );
	}

	/**
	 * Add a {@link TransformListener} to notify about viewer transformation
	 * changes. Listeners will be notified when a new image has been rendered
	 * (immediately before that image is displayed) with the viewer transform
	 * used to render that image.
	 *
	 * @param listener
	 *            the transform listener to add.
	 * @param index
	 *            position in the list of listeners at which to insert this one.
	 */
	public void addTransformListener( final TransformListener< AffineTransform3D > listener, final int index )
	{
		synchronized ( paintedTransformListeners )
		{
			final int s = paintedTransformListeners.size();
			paintedTransformListeners.add( index < 0 ? 0 : index > s ? s : index, listener );
			listener.transformChanged( paintedTransform );
		}
	}

	/**
	 * Remove a {@link TransformListener}.
	 *
	 * @param listener
	 *            the transform listener to remove.
	 */
	public void removeTransformListener( final TransformListener< AffineTransform3D > listener )
	{
		synchronized ( paintedTransformListeners )
		{
			paintedTransformListeners.remove( listener );
		}
	}

	/**
	 * @return the latest {@link ARGBRenderImage} that was
	 *         {@link #setRenderedImageAndTransform(ARGBRenderImage, AffineTransform3D)
	 *         set} by the renderer, or {@code null} if there was no change
	 *         since the last call.
	 */
	ARGBRenderImage getLatestImage()
	{
		boolean notifyTransformListeners = false;
		boolean imageUpdated = false;
		ARGBRenderImage img = null;
		synchronized ( this )
		{
			if ( pending )
			{
				final ARGBRenderImage tmp = paintedImage;
				paintedImage = pendingImage;
				paintedTransform.set( pendingTransform );
				pendingImage = tmp;
				pending = false;
				notifyTransformListeners = true;
				imageUpdated = true;
			}
			else if ( currentImageUpdated )
				imageUpdated = true;
			currentImageUpdated = false;
			img = paintedImage;
		}
		if ( img != null )
		{
			if ( notifyTransformListeners )
				for ( final TransformListener< AffineTransform3D > listener : paintedTransformListeners )
					listener.transformChanged( paintedTransform );
		}
		return imageUpdated ? img : null;
	}

	synchronized void currentImageUpdated()
	{
		currentImageUpdated = true;
	}
}
