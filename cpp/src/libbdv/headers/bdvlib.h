
unsigned long bdvStartJvm(const char* memOption = 0, const char* jarPath = 0);

unsigned long bdvStopJvm();


class BigDataViewerBitmap
{
public:
	int width;
	int height;
	int* data;
	bool isComplete;

	~BigDataViewerBitmap();

private:
	class ArrayHandle;
	ArrayHandle* handle;
	BigDataViewerBitmap(int width, int height, int* data, bool isComplete, ArrayHandle* handle )
		: width(width), height(height), data(data), isComplete(isComplete), handle(handle) {}
	friend class BigDataViewer;
};


class BigDataViewer
{
public:
	BigDataViewer(const char* sURL, int width, int height, double* screenscales, int screenscales_size);

	BigDataViewer(const BigDataViewer* shareCacheWith, int width, int height, double* screenscales, int screenscales_size);

	~BigDataViewer();

	void setTransform(double* m3x4);

	double* getTransform();

	int getNumTimepoints();

	int getNumSources();

	bool isSourceVisible(int source);

	int getSourceParamsMin(int source);

	int getSourceParamsMax(int source);

	int getSourceParamsColor(int source);

	void setTimepoint(int timepoint);

	void setLinearInterpolation(bool enableLinearInterpolation);

	void setSourceVisible(int source, bool visible);

	void setSourceParams(int source, int min, int max, int argb);

	BigDataViewerBitmap* getRenderedBitmap();

private:
	int __id;
};
