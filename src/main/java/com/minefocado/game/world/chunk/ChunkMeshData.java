package main.java.com.minefocado.game.world.chunk;

import org.lwjgl.system.MemoryUtil;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Contiene los datos de un mesh de chunk sin crear objetos OpenGL.
 * Esta clase es segura para uso en hilos secundarios ya que no utiliza operaciones OpenGL.
 */
public class ChunkMeshData {
    // Datos de vértices
    private float[] positions;
    private float[] normals;
    private float[] textureCoords;
    private int[] indices;
    
    // Contador de vértices
    private int vertexCount;
    
    /**
     * Crea un nuevo objeto de datos de mesh vacío
     */
    public ChunkMeshData() {
        this.positions = new float[0];
        this.normals = new float[0];
        this.textureCoords = new float[0];
        this.indices = new int[0];
        this.vertexCount = 0;
    }
    
    /**
     * Crea un nuevo objeto de datos de mesh con los datos proporcionados
     * 
     * @param positions Posiciones de vértices
     * @param normals Normales de vértices
     * @param textureCoords Coordenadas de textura
     * @param indices Índices de vértices
     */
    public ChunkMeshData(float[] positions, float[] normals, float[] textureCoords, int[] indices) {
        this.positions = positions;
        this.normals = normals;
        this.textureCoords = textureCoords;
        this.indices = indices;
        this.vertexCount = indices.length;
    }
    
    /**
     * Obtiene las posiciones de vértices
     * 
     * @return Array de posiciones de vértices
     */
    public float[] getPositions() {
        return positions;
    }
    
    /**
     * Obtiene las normales de vértices
     * 
     * @return Array de normales de vértices
     */
    public float[] getNormals() {
        return normals;
    }
    
    /**
     * Obtiene las coordenadas de textura
     * 
     * @return Array de coordenadas de textura
     */
    public float[] getTextureCoords() {
        return textureCoords;
    }
    
    /**
     * Obtiene los índices de vértices
     * 
     * @return Array de índices de vértices
     */
    public int[] getIndices() {
        return indices;
    }
    
    /**
     * Verifica si el mesh contiene datos
     * 
     * @return true si el mesh tiene vértices, false en caso contrario
     */
    public boolean hasData() {
        return vertexCount > 0;
    }
    
    /**
     * Obtiene la cantidad de vértices
     * 
     * @return Número de vértices
     */
    public int getVertexCount() {
        return vertexCount;
    }
    
    /**
     * Crea un ChunkMesh a partir de estos datos
     * IMPORTANTE: Este método DEBE ser llamado desde el hilo principal 
     * con contexto OpenGL activo
     * 
     * @return Un nuevo ChunkMesh con objetos OpenGL creados
     */
    public ChunkMesh createMesh(int chunkX, int chunkZ) {
        if (!hasData()) {
            return null;
        }
        
        // Crear y retornar un nuevo ChunkMesh
        return new ChunkMesh(chunkX, chunkZ, this);
    }
}