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
    // OpenGL object IDs
    private final int vaoId;         // Vertex Array Object
    private final int posVboId;      // Position Vertex Buffer Object
    private final int colorVboId;    // Color Vertex Buffer Object
    private final int texCoordVboId; // Texture Coordinate VBO
    private final int normalVboId;   // Normal VBO
    private final int indexVboId;    // Index Buffer Object
    
    // Mesh data
    private final int vertexCount;
    
    // Model matrix for transforming
    private final Matrix4f modelMatrix;
    
    // Chunk position in world space
    private final int chunkX;
    private final int chunkZ;
    
    /**
     * Creates a new chunk mesh with the provided vertex data
     * 
     * @param positions Vertex positions (3 floats per vertex: x, y, z)
     * @param colors Vertex colors (3 floats per vertex: r, g, b)
     * @param texCoords Texture coordinates (2 floats per vertex: u, v)
     * @param normals Vertex normals (3 floats per vertex: nx, ny, nz)
     * @param indices Triangles indices (3 indices per triangle)
     */
    public ChunkMesh(float[] positions, float[] colors, float[] texCoords, 
                   float[] normals, int[] indices) {
        this(0, 0, positions, colors, texCoords, normals, indices);
    }
    
    /**
     * Creates a new chunk mesh with the provided vertex data and chunk position
     * 
     * @param chunkX X coordinate of the chunk
     * @param chunkZ Z coordinate of the chunk
     * @param positions Vertex positions (3 floats per vertex: x, y, z)
     * @param colors Vertex colors (3 floats per vertex: r, g, b)
     * @param texCoords Texture coordinates (2 floats per vertex: u, v)
     * @param normals Vertex normals (3 floats per vertex: nx, ny, nz)
     * @param indices Triangles indices (3 indices per triangle)
     */
    public ChunkMesh(int chunkX, int chunkZ, float[] positions, float[] colors, float[] texCoords, 
                   float[] normals, int[] indices) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        
        // Calculate model matrix based on chunk position
        modelMatrix = new Matrix4f().identity().translate(
                chunkX * Chunk.WIDTH, 
                0, 
                chunkZ * Chunk.DEPTH);
        
        // Create and bind VAO
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
        
        // Color VBO
        colorVboId = glGenBuffers();
        FloatBuffer colorBuffer = MemoryUtil.memAllocFloat(colors.length);
        colorBuffer.put(colors).flip();
        glBindBuffer(GL_ARRAY_BUFFER, colorVboId);
        glBufferData(GL_ARRAY_BUFFER, colorBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
        MemoryUtil.memFree(colorBuffer);
        
        // Texture Coordinates VBO
        texCoordVboId = glGenBuffers();
        FloatBuffer texCoordBuffer = MemoryUtil.memAllocFloat(texCoords.length);
        texCoordBuffer.put(texCoords).flip();
        glBindBuffer(GL_ARRAY_BUFFER, texCoordVboId);
        glBufferData(GL_ARRAY_BUFFER, texCoordBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(2, 2, GL_FLOAT, false, 0, 0);
        MemoryUtil.memFree(texCoordBuffer);
        
        // Normal VBO
        normalVboId = glGenBuffers();
        FloatBuffer normalBuffer = MemoryUtil.memAllocFloat(normals.length);
        normalBuffer.put(normals).flip();
        glBindBuffer(GL_ARRAY_BUFFER, normalVboId);
        glBufferData(GL_ARRAY_BUFFER, normalBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(3, 3, GL_FLOAT, false, 0, 0);
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
        glEnableVertexAttribArray(1); // Color
        glEnableVertexAttribArray(2); // Texture Coordinates
        glEnableVertexAttribArray(3); // Normal
        
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
        if (vertexCount == 0) {
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
     * Cleans up resources used by the mesh
     */
    public void cleanup() {
        // Disable vertex attribute arrays
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glDisableVertexAttribArray(2);
        glDisableVertexAttribArray(3);
        
        // Delete VBOs
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glDeleteBuffers(posVboId);
        glDeleteBuffers(colorVboId);
        glDeleteBuffers(texCoordVboId);
        glDeleteBuffers(normalVboId);
        glDeleteBuffers(indexVboId);
        
        // Delete the VAO
        glBindVertexArray(0);
        glDeleteVertexArrays(vaoId);
    }
}