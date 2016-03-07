#include <unistd.h>
#include <png.h>
#include <iostream>
#include <sstream>
#include <stdlib.h>

#include "bdvlib.h"

using namespace std;

void write_png_file(char* file_name, int width, int height, int* data);

int main(int argc, char ** argv)
{
	/*
	 * first start the JVM. parameters are:
	 * - the maximum memory option of the JVM.
	 * - the path to the directory containing the jar files.
	 */
	bdvStartJvm("-Xmx4G", "./jars/");

	{
		/*
		 * Open a BigDataViewer dataset.
		 * parameters are:
		 * - path to dataset xml file (or url).
		 * - width of full-scale rendered images.
		 * - height of full-scale rendered images.
		 * - array of scaling factors: When updating transformation, etc,
		 *      the renderer will first render low-resolution images (fast),
		 *      then work it's way up to the full-scale image.
		 *      In the example this means that the rendered sized will be
		 *      (in reverse order) 800x600, 640x450, 400x300, 200x150, 100x75.
		 * - number of elemenst of scaling factors array.
		 */
		double screenscales[] = { 1, 0.75, 0.5, 0.25, 0.125 };
		BigDataViewer bdv("http://tomancak-srv1.mpi-cbg.de:8081/Drosophila", 800, 600, screenscales, sizeof(screenscales)/sizeof(double), 3);

		/*
		 * Print some information about the dataset.
		 */
		cout << "numTimepoints = " << bdv.getNumTimepoints() << endl;
		int numSources = bdv.getNumSources();
		cout << "numSource = " << numSources << endl;
		for ( int i = 0; i < numSources; ++i )
		{
			cout << "source " << i << " is " << (bdv.isSourceVisible(i) ? "visible" : "not visible") << endl;
			cout << " min = " << bdv.getSourceParamsMin(i) << endl;
			cout << " max = " << bdv.getSourceParamsMax(i) << endl;
			cout << " color = " << bdv.getSourceParamsColor(i) << endl;
		}
		double* t = bdv.getTransform();
		cout << t[0] << "  " << t[1] << "  " << t[2] << "  " << t[3] << endl;
		cout << t[4] << "  " << t[5] << "  " << t[6] << "  " << t[7] << endl;
		cout << t[8] << "  " << t[9] << "  " << t[10] << "  " << t[11] << endl;
		delete t;

		/*
		 * Set a new transform.
		 * transform is represented as a double array with the 12 elements
		 * of the upper 3x4 part of a 4x4 3D affine matrix. (without the final [0 0 0 1] row).
		 */
		double t2[] = {
				0.48179778451100014, -0.015139443968170565, 0.38703521637412, -49.847175227038235,
				0.01752952012083952, 0.6179334597195949, 0.0023498766862651197, -18.37203762649213,
				-0.38693433215697026, 0.009143480324372823, 0.4820298600739718, 326.2586860796611 };
		bdv.setTransform( t2 );

		/*
		 * Set some other parameters...
		 */
		bdv.setLinearInterpolation(true);
		bdv.setSourceVisible(0, true);
		bdv.setSourceVisible(1, true);
		bdv.setSourceVisible(2, true);
		bdv.setSourceVisible(3, false);
		bdv.setSourceVisible(4, false);
		bdv.setSourceVisible(5, false);
		bdv.setTimepoint(5);
		bdv.setSourceParams(0, 200, 1080, 0xff00ff00);
		bdv.setSourceParams(1, 200, 1080, 0xffff00ff);
		bdv.setSourceParams(2, 200, 1080, 0xff00ffff);

		/*
		 * Poll rendered images and save them as png.
		 * (Png writing is slightly wrong: red and blue channels are flipped. But who cares...)
		 */
		int imgNumber = 1;
		for ( int i = 0; i < 500; ++i )
		{
			/*
			 * This will return the latest rendered image
			 * or NULL if there were no changes since the last call.
			 */
			BigDataViewerBitmap* bitmap = bdv.getRenderedBitmap();
			if ( bitmap != NULL )
			{
				/*
				 * If we got a bitmap, we are now in a critical section:
				 * The getRenderedBitmap() method obtains the data from the java array by GetPrimitiveArrayCritical().
				 * Do not call any BigDataViewer methods (or any other JNI functions) until bitmap is deleted.
				 * The BigDataViewerBitmap destructor triggers ReleasePrimitiveArrayCritical() to release the java array.
				 * Also, "delete bitmap" must happen on the same thread that called getRenderedBitmap().
				 */
				ostringstream os;
				os << "test" << (imgNumber++) << ".png";
				write_png_file( const_cast<char*>(os.str().c_str()), bitmap->width, bitmap->height, bitmap->data);
				delete bitmap;
			}
			// wait, try again...
			usleep(10000);
		}

		/*
		 * bdv goes out of scope here.
		 * The BigDataViewerBitmap destructor cleans up threads on the java side.
		 */
	}

	bdvStopJvm();

	return 0;
}

void write_png_file(char* file_name, int width, int height, int* data)
{
	FILE *fp = fopen( file_name, "wb" );
	png_structp png_ptr = png_create_write_struct( PNG_LIBPNG_VER_STRING, NULL, NULL, NULL );
	png_infop info_ptr = png_create_info_struct( png_ptr );
	setjmp( png_jmpbuf( png_ptr ) );
	png_init_io( png_ptr, fp );
	setjmp( png_jmpbuf( png_ptr ) );
	png_byte color_type = PNG_COLOR_TYPE_RGBA;
	png_byte bit_depth = 8;
	png_set_IHDR( png_ptr, info_ptr, width, height, bit_depth, color_type, PNG_INTERLACE_NONE, PNG_COMPRESSION_TYPE_BASE, PNG_FILTER_TYPE_BASE );
	png_write_info( png_ptr, info_ptr );
	setjmp( png_jmpbuf( png_ptr ) );
    png_bytep *row_pointers = (png_bytep *)malloc( sizeof(png_bytep) * height);
    for( int i = 0; i < height; i++)
    {
        row_pointers[i] = ( (png_bytep) data ) + ( width * 4 * i );
    }
	png_write_image( png_ptr, row_pointers );
	setjmp( png_jmpbuf( png_ptr ) );
	png_write_end( png_ptr, NULL );
	free (row_pointers);
	fclose( fp );
}
