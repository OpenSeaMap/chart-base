package osmb.program.tiles;

public interface IfMemoryTileCacheHolder
{
	/**
	 * Exposes the MemoryTileCache to some external. Usually to some {link TileLoader}
	 */
	MemoryTileCache getTileImageCache();
}
