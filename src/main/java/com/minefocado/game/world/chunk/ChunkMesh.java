package main.java.com.minefocado.game.world.chunk;

import org.lwjgl.system.MemoryUtil;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import main.java.com.minefocado.game.render.ShaderProgram;

/**
 * Representa una malla para renderizar un chunk en OpenGL
 * Maneja la gestión de datos de vértices y renderizado
 */
public class ChunkMesh {
    // IDs de objetos OpenGL - eliminado 'final' para que puedan ser nulos después de la limpieza
    private int vaoId;         // Objeto de Array de Vértices
    private int posVboId;      // Objeto de Buffer de Vértices para Posiciones
    private int texCoordVboId; // VBO para Coordenadas de Textura
    private int normalVboId;   // VBO para Normales
    private int indexVboId;    // Objeto de Buffer de Índices
    
    // Bandera para rastrear si la malla ha sido limpiada
    private boolean isCleanedUp = false;
    
    // Datos de la malla
    private final int vertexCount;
    
    // Matriz modelo para transformación
    private final Matrix4f modelMatrix;
    
    // Posición del chunk en el espacio del mundo
    private final int chunkX;
    private final int chunkZ;
    
    /**
     * Crea una nueva malla de chunk a partir de los datos proporcionados en ChunkMeshData
     * IMPORTANTE: Este constructor DEBE ser llamado únicamente desde el hilo principal que tiene el contexto OpenGL
     * 
     * @param chunkX Coordenada X del chunk
     * @param chunkZ Coordenada Z del chunk
     * @param meshData Los datos de mesh pre-calculados
     */
    public ChunkMesh(int chunkX, int chunkZ, ChunkMeshData meshData) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        
        // Calcular la matriz modelo basada en la posición del chunk
        modelMatrix = new Matrix4f().identity().translate(
                chunkX * Chunk.WIDTH, 
                0, 
                chunkZ * Chunk.DEPTH);
        
        // Obtener datos de la malla desde ChunkMeshData
        float[] positions = meshData.getPositions();
        float[] normals = meshData.getNormals();
        float[] texCoords = meshData.getTextureCoords();
        int[] indices = meshData.getIndices();

        // Si no hay datos, crear una malla vacía
        if (!meshData.hasData()) {
            // Inicializar con datos vacíos
            this.vaoId = -1;
            this.posVboId = -1;
            this.texCoordVboId = -1;
            this.normalVboId = -1;
            this.indexVboId = -1;
            this.vertexCount = 0;
            return;
        }
        
        // Crear y enlazar VAO - SOLO EN HILO PRINCIPAL
        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);
        
        // VBO de Posiciones
        posVboId = glGenBuffers();
        FloatBuffer posBuffer = MemoryUtil.memAllocFloat(positions.length);
        posBuffer.put(positions).flip();
        glBindBuffer(GL_ARRAY_BUFFER, posVboId);
        glBufferData(GL_ARRAY_BUFFER, posBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        MemoryUtil.memFree(posBuffer);
        
        // VBO de Coordenadas de Textura
        texCoordVboId = glGenBuffers();
        FloatBuffer texCoordBuffer = MemoryUtil.memAllocFloat(texCoords.length);
        texCoordBuffer.put(texCoords).flip();
        glBindBuffer(GL_ARRAY_BUFFER, texCoordVboId);
        glBufferData(GL_ARRAY_BUFFER, texCoordBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
        MemoryUtil.memFree(texCoordBuffer);
        
        // VBO de Normales
        normalVboId = glGenBuffers();
        FloatBuffer normalBuffer = MemoryUtil.memAllocFloat(normals.length);
        normalBuffer.put(normals).flip();
        glBindBuffer(GL_ARRAY_BUFFER, normalVboId);
        glBufferData(GL_ARRAY_BUFFER, normalBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
        MemoryUtil.memFree(normalBuffer);
        
        // VBO de Índices
        indexVboId = glGenBuffers();
        IntBuffer indexBuffer = MemoryUtil.memAllocInt(indices.length);
        indexBuffer.put(indices).flip();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexVboId);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(indexBuffer);
        
        // Habilitar todos los arrays de atributos de vértices
        glEnableVertexAttribArray(0); // Posición
        glEnableVertexAttribArray(1); // Coordenadas de Textura
        glEnableVertexAttribArray(2); // Normal
        
        // Desenlazar VAO
        glBindVertexArray(0);
        
        // Almacenar el conteo de vértices
        vertexCount = indices.length;
    }
    
    /**
     * Renderiza la malla con el shader especificado
     * 
     * @param shader El programa de shader a usar
     */
    public void render(ShaderProgram shader) {
        if (vertexCount == 0 || vaoId == -1) {
            return; // No renderizar mallas vacías
        }
        
        // Establecer la matriz modelo en el shader
        shader.setUniform("modelMatrix", modelMatrix);
        
        // Enlazar al VAO
        glBindVertexArray(vaoId);
        
        // Dibujar la malla
        glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0);
        
        // Desenlazar del VAO
        glBindVertexArray(0);
    }
    
    /**
     * Obtiene la matriz modelo para esta malla de chunk
     * 
     * @return La matriz modelo
     */
    public Matrix4f getModelMatrix() {
        return modelMatrix;
    }
    
    /**
     * Obtiene la coordenada X del chunk en coordenadas de chunk
     * 
     * @return Coordenada X del chunk
     */
    public int getChunkX() {
        return chunkX;
    }
    
    /**
     * Obtiene la coordenada Z del chunk en coordenadas de chunk
     * 
     * @return Coordenada Z del chunk
     */
    public int getChunkZ() {
        return chunkZ;
    }
    
    /**
     * Verifica si esta malla tiene datos de vértices
     * 
     * @return True si la malla contiene vértices
     */
    public boolean hasVertices() {
        return vertexCount > 0 && vaoId != -1;
    }
    
    /**
     * Limpia los recursos utilizados por la malla
     * Seguro para hilos - Puede ser llamado desde cualquier hilo
     */
    public void cleanup() {
        if (vaoId == -1 || isCleanedUp) {
            return; // Malla vacía o ya limpiada
        }
        
        // Si estamos en el hilo principal, limpiar directamente
        // De lo contrario, encolar para limpieza en el hilo principal
        if (org.lwjgl.glfw.GLFW.glfwGetCurrentContext() != 0) {
            // Estamos en el hilo principal con un contexto OpenGL
            cleanupOnMainThread();
        } else {
            // Estamos en un hilo de fondo, encolar para limpieza en el hilo principal
            main.java.com.minefocado.game.MinefocadoGame.queueMeshForCleanup(this);
        }
    }
    
    /**
     * Realiza las operaciones de limpieza de OpenGL (DEBE ser llamado desde el hilo principal)
     * Esto se llama directamente desde cleanup() si estamos en el hilo principal
     * o a través de la cola de limpieza en MinefocadoGame
     */
    public void cleanupOnMainThread() {
        if (vaoId == -1 || isCleanedUp) {
            return; // Malla vacía o ya limpiada
        }
        
        try {
            // Deshabilitar arrays de atributos de vértices
            glDisableVertexAttribArray(0);
            glDisableVertexAttribArray(1);
            glDisableVertexAttribArray(2);
            
            // Eliminar VBOs
            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glDeleteBuffers(posVboId);
            glDeleteBuffers(texCoordVboId);
            glDeleteBuffers(normalVboId);
            glDeleteBuffers(indexVboId);
            
            // Eliminar el VAO
            glBindVertexArray(0);
            glDeleteVertexArrays(vaoId);
            
            // Marcar como limpiada
            isCleanedUp = true;
        } catch (Exception e) {
            System.err.println("Error durante la limpieza de la malla: " + e.getMessage());
        }
    }
}