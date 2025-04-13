package main.java.com.minefocado.game.world;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import main.java.com.minefocado.game.render.ShaderLoader;
import main.java.com.minefocado.game.render.ShaderProgram;
import main.java.com.minefocado.game.render.Texture;
import main.java.com.minefocado.game.world.blocks.Block;
import main.java.com.minefocado.game.world.blocks.BlockRegistry;
import main.java.com.minefocado.game.world.chunk.Chunk;
import main.java.com.minefocado.game.world.chunk.ChunkMesh;
import main.java.com.minefocado.game.world.chunk.ChunkMeshBuilder;
import main.java.com.minefocado.game.world.chunk.ChunkMeshData;
import main.java.com.minefocado.game.world.generation.TerrainGenerator;

/**
 * Represents the game world, managing chunks, terrain generation,
 * and providing access to blocks at any position.
 */
public class World {
    // Default world seed
    private static final long DEFAULT_SEED = 12345L;
    
    // Chunk render distance (chunks from player in each direction)
    private static final int RENDER_DISTANCE = 8;
    
    // The radius at which we load chunks
    private static final int LOAD_DISTANCE = RENDER_DISTANCE + 2;
    
    // Map of loaded chunks
    private final Map<ChunkPos, Chunk> chunks;
    
    // Terrain generator for this world
    private final TerrainGenerator terrainGenerator;
    
    // Chunk mesh builder
    private final ChunkMeshBuilder meshBuilder;
    
    // Thread pool for async chunk operations
    private final ExecutorService chunkExecutor;
    
    // World seed
    private final long seed;
    
    // Block registry reference
    private final BlockRegistry blockRegistry;
    
    // Shader program for rendering chunks
    private ShaderProgram shaderProgram;
    
    // Texture atlas for blocks
    private Texture textureAtlas;
    
    // Current player chunk for loading/unloading calculations
    private int playerChunkX;
    private int playerChunkZ;
    
    // Lista de chunks que necesitan crear su mesh (solo para hilos principales)
    private final List<Chunk> meshCreationQueue = new ArrayList<>();
    
    /**
     * Creates a new world with the default seed
     */
    public World() {
        this(DEFAULT_SEED);
    }
    
    /**
     * Creates a new world with the specified seed
     * 
     * @param seed The world seed
     */
    public World(long seed) {
        this.seed = seed;
        this.chunks = new ConcurrentHashMap<>();
        this.terrainGenerator = new TerrainGenerator(seed);
        this.meshBuilder = new ChunkMeshBuilder();
        this.blockRegistry = BlockRegistry.getInstance();
        
        // Create a thread pool for chunk operations
        this.chunkExecutor = Executors.newFixedThreadPool(
                Math.max(1, Runtime.getRuntime().availableProcessors() - 1)
        );
        
        // Initialize player position
        this.playerChunkX = 0;
        this.playerChunkZ = 0;
        
        // Initialize rendering components
        initRendering();
    }
    
    /**
     * Initializes shader and texture resources
     */
    protected void initRendering() {
        try {
            // Load shader program
            String vertexShaderCode = ShaderLoader.getDefaultVertexShader();
            String fragmentShaderCode = ShaderLoader.getDefaultFragmentShader();
            shaderProgram = new ShaderProgram(vertexShaderCode, fragmentShaderCode);
            
            // Create uniforms - Implementando manejo de errores para cada uniform
            try {
                shaderProgram.createUniform("projectionMatrix");
                shaderProgram.createUniform("viewMatrix");
                shaderProgram.createUniform("modelMatrix");
                shaderProgram.createUniform("textureSampler");
                shaderProgram.createUniform("lightPosition");
                shaderProgram.createUniform("viewPosition");
                shaderProgram.createUniform("ambientStrength");
            } catch (Exception e) {
                System.err.println("Warning: Failed to create some shader uniforms: " + e.getMessage());
                System.err.println("This may cause rendering issues but will not prevent the game from running.");
                // Continuamos a pesar del error para que el juego pueda ejecutarse
            }
            
            // Load texture atlas (creates a default one if not found)
            textureAtlas = Texture.loadTexture("textures/blocks.png");
            
            System.out.println("World rendering initialized successfully");
        } catch (Exception e) {
            System.err.println("Failed to initialize world rendering: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Gets the block at a world position
     * 
     * @param worldX X coordinate in world space
     * @param worldY Y coordinate in world space
     * @param worldZ Z coordinate in world space
     * @return The block at that position, or AIR if outside loaded chunks
     */
    public Block getBlockAt(int worldX, int worldY, int worldZ) {
        // Convert to chunk coordinates
        int chunkX = Math.floorDiv(worldX, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(worldZ, Chunk.DEPTH);
        
        // Calculate local coordinates within the chunk
        int localX = Math.floorMod(worldX, Chunk.WIDTH);
        int localZ = Math.floorMod(worldZ, Chunk.DEPTH);
        
        // Get the chunk
        Chunk chunk = getChunk(chunkX, chunkZ);
        if (chunk != null) {
            return chunk.getBlock(localX, worldY, localZ);
        }
        
        // Return air for unloaded chunks
        return blockRegistry.getBlock(BlockRegistry.AIR_ID);
    }
    
    /**
     * Sets a block at a world position
     * 
     * @param worldX X coordinate in world space
     * @param worldY Y coordinate in world space
     * @param worldZ Z coordinate in world space
     * @param blockId The block ID to place
     * @return True if the block was placed successfully
     */
    public boolean setBlockAt(int worldX, int worldY, int worldZ, int blockId) {
        // Convert to chunk coordinates
        int chunkX = Math.floorDiv(worldX, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(worldZ, Chunk.DEPTH);
        
        // Calculate local coordinates within the chunk
        int localX = Math.floorMod(worldX, Chunk.WIDTH);
        int localZ = Math.floorMod(worldZ, Chunk.DEPTH);
        
        // Get the chunk
        Chunk chunk = getChunk(chunkX, chunkZ);
        if (chunk != null) {
            chunk.setBlockId(localX, worldY, localZ, blockId);
            
            // Update adjacent chunks if this block is on a chunk boundary
            if (localX == 0 || localX == Chunk.WIDTH - 1 || 
                localZ == 0 || localZ == Chunk.DEPTH - 1) {
                updateAdjacentChunks(chunkX, chunkZ);
            }
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Updates adjacent chunks to ensure mesh edges are consistent
     */
    private void updateAdjacentChunks(int chunkX, int chunkZ) {
        // Mark adjacent chunks as dirty if they exist
        // This ensures consistent rendering at chunk boundaries
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue; // Skip the center chunk
                
                Chunk adjacentChunk = getChunk(chunkX + dx, chunkZ + dz);
                if (adjacentChunk != null) {
                    adjacentChunk.setMeshDirty(true);
                }
            }
        }
    }
    
    /**
     * Gets a chunk at the specified chunk coordinates
     * 
     * @param chunkX Chunk X coordinate
     * @param chunkZ Chunk Z coordinate
     * @return The chunk, or null if not loaded
     */
    public Chunk getChunk(int chunkX, int chunkZ) {
        return chunks.get(new ChunkPos(chunkX, chunkZ));
    }
    
    /**
     * Updates the world based on the player's position,
     * loading/unloading chunks as needed
     * 
     * @param worldX Player's X position in world space
     * @param worldZ Player's Z position in world space
     */
    public void update(float worldX, float worldZ) {
        // Calculate player's current chunk
        int newChunkX = Math.floorDiv((int)worldX, Chunk.WIDTH);
        int newChunkZ = Math.floorDiv((int)worldZ, Chunk.DEPTH);
        
        // Only load/unload chunks if the player has moved to a different chunk
        if (newChunkX != playerChunkX || newChunkZ != playerChunkZ) {
            playerChunkX = newChunkX;
            playerChunkZ = newChunkZ;
            
            // Load new chunks in a separate thread
            chunkExecutor.execute(this::loadAndUnloadChunks);
        }
        
        // Procesar la cola de meshes pendientes (desde el hilo principal)
        processMeshCreationQueue();
    }
    
    /**
     * Procesa meshes pendientes (DEBE ejecutarse en el hilo principal)
     */
    private void processMeshCreationQueue() {
        // Límite de meshes a procesar por frame para evitar cuelgues
        final int MAX_MESHES_PER_FRAME = 3;
        int processed = 0;
        
        synchronized (meshCreationQueue) {
            List<Chunk> processedChunks = new ArrayList<>();
            
            for (Chunk chunk : meshCreationQueue) {
                if (processed >= MAX_MESHES_PER_FRAME) {
                    break;
                }
                
                // Crear el mesh a partir de los datos en el hilo principal
                if (chunk.hasMeshData()) {
                    ChunkMeshData meshData = chunk.getMeshData();
                    // Pasar las coordenadas del chunk para posicionamiento correcto
                    ChunkMesh mesh = meshData.createMesh(chunk.getChunkX(), chunk.getChunkZ());
                    
                    // Asignar el mesh al chunk
                    chunk.setMesh(mesh);
                    
                    // Ya no necesitamos los datos del mesh
                    chunk.setMeshData(null);
                    
                    // Marcar el chunk como procesado
                    processedChunks.add(chunk);
                    processed++;
                }
            }
            
            // Eliminar los chunks procesados de la cola
            meshCreationQueue.removeAll(processedChunks);
        }
    }
    
    /**
     * Updates meshes for chunks that have been modified
     */
    public void updateChunkMeshes() {
        // Check dirty chunks and schedule their mesh rebuilding if needed
        for (Chunk chunk : chunks.values()) {
            if (chunk.isMeshDirty()) {
                // Solo generamos los datos en el hilo principal
                // pero los cargamos a OpenGL en updateMeshes
                final Chunk dirtyChunk = chunk;
                
                // Ejecutar la parte de cálculo en un hilo secundario
                chunkExecutor.execute(() -> {
                    ChunkMeshData meshData = meshBuilder.buildMeshData(dirtyChunk);
                    dirtyChunk.setMeshData(meshData);
                    
                    // Añadir a la cola para crear el mesh real en el hilo principal
                    synchronized (meshCreationQueue) {
                        if (!meshCreationQueue.contains(dirtyChunk)) {
                            meshCreationQueue.add(dirtyChunk);
                        }
                    }
                    
                    dirtyChunk.setMeshDirty(false);
                });
            }
        }
    }
    
    /**
     * Loads and unloads chunks based on player position
     */
    private void loadAndUnloadChunks() {
        Set<ChunkPos> toKeep = new HashSet<>();
        
        // Calculate which chunks should be loaded
        for (int x = playerChunkX - LOAD_DISTANCE; x <= playerChunkX + LOAD_DISTANCE; x++) {
            for (int z = playerChunkZ - LOAD_DISTANCE; z <= playerChunkZ + LOAD_DISTANCE; z++) {
                ChunkPos pos = new ChunkPos(x, z);
                toKeep.add(pos);
                
                // If this chunk isn't loaded yet, load it
                if (!chunks.containsKey(pos)) {
                    loadChunk(x, z);
                }
            }
        }
        
        // Find chunks to unload (outside render distance)
        List<ChunkPos> toRemove = new ArrayList<>();
        for (ChunkPos pos : chunks.keySet()) {
            if (!toKeep.contains(pos)) {
                toRemove.add(pos);
            }
        }
        
        // Unload the chunks
        for (ChunkPos pos : toRemove) {
            unloadChunk(pos);
        }
        
        // Generate terrain features in a second pass once all nearby chunks are available
        for (int x = playerChunkX - LOAD_DISTANCE + 2; x <= playerChunkX + LOAD_DISTANCE - 2; x++) {
            for (int z = playerChunkZ - LOAD_DISTANCE + 2; z <= playerChunkZ + LOAD_DISTANCE - 2; z++) {
                Chunk chunk = getChunk(x, z);
                
                // Only populate if the chunk is generated but not populated
                if (chunk != null && chunk.isGenerated() && !chunk.isPopulated()) {
                    // Populate the chunk with features like trees
                    terrainGenerator.populateChunk(chunk);
                    
                    // Mark the chunk dirty to rebuild its mesh
                    chunk.setMeshDirty(true);
                }
            }
        }
    }
    
    /**
     * Loads a chunk at the specified coordinates
     * 
     * @param chunkX Chunk X coordinate
     * @param chunkZ Chunk Z coordinate
     */
    private void loadChunk(int chunkX, int chunkZ) {
        // Create a new chunk
        Chunk chunk = new Chunk(this, chunkX, chunkZ);
        
        // Generate terrain for the chunk
        terrainGenerator.generateTerrain(chunk);
        
        // Add to loaded chunks (sin construir el mesh todavía)
        chunks.put(new ChunkPos(chunkX, chunkZ), chunk);
        
        // Programar la construcción de datos del mesh en un hilo secundario
        chunkExecutor.execute(() -> {
            // Generar datos del mesh
            ChunkMeshData meshData = meshBuilder.buildMeshData(chunk);
            chunk.setMeshData(meshData);
            
            // Añadir a la cola para crear el mesh en el hilo principal
            synchronized (meshCreationQueue) {
                if (!meshCreationQueue.contains(chunk)) {
                    meshCreationQueue.add(chunk);
                }
            }
        });
    }
    
    /**
     * Unloads a chunk and frees its resources
     * 
     * @param pos Chunk position to unload
     */
    private void unloadChunk(ChunkPos pos) {
        Chunk chunk = chunks.remove(pos);
        
        // Cleanup chunk mesh resources
        if (chunk != null && chunk.getMesh() != null) {
            chunk.getMesh().cleanup();
        }
    }
    
    /**
     * Renders visible chunks
     * 
     * @param projectionMatrix Projection matrix from the camera
     * @param viewMatrix View matrix from the camera
     * @param playerPos Player position for lighting
     */
    public void render(Matrix4f projectionMatrix, Matrix4f viewMatrix, Vector3f playerPos) {
        if (shaderProgram == null || textureAtlas == null) {
            return;
        }
        
        // Bind shader and texture
        shaderProgram.bind();
        textureAtlas.bind();
        
        // Set shared uniforms with try-catch para cada uno
        try {
            shaderProgram.setUniform("projectionMatrix", projectionMatrix);
        } catch (Exception e) {
            // Ignorar si el uniform no existe
        }
        
        try {
            shaderProgram.setUniform("viewMatrix", viewMatrix);
        } catch (Exception e) {
            // Ignorar si el uniform no existe
        }
        
        try {
            shaderProgram.setUniform("textureSampler", 0); // Texture unit 0
        } catch (Exception e) {
            // Ignorar si el uniform no existe
        }
        
        // Set lighting uniforms
        try {
            shaderProgram.setUniform("lightPosition", new Vector3f(0, 100, 0)); // Sun position
        } catch (Exception e) {
            // Ignorar si el uniform no existe
        }
        
        try {
            shaderProgram.setUniform("viewPosition", playerPos);
        } catch (Exception e) {
            // Ignorar si el uniform no existe
        }
        
        try {
            shaderProgram.setUniform("ambientStrength", 0.6f);
        } catch (Exception e) {
            // Ignorar si el uniform no existe
        }
        
        // Render all chunks that are in render distance
        for (Chunk chunk : chunks.values()) {
            int dx = chunk.getChunkX() - playerChunkX;
            int dz = chunk.getChunkZ() - playerChunkZ;
            
            // Only render chunks within render distance
            if (Math.abs(dx) <= RENDER_DISTANCE && Math.abs(dz) <= RENDER_DISTANCE) {
                ChunkMesh mesh = chunk.getMesh();
                if (mesh != null) {
                    try {
                        mesh.render(shaderProgram);
                    } catch (Exception e) {
                        System.err.println("Error rendering chunk at " + chunk.getChunkX() + "," + chunk.getChunkZ() + ": " + e.getMessage());
                    }
                }
            }
        }
        
        // Unbind
        textureAtlas.unbind();
        shaderProgram.unbind();
    }
    
    /**
     * Cleanup world resources
     */
    public void cleanup() {
        // Shutdown chunk executor
        chunkExecutor.shutdown();
        
        // Cleanup all chunk meshes
        for (Chunk chunk : chunks.values()) {
            if (chunk.getMesh() != null) {
                chunk.getMesh().cleanup();
            }
        }
        
        // Clear chunks
        chunks.clear();
    }
    
    /**
     * Gets the world seed
     */
    public long getSeed() {
        return seed;
    }
    
    /**
     * Gets the number of loaded chunks
     */
    public int getLoadedChunkCount() {
        return chunks.size();
    }
    
    /**
     * Represents a chunk position in the world
     */
    private static class ChunkPos {
        private final int x;
        private final int z;
        
        public ChunkPos(int x, int z) {
            this.x = x;
            this.z = z;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ChunkPos chunkPos = (ChunkPos) o;
            return x == chunkPos.x && z == chunkPos.z;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(x, z);
        }
    }
}