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
 * Representa el mundo del juego, administrando chunks, generación de terreno,
 * y proporcionando acceso a bloques en cualquier posición.
 */
public class World {
    // Semilla del mundo por defecto
    private static final long DEFAULT_SEED = 12345L;
    
    // Distancia de renderizado de chunks (chunks desde el jugador en cada dirección)
    private static final int RENDER_DISTANCE = 8;
    
    // El radio en el que cargamos chunks
    private static final int LOAD_DISTANCE = RENDER_DISTANCE + 2;
    
    // Mapa de chunks cargados
    private final Map<ChunkPos, Chunk> chunks;
    
    // Generador de terreno para este mundo
    private final TerrainGenerator terrainGenerator;
    
    // Constructor de mallas de chunks
    private final ChunkMeshBuilder meshBuilder;
    
    // Pool de hilos para operaciones asíncronas de chunks
    private final ExecutorService chunkExecutor;
    
    // Bandera para evitar envío de tareas cuando el ejecutor está cerrándose
    private boolean isShuttingDown = false;
    
    // Número máximo de operaciones de carga de chunks a programar a la vez
    private static final int MAX_CHUNK_LOAD_QUEUE = 100;
    
    // Seguimiento del recuento de tareas activas
    private int pendingChunkTasks = 0;
    
    // Semilla del mundo
    private final long seed;
    
    // Referencia al registro de bloques
    private final BlockRegistry blockRegistry;
    
    // Programa de shader para renderizar chunks
    private ShaderProgram shaderProgram;
    
    // Atlas de texturas para bloques
    private Texture textureAtlas;
    
    // Chunk actual del jugador para cálculos de carga/descarga
    private int playerChunkX;
    private int playerChunkZ;
    
    // Lista de chunks que necesitan crear su mesh (solo para hilos principales)
    private final List<Chunk> meshCreationQueue = new ArrayList<>();
    
    /**
     * Crea un nuevo mundo con la semilla predeterminada
     */
    public World() {
        this(DEFAULT_SEED);
    }
    
    /**
     * Crea un nuevo mundo con la semilla especificada
     * 
     * @param seed La semilla del mundo
     */
    public World(long seed) {
        this.seed = seed;
        this.chunks = new ConcurrentHashMap<>();
        this.terrainGenerator = new TerrainGenerator(seed);
        this.meshBuilder = new ChunkMeshBuilder();
        this.blockRegistry = BlockRegistry.getInstance();
        
        // Crear un pool de hilos para operaciones de chunk
        this.chunkExecutor = Executors.newFixedThreadPool(
                Math.max(1, Runtime.getRuntime().availableProcessors() - 1)
        );
        
        // Inicializar posición del jugador
        this.playerChunkX = 0;
        this.playerChunkZ = 0;
        
        // Inicializar componentes de renderizado
        initRendering();
    }
    
    /**
     * Inicializa recursos de shader y textura
     */
    protected void initRendering() {
        try {
            // Cargar programa de shader
            String vertexShaderCode = ShaderLoader.getDefaultVertexShader();
            String fragmentShaderCode = ShaderLoader.getDefaultFragmentShader();
            shaderProgram = new ShaderProgram(vertexShaderCode, fragmentShaderCode);
            
            // Crear uniformes - Implementando manejo de errores para cada uniform
            try {
                shaderProgram.createUniform("projectionMatrix");
                shaderProgram.createUniform("viewMatrix");
                shaderProgram.createUniform("modelMatrix");
                shaderProgram.createUniform("textureSampler");
                shaderProgram.createUniform("lightPosition");
                shaderProgram.createUniform("viewPosition");
                shaderProgram.createUniform("ambientStrength");
            } catch (Exception e) {
                System.err.println("Advertencia: Error al crear algunos uniformes del shader: " + e.getMessage());
                System.err.println("Esto puede causar problemas de renderizado pero no impedirá que el juego se ejecute.");
                // Continuamos a pesar del error para que el juego pueda ejecutarse
            }
            
            // Cargar atlas de textura (crea uno predeterminado si no se encuentra)
            textureAtlas = Texture.loadTexture("textures/blocks.png");
            
            System.out.println("Renderizado del mundo inicializado correctamente");
        } catch (Exception e) {
            System.err.println("Error al inicializar el renderizado del mundo: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Obtiene el bloque en una posición del mundo
     * 
     * @param worldX Coordenada X en espacio del mundo
     * @param worldY Coordenada Y en espacio del mundo
     * @param worldZ Coordenada Z en espacio del mundo
     * @return El bloque en esa posición, o AIRE si está fuera de chunks cargados
     */
    public Block getBlockAt(int worldX, int worldY, int worldZ) {
        // Convertir a coordenadas de chunk
        int chunkX = Math.floorDiv(worldX, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(worldZ, Chunk.DEPTH);
        
        // Calcular coordenadas locales dentro del chunk
        int localX = Math.floorMod(worldX, Chunk.WIDTH);
        int localZ = Math.floorMod(worldZ, Chunk.DEPTH);
        
        // Obtener el chunk
        Chunk chunk = getChunk(chunkX, chunkZ);
        if (chunk != null) {
            return chunk.getBlock(localX, worldY, localZ);
        }
        
        // Devolver aire para chunks no cargados
        return blockRegistry.getBlock(BlockRegistry.AIR_ID);
    }
    
    /**
     * Coloca un bloque en una posición del mundo
     * 
     * @param worldX Coordenada X en espacio del mundo
     * @param worldY Coordenada Y en espacio del mundo
     * @param worldZ Coordenada Z en espacio del mundo
     * @param blockId El ID del bloque a colocar
     * @return Verdadero si el bloque se colocó correctamente
     */
    public boolean setBlockAt(int worldX, int worldY, int worldZ, int blockId) {
        // Convertir a coordenadas de chunk
        int chunkX = Math.floorDiv(worldX, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(worldZ, Chunk.DEPTH);
        
        // Calcular coordenadas locales dentro del chunk
        int localX = Math.floorMod(worldX, Chunk.WIDTH);
        int localZ = Math.floorMod(worldZ, Chunk.DEPTH);
        
        // Obtener el chunk
        Chunk chunk = getChunk(chunkX, chunkZ);
        if (chunk != null) {
            chunk.setBlockId(localX, worldY, localZ, blockId);
            
            // Actualizar chunks adyacentes si este bloque está en un límite de chunk
            if (localX == 0 || localX == Chunk.WIDTH - 1 || 
                localZ == 0 || localZ == Chunk.DEPTH - 1) {
                updateAdjacentChunks(chunkX, chunkZ);
            }
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Actualiza chunks adyacentes para asegurar consistencia en los bordes de malla
     */
    private void updateAdjacentChunks(int chunkX, int chunkZ) {
        // Marcar chunks adyacentes como sucios si existen
        // Esto asegura renderizado consistente en los límites de chunk
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue; // Omitir el chunk central
                
                Chunk adjacentChunk = getChunk(chunkX + dx, chunkZ + dz);
                if (adjacentChunk != null) {
                    adjacentChunk.setMeshDirty(true);
                }
            }
        }
    }
    
    /**
     * Obtiene un chunk en las coordenadas de chunk especificadas
     * 
     * @param chunkX Coordenada X del chunk
     * @param chunkZ Coordenada Z del chunk
     * @return El chunk, o null si no está cargado
     */
    public Chunk getChunk(int chunkX, int chunkZ) {
        return chunks.get(new ChunkPos(chunkX, chunkZ));
    }
    
    /**
     * Actualiza el mundo basado en la posición del jugador,
     * cargando/descargando chunks según sea necesario
     * 
     * @param worldX Posición X del jugador en espacio del mundo
     * @param worldZ Posición Z del jugador en espacio del mundo
     */
    public void update(float worldX, float worldZ) {
        // Calcular chunk actual del jugador
        int newChunkX = Math.floorDiv((int)worldX, Chunk.WIDTH);
        int newChunkZ = Math.floorDiv((int)worldZ, Chunk.DEPTH);
        
        // Solo cargar/descargar chunks si el jugador se ha movido a un chunk diferente
        if (newChunkX != playerChunkX || newChunkZ != playerChunkZ) {
            playerChunkX = newChunkX;
            playerChunkZ = newChunkZ;
            
            // Cargar nuevos chunks en un hilo separado
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
     * Actualiza mallas para chunks que han sido modificados
     */
    public void updateChunkMeshes() {
        // Verificar chunks sucios y programar su reconstrucción de malla si es necesario
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
     * Carga y descarga chunks basados en la posición del jugador
     */
    private void loadAndUnloadChunks() {
        Set<ChunkPos> toKeep = new HashSet<>();
        
        // Calcular qué chunks deben estar cargados
        for (int x = playerChunkX - LOAD_DISTANCE; x <= playerChunkX + LOAD_DISTANCE; x++) {
            for (int z = playerChunkZ - LOAD_DISTANCE; z <= playerChunkZ + LOAD_DISTANCE; z++) {
                ChunkPos pos = new ChunkPos(x, z);
                toKeep.add(pos);
                
                // Si este chunk aún no está cargado, cargarlo
                if (!chunks.containsKey(pos)) {
                    loadChunk(x, z);
                }
            }
        }
        
        // Encontrar chunks para descargar (fuera de la distancia de renderizado)
        List<ChunkPos> toRemove = new ArrayList<>();
        for (ChunkPos pos : chunks.keySet()) {
            if (!toKeep.contains(pos)) {
                toRemove.add(pos);
            }
        }
        
        // Descargar los chunks
        for (ChunkPos pos : toRemove) {
            unloadChunk(pos);
        }
        
        // Generar características de terreno en un segundo pase una vez que todos los chunks cercanos estén disponibles
        for (int x = playerChunkX - LOAD_DISTANCE + 2; x <= playerChunkX + LOAD_DISTANCE - 2; x++) {
            for (int z = playerChunkZ - LOAD_DISTANCE + 2; z <= playerChunkZ + LOAD_DISTANCE - 2; z++) {
                Chunk chunk = getChunk(x, z);
                
                // Solo poblar si el chunk está generado pero no poblado
                if (chunk != null && chunk.isGenerated() && !chunk.isPopulated()) {
                    // Poblar el chunk con características como árboles
                    terrainGenerator.populateChunk(chunk);
                    
                    // Marcar el chunk como sucio para reconstruir su malla
                    chunk.setMeshDirty(true);
                }
            }
        }
    }
    
    /**
     * Carga un chunk en las coordenadas especificadas
     * 
     * @param chunkX Coordenada X del chunk
     * @param chunkZ Coordenada Z del chunk
     */
    private void loadChunk(int chunkX, int chunkZ) {
        // Omitir si estamos cerrando o tenemos demasiadas tareas pendientes
        if (isShuttingDown || pendingChunkTasks >= MAX_CHUNK_LOAD_QUEUE) {
            return;
        }
        
        // Crear un nuevo chunk
        Chunk chunk = new Chunk(this, chunkX, chunkZ);
        
        // Generar terreno para el chunk
        terrainGenerator.generateTerrain(chunk);
        
        // Añadir a chunks cargados (sin construir el mesh todavía)
        chunks.put(new ChunkPos(chunkX, chunkZ), chunk);
        
        try {
            // Seguimiento del número de tareas pendientes
            synchronized(this) {
                pendingChunkTasks++;
            }
            
            // Programar la construcción de datos del mesh en un hilo secundario
            chunkExecutor.execute(() -> {
                try {
                    // Generar datos del mesh
                    ChunkMeshData meshData = meshBuilder.buildMeshData(chunk);
                    chunk.setMeshData(meshData);
                    
                    // Añadir a la cola para crear el mesh en el hilo principal
                    synchronized (meshCreationQueue) {
                        if (!meshCreationQueue.contains(chunk)) {
                            meshCreationQueue.add(chunk);
                        }
                    }
                } finally {
                    // Siempre decrementar el conteo de tareas al terminar
                    synchronized(World.this) {
                        pendingChunkTasks--;
                    }
                }
            });
        } catch (Exception e) {
            // Manejar rechazo de pool de hilos u otros errores
            synchronized(this) {
                pendingChunkTasks--; // Decrementar ya que fallamos en encolar
            }
            System.err.println("Error al programar generación de malla de chunk para el chunk " + 
                              chunkX + "," + chunkZ + ": " + e.getMessage());
        }
    }
    
    /**
     * Descarga un chunk y libera sus recursos
     * 
     * @param pos Posición del chunk a descargar
     */
    private void unloadChunk(ChunkPos pos) {
        Chunk chunk = chunks.remove(pos);
        
        // Limpiar recursos de malla del chunk
        if (chunk != null && chunk.getMesh() != null) {
            chunk.getMesh().cleanup();
        }
    }
    
    /**
     * Renderiza chunks visibles
     * 
     * @param projectionMatrix Matriz de proyección de la cámara
     * @param viewMatrix Matriz de vista de la cámara
     * @param playerPos Posición del jugador para iluminación
     */
    public void render(Matrix4f projectionMatrix, Matrix4f viewMatrix, Vector3f playerPos) {
        if (shaderProgram == null || textureAtlas == null) {
            return;
        }
        
        // Vincular shader y textura
        shaderProgram.bind();
        textureAtlas.bind();
        
        // Establecer uniformes compartidos con try-catch para cada uno
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
            shaderProgram.setUniform("textureSampler", 0); // Unidad de textura 0
        } catch (Exception e) {
            // Ignorar si el uniform no existe
        }
        
        // Establecer uniformes de iluminación
        try {
            shaderProgram.setUniform("lightPosition", new Vector3f(0, 100, 0)); // Posición del sol
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
        
        // Renderizar todos los chunks que están en distancia de renderizado
        for (Chunk chunk : chunks.values()) {
            int dx = chunk.getChunkX() - playerChunkX;
            int dz = chunk.getChunkZ() - playerChunkZ;
            
            // Solo renderizar chunks dentro de la distancia de renderizado
            if (Math.abs(dx) <= RENDER_DISTANCE && Math.abs(dz) <= RENDER_DISTANCE) {
                ChunkMesh mesh = chunk.getMesh();
                if (mesh != null) {
                    try {
                        mesh.render(shaderProgram);
                    } catch (Exception e) {
                        System.err.println("Error al renderizar chunk en " + chunk.getChunkX() + "," + chunk.getChunkZ() + ": " + e.getMessage());
                    }
                }
            }
        }
        
        // Desvincular
        textureAtlas.unbind();
        shaderProgram.unbind();
    }
    
    /**
     * Limpia recursos del mundo
     */
    public void cleanup() {
        // Marcar como cerrando para evitar nuevas tareas
        isShuttingDown = true;
        
        // Apagar el ejecutor de chunk
        chunkExecutor.shutdown();
        
        // Limpiar todas las mallas de chunk
        for (Chunk chunk : chunks.values()) {
            if (chunk.getMesh() != null) {
                chunk.getMesh().cleanup();
            }
        }
        
        // Limpiar chunks
        chunks.clear();
    }
    
    /**
     * Obtiene la semilla del mundo
     */
    public long getSeed() {
        return seed;
    }
    
    /**
     * Obtiene el número de chunks cargados
     */
    public int getLoadedChunkCount() {
        return chunks.size();
    }
    
    /**
     * Precarga el área alrededor del punto de spawn para garantizar
     * que el jugador aparezca en un mundo correctamente generado
     * 
     * @param worldX Coordenada X del punto de spawn
     * @param worldZ Coordenada Z del punto de spawn
     */
    public void preloadSpawnArea(float worldX, float worldZ) {
        // Calcular el chunk donde aparecerá el jugador
        int spawnChunkX = Math.floorDiv((int)worldX, Chunk.WIDTH);
        int spawnChunkZ = Math.floorDiv((int)worldZ, Chunk.DEPTH);
        
        // Guardar esta posición como la posición del jugador
        playerChunkX = spawnChunkX;
        playerChunkZ = spawnChunkZ;
        
        // Radio de carga inicial (más pequeño que el radio normal para cargar rápido)
        int preloadRadius = 2;
        
        System.out.println("Precargando chunks alrededor del spawn...");
        
        // Cargar inmediatamente el chunk del spawn y los chunks adyacentes
        for (int x = spawnChunkX - preloadRadius; x <= spawnChunkX + preloadRadius; x++) {
            for (int z = spawnChunkZ - preloadRadius; z <= spawnChunkZ + preloadRadius; z++) {
                // Priorizar el chunk central (donde aparecerá el jugador)
                if (x == spawnChunkX && z == spawnChunkZ) {
                    Chunk chunk = new Chunk(this, x, z);
                    terrainGenerator.generateTerrain(chunk);
                    terrainGenerator.populateChunk(chunk);
                    
                    // Generar mesh sincronizadamente para el chunk central
                    ChunkMeshData meshData = meshBuilder.buildMeshData(chunk);
                    chunk.setMeshData(meshData);
                    
                    // Añadir a chunks cargados
                    chunks.put(new ChunkPos(x, z), chunk);
                    
                    // Añadir a la cola para crear el mesh en el hilo principal
                    synchronized (meshCreationQueue) {
                        if (!meshCreationQueue.contains(chunk)) {
                            meshCreationQueue.add(chunk);
                        }
                    }
                } else {
                    // Cargar los demás chunks normalmente
                    loadChunk(x, z);
                }
            }
        }
        
        // Procesar la cola de meshes para asegurarse de que el mesh del chunk central esté listo
        processMeshCreationQueue();
        
        System.out.println("Área de spawn precargada con éxito");
    }
    
    /**
     * Representa una posición de chunk en el mundo
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