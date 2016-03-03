package osmb.mapsources;

public class CustomLocalMapSource extends ACLocalMapSource implements IfLocalTileProvider
{
	public static enum CustomMapSourceType
	{
		DIR_ZOOM_X_Y, DIR_ZOOM_Y_X, QUADKEY;
	}

}
