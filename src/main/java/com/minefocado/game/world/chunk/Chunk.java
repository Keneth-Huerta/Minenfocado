package main.java.com.minefocado.game.world.chunk;

import main.java.com.minefocado.game.world.World;
import main.java.com.minefocado.game.world.blocks.Block;
import main.java.com.minefocado.game.world.blocks.BlockRegistry;

/**
 * Represents a chunk of blocks in the voxel world.
 * A chunk is a fixed-size 3D grid of blocks.
 */
public class Chunk {
    // Chunk dimensions
    public static final int WIDTH = 16;
    public static final int HEIGHT = 256;
    public static final int DEPTH = 16;
    public static final int VOLUME = WIDTH * HEIGHT * DEPTH;
    
    // Position in chunk coordinates (not block coordinates)
    private final int chunkX;
    private final int chunkZ;
    
    // Block data
    private final byte[] blockIds;
    
    // Reference to the parent world and block registry
    private final World world;
    private final BlockRegistry blockRegistry;
    
    // Chunk state tracking
    private boolean modified;
    private boolean generated;
    private boolean populated;
    private boolean meshDirty;
    
    // Chunk mesh for rendering
    private ChunkMesh mesh;
    
    // Datos del mesh sin operaciones OpenGL (para construcción segura en hilos secundarios)
    private ChunkMeshData meshData;
    
    /**
     * Creates a new chunk at the specified coordinates
     * 
     * @param world The parent world
     * @param chunkX X coordinate in chunk space
     * @param chunkZ Z coordinate in chunk space
     */
    public Chunk(World world, int chunkX, int chunkZ) {
        this.world = world;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.blockRegistry = BlockRegistry.getInstance();
        this.blockIds = new byte[VOLUME];
        this.generated = false;
        this.populated = false;
        this.modified = false;
        this.meshDirty = true;
    }
    
    /**
     * Gets the X coordinate of this chunk in chunk space
     */
    public int getChunkX() {
        return chunkX;
    }
    
    /**
     * Gets the Z coordinate of this chunk in chunk space
     */
    public int getChunkZ() {
        return chunkZ;
    }
    
    /**
     * Gets the block ID at the specified local coordinates
     * 
     * @param x Local X coordinate (0-15)
     * @param y Y coordinate (0-255)
     * @param z Local Z coordinate (0-15)
     * @return Block ID
     */
    public byte getBlockId(int x, int y, int z) {
        if (!isValidCoordinate(x, y, z)) {
            return BlockRegistry.AIR_ID;
        }
        
        int index = getIndex(x, y, z);
        return blockIds[index];
    }
    
    /**
     * Sets the block ID at the specified local coordinates
     * 
     * @param x Local X coordinate (0-15)
     * @param y Y coordinate (0-255)
     * @param z Local Z coordinate (0-15)
     * @param blockId The block ID to set
     */
    public void setBlockId(int x, int y, int z, byte blockId) {
        if (!isValidCoordinate(x, y, z)) {
            return;
        }
        
        int index = getIndex(x, y, z);
        byte oldBlockId = blockIds[index];
        
        if (oldBlockId != blockId) {
            blockIds[index] = blockId;
            modified = true;
            meshDirty = true;
            
            // Update neighboring chunks if the block is on the edge
            if (x == 0 || x == WIDTH - 1 || z == 0 || z == DEPTH - 1) {
                updateNeighborChunks(x, y, z);
            }
        }
    }
    
    /**
     * Sets the block ID at the specified local coordinates (int version)
     */
    public void setBlockId(int x, int y, int z, int blockId) {
        setBlockId(x, y, z, (byte) blockId);
    }
    
    /**
     * Gets the Block object at the specified local coordinates
     * 
     * @param x Local X coordinate (0-15)
     * @param y Y coordinate (0-255)
     * @param z Local Z coordinate (0-15)
     * @return Block object
     */
    public Block getBlock(int x, int y, int z) {
        byte id = getBlockId(x, y, z);
        return blockRegistry.getBlock(id);
    }
    
    /**
     * Sets all blocks in the chunk to the specified block ID
     * 
     * @param blockId The block ID to fill with
     */
    public void fill(byte blockId) {
        for (int i = 0; i < VOLUME; i++) {
            blockIds[i] = blockId;
        }
        modified = true;
        meshDirty = true;
    }
    
    /**
     * Converts local chunk coordinates to an index in the block array
     */
    private int getIndex(int x, int y, int z) {
        return y * WIDTH * DEPTH + z * WIDTH + x;
    }
    
    /**
     * Checks if the coordinates are valid for this chunk
     */
    private boolean isValidCoordinate(int x, int y, int z) {
        return x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT && z >= 0 && z < DEPTH;
    }
    
    /**
     * Updates neighboring chunks when a block on the edge changes
     */
    private void updateNeighborChunks(int x, int y, int z) {
        // Notify neighboring chunks that their mesh needs updating
        if (x == 0) {
            Chunk neighbor = world.getChunk(chunkX - 1, chunkZ);
            if (neighbor != null) {
                neighbor.setMeshDirty(true);
            }
        } else if (x == WIDTH - 1) {
            Chunk neighbor = world.getChunk(chunkX + 1, chunkZ);
            if (neighbor != null) {
                neighbor.setMeshDirty(true);
            }
        }
        
        if (z == 0) {
            Chunk neighbor = world.getChunk(chunkX, chunkZ - 1);
            if (neighbor != null) {
                neighbor.setMeshDirty(true);
            }
        } else if (z == DEPTH - 1) {
            Chunk neighbor = world.getChunk(chunkX, chunkZ + 1);
            if (neighbor != null) {
                neighbor.setMeshDirty(true);
            }
        }
    }
    
    /**
     * Gets a block in world space, handling chunk borders
     * 
     * @param worldX X coordinate in world space
     * @param worldY Y coordinate in world space
     * @param worldZ Z coordinate in world space
     * @return Block at the specified position, or air if out of bounds
     */
    public Block getBlockInWorld(int worldX, int worldY, int worldZ) {
        // Convert world space to chunk space
        int localX = worldX - (chunkX * WIDTH);
        int localZ = worldZ - (chunkZ * DEPTH);
        
        // If local coordinates are outside this chunk, get from the appropriate chunk
        if (localX < 0 || localX >= WIDTH || localZ < 0 || localZ >= DEPTH || worldY < 0 || worldY >= HEIGHT) {
            return world.getBlockAt(worldX, worldY, worldZ);
        }
        
        return getBlock(localX, worldY, localZ);
    }
    
    /**
     * Gets the mesh for this chunk, building it if necessary
     */
    public ChunkMesh getMesh() {
        return mesh;
    }
    
    /**
     * Sets the mesh for this chunk
     * @param mesh El mesh a establecer
     */
    public void setMesh(ChunkMesh mesh) {
        // Limpia el mesh anterior si existe
        if (this.mesh != null) {
            this.mesh.cleanup();
        }
        this.mesh = mesh;
    }
    
    /**
     * Gets the mesh data for this chunk
     */
    public ChunkMeshData getMeshData() {
        return meshData;
    }
    
    /**
     * Sets the mesh data for this chunk
     * @param meshData Los datos de mesh a establecer
     */
    public void setMeshData(ChunkMeshData meshData) {
        this.meshData = meshData;
    }
    
    /**
     * Verifica si hay datos de mesh disponibles para crear un mesh OpenGL
     */
    public boolean hasMeshData() {
        return meshData != null;
    }
    
    /**
     * Checks if this chunk has been modified since last save
     */
    public boolean isModified() {
        return modified;
    }
    
    /**
     * Sets the modified state of this chunk
     */
    public void setModified(boolean modified) {
        this.modified = modified;
    }
    
    /**
     * Checks if this chunk has had terrain generated
     */
    public boolean isGenerated() {
        return generated;
    }
    
    /**
     * Sets whether this chunk has had terrain generated
     */
    public void setGenerated(boolean generated) {
        this.generated = generated;
    }
    
    /**
     * Checks if this chunk has had features populated
     */
    public boolean isPopulated() {
        return populated;
    }
    
    /**
     * Sets whether this chunk has had features populated
     */
    public void setPopulated(boolean populated) {
        this.populated = populated;
    }
    
    /**
     * Gets the dirty state of the chunk mesh
     */
    public boolean isMeshDirty() {
        return meshDirty;
    }
    
    /**
     * Sets the dirty state of the chunk mesh
     */
    public void setMeshDirty(boolean meshDirty) {
        this.meshDirty = meshDirty;
    }
    
    /**
     * Gets the World object that contains this chunk
     */
    public World getWorld() {
        return world;
    }
    
    /**
     * Disposes of the chunk's resources
     */
    public void dispose() {
        if (mesh != null) {
            mesh.cleanup();
            mesh = null;
        }
        // Liberamos también los datos del mesh
        meshData = null;
    }
}