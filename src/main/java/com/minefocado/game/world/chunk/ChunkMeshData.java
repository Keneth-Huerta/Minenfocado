package main.java.com.minefocado.game.world.chunk;

/**
 * Clase que almacena datos de mesh para un chunk sin utilizar OpenGL
 * Permite la generación de meshes en hilos secundarios de forma segura
 */
public class ChunkMeshData {
    // Datos del mesh
    private final float[] positions;
    private final float[] colors;
    private final float[] texCoords;
    private final float[] normals;
    private final int[] indices;
    
    // Posición del chunk
    private final int chunkX;
    private final int chunkZ;
    
    /**
     * Crea un nuevo objeto de datos de mesh
     * 
     * @param chunkX Coordenada X del chunk
     * @param chunkZ Coordenada Z del chunk
     * @param positions Posiciones de los vértices
     * @param colors Colores de los vértices
     * @param texCoords Coordenadas de textura
     * @param normals Vectores normales
     * @param indices Índices para los triángulos
     */
    public ChunkMeshData(int chunkX, int chunkZ, float[] positions, float[] colors, 
                          float[] texCoords, float[] normals, int[] indices) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.positions = positions;
        this.colors = colors;
        this.texCoords = texCoords;
        this.normals = normals;
        this.indices = indices;
    }
    
    /**
     * Devuelve los datos de posiciones
     */
    public float[] getPositions() {
        return positions;
    }
    
    /**
     * Devuelve los datos de colores
     */
    public float[] getColors() {
        return colors;
    }
    
    /**
     * Devuelve las coordenadas de textura
     */
    public float[] getTexCoords() {
        return texCoords;
    }
    
    /**
     * Devuelve los datos de normales
     */
    public float[] getNormals() {
        return normals;
    }
    
    /**
     * Devuelve los índices
     */
    public int[] getIndices() {
        return indices;
    }
    
    /**
     * Devuelve la coordenada X del chunk
     */
    public int getChunkX() {
        return chunkX;
    }
    
    /**
     * Devuelve la coordenada Z del chunk
     */
    public int getChunkZ() {
        return chunkZ;
    }
    
    /**
     * Verifica si el mesh está vacío (no hay vértices para renderizar)
     */
    public boolean isEmpty() {
        return indices.length == 0;
    }
    
    /**
     * Crea un ChunkMesh a partir de estos datos (DEBE ejecutarse en el hilo principal)
     */
    public ChunkMesh createMesh() {
        return new ChunkMesh(chunkX, chunkZ, positions, colors, texCoords, normals, indices);
    }
}