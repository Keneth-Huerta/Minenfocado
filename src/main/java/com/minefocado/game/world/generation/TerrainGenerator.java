package main.java.com.minefocado.game.world.generation;

import java.util.Random;

import main.java.com.minefocado.game.world.blocks.Block;
import main.java.com.minefocado.game.world.blocks.BlockRegistry;
import main.java.com.minefocado.game.world.chunk.Chunk;

/**
 * Maneja la generación de terreno para el mundo de vóxeles.
 * Utiliza ruido de Perlin para generar terreno realista.
 */
public class TerrainGenerator {
    // Constantes de generación de terreno
    private static final int SEA_LEVEL = 62;
    private static final int DIRT_DEPTH = 5;
    private static final int BEACH_HEIGHT = 4;
    private static final int STONE_HEIGHT = 48;
    
    // Constantes de generación de cuevas
    private static final double CAVE_THRESHOLD = 0.3;
    private static final int CAVE_OCTAVES = 3;
    private static final double CAVE_SCALE = 40.0;
    
    // Constantes de generación de árboles
    private static final int TREE_HEIGHT_MIN = 4;
    private static final int TREE_HEIGHT_MAX = 7;
    private static final double TREE_PROBABILITY = 0.005; // 0.5% de probabilidad por bloque adecuado
    
    // Constantes de bioma
    private static final double BIOME_SCALE = 200.0;
    private static final int BIOME_OCTAVES = 2;
    
    // Tipos de bioma
    private static final int BIOME_PLAINS = 0;
    private static final int BIOME_FOREST = 1;
    private static final int BIOME_DESERT = 2;
    private static final int BIOME_MOUNTAINS = 3;
    
    // Configuración de altura por bioma (altura máxima, escala de terreno, octavas)
    private static final double[][] BIOME_SETTINGS = {
        { 24.0, 100.0, 3 }, // Llanuras
        { 32.0, 100.0, 3 }, // Bosque
        { 16.0, 120.0, 2 }, // Desierto
        { 64.0, 80.0, 4 }   // Montañas
    };
    
    // Generadores de ruido
    private final PerlinNoise heightNoise;
    private final PerlinNoise caveNoise;
    private final PerlinNoise biomeNoise;
    
    // Random para elementos no basados en ruido (árboles, etc.)
    private final Random random;
    private final long seed;
    
    // Referencia al registro de bloques
    private final BlockRegistry blockRegistry;
    
    /**
     * Crea un nuevo generador de terreno con la semilla especificada
     * 
     * @param seed Semilla aleatoria para generación de terreno
     */
    public TerrainGenerator(long seed) {
        this.seed = seed;
        this.heightNoise = new PerlinNoise(seed);
        this.caveNoise = new PerlinNoise(seed + 1); // Semilla diferente para cuevas
        this.biomeNoise = new PerlinNoise(seed + 2); // Semilla diferente para biomas
        this.random = new Random(seed);
        this.blockRegistry = BlockRegistry.getInstance();
    }
    
    /**
     * Genera terreno para un chunk
     * 
     * @param chunk El chunk para generar terreno
     */
    public void generateTerrain(Chunk chunk) {
        int chunkX = chunk.getChunkX();
        int chunkZ = chunk.getChunkZ();
        
        // Llenar chunk con aire inicialmente
        chunk.fill(BlockRegistry.AIR_ID);
        
        // Generar terreno base
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                int worldX = chunkX * Chunk.WIDTH + x;
                int worldZ = chunkZ * Chunk.DEPTH + z;
                
                // Determinar bioma para esta columna
                int biome = getBiomeAt(worldX, worldZ);
                
                // Obtener altura base del terreno
                int terrainHeight = getTerrainHeight(worldX, worldZ, biome);
                
                // Capa de bedrock en el fondo
                chunk.setBlockId(x, 0, z, BlockRegistry.BEDROCK_ID);
                
                // Capa de piedra
                int stoneHeight = Math.min(terrainHeight, STONE_HEIGHT);
                for (int y = 1; y <= stoneHeight; y++) {
                    // Comprobar cuevas
                    if (!isCave(worldX, y, worldZ)) {
                        chunk.setBlockId(x, y, z, BlockRegistry.STONE_ID);
                    }
                }
                
                // Aplicar capas superficiales específicas de bioma
                applyBiomeSurface(chunk, x, z, terrainHeight, biome);
            }
        }
        
        chunk.setGenerated(true);
    }
    
    /**
     * Añade características como árboles, plantas, etc. a un chunk generado
     * 
     * @param chunk El chunk a poblar con características
     */
    public void populateChunk(Chunk chunk) {
        if (!chunk.isGenerated()) {
            return;
        }
        
        int chunkX = chunk.getChunkX();
        int chunkZ = chunk.getChunkZ();
        
        // Random con semilla específica de chunk para generación consistente
        Random chunkRandom = new Random(seed + chunkX * 341873128712L + chunkZ * 132897987541L);
        
        // Generar árboles
        for (int x = 2; x < Chunk.WIDTH - 2; x++) {
            for (int z = 2; z < Chunk.DEPTH - 2; z++) {
                int worldX = chunkX * Chunk.WIDTH + x;
                int worldZ = chunkZ * Chunk.DEPTH + z;
                
                // Determinar bioma
                int biome = getBiomeAt(worldX, worldZ);
                
                // Solo generar árboles en bioma de bosque con mayor probabilidad
                // y llanuras con menor probabilidad
                double treeProbability = 0;
                if (biome == BIOME_FOREST) {
                    treeProbability = TREE_PROBABILITY * 4;
                } else if (biome == BIOME_PLAINS) {
                    treeProbability = TREE_PROBABILITY;
                }
                
                // Encontrar bloque sólido superior
                int y;
                for (y = Chunk.HEIGHT - 1; y > 0; y--) {
                    Block block = chunk.getBlock(x, y, z);
                    if (block.isSolid()) {
                        break;
                    }
                }
                
                // Si el bloque superior es hierba y la comprobación aleatoria pasa, generar un árbol
                if (chunk.getBlockId(x, y, z) == BlockRegistry.GRASS_ID && 
                        chunkRandom.nextDouble() < treeProbability) {
                    generateTree(chunk, x, y + 1, z, chunkRandom);
                }
            }
        }
        
        chunk.setPopulated(true);
    }
    
    /**
     * Genera un árbol en la posición especificada
     * 
     * @param chunk El chunk para colocar el árbol
     * @param x Posición X local en chunk
     * @param y Posición Y (debería ser encima del suelo)
     * @param z Posición Z local en chunk
     * @param random Instancia Random para generación del árbol
     */
    private void generateTree(Chunk chunk, int x, int y, int z, Random random) {
        // Determinar altura del árbol
        int treeHeight = TREE_HEIGHT_MIN + random.nextInt(TREE_HEIGHT_MAX - TREE_HEIGHT_MIN + 1);
        
        // Generar tronco
        for (int trunkY = y; trunkY < y + treeHeight; trunkY++) {
            if (trunkY < 0 || trunkY >= Chunk.HEIGHT) continue;
            chunk.setBlockId(x, trunkY, z, BlockRegistry.WOOD_ID);
        }
        
        // Generar hojas
        int leafRadius = 2;
        int leafBottom = y + treeHeight - 3;
        int leafTop = y + treeHeight;
        
        for (int leafY = leafBottom; leafY <= leafTop; leafY++) {
            if (leafY < 0 || leafY >= Chunk.HEIGHT) continue;
            
            // Radio más pequeño en la cima
            int radius = (leafY == leafTop) ? 1 : leafRadius;
            
            for (int leafX = x - radius; leafX <= x + radius; leafX++) {
                for (int leafZ = z - radius; leafZ <= z + radius; leafZ++) {
                    // Omitir esquinas para apariencia redondeada
                    if ((leafX == x - radius || leafX == x + radius) && 
                        (leafZ == z - radius || leafZ == z + radius)) {
                        continue;
                    }
                    
                    // Omitir posición de tronco
                    if (leafX == x && leafZ == z) {
                        continue;
                    }
                    
                    // Si está dentro de los límites del chunk y no es un bloque sólido, colocar hoja
                    if (leafX >= 0 && leafX < Chunk.WIDTH && leafZ >= 0 && leafZ < Chunk.DEPTH) {
                        if (!chunk.getBlock(leafX, leafY, leafZ).isSolid()) {
                            chunk.setBlockId(leafX, leafY, leafZ, BlockRegistry.LEAVES_ID);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Aplica bloques superficiales específicos de bioma
     * 
     * @param chunk El chunk a modificar
     * @param x Posición X local en chunk
     * @param z Posición Z local en chunk
     * @param height Altura del terreno en esta posición
     * @param biome Tipo de bioma
     */
    private void applyBiomeSurface(Chunk chunk, int x, int z, int height, int biome) {
        // Aplicar bloques superficiales apropiados según bioma y altura
        if (height < SEA_LEVEL) {
            // Áreas de agua
            for (int y = height + 1; y <= SEA_LEVEL; y++) {
                chunk.setBlockId(x, y, z, BlockRegistry.WATER_ID);
            }
            
            // Superficies submarinas
            if (height >= SEA_LEVEL - BEACH_HEIGHT) {
                // Arena para áreas poco profundas
                chunk.setBlockId(x, height, z, BlockRegistry.SAND_ID);
                
                // Añadir una capa de arena debajo
                for (int y = height - 1; y > height - 3 && y > 0; y--) {
                    if (chunk.getBlockId(x, y, z) == BlockRegistry.STONE_ID) {
                        chunk.setBlockId(x, y, z, BlockRegistry.SAND_ID);
                    }
                }
            } else {
                // Tierra para áreas más profundas
                chunk.setBlockId(x, height, z, BlockRegistry.DIRT_ID);
            }
        } else {
            // Por encima del agua
            switch (biome) {
                case BIOME_DESERT:
                    // Desierto: superficie de arena con arena debajo
                    chunk.setBlockId(x, height, z, BlockRegistry.SAND_ID);
                    
                    // Añadir una capa de arena debajo
                    for (int y = height - 1; y > height - 4 && y > 0; y--) {
                        if (chunk.getBlockId(x, y, z) == BlockRegistry.STONE_ID) {
                            chunk.setBlockId(x, y, z, BlockRegistry.SAND_ID);
                        }
                    }
                    break;
                    
                case BIOME_MOUNTAINS:
                    // Montañas: áreas más altas son de piedra, más bajas son de hierba/tierra
                    if (height > SEA_LEVEL + 20) {
                        // Cimas de piedra para montañas altas
                        if (height > SEA_LEVEL + 35) {
                            // Sin cambios en capa superior, mantener como piedra
                        } else {
                            // Transiciones de hierba en elevaciones más bajas
                            chunk.setBlockId(x, height, z, BlockRegistry.GRASS_ID);
                            chunk.setBlockId(x, height - 1, z, BlockRegistry.DIRT_ID);
                        }
                    } else {
                        // Superficie estándar
                        chunk.setBlockId(x, height, z, BlockRegistry.GRASS_ID);
                        for (int y = height - 1; y > height - DIRT_DEPTH && y > 0; y--) {
                            if (chunk.getBlockId(x, y, z) == BlockRegistry.STONE_ID) {
                                chunk.setBlockId(x, y, z, BlockRegistry.DIRT_ID);
                            }
                        }
                    }
                    break;
                    
                case BIOME_PLAINS:
                case BIOME_FOREST:
                default:
                    // Hierba y tierra estándar
                    if (height <= SEA_LEVEL + BEACH_HEIGHT) {
                        // Playa
                        chunk.setBlockId(x, height, z, BlockRegistry.SAND_ID);
                        for (int y = height - 1; y > height - 3 && y > 0; y--) {
                            if (chunk.getBlockId(x, y, z) == BlockRegistry.STONE_ID) {
                                chunk.setBlockId(x, y, z, BlockRegistry.SAND_ID);
                            }
                        }
                    } else {
                        // Hierba con tierra debajo
                        chunk.setBlockId(x, height, z, BlockRegistry.GRASS_ID);
                        for (int y = height - 1; y > height - DIRT_DEPTH && y > 0; y--) {
                            if (chunk.getBlockId(x, y, z) == BlockRegistry.STONE_ID) {
                                chunk.setBlockId(x, y, z, BlockRegistry.DIRT_ID);
                            }
                        }
                    }
                    break;
            }
        }
    }
    
    /**
     * Determina el tipo de bioma en las coordenadas especificadas
     * 
     * @param worldX Coordenada X en espacio del mundo
     * @param worldZ Coordenada Z en espacio del mundo
     * @return Tipo de bioma (0-3)
     */
    public int getBiomeAt(int worldX, int worldZ) {
        // Usar ruido de Perlin para transiciones suaves de bioma
        double biomeValue = biomeNoise.octaveNoise(
                worldX / BIOME_SCALE, 0, worldZ / BIOME_SCALE, BIOME_OCTAVES, 0.5);
        
        // Mapear valor de ruido a tipo de bioma
        if (biomeValue < -0.5) {
            return BIOME_DESERT;
        } else if (biomeValue < 0) {
            return BIOME_PLAINS;
        } else if (biomeValue < 0.5) {
            return BIOME_FOREST;
        } else {
            return BIOME_MOUNTAINS;
        }
    }
    
    /**
     * Obtiene la altura del terreno en las coordenadas especificadas
     * 
     * @param worldX Coordenada X en espacio del mundo
     * @param worldZ Coordenada Z en espacio del mundo
     * @param biome Tipo de bioma a usar para cálculo de altura
     * @return Coordenada Y de superficie
     */
    public int getTerrainHeight(int worldX, int worldZ, int biome) {
        // Obtener configuración para este bioma
        double maxHeight = BIOME_SETTINGS[biome][0];
        double terrainScale = BIOME_SETTINGS[biome][1];
        int octaves = (int) BIOME_SETTINGS[biome][2];
        
        // Usar ruido de Perlin para calcular la altura base
        double noiseHeight = heightNoise.octaveNoise(
                worldX / terrainScale, 0, worldZ / terrainScale, octaves, 0.5);
        
        // Mapear el ruido de altura al rango correcto
        noiseHeight = (noiseHeight + 1.0) / 2.0; // Mapear de [-1,1] a [0,1]
        
        // Ajustar altura base según bioma
        int baseHeight = (int) (noiseHeight * maxHeight);
        
        // Aplicar nivel del mar como base
        return baseHeight + SEA_LEVEL - 10;
    }
    
    /**
     * Determina si debería haber una cueva en las coordenadas especificadas
     * 
     * @param worldX Coordenada X
     * @param worldY Coordenada Y
     * @param worldZ Coordenada Z
     * @return Verdadero si debería haber una cueva (bloque de aire)
     */
    private boolean isCave(int worldX, int worldY, int worldZ) {
        // No generar cuevas por encima del nivel del mar o cerca del fondo
        if (worldY > SEA_LEVEL - 5 || worldY < 10) {
            return false;
        }
        
        // Ruido de Perlin 3D para sistema de cuevas
        double caveNoiseSample = caveNoise.octaveNoise(
                worldX / CAVE_SCALE, worldY / CAVE_SCALE, worldZ / CAVE_SCALE, 
                CAVE_OCTAVES, 0.5);
        
        // Usar un umbral para determinar si esto es una cueva
        // Umbral más alto cerca de la superficie para menos entradas de cueva superficiales
        double threshold = CAVE_THRESHOLD;
        if (worldY > SEA_LEVEL - 15) {
            threshold += (worldY - (SEA_LEVEL - 15)) * 0.05;
        }
        
        return caveNoiseSample > threshold;
    }
}