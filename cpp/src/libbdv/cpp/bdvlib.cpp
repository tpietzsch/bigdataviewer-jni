#include "bdvlib.h"
#include <jni.h>
#include <dirent.h>

#include <string>
#include <sstream>
#include <iostream>
#include <boost/algorithm/string/predicate.hpp>

using std::string;
using std::cout;
using std::cerr;
using std::endl;
using std::ostringstream;
using boost::ends_with;

#define JAR_SEPARATOR ":"

static JavaVM* theJvm = NULL;

BigDataViewer::BigDataViewer (const char* sURL, int width, int height, double* screenscales, int screenscales_size)
{
	if (theJvm == NULL)
		cerr << "start JVM first!" << endl;

	JNIEnv* jniEnv;
	theJvm->AttachCurrentThread((void**)&jniEnv, NULL);
	jclass BigDataViewerJniClass = jniEnv->FindClass("bdv/BigDataViewerJni");
	if (BigDataViewerJniClass == NULL)
	{
		cerr << "Unable to locate class: bdv/BigDataViewerJni" << endl;
		return;
	}
	jmethodID constructID = jniEnv->GetStaticMethodID(BigDataViewerJniClass, "construct", "(Ljava/lang/String;II[D)I");
	if (constructID == NULL)
	{
		cerr << "Unable to locate method: construct()" << endl;
		return;
	}

	jstring jUrl = jniEnv->NewStringUTF(sURL);
	jdoubleArray array = jniEnv->NewDoubleArray( screenscales_size );
	jniEnv->SetDoubleArrayRegion( array, 0, screenscales_size, screenscales );
	__id = jniEnv->CallStaticIntMethod(BigDataViewerJniClass, constructID, jUrl, width, height, array);

	theJvm->DetachCurrentThread();
}

BigDataViewer::~BigDataViewer()
{
	JNIEnv* jniEnv;
	theJvm->AttachCurrentThread((void**)&jniEnv, NULL);
	jclass BigDataViewerJniClass = jniEnv->FindClass("bdv/BigDataViewerJni");
	if (BigDataViewerJniClass == NULL)
	{
		cerr << "Unable to locate class: bdv/BigDataViewerJni" << endl;
		return;
	}
	jmethodID destructID = jniEnv->GetStaticMethodID(BigDataViewerJniClass, "destruct", "(I)V");
	if (destructID == NULL)
	{
		cerr << "Unable to locate method: destruct()" << endl;
		return;
	}

	jniEnv->CallStaticVoidMethod(BigDataViewerJniClass, destructID, __id);

	theJvm->DetachCurrentThread();
}


void BigDataViewer::setTransform( double* m3x4 )
{
	JNIEnv* jniEnv;
	theJvm->AttachCurrentThread((void**)&jniEnv, NULL);
	jclass BigDataViewerJniClass = jniEnv->FindClass("bdv/BigDataViewerJni");
	if (BigDataViewerJniClass == NULL)
	{
		cerr << "Unable to locate class: bdv/BigDataViewerJni" << endl;
		return;
	}
	jmethodID setTransformID = jniEnv->GetStaticMethodID(BigDataViewerJniClass, "setTransform", "(I[D)V");
	if (setTransformID == NULL)
	{
		cerr << "Unable to locate method: setTransform()" << endl;
		return;
	}

	jdoubleArray matrix = jniEnv->NewDoubleArray( 12 );
	jniEnv->SetDoubleArrayRegion( matrix, 0, 12, m3x4 );
	jniEnv->CallStaticVoidMethod(BigDataViewerJniClass, setTransformID, __id, matrix );

	theJvm->DetachCurrentThread();
}

double* BigDataViewer::getTransform()
{
	JNIEnv* jniEnv;
	theJvm->AttachCurrentThread((void**)&jniEnv, NULL);
	jclass BigDataViewerJniClass = jniEnv->FindClass("bdv/BigDataViewerJni");
	if (BigDataViewerJniClass == NULL)
	{
		cerr << "Unable to locate class: bdv/BigDataViewerJni" << endl;
		return NULL;
	}
	jmethodID getTransformID = jniEnv->GetStaticMethodID(BigDataViewerJniClass, "getTransform", "(I[D)V");
	if (getTransformID == NULL)
	{
		cerr << "Unable to locate method: getTransform()" << endl;
		return NULL;
	}

	jdoubleArray matrix = jniEnv->NewDoubleArray( 12 );
	jniEnv->CallStaticVoidMethod(BigDataViewerJniClass, getTransformID, __id, matrix );
	double* m3x4 = new double[12];
	jniEnv->GetDoubleArrayRegion( matrix, 0, 12, m3x4 );

	theJvm->DetachCurrentThread();
	return m3x4;
}

int BigDataViewer::getNumTimepoints()
{
	JNIEnv* jniEnv;
	theJvm->AttachCurrentThread((void**)&jniEnv, NULL);
	jclass BigDataViewerJniClass = jniEnv->FindClass("bdv/BigDataViewerJni");
	if (BigDataViewerJniClass == NULL)
	{
		cerr << "Unable to locate class: bdv/BigDataViewerJni" << endl;
		return -1;
	}
	jmethodID getNumTimepointsID = jniEnv->GetStaticMethodID(BigDataViewerJniClass, "getNumTimepoints", "(I)I");
	if (getNumTimepointsID == NULL)
	{
		cerr << "Unable to locate method: getNumTimepoints()" << endl;
		return -1;
	}

	int numTimepoints = jniEnv->CallStaticIntMethod(BigDataViewerJniClass, getNumTimepointsID, __id);

	theJvm->DetachCurrentThread();
	return numTimepoints;
}

int BigDataViewer::getNumSources()
{
	JNIEnv* jniEnv;
	theJvm->AttachCurrentThread((void**)&jniEnv, NULL);
	jclass BigDataViewerJniClass = jniEnv->FindClass("bdv/BigDataViewerJni");
	if (BigDataViewerJniClass == NULL)
	{
		cerr << "Unable to locate class: bdv/BigDataViewerJni" << endl;
		return -1;
	}
	jmethodID getNumSourcesID = jniEnv->GetStaticMethodID(BigDataViewerJniClass, "getNumSources", "(I)I");
	if (getNumSourcesID == NULL)
	{
		cerr << "Unable to locate method: getNumSources()" << endl;
		return -1;
	}

	int numSources = jniEnv->CallStaticIntMethod(BigDataViewerJniClass, getNumSourcesID, __id);

	theJvm->DetachCurrentThread();
	return numSources;
}

bool BigDataViewer::isSourceVisible( int source )
{
	JNIEnv* jniEnv;
	theJvm->AttachCurrentThread((void**)&jniEnv, NULL);
	jclass BigDataViewerJniClass = jniEnv->FindClass("bdv/BigDataViewerJni");
	if (BigDataViewerJniClass == NULL)
	{
		cerr << "Unable to locate class: bdv/BigDataViewerJni" << endl;
		return false;
	}
	jmethodID isSourceVisibleID = jniEnv->GetStaticMethodID(BigDataViewerJniClass, "isSourceVisible", "(II)Z");
	if (isSourceVisibleID == NULL)
	{
		cerr << "Unable to locate method: isSourceVisible()" << endl;
		return false;
	}


	jboolean visible = jniEnv->CallStaticBooleanMethod(BigDataViewerJniClass, isSourceVisibleID, __id, source);

	theJvm->DetachCurrentThread();
	return visible;
}

int BigDataViewer::getSourceParamsMin( int source )
{
	JNIEnv* jniEnv;
	theJvm->AttachCurrentThread((void**)&jniEnv, NULL);
	jclass BigDataViewerJniClass = jniEnv->FindClass("bdv/BigDataViewerJni");
	if (BigDataViewerJniClass == NULL)
	{
		cerr << "Unable to locate class: bdv/BigDataViewerJni" << endl;
		return -1;
	}
	jmethodID getSourceParamsMinID = jniEnv->GetStaticMethodID(BigDataViewerJniClass, "getSourceParamsMin", "(II)I");
	if (getSourceParamsMinID == NULL)
	{
		cerr << "Unable to locate method: getSourceParamsMin()" << endl;
		return -1;
	}
	int min = jniEnv->CallStaticIntMethod(BigDataViewerJniClass, getSourceParamsMinID, __id, source);

	theJvm->DetachCurrentThread();
	return min;
}

int BigDataViewer::getSourceParamsMax( int source )
{
	JNIEnv* jniEnv;
	theJvm->AttachCurrentThread((void**)&jniEnv, NULL);
	jclass BigDataViewerJniClass = jniEnv->FindClass("bdv/BigDataViewerJni");
	if (BigDataViewerJniClass == NULL)
	{
		cerr << "Unable to locate class: bdv/BigDataViewerJni" << endl;
		return -1;
	}
	jmethodID getSourceParamsMaxID = jniEnv->GetStaticMethodID(BigDataViewerJniClass, "getSourceParamsMax", "(II)I");
	if (getSourceParamsMaxID == NULL)
	{
		cerr << "Unable to locate method: getSourceParamsMax()" << endl;
		return -1;
	}

	int max = jniEnv->CallStaticIntMethod(BigDataViewerJniClass, getSourceParamsMaxID, __id, source);

	theJvm->DetachCurrentThread();
	return max;
}

int BigDataViewer::getSourceParamsColor( int source )
{
	JNIEnv* jniEnv;
	theJvm->AttachCurrentThread((void**)&jniEnv, NULL);
	jclass BigDataViewerJniClass = jniEnv->FindClass("bdv/BigDataViewerJni");
	if (BigDataViewerJniClass == NULL)
	{
		cerr << "Unable to locate class: bdv/BigDataViewerJni" << endl;
		return -1;
	}
	jmethodID getSourceParamsColorID = jniEnv->GetStaticMethodID(BigDataViewerJniClass, "getSourceParamsColor", "(II)I");
	if (getSourceParamsColorID == NULL)
	{
		cerr << "Unable to locate method: getSourceParamsColor()" << endl;
		return -1;
	}

	int argb = jniEnv->CallStaticIntMethod(BigDataViewerJniClass, getSourceParamsColorID, __id, source);

	theJvm->DetachCurrentThread();
	return argb;
}

void BigDataViewer::setTimepoint( int timepoint )
{
	JNIEnv* jniEnv;
	theJvm->AttachCurrentThread((void**)&jniEnv, NULL);
	jclass BigDataViewerJniClass = jniEnv->FindClass("bdv/BigDataViewerJni");
	if (BigDataViewerJniClass == NULL)
	{
		cerr << "Unable to locate class: bdv/BigDataViewerJni" << endl;
		return;
	}
	jmethodID setTimepointID = jniEnv->GetStaticMethodID(BigDataViewerJniClass, "setTimepoint", "(II)V");
	if (setTimepointID == NULL)
	{
		cerr << "Unable to locate method: setTimepoint()" << endl;
		return;
	}

	jniEnv->CallStaticVoidMethod(BigDataViewerJniClass, setTimepointID, __id, timepoint);

	theJvm->DetachCurrentThread();
}

void BigDataViewer::setLinearInterpolation( bool enableLinearInterpolation )
{
	JNIEnv* jniEnv;
	theJvm->AttachCurrentThread((void**)&jniEnv, NULL);
	jclass BigDataViewerJniClass = jniEnv->FindClass("bdv/BigDataViewerJni");
	if (BigDataViewerJniClass == NULL)
	{
		cerr << "Unable to locate class: bdv/BigDataViewerJni" << endl;
		return;
	}
	jmethodID setLinearInterpolationID = jniEnv->GetStaticMethodID(BigDataViewerJniClass, "setLinearInterpolation", "(IZ)V");
	if (setLinearInterpolationID == NULL)
	{
		cerr << "Unable to locate method: setLinearInterpolation()" << endl;
		return;
	}

	jniEnv->CallStaticVoidMethod(BigDataViewerJniClass, setLinearInterpolationID, __id, enableLinearInterpolation);

	theJvm->DetachCurrentThread();
}

void BigDataViewer::setSourceVisible( int source, bool visible )
{
	JNIEnv* jniEnv;
	theJvm->AttachCurrentThread((void**)&jniEnv, NULL);
	jclass BigDataViewerJniClass = jniEnv->FindClass("bdv/BigDataViewerJni");
	if (BigDataViewerJniClass == NULL)
	{
		cerr << "Unable to locate class: bdv/BigDataViewerJni" << endl;
		return;
	}
	jmethodID setSourceVisibleID = jniEnv->GetStaticMethodID(BigDataViewerJniClass, "setSourceVisible", "(IIZ)V");
	if (setSourceVisibleID == NULL)
	{
		cerr << "Unable to locate method: setSourceVisible()" << endl;
		return;
	}

	jniEnv->CallStaticVoidMethod(BigDataViewerJniClass, setSourceVisibleID, __id, source, visible);

	theJvm->DetachCurrentThread();
}

void BigDataViewer::setSourceParams( int source, int min, int max, int argb )
{
	JNIEnv* jniEnv;
	theJvm->AttachCurrentThread((void**)&jniEnv, NULL);
	jclass BigDataViewerJniClass = jniEnv->FindClass("bdv/BigDataViewerJni");
	if (BigDataViewerJniClass == NULL)
	{
		cerr << "Unable to locate class: bdv/BigDataViewerJni" << endl;
		return;
	}
	jmethodID setSourceParamsID = jniEnv->GetStaticMethodID(BigDataViewerJniClass, "setSourceParams", "(IIIII)V");
	if (setSourceParamsID == NULL)
	{
		cerr << "Unable to locate method: setSourceParams()" << endl;
		return;
	}

	jniEnv->CallStaticVoidMethod(BigDataViewerJniClass, setSourceParamsID, __id, source, min, max, argb);

	theJvm->DetachCurrentThread();
}

class BigDataViewerBitmap::ArrayHandle
{
public:
	ArrayHandle(jintArray dataArray, int* data, JNIEnv* jniEnv)
		: dataArray(dataArray), data(data), jniEnv(jniEnv)
	{}
	~ArrayHandle()
	{
		jniEnv->ReleasePrimitiveArrayCritical(dataArray, data, JNI_ABORT);
		theJvm->DetachCurrentThread();
	}
	jintArray dataArray;
	int* data;
	JNIEnv* jniEnv;
};

BigDataViewerBitmap* BigDataViewer::getRenderedBitmap()
{
	JNIEnv* jniEnv;
	theJvm->AttachCurrentThread((void**)&jniEnv, NULL);
	jclass BigDataViewerJniClass = jniEnv->FindClass("bdv/BigDataViewerJni");
	if (BigDataViewerJniClass == NULL)
	{
		cerr << "Unable to locate class: bdv/BigDataViewerJni" << endl;
		return NULL;
	}
	jclass ARGBRenderImageClass = jniEnv->FindClass("bdv/ARGBRenderImage");
	if (ARGBRenderImageClass == NULL)
	{
		cerr << "Unable to locate class: bdv/ARGBRenderImage" << endl;
		return NULL;
	}
	jmethodID getRenderedBitmapID = jniEnv->GetStaticMethodID(BigDataViewerJniClass, "getRenderedBitmap", "(I)Lbdv/ARGBRenderImage;");
	if (getRenderedBitmapID == NULL)
	{
		cerr << "Unable to locate method: getRenderedBitmap()" << endl;
		return NULL;
	}
	jmethodID getWidthID = jniEnv->GetMethodID(ARGBRenderImageClass, "getWidth", "()I");
	if (getWidthID == NULL)
	{
		cerr << "Unable to locate method: getWidth()" << endl;
		return NULL;
	}
	jmethodID getHeightID = jniEnv->GetMethodID(ARGBRenderImageClass, "getHeight", "()I");
	if (getHeightID == NULL)
	{
		cerr << "Unable to locate method: getHeight()" << endl;
		return NULL;
	}
	jmethodID getDataID = jniEnv->GetMethodID(ARGBRenderImageClass, "getData", "()[I");
	if (getDataID == NULL)
	{
		cerr << "Unable to locate method: getData()" << endl;
		return NULL;
	}

	jobject renderImage = jniEnv->CallStaticObjectMethod(BigDataViewerJniClass, getRenderedBitmapID, __id);
	if (renderImage != NULL)
	{
		int width = jniEnv->CallIntMethod(renderImage, getWidthID);
		int height = jniEnv->CallIntMethod(renderImage, getHeightID);
		jintArray dataArray = (jintArray) jniEnv->CallObjectMethod(renderImage, getDataID);
		jboolean isCopy;
		int* data = (int*) jniEnv->GetPrimitiveArrayCritical(dataArray, &isCopy);
//		cout << "isCopy = " << (isCopy ? "true" : "false" ) << endl;
		return new BigDataViewerBitmap(width, height, data, new BigDataViewerBitmap::ArrayHandle(dataArray, data, jniEnv));
	}
	else
	{
		theJvm->DetachCurrentThread();
		return NULL;
	}
}

BigDataViewerBitmap::~BigDataViewerBitmap()
{
	delete handle;
}

string* get_cv_jars (const char* path)
{
	DIR* jar_dir = opendir(path);
	if (jar_dir != NULL)
	{
		struct dirent* entry = NULL;

		ostringstream os;

		while ((entry = readdir(jar_dir)) != NULL)
			if (ends_with(entry->d_name, ".jar"))
				os << path << "/" << entry->d_name << JAR_SEPARATOR;

		closedir(jar_dir);
		return new string(os.str());
	} else {
		cerr << "could not open directory " << path << endl;
		return NULL;
	}
}

unsigned long bdvStartJvm (const char* memOption, const char* jarPath)
{
	if (theJvm != NULL)
	{
		cerr << "JVM already running" << endl;
		return 1;
	}

	const char* path = (jarPath != NULL && strlen(jarPath) > 0) ? jarPath : "./jars";
	const char* mem = (memOption != NULL && strlen(memOption) > 0) ? memOption : "-Xmx4G";

	string* jars = get_cv_jars(path);
	if (jars == NULL)
		return 2;
	string classPath = "-Djava.class.path=" + *jars;

	JavaVMOption* options = new JavaVMOption[2];
	options[0].optionString = (char*)mem;
	options[1].optionString = (char*)classPath.c_str();
	JavaVMInitArgs* initArgs = new JavaVMInitArgs();
	initArgs->version = JNI_VERSION_1_6;
	initArgs->options = options;
	initArgs->nOptions = 2;
	initArgs->ignoreUnrecognized = false;

	JNIEnv* jniEnv;
	jint res = JNI_CreateJavaVM(&theJvm, (void**)&jniEnv, initArgs);
	if (res != JNI_OK)
		return 3;

	return 0;
}

unsigned long bdvStopJvm()
{
    theJvm->DetachCurrentThread();
    theJvm->DestroyJavaVM();
    return 0;
}
