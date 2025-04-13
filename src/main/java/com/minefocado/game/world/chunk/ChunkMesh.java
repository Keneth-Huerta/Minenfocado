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
 * Represents a mesh for rendering a chunk in OpenGL
 * Handles vertex data management and rendering
 */
public class ChunkMesh {
    // OpenGL object IDs - removed 'final' so they can be set to null after cleanup
    private int vaoId;         // Vertex Array Object
    private int posVboId;      // Position Vertex Buffer Object
    private int texCoordVboId; // Texture Coordinate VBO
    private int normalVboId;   // Normal VBO
    private int indexVboId;    // Index Buffer Object
    
    // Flag to track if mesh has been cleaned up
    private boolean isCleanedUp = false;
    
    // Mesh data
    private final int vertexCount;
    
    // Model matrix for transforming
    private final Matrix4f modelMatrix;
    
    // Chunk position in world space
    private final int chunkX;
    private final int chunkZ;
    
    /**
     * Creates a new chunk mesh from the provided ChunkMeshData
     * IMPORTANTE: Este constructor DEBE ser llamado únicamente desde el hilo principal que tiene el contexto OpenGL
     * 
     * @param chunkX X coordinate of the chunk
     * @param chunkZ Z coordinate of the chunk
     * @param meshData Los datos de mesh pre-calculados
     */
    public ChunkMesh(int chunkX, int chunkZ, ChunkMeshData meshData) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        
        // Calculate model matrix based on chunk position
        modelMatrix = new Matrix4f().identity().translate(
                chunkX * Chunk.WIDTH, 
                0, 
                chunkZ * Chunk.DEPTH);
        
        // Get mesh data from ChunkMeshData
        float[] positions = meshData.getPositions();
        float[] normals = meshData.getNormals();
        float[] texCoords = meshData.getTextureCoords();
        int[] indices = meshData.getIndices();

        // Si no hay datos, crear un mesh vacío
        if (!meshData.hasData()) {
            // Initialize with empty data
            this.vaoId = -1;
            this.posVboId = -1;
            this.texCoordVboId = -1;
            this.normalVboId = -1;
            this.indexVboId = -1;
            this.vertexCount = 0;
            return;
        }
        
        // Create and bind VAO - SOLO EN HILO PRINCIPAL
        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);
        
        // Position VBO
        posVboId = glGenBuffers();
        FloatBuffer posBuffer = MemoryUtil.memAllocFloat(positions.length);
        posBuffer.put(positions).flip();
        glBindBuffer(GL_ARRAY_BUFFER, posVboId);
        glBufferData(GL_ARRAY_BUFFER, posBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        MemoryUtil.memFree(posBuffer);
        
        // Texture Coordinates VBO
        texCoordVboId = glGenBuffers();
        FloatBuffer texCoordBuffer = MemoryUtil.memAllocFloat(texCoords.length);
        texCoordBuffer.put(texCoords).flip();
        glBindBuffer(GL_ARRAY_BUFFER, texCoordVboId);
        glBufferData(GL_ARRAY_BUFFER, texCoordBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
        MemoryUtil.memFree(texCoordBuffer);
        
        // Normal VBO
        normalVboId = glGenBuffers();
        FloatBuffer normalBuffer = MemoryUtil.memAllocFloat(normals.length);
        normalBuffer.put(normals).flip();
        glBindBuffer(GL_ARRAY_BUFFER, normalVboId);
        glBufferData(GL_ARRAY_BUFFER, normalBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
        MemoryUtil.memFree(normalBuffer);
        
        // Index VBO
        indexVboId = glGenBuffers();
        IntBuffer indexBuffer = MemoryUtil.memAllocInt(indices.length);
        indexBuffer.put(indices).flip();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexVboId);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(indexBuffer);
        
        // Enable all vertex attribute arrays
        glEnableVertexAttribArray(0); // Position
        glEnableVertexAttribArray(1); // Texture Coordinates
        glEnableVertexAttribArray(2); // Normal
        
        // Unbind VAO
        glBindVertexArray(0);
        
        // Store vertex count
        vertexCount = indices.length;
    }
    
    /**
     * Renders the mesh with the specified shader
     * 
     * @param shader The shader program to use
     */
    public void render(ShaderProgram shader) {
        if (vertexCount == 0 || vaoId == -1) {
            return; // Don't render empty meshes
        }
        
        // Set model matrix in shader
        shader.setUniform("modelMatrix", modelMatrix);
        
        // Bind to the VAO
        glBindVertexArray(vaoId);
        
        // Draw the mesh
        glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0);
        
        // Unbind from the VAO
        glBindVertexArray(0);
    }
    
    /**
     * Gets the model matrix for this chunk mesh
     * 
     * @return The model matrix
     */
    public Matrix4f getModelMatrix() {
        return modelMatrix;
    }
    
    /**
     * Gets the X coordinate of the chunk in chunk coordinates
     * 
     * @return Chunk X coordinate
     */
    public int getChunkX() {
        return chunkX;
    }
    
    /**
     * Gets the Z coordinate of the chunk in chunk coordinates
     * 
     * @return Chunk Z coordinate
     */
    public int getChunkZ() {
        return chunkZ;
    }
    
    /**
     * Check if this mesh has vertex data
     * 
     * @return True if the mesh contains vertices
     */
    public boolean hasVertices() {
        return vertexCount > 0 && vaoId != -1;
    }
    
    /**
     * Cleans up resources used by the mesh
     * Thread-safe - Can be called from any thread
     */
    public void cleanup() {
        if (vaoId == -1 || isCleanedUp) {
            return; // Empty mesh or already cleaned
        }
        
        // If we're on the main thread, clean up directly
        // Otherwise, queue for cleanup on the main thread
        if (org.lwjgl.glfw.GLFW.glfwGetCurrentContext() != 0) {
            // We're on the main thread with an OpenGL context
            cleanupOnMainThread();
        } else {
            // We're on a background thread, queue for cleanup on main thread
            main.java.com.minefocado.game.MinefocadoGame.queueMeshForCleanup(this);
        }
    }
    
    /**
     * Perform actual OpenGL cleanup operations (MUST be called from main thread)
     * This is called either directly from cleanup() if on main thread
     * or via the cleanup queue in MinefocadoGame
     */
    public void cleanupOnMainThread() {
        if (vaoId == -1 || isCleanedUp) {
            return; // Empty mesh or already cleaned up
        }
        
        try {
            // Disable vertex attribute arrays
            glDisableVertexAttribArray(0);
            glDisableVertexAttribArray(1);
            glDisableVertexAttribArray(2);
            
            // Delete VBOs
            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glDeleteBuffers(posVboId);
            glDeleteBuffers(texCoordVboId);
            glDeleteBuffers(normalVboId);
            glDeleteBuffers(indexVboId);
            
            // Delete the VAO
            glBindVertexArray(0);
            glDeleteVertexArrays(vaoId);
            
            // Mark as cleaned up
            isCleanedUp = true;
        } catch (Exception e) {
            System.err.println("Error during mesh cleanup: " + e.getMessage());
        }
    }
}