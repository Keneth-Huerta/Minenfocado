package main.java.com.minefocado.game.world.chunk;

import main.java.com.minefocado.game.world.World;
import main.java.com.minefocado.game.world.blocks.Block;
import main.java.com.minefocado.game.world.blocks.BlockRegistry;

/**
 * Representa un chunk de bloques en el mundo de vóxeles.
 * Un chunk es una cuadrícula 3D de tamaño fijo de bloques.
 */
public class Chunk {
    // Dimensiones del chunk
    public static final int WIDTH = 16;
    public static final int HEIGHT = 256;
    public static final int DEPTH = 16;
    public static final int VOLUME = WIDTH * HEIGHT * DEPTH;
    
    // Posición en coordenadas de chunk (no coordenadas de bloque)
    private final int chunkX;
    private final int chunkZ;
    
    // Datos de bloques
    private final byte[] blockIds;
    
    // Referencia al mundo padre y registro de bloques
    private final World world;
    private final BlockRegistry blockRegistry;
    
    // Seguimiento del estado del chunk
    private boolean modified;
    private boolean generated;
    private boolean populated;
    private boolean meshDirty;
    
    // Malla del chunk para renderización
    private ChunkMesh mesh;
    
    // Datos de malla sin operaciones OpenGL (para construcción segura en hilos secundarios)
    private ChunkMeshData meshData;
    
    /**
     * Crea un nuevo chunk en las coordenadas especificadas
     * 
     * @param world El mundo padre
     * @param chunkX Coordenada X en espacio de chunk
     * @param chunkZ Coordenada Z en espacio de chunk
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
     * Obtiene la coordenada X de este chunk en espacio de chunk
     */
    public int getChunkX() {
        return chunkX;
    }
    
    /**
     * Obtiene la coordenada Z de este chunk en espacio de chunk
     */
    public int getChunkZ() {
        return chunkZ;
    }
    
    /**
     * Obtiene el ID del bloque en las coordenadas especificadas
     * 
     * @param x Coordenada X local (0-15)
     * @param y Coordenada Y (0-255)
     * @param z Coordenada Z local (0-15)
     * @return ID del bloque en esa posición, o 0 (aire) si está fuera de límites
     */
    public byte getBlockId(int x, int y, int z) {
        if (!isValidCoordinate(x, y, z)) {
            return BlockRegistry.AIR_ID;
        }
        
        int index = getIndex(x, y, z);
        return blockIds[index];
    }
    
    /**
     * Establece un bloque en las coordenadas especificadas
     * 
     * @param x Coordenada X local (0-15)
     * @param y Coordenada Y (0-255)
     * @param z Coordenada Z local (0-15)
     * @param blockId ID del bloque a colocar
     * @return Verdadero si el bloque se colocó correctamente
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
            
            // Actualizar chunks vecinos si este bloque está en un borde
            if (x == 0 || x == WIDTH - 1 || z == 0 || z == DEPTH - 1) {
                updateNeighborChunks(x, y, z);
            }
        }
    }
    
    /**
     * Establece el ID del bloque en las coordenadas especificadas (versión int)
     */
    public void setBlockId(int x, int y, int z, int blockId) {
        setBlockId(x, y, z, (byte) blockId);
    }
    
    /**
     * Obtiene un objeto de bloque en las coordenadas especificadas
     * 
     * @param x Coordenada X local (0-15)
     * @param y Coordenada Y (0-255)
     * @param z Coordenada Z local (0-15)
     * @return Objeto de bloque en esa posición, o AIRE si está fuera de límites
     */
    public Block getBlock(int x, int y, int z) {
        byte id = getBlockId(x, y, z);
        
        // Check if blockRegistry is null
        if (blockRegistry == null) {
            System.err.println("Error: BlockRegistry is null in Chunk.getBlock");
            return null;
        }
        
        Block block = blockRegistry.getBlock(id);
        
        // If the block is null, return air block as fallback
        if (block == null) {
            System.err.println("Warning: Got null block for ID " + id + " at " + x + "," + y + "," + z);
            return blockRegistry.getBlock(BlockRegistry.AIR_ID);
        }
        
        return block;
    }
    
    /**
     * Rellena todos los bloques en el chunk con el ID de bloque especificado
     * 
     * @param blockId El ID de bloque con el que rellenar
     */
    public void fill(byte blockId) {
        for (int i = 0; i < VOLUME; i++) {
            blockIds[i] = blockId;
        }
        modified = true;
        meshDirty = true;
    }
    
    /**
     * Convierte coordenadas locales de chunk a un índice en el array de bloques
     */
    private int getIndex(int x, int y, int z) {
        return y * WIDTH * DEPTH + z * WIDTH + x;
    }
    
    /**
     * Comprueba si las coordenadas son válidas para este chunk
     */
    private boolean isValidCoordinate(int x, int y, int z) {
        return x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT && z >= 0 && z < DEPTH;
    }
    
    /**
     * Actualiza chunks vecinos cuando un bloque en el borde cambia
     */
    private void updateNeighborChunks(int x, int y, int z) {
        // Notificar a los chunks vecinos que su malla necesita actualización
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
     * Obtiene un bloque en espacio del mundo, manejando bordes de chunk
     * 
     * @param worldX Coordenada X en espacio del mundo
     * @param worldY Coordenada Y en espacio del mundo
     * @param worldZ Coordenada Z en espacio del mundo
     * @return Bloque en la posición especificada, o aire si está fuera de límites
     */
    public Block getBlockInWorld(int worldX, int worldY, int worldZ) {
        // Check if blockRegistry is null
        if (blockRegistry == null) {
            System.err.println("Error: BlockRegistry is null in Chunk.getBlockInWorld");
            return null;
        }
        
        // Si las coordenadas Y están fuera de rango, devolver aire
        if (worldY < 0 || worldY >= HEIGHT) {
            return blockRegistry.getBlock(BlockRegistry.AIR_ID);
        }
        
        // Convertir espacio del mundo a espacio de chunk
        int chunkWorldX = chunkX * WIDTH;
        int chunkWorldZ = chunkZ * DEPTH;
        
        int localX = worldX - chunkWorldX;
        int localZ = worldZ - chunkWorldZ;
        
        // Si las coordenadas locales están dentro de este chunk, obtener el bloque directamente
        if (localX >= 0 && localX < WIDTH && localZ >= 0 && localZ < DEPTH) {
            return getBlock(localX, worldY, localZ);
        } else {
            // Fuera de este chunk, delegar al mundo para obtener el bloque correcto
            try {
                if (world == null) {
                    System.err.println("Error: World is null in Chunk.getBlockInWorld");
                    return blockRegistry.getBlock(BlockRegistry.AIR_ID);
                }
                
                Block block = world.getBlockAt(worldX, worldY, worldZ);
                if (block == null) {
                    System.err.println("Warning: Got null block from world at " + worldX + "," + worldY + "," + worldZ);
                    return blockRegistry.getBlock(BlockRegistry.AIR_ID);
                }
                return block;
            } catch (Exception e) {
                // En caso de error (como chunks no cargados), devolver aire
                System.err.println("Error obteniendo bloque en mundo: " + e.getMessage());
                return blockRegistry.getBlock(BlockRegistry.AIR_ID);
            }
        }
    }
    
    /**
     * Obtiene la malla para este chunk, construyéndola si es necesario
     */
    public ChunkMesh getMesh() {
        return mesh;
    }
    
    /**
     * Establece la malla para este chunk
     * @param mesh La malla a establecer
     */
    public void setMesh(ChunkMesh mesh) {
        // Limpia el mesh anterior si existe
        if (this.mesh != null) {
            this.mesh.cleanup();
        }
        this.mesh = mesh;
    }
    
    /**
     * Obtiene los datos de malla para este chunk
     */
    public ChunkMeshData getMeshData() {
        return meshData;
    }
    
    /**
     * Establece los datos de malla para este chunk
     * @param meshData Los datos de malla a establecer
     */
    public void setMeshData(ChunkMeshData meshData) {
        this.meshData = meshData;
    }
    
    /**
     * Verifica si hay datos de malla disponibles para crear un mesh OpenGL
     */
    public boolean hasMeshData() {
        return meshData != null;
    }
    
    /**
     * Comprueba si este chunk está modificado (tiene cambios no guardados)
     */
    public boolean isModified() {
        return modified;
    }
    
    /**
     * Establece el estado modificado de este chunk
     */
    public void setModified(boolean modified) {
        this.modified = modified;
    }
    
    /**
     * Comprueba si este chunk tiene terreno generado
     */
    public boolean isGenerated() {
        return generated;
    }
    
    /**
     * Establece si este chunk tiene terreno generado
     */
    public void setGenerated(boolean generated) {
        this.generated = generated;
    }
    
    /**
     * Comprueba si este chunk tiene características pobladas
     */
    public boolean isPopulated() {
        return populated;
    }
    
    /**
     * Establece si este chunk tiene características pobladas
     */
    public void setPopulated(boolean populated) {
        this.populated = populated;
    }
    
    /**
     * Obtiene el estado sucio de la malla del chunk
     */
    public boolean isMeshDirty() {
        return meshDirty;
    }
    
    /**
     * Establece el estado sucio de la malla del chunk
     */
    public void setMeshDirty(boolean meshDirty) {
        this.meshDirty = meshDirty;
    }
    
    /**
     * Obtiene el objeto World que contiene este chunk
     */
    public World getWorld() {
        return world;
    }
    
    /**
     * Libera los recursos del chunk
     */
    public void dispose() {
        if (mesh != null) {
            mesh.cleanup();
            mesh = null;
        }
        // Liberar también los datos de la malla
        meshData = null;
    }
}