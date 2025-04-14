package main.java.com.minefocado.game.world.chunk;

import java.util.ArrayList;
import java.util.List;

import main.java.com.minefocado.game.world.blocks.Block;
import main.java.com.minefocado.game.world.blocks.BlockRegistry;

/**
 * Construye una malla de chunk a partir de datos de bloques
 */
public class ChunkMeshBuilder {
    // Vectores de dirección para las 6 caras de un cubo
    // Orden: ARRIBA, ABAJO, FRENTE, ATRÁS, IZQUIERDA, DERECHA
    private static final int[][] FACE_DIRECTIONS = {
        { 0, 1, 0 },  // ARRIBA
        { 0, -1, 0 }, // ABAJO
        { 0, 0, 1 },  // FRENTE
        { 0, 0, -1 }, // ATRÁS
        { -1, 0, 0 }, // IZQUIERDA
        { 1, 0, 0 }   // DERECHA
    };
    
    // Vértices para cada cara (4 vértices por cara, 3 coordenadas por vértice)
    private static final float[][][] FACE_VERTICES = {
        // Cara ARRIBA (y = 1)
        {
            { 0, 1, 0 },
            { 1, 1, 0 },
            { 1, 1, 1 },
            { 0, 1, 1 }
        },
        // Cara ABAJO (y = 0)
        {
            { 0, 0, 1 },
            { 1, 0, 1 },
            { 1, 0, 0 },
            { 0, 0, 0 }
        },
        // Cara FRENTE (z = 1)
        {
            { 0, 0, 1 },
            { 0, 1, 1 },
            { 1, 1, 1 },
            { 1, 0, 1 }
        },
        // Cara ATRÁS (z = 0)
        {
            { 1, 0, 0 },
            { 1, 1, 0 },
            { 0, 1, 0 },
            { 0, 0, 0 }
        },
        // Cara IZQUIERDA (x = 0)
        {
            { 0, 0, 0 },
            { 0, 1, 0 },
            { 0, 1, 1 },
            { 0, 0, 1 }
        },
        // Cara DERECHA (x = 1)
        {
            { 1, 0, 1 },
            { 1, 1, 1 },
            { 1, 1, 0 },
            { 1, 0, 0 }
        }
    };
    
    // Coordenadas de textura para cada vértice de la cara
    private static final float[][] TEX_COORDS = {
        { 0, 0 },
        { 1, 0 },
        { 1, 1 },
        { 0, 1 }
    };
    
    // Vectores normales para cada cara
    private static final float[][] NORMALS = {
        { 0, 1, 0 },  // ARRIBA
        { 0, -1, 0 }, // ABAJO
        { 0, 0, 1 },  // FRENTE
        { 0, 0, -1 }, // ATRÁS
        { -1, 0, 0 }, // IZQUIERDA
        { 1, 0, 0 }   // DERECHA
    };
    
    // Intensidad de luz para cada cara (iluminación direccional simulada)
    private static final float[] FACE_LIGHT = {
        1.0f,  // ARRIBA (brillante)
        0.4f,  // ABAJO (oscuro)
        0.8f,  // FRENTE
        0.8f,  // ATRÁS
        0.6f,  // IZQUIERDA
        0.6f   // DERECHA
    };
    
    // Tamaño de la cuadrícula del atlas de texturas (por ejemplo, 16x16 texturas en el atlas)
    private static final float TEXTURE_ATLAS_SIZE = 16.0f;
    
    // Referencia al registro de bloques
    private final BlockRegistry blockRegistry;
    
    public ChunkMeshBuilder() {
        blockRegistry = BlockRegistry.getInstance();
    }
    
    /**
     * Construye datos de malla a partir de datos de chunk (seguro para hilos secundarios)
     * 
     * @param chunk El chunk para el que construir datos de malla
     * @return Los datos de la malla construida (sin objetos OpenGL)
     */
    public ChunkMeshData buildMeshData(Chunk chunk) {
        List<Float> positions = new ArrayList<>();
        List<Float> texCoords = new ArrayList<>();
        List<Float> normals = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        
        int indexCount = 0;
        
        // Iterar a través de todos los bloques en el chunk
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int y = 0; y < Chunk.HEIGHT; y++) {
                for (int z = 0; z < Chunk.DEPTH; z++) {
                    Block block = chunk.getBlock(x, y, z);
                    
                    // Saltar bloques de aire
                    if (block.getId() == BlockRegistry.AIR_ID) {
                        continue;
                    }
                    
                    // Añadir todas las caras visibles del bloque a la malla
                    addBlock(chunk, x, y, z, positions, indices, texCoords, normals, block);
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
        
        // Crear y retornar los datos de la malla (sin objetos OpenGL)
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
            return true;
        }
    }
    
    /**
     * Añade una cara de cubo con los vértices dados
     * 
     * @param vertices Lista de vértices a llenar
     * @param indices Lista de índices a llenar
     * @param textureCoords Lista de coordenadas de textura a llenar
     * @param normals Lista de normales a llenar
     * @param x1,y1,z1,x2,y2,z2,x3,y3,z3,x4,y4,z4 Coordenadas de los cuatro vértices de la cara
     * @param nx,ny,nz Componentes del vector normal
     * @param textureX,textureY Coordenada de textura en el atlas para esta cara de bloque
     */
    private void addFace(List<Float> vertices, List<Integer> indices, List<Float> textureCoords, List<Float> normals,
                        float x1, float y1, float z1,
                        float x2, float y2, float z2,
                        float x3, float y3, float z3,
                        float x4, float y4, float z4,
                        float nx, float ny, float nz,
                        float textureX, float textureY) {
        
        // Índice base para esta cara
        int baseIndex = vertices.size() / 3;
        
        // Añadir vértices
        vertices.add(x1); vertices.add(y1); vertices.add(z1);
        vertices.add(x2); vertices.add(y2); vertices.add(z2);
        vertices.add(x3); vertices.add(y3); vertices.add(z3);
        vertices.add(x4); vertices.add(y4); vertices.add(z4);
        
        // Añadir índices (dos triángulos por cara)
        indices.add(baseIndex);
        indices.add(baseIndex + 1);
        indices.add(baseIndex + 2);
        
        indices.add(baseIndex);
        indices.add(baseIndex + 2);
        indices.add(baseIndex + 3);
        
        // Calcular coordenadas de textura
        float textureSize = 1.0f / 16.0f; // 16 texturas en el atlas
        float texLeft = textureX * textureSize;
        float texRight = texLeft + textureSize;
        float texTop = textureY * textureSize;
        float texBottom = texTop + textureSize;
        
        // Añadir coordenadas de textura para los 4 vértices
        textureCoords.add(texLeft);  textureCoords.add(texBottom); // BL
        textureCoords.add(texRight); textureCoords.add(texBottom); // BR
        textureCoords.add(texRight); textureCoords.add(texTop);    // TR
        textureCoords.add(texLeft);  textureCoords.add(texTop);    // TL
        
        // Añadir normales para cada vértice
        for (int i = 0; i < 4; i++) {
            normals.add(nx);
            normals.add(ny);
            normals.add(nz);
        }
    }
    
    /**
     * Añade todas las caras visibles de un bloque a la malla
     * 
     * @param chunk El chunk actual
     * @param blockX Coordenada X del bloque
     * @param blockY Coordenada Y del bloque
     * @param blockZ Coordenada Z del bloque
     * @param vertices Lista de vértices a llenar
     * @param indices Lista de índices a llenar
     * @param textureCoords Lista de coordenadas de textura a llenar
     * @param normals Lista de normales a llenar
     * @param block El bloque a renderizar
     */
    private void addBlock(Chunk chunk, int blockX, int blockY, int blockZ, 
                     List<Float> vertices, List<Integer> indices, 
                     List<Float> textureCoords, List<Float> normals,
                     Block block) {
        
        // Posición del bloque en el espacio del mundo
        float worldX = blockX;
        float worldY = blockY;
        float worldZ = blockZ;
        
        // Tamaño del bloque
        float size = 0.5f;
        
        // Cada cara del cubo tiene coordenadas diferentes para su textura
        float topTexX = block.getTopTextureX();
        float topTexY = block.getTopTextureY();
        float sideTexX = block.getSideTextureX();
        float sideTexY = block.getSideTextureY();
        float bottomTexX = block.getBottomTextureX();
        float bottomTexY = block.getBottomTextureY();
        
        // Añadir caras visibles (comprobando oclusión de cada cara)
        
        // Cara superior (+Y)
        if (shouldRenderFace(chunk, blockX, blockY, blockZ, 0, 1, 0)) {
            addFace(vertices, indices, textureCoords, normals,
                worldX - size, worldY + size, worldZ - size,  // x1,y1,z1
                worldX - size, worldY + size, worldZ + size,  // x2,y2,z2
                worldX + size, worldY + size, worldZ + size,  // x3,y3,z3
                worldX + size, worldY + size, worldZ - size,  // x4,y4,z4
                0.0f, 1.0f, 0.0f,                             // nx,ny,nz (normal)
                topTexX, topTexY                              // textureX,textureY
            );
        }
        
        // Cara inferior (-Y)
        if (shouldRenderFace(chunk, blockX, blockY, blockZ, 0, -1, 0)) {
            addFace(vertices, indices, textureCoords, normals,
                worldX - size, worldY - size, worldZ + size,  // x1,y1,z1
                worldX - size, worldY - size, worldZ - size,  // x2,y2,z2
                worldX + size, worldY - size, worldZ - size,  // x3,y3,z3
                worldX + size, worldY - size, worldZ + size,  // x4,y4,z4
                0.0f, -1.0f, 0.0f,                            // nx,ny,nz (normal)
                bottomTexX, bottomTexY                        // textureX,textureY
            );
        }
        
        // Cara frontal (+Z)
        if (shouldRenderFace(chunk, blockX, blockY, blockZ, 0, 0, 1)) {
            addFace(vertices, indices, textureCoords, normals,
                worldX - size, worldY - size, worldZ + size,  // x1,y1,z1
                worldX + size, worldY - size, worldZ + size,  // x2,y2,z2
                worldX + size, worldY + size, worldZ + size,  // x3,y3,z3
                worldX - size, worldY + size, worldZ + size,  // x4,y4,z4
                0.0f, 0.0f, 1.0f,                             // nx,ny,nz (normal)
                sideTexX, sideTexY                            // textureX,textureY
            );
        }
        
        // Cara trasera (-Z)
        if (shouldRenderFace(chunk, blockX, blockY, blockZ, 0, 0, -1)) {
            addFace(vertices, indices, textureCoords, normals,
                worldX + size, worldY - size, worldZ - size,  // x1,y1,z1
                worldX - size, worldY - size, worldZ - size,  // x2,y2,z2
                worldX - size, worldY + size, worldZ - size,  // x3,y3,z3
                worldX + size, worldY + size, worldZ - size,  // x4,y4,z4
                0.0f, 0.0f, -1.0f,                            // nx,ny,nz (normal)
                sideTexX, sideTexY                            // textureX,textureY
            );
        }
        
        // Cara derecha (+X)
        if (shouldRenderFace(chunk, blockX, blockY, blockZ, 1, 0, 0)) {
            addFace(vertices, indices, textureCoords, normals,
                worldX + size, worldY - size, worldZ + size,  // x1,y1,z1
                worldX + size, worldY - size, worldZ - size,  // x2,y2,z2
                worldX + size, worldY + size, worldZ - size,  // x3,y3,z3
                worldX + size, worldY + size, worldZ + size,  // x4,y4,z4
                1.0f, 0.0f, 0.0f,                             // nx,ny,nz (normal)
                sideTexX, sideTexY                            // textureX,textureY
            );
        }
        
        // Cara izquierda (-X)
        if (shouldRenderFace(chunk, blockX, blockY, blockZ, -1, 0, 0)) {
            addFace(vertices, indices, textureCoords, normals,
                worldX - size, worldY - size, worldZ - size,  // x1,y1,z1
                worldX - size, worldY - size, worldZ + size,  // x2,y2,z2
                worldX - size, worldY + size, worldZ + size,  // x3,y3,z3
                worldX - size, worldY + size, worldZ - size,  // x4,y4,z4
                -1.0f, 0.0f, 0.0f,                            // nx,ny,nz (normal)
                sideTexX, sideTexY                            // textureX,textureY
            );
        }
    }
}