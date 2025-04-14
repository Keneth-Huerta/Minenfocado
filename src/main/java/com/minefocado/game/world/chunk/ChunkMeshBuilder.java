package main.java.com.minefocado.game.world.chunk;

import java.util.ArrayList;
import java.util.List;

import main.java.com.minefocado.game.world.World;
import main.java.com.minefocado.game.world.blocks.Block;
import main.java.com.minefocado.game.world.blocks.BlockRegistry;

/**
 * Builds a chunk mesh from block data
 */
public class ChunkMeshBuilder {
    // Size of texture atlas grid (e.g., 16x16 textures in atlas)
    private static final float TEXTURE_ATLAS_SIZE = 16.0f;
    
    public ChunkMeshBuilder() {
    }
    
    /**
     * Builds mesh data from chunk data (seguro para hilos secundarios)
     * 
     * @param chunk El chunk para el que construir datos de mesh
     * @return Los datos del mesh construido (sin objetos OpenGL)
     */
    public ChunkMeshData buildMeshData(Chunk chunk) {
        // Check if chunk is null
        if (chunk == null) {
            System.err.println("Warning: Attempted to build mesh for null chunk");
            return new ChunkMeshData(
                new float[0],
                new float[0],
                new float[0],
                new int[0]
            );
        }
        
        List<Float> positions = new ArrayList<>();
        List<Float> texCoords = new ArrayList<>();
        List<Float> normals = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        
        // Iterate through all blocks in the chunk
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int y = 0; y < Chunk.HEIGHT; y++) {
                for (int z = 0; z < Chunk.DEPTH; z++) {
                    Block block = chunk.getBlock(x, y, z);
                    
                    // Skip null blocks or air blocks
                    if (block == null || block.getId() == BlockRegistry.AIR_ID) {
                        continue;
                    }
                    
                    // Añadir todas las caras visibles del bloque a la malla
                    addBlock(chunk, block, x, y, z, positions, indices, texCoords, normals, null);
                    
                    // Continue to iterate to next block
                    continue;
                }
            }
        }
        
        // Convertir listas a arrays
        float[] positionArray = new float[positions.size()];
        float[] textureArray = new float[texCoords.size()];
        float[] normalArray = new float[normals.size()];
        int[] indexArray = new int[indices.size()];
        
        for (int i = 0; i < positions.size(); i++) {
            positionArray[i] = positions.get(i);
        }
        
        for (int i = 0; i < texCoords.size(); i++) {
            textureArray[i] = texCoords.get(i);
        }
        
        for (int i = 0; i < normals.size(); i++) {
            normalArray[i] = normals.get(i);
        }
        
        for (int i = 0; i < indices.size(); i++) {
            indexArray[i] = indices.get(i);
        }
        
        // Crear y retornar los datos del mesh (sin objetos OpenGL)
        return new ChunkMeshData(
            positionArray,
            normalArray,
            textureArray,
            indexArray
        );
    }
    
    /**
     * Determina si se debe renderizar una cara de bloque basado en la visibilidad del bloque adyacente
     * 
     * @param chunk El chunk actual
     * @param blockX Coordenada X del bloque actual
     * @param blockY Coordenada Y del bloque actual
     * @param blockZ Coordenada Z del bloque actual
     * @param faceX Componente X del vector normal de la cara
     * @param faceY Componente Y del vector normal de la cara
     * @param faceZ Componente Z del vector normal de la cara
     * @return true si la cara debe ser visible (bloque adyacente es transparente o no existe)
     */
    private boolean shouldRenderFace(Chunk chunk, int blockX, int blockY, int blockZ, int faceX, int faceY, int faceZ) {
        // Safety check - if chunk is null, always render face
        if (chunk == null) {
            System.err.println("Warning: Null chunk in shouldRenderFace");
            return true;
        }
        
        // Calcular coordenadas del bloque adyacente
        int adjacentX = blockX + faceX;
        int adjacentY = blockY + faceY;
        int adjacentZ = blockZ + faceZ;
        
        // Si está fuera de las coordenadas Y, siempre renderizar (cielo o void)
        if (adjacentY < 0 || adjacentY >= Chunk.HEIGHT) {
            return true;
        }
        
        Block adjacentBlock;
        
        try {
            // Si está dentro del mismo chunk, usar getBlock directo
            if (adjacentX >= 0 && adjacentX < Chunk.WIDTH && adjacentZ >= 0 && adjacentZ < Chunk.DEPTH) {
                adjacentBlock = chunk.getBlock(adjacentX, adjacentY, adjacentZ);
            } else {
                // Si está fuera del chunk, calcular coordenadas del mundo
                int worldX = chunk.getChunkX() * Chunk.WIDTH + adjacentX;
                int worldZ = chunk.getChunkZ() * Chunk.DEPTH + adjacentZ;
                
                // Usar getBlockInWorld que maneja el caso de bloques en chunks vecinos
                adjacentBlock = chunk.getBlockInWorld(worldX, adjacentY, worldZ);
            }
            
            // Si el bloque adyacente no es sólido u opaco, renderizar la cara
            return adjacentBlock == null || !adjacentBlock.isOpaque();
        } catch (Exception e) {
            // Si algo sale mal (por ejemplo, el chunk adyacente no está cargado), 
            // renderizar la cara para evitar agujeros
            System.err.println("Error checking adjacent block: " + e.getMessage());
            return true;
        }
    }
    
    /**
     * Añade una cara a la malla
     * 
     * @param vertices Lista de vértices
     * @param indices Lista de índices
     * @param textureCoords Lista de coordenadas UV
     * @param normals Lista de normales
     * @param v1x Vertice 1 - X
     * @param v1y Vertice 1 - Y
     * @param v1z Vertice 1 - Z
     * @param v2x Vertice 2 - X
     * @param v2y Vertice 2 - Y
     * @param v2z Vertice 2 - Z
     * @param v3x Vertice 3 - X
     * @param v3y Vertice 3 - Y
     * @param v3z Vertice 3 - Z
     * @param v4x Vertice 4 - X
     * @param v4y Vertice 4 - Y
     * @param v4z Vertice 4 - Z
     * @param nx Normal - X
     * @param ny Normal - Y
     * @param nz Normal - Z
     * @param textureIndex Índice de textura
     * @param textureVariant Variante de textura
     */
    private void addFace(List<Float> vertices, List<Integer> indices, 
                       List<Float> textureCoords, List<Float> normals,
                       float v1x, float v1y, float v1z,
                       float v2x, float v2y, float v2z,
                       float v3x, float v3y, float v3z,
                       float v4x, float v4y, float v4z,
                       float nx, float ny, float nz,
                       int textureIndex, int textureVariant) {
        
        // Validate collections to prevent NPEs
        if (vertices == null || indices == null || textureCoords == null || normals == null) {
            System.err.println("Warning: Null collection in addFace method");
            return;
        }
        
        try {
            // Índice inicial
            int startIndex = vertices.size() / 3;
            
            // Añadir los vértices
            vertices.add(v1x);
            vertices.add(v1y);
            vertices.add(v1z);
            
            vertices.add(v2x);
            vertices.add(v2y);
            vertices.add(v2z);
            
            vertices.add(v3x);
            vertices.add(v3y);
            vertices.add(v3z);
            
            vertices.add(v4x);
            vertices.add(v4y);
            vertices.add(v4z);
            
            // Añadir los índices para los dos triángulos que forman la cara
            indices.add(startIndex);
            indices.add(startIndex + 1);
            indices.add(startIndex + 2);
            
            indices.add(startIndex);
            indices.add(startIndex + 2);
            indices.add(startIndex + 3);
            
            // Añadir las coordenadas de textura para cada vértice
            addTextureCoordinates(textureCoords, textureIndex, textureVariant);
            
            // Añadir las normales para cada vértice
            for (int i = 0; i < 4; i++) {
                normals.add(nx);
                normals.add(ny);
                normals.add(nz);
            }
        } catch (Exception e) {
            System.err.println("Error in addFace method: " + e.getMessage());
        }
    }
    
    /**
     * Añade un bloque a la malla si es visible
     * 
     * @param chunk El chunk actual
     * @param block El bloque a añadir
     * @param x Posición X relativa al chunk
     * @param y Posición Y relativa al chunk
     * @param z Posición Z relativa al chunk
     * @param vertices Lista de vértices
     * @param indices Lista de índices
     * @param textureCoords Lista de coordenadas UV
     * @param normals Lista de normales
     * @param world El mundo para comprobar bloques adyacentes
     */
    private void addBlock(Chunk chunk, Block block, int x, int y, int z,
                        List<Float> vertices, List<Integer> indices, 
                        List<Float> textureCoords, List<Float> normals, World world) {
        
        // Validate essential inputs to prevent NPEs
        if (chunk == null || block == null) {
            System.err.println("Warning: Null chunk or block in addBlock method");
            return;
        }
        
        if (vertices == null || indices == null || textureCoords == null || normals == null) {
            System.err.println("Warning: Null collection in addBlock method");
            return;
        }
        
        try {
            // Removed unused worldX and worldZ variables
            
            // Comprobar cada cara del bloque
            // Solo renderizar las caras que están expuestas al aire o a bloques transparentes
            
            // Cara superior (Y+)
            if (shouldRenderFace(chunk, x, y, z, 0, 1, 0)) {
                addFace(vertices, indices, textureCoords, normals,
                        x, y + 1, z,
                        x + 1, y + 1, z,
                        x + 1, y + 1, z + 1,
                        x, y + 1, z + 1,
                        0, 1, 0,
                        block.getTextureIndex(Block.FACE_UP), 0);
            }
            
            // Cara inferior (Y-)
            if (shouldRenderFace(chunk, x, y, z, 0, -1, 0)) {
                addFace(vertices, indices, textureCoords, normals,
                        x, y, z + 1,
                        x + 1, y, z + 1,
                        x + 1, y, z,
                        x, y, z,
                        0, -1, 0,
                        block.getTextureIndex(Block.FACE_DOWN), 0);
            }
            
            // Cara frontal (Z+)
            if (shouldRenderFace(chunk, x, y, z, 0, 0, 1)) {
                addFace(vertices, indices, textureCoords, normals,
                        x, y, z + 1,
                        x, y + 1, z + 1,
                        x + 1, y + 1, z + 1,
                        x + 1, y, z + 1,
                        0, 0, 1,
                        block.getTextureIndex(Block.FACE_FRONT), 0);
            }
            
            // Cara trasera (Z-)
            if (shouldRenderFace(chunk, x, y, z, 0, 0, -1)) {
                addFace(vertices, indices, textureCoords, normals,
                        x + 1, y, z,
                        x + 1, y + 1, z,
                        x, y + 1, z,
                        x, y, z,
                        0, 0, -1,
                        block.getTextureIndex(Block.FACE_BACK), 0);
            }
            
            // Cara derecha (X+)
            if (shouldRenderFace(chunk, x, y, z, 1, 0, 0)) {
                addFace(vertices, indices, textureCoords, normals,
                        x + 1, y, z + 1,
                        x + 1, y + 1, z + 1,
                        x + 1, y + 1, z,
                        x + 1, y, z,
                        1, 0, 0,
                        block.getTextureIndex(Block.FACE_RIGHT), 0);
            }
            
            // Cara izquierda (X-)
            if (shouldRenderFace(chunk, x, y, z, -1, 0, 0)) {
                addFace(vertices, indices, textureCoords, normals,
                        x, y, z,
                        x, y + 1, z,
                        x, y + 1, z + 1,
                        x, y, z + 1,
                        -1, 0, 0,
                        block.getTextureIndex(Block.FACE_LEFT), 0);
            }
        } catch (Exception e) {
            System.err.println("Error in addBlock method: " + e.getMessage());
        }
    }
    
    /**
     * Añade coordenadas de textura para una cara completa
     * 
     * @param textureCoords Lista de coordenadas de textura
     * @param textureIndex Índice de la textura en el atlas
     * @param textureVariant Variante de la textura (rotación)
     */
    private void addTextureCoordinates(List<Float> textureCoords, int textureIndex, int textureVariant) {
        if (textureCoords == null) {
            System.err.println("Warning: Null textureCoords in addTextureCoordinates");
            return;
        }
        
        try {
            // Calculate texture atlas coordinates
            float textureSize = 1.0f / TEXTURE_ATLAS_SIZE;
            float textureU = (textureIndex % TEXTURE_ATLAS_SIZE) * textureSize;
            float textureV = (textureIndex / TEXTURE_ATLAS_SIZE) * textureSize;
            
            // Add the four corners of texture (can be rotated based on variant)
            switch (textureVariant % 4) {
                case 0: // No rotation
                    textureCoords.add(textureU);                   // U0
                    textureCoords.add(textureV + textureSize);     // V0
                    
                    textureCoords.add(textureU + textureSize);     // U1
                    textureCoords.add(textureV + textureSize);     // V1
                    
                    textureCoords.add(textureU + textureSize);     // U2
                    textureCoords.add(textureV);                   // V2
                    
                    textureCoords.add(textureU);                   // U3
                    textureCoords.add(textureV);                   // V3
                    break;
                    
                case 1: // 90 degrees rotation
                    textureCoords.add(textureU);                   // U3
                    textureCoords.add(textureV);                   // V3
                    
                    textureCoords.add(textureU);                   // U0
                    textureCoords.add(textureV + textureSize);     // V0
                    
                    textureCoords.add(textureU + textureSize);     // U1
                    textureCoords.add(textureV + textureSize);     // V1
                    
                    textureCoords.add(textureU + textureSize);     // U2
                    textureCoords.add(textureV);                   // V2
                    break;
                    
                case 2: // 180 degrees rotation
                    textureCoords.add(textureU + textureSize);     // U2
                    textureCoords.add(textureV);                   // V2
                    
                    textureCoords.add(textureU);                   // U3
                    textureCoords.add(textureV);                   // V3
                    
                    textureCoords.add(textureU);                   // U0
                    textureCoords.add(textureV + textureSize);     // V0
                    
                    textureCoords.add(textureU + textureSize);     // U1
                    textureCoords.add(textureV + textureSize);     // V1
                    break;
                    
                case 3: // 270 degrees rotation
                    textureCoords.add(textureU + textureSize);     // U1
                    textureCoords.add(textureV + textureSize);     // V1
                    
                    textureCoords.add(textureU + textureSize);     // U2
                    textureCoords.add(textureV);                   // V2
                    
                    textureCoords.add(textureU);                   // U3
                    textureCoords.add(textureV);                   // V3
                    
                    textureCoords.add(textureU);                   // U0
                    textureCoords.add(textureV + textureSize);     // V0
                    break;
            }
        } catch (Exception e) {
            System.err.println("Error in addTextureCoordinates: " + e.getMessage());
        }
    }
}