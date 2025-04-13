package main.java.com.minefocado.game.world.chunk;

import java.util.ArrayList;
import java.util.List;

import main.java.com.minefocado.game.world.blocks.Block;
import main.java.com.minefocado.game.world.blocks.BlockRegistry;

/**
 * Builds a chunk mesh from block data
 */
public class ChunkMeshBuilder {
    // Direction vectors for the 6 faces of a cube
    // Order: UP, DOWN, FRONT, BACK, LEFT, RIGHT
    private static final int[][] FACE_DIRECTIONS = {
        { 0, 1, 0 },  // UP
        { 0, -1, 0 }, // DOWN
        { 0, 0, 1 },  // FRONT
        { 0, 0, -1 }, // BACK
        { -1, 0, 0 }, // LEFT
        { 1, 0, 0 }   // RIGHT
    };
    
    // Vertices for each face (4 vertices per face, 3 coords per vertex)
    private static final float[][][] FACE_VERTICES = {
        // UP face (y = 1)
        {
            { 0, 1, 0 },
            { 1, 1, 0 },
            { 1, 1, 1 },
            { 0, 1, 1 }
        },
        // DOWN face (y = 0)
        {
            { 0, 0, 1 },
            { 1, 0, 1 },
            { 1, 0, 0 },
            { 0, 0, 0 }
        },
        // FRONT face (z = 1)
        {
            { 0, 0, 1 },
            { 0, 1, 1 },
            { 1, 1, 1 },
            { 1, 0, 1 }
        },
        // BACK face (z = 0)
        {
            { 1, 0, 0 },
            { 1, 1, 0 },
            { 0, 1, 0 },
            { 0, 0, 0 }
        },
        // LEFT face (x = 0)
        {
            { 0, 0, 0 },
            { 0, 1, 0 },
            { 0, 1, 1 },
            { 0, 0, 1 }
        },
        // RIGHT face (x = 1)
        {
            { 1, 0, 1 },
            { 1, 1, 1 },
            { 1, 1, 0 },
            { 1, 0, 0 }
        }
    };
    
    // Texture coordinates for each face vertex
    private static final float[][] TEX_COORDS = {
        { 0, 0 },
        { 1, 0 },
        { 1, 1 },
        { 0, 1 }
    };
    
    // Normal vectors for each face
    private static final float[][] NORMALS = {
        { 0, 1, 0 },  // UP
        { 0, -1, 0 }, // DOWN
        { 0, 0, 1 },  // FRONT
        { 0, 0, -1 }, // BACK
        { -1, 0, 0 }, // LEFT
        { 1, 0, 0 }   // RIGHT
    };
    
    // Light intensity for each face (simulated directional lighting)
    private static final float[] FACE_LIGHT = {
        1.0f,  // UP (bright)
        0.4f,  // DOWN (dark)
        0.8f,  // FRONT
        0.8f,  // BACK
        0.6f,  // LEFT
        0.6f   // RIGHT
    };
    
    // Size of texture atlas grid (e.g., 16x16 textures in atlas)
    private static final float TEXTURE_ATLAS_SIZE = 16.0f;
    
    // Block registry reference
    private final BlockRegistry blockRegistry;
    
    public ChunkMeshBuilder() {
        blockRegistry = BlockRegistry.getInstance();
    }
    
    /**
     * Builds mesh data from chunk data (seguro para hilos secundarios)
     * 
     * @param chunk The chunk to build a mesh for
     * @return The constructed chunk mesh data
     */
    public ChunkMeshData buildMeshData(Chunk chunk) {
        List<Float> positions = new ArrayList<>();
        List<Float> colors = new ArrayList<>();
        List<Float> texCoords = new ArrayList<>();
        List<Float> normals = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        
        int indexCount = 0;
        
        // Iterate through all blocks in the chunk
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int y = 0; y < Chunk.HEIGHT; y++) {
                for (int z = 0; z < Chunk.DEPTH; z++) {
                    Block block = chunk.getBlock(x, y, z);
                    
                    // Skip air blocks
                    if (block.getId() == BlockRegistry.AIR_ID) {
                        continue;
                    }
                    
                    // Check each face of the block
                    for (int faceIndex = 0; faceIndex < 6; faceIndex++) {
                        // Get the adjacent block in this direction
                        int nx = x + FACE_DIRECTIONS[faceIndex][0];
                        int ny = y + FACE_DIRECTIONS[faceIndex][1];
                        int nz = z + FACE_DIRECTIONS[faceIndex][2];
                        
                        Block adjacentBlock;
                        
                        // If the adjacent block is outside this chunk, get it from the world
                        if (nx < 0 || nx >= Chunk.WIDTH || nz < 0 || nz >= Chunk.DEPTH || ny < 0 || ny >= Chunk.HEIGHT) {
                            // Por defecto, considerar los bloques fuera del chunk como aire
                            adjacentBlock = blockRegistry.getBlock(BlockRegistry.AIR_ID);
                        } else {
                            adjacentBlock = chunk.getBlock(nx, ny, nz);
                        }
                        
                        // Only add face if it should be rendered
                        if (block.shouldRenderFace(adjacentBlock)) {
                            // Get texture index for this face
                            int textureIndex = block.getTextureIndex(faceIndex);
                            
                            // Calculate texture atlas coordinates
                            float textureX = (textureIndex % (int)TEXTURE_ATLAS_SIZE) / TEXTURE_ATLAS_SIZE;
                            float textureY = (textureIndex / (int)TEXTURE_ATLAS_SIZE) / TEXTURE_ATLAS_SIZE;
                            float textureSizeX = 1.0f / TEXTURE_ATLAS_SIZE;
                            float textureSizeY = 1.0f / TEXTURE_ATLAS_SIZE;
                            
                            // Add face to mesh
                            for (int i = 0; i < 4; i++) {
                                // Position
                                positions.add(x + FACE_VERTICES[faceIndex][i][0]);
                                positions.add(y + FACE_VERTICES[faceIndex][i][1]);
                                positions.add(z + FACE_VERTICES[faceIndex][i][2]);
                                
                                // Color (simple ambient occlusion)
                                float light = FACE_LIGHT[faceIndex];
                                colors.add(light);
                                colors.add(light);
                                colors.add(light);
                                
                                // Texture coordinates with atlas offset
                                texCoords.add(textureX + TEX_COORDS[i][0] * textureSizeX);
                                texCoords.add(textureY + TEX_COORDS[i][1] * textureSizeY);
                                
                                // Normal
                                normals.add(NORMALS[faceIndex][0]);
                                normals.add(NORMALS[faceIndex][1]);
                                normals.add(NORMALS[faceIndex][2]);
                            }
                            
                            // Add indices for two triangles (counter-clockwise)
                            indices.add(indexCount);
                            indices.add(indexCount + 1);
                            indices.add(indexCount + 2);
                            indices.add(indexCount + 2);
                            indices.add(indexCount + 3);
                            indices.add(indexCount);
                            
                            indexCount += 4;
                        }
                    }
                }
            }
        }
        
        // Convert lists to arrays
        float[] posArray = new float[positions.size()];
        float[] colorArray = new float[colors.size()];
        float[] texCoordArray = new float[texCoords.size()];
        float[] normalArray = new float[normals.size()];
        int[] indexArray = new int[indices.size()];
        
        // Fill arrays
        for (int i = 0; i < positions.size(); i++) {
            posArray[i] = positions.get(i);
        }
        
        for (int i = 0; i < colors.size(); i++) {
            colorArray[i] = colors.get(i);
        }
        
        for (int i = 0; i < texCoords.size(); i++) {
            texCoordArray[i] = texCoords.get(i);
        }
        
        for (int i = 0; i < normals.size(); i++) {
            normalArray[i] = normals.get(i);
        }
        
        for (int i = 0; i < indices.size(); i++) {
            indexArray[i] = indices.get(i);
        }
        
        // Create and return the mesh data
        return new ChunkMeshData(
            chunk.getChunkX(), 
            chunk.getChunkZ(), 
            posArray, 
            colorArray, 
            texCoordArray, 
            normalArray, 
            indexArray
        );
    }
    
    /**
     * Método legacy para mantener compatibilidad que convierte datos a mesh
     * SOLO debe llamarse desde el hilo principal
     * 
     * @param chunk El chunk para el que construir un mesh
     * @return El mesh construido
     */
    public ChunkMesh buildMesh(Chunk chunk) {
        ChunkMeshData meshData = buildMeshData(chunk);
        if (meshData.isEmpty()) {
            return new ChunkMesh(
                meshData.getChunkX(), 
                meshData.getChunkZ(), 
                new float[0], new float[0], new float[0], new float[0], new int[0]
            );
        }
        return meshData.createMesh();
    }
}