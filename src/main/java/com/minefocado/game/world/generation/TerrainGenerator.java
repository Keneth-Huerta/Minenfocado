package main.java.com.minefocado.game.world.generation;

import java.util.Random;

import main.java.com.minefocado.game.world.blocks.Block;
import main.java.com.minefocado.game.world.blocks.BlockRegistry;
import main.java.com.minefocado.game.world.chunk.Chunk;

/**
 * Handles terrain generation for the voxel world.
 * Uses Perlin noise to generate realistic terrain.
 */
public class TerrainGenerator {
    // Terrain generation constants
    private static final int SEA_LEVEL = 62;
    private static final int DIRT_DEPTH = 5;
    private static final int BEACH_HEIGHT = 4;
    private static final int STONE_HEIGHT = 48;
    
    // Cave generation constants
    private static final double CAVE_THRESHOLD = 0.3;
    private static final int CAVE_OCTAVES = 3;
    private static final double CAVE_SCALE = 40.0;
    
    // Tree generation constants
    private static final int TREE_HEIGHT_MIN = 4;
    private static final int TREE_HEIGHT_MAX = 7;
    private static final double TREE_PROBABILITY = 0.005; // 0.5% chance per suitable block
    
    // Biome constants
    private static final double BIOME_SCALE = 200.0;
    private static final int BIOME_OCTAVES = 2;
    
    // Biome types
    private static final int BIOME_PLAINS = 0;
    private static final int BIOME_FOREST = 1;
    private static final int BIOME_DESERT = 2;
    private static final int BIOME_MOUNTAINS = 3;
    
    // Height settings per biome (max height, terrain scale, octaves)
    private static final double[][] BIOME_SETTINGS = {
        { 24.0, 100.0, 3 }, // Plains
        { 32.0, 100.0, 3 }, // Forest
        { 16.0, 120.0, 2 }, // Desert
        { 64.0, 80.0, 4 }   // Mountains
    };
    
    // Noise generators
    private final PerlinNoise heightNoise;
    private final PerlinNoise caveNoise;
    private final PerlinNoise biomeNoise;
    
    // Random for non-noise elements (trees, etc.)
    private final Random random;
    private final long seed;
    
    // Block registry reference
    private final BlockRegistry blockRegistry;
    
    /**
     * Creates a new terrain generator with the specified seed
     * 
     * @param seed Random seed for terrain generation
     */
    public TerrainGenerator(long seed) {
        this.seed = seed;
        this.heightNoise = new PerlinNoise(seed);
        this.caveNoise = new PerlinNoise(seed + 1); // Different seed for caves
        this.biomeNoise = new PerlinNoise(seed + 2); // Different seed for biomes
        this.random = new Random(seed);
        this.blockRegistry = BlockRegistry.getInstance();
    }
    
    /**
     * Generates terrain for a chunk
     * 
     * @param chunk The chunk to generate terrain for
     */
    public void generateTerrain(Chunk chunk) {
        int chunkX = chunk.getChunkX();
        int chunkZ = chunk.getChunkZ();
        
        // Fill chunk with air initially
        chunk.fill(BlockRegistry.AIR_ID);
        
        // Generate base terrain
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                int worldX = chunkX * Chunk.WIDTH + x;
                int worldZ = chunkZ * Chunk.DEPTH + z;
                
                // Determine biome for this column
                int biome = getBiomeAt(worldX, worldZ);
                
                // Get base terrain height
                int terrainHeight = getTerrainHeight(worldX, worldZ, biome);
                
                // Bedrock layer at bottom
                chunk.setBlockId(x, 0, z, BlockRegistry.BEDROCK_ID);
                
                // Stone layer
                int stoneHeight = Math.min(terrainHeight, STONE_HEIGHT);
                for (int y = 1; y <= stoneHeight; y++) {
                    // Check for caves
                    if (!isCave(worldX, y, worldZ)) {
                        chunk.setBlockId(x, y, z, BlockRegistry.STONE_ID);
                    }
                }
                
                // Apply biome-specific surface layers
                applyBiomeSurface(chunk, x, z, terrainHeight, biome);
            }
        }
        
        chunk.setGenerated(true);
    }
    
    /**
     * Adds features like trees, plants, etc. to a generated chunk
     * 
     * @param chunk The chunk to populate with features
     */
    public void populateChunk(Chunk chunk) {
        if (!chunk.isGenerated()) {
            return;
        }
        
        int chunkX = chunk.getChunkX();
        int chunkZ = chunk.getChunkZ();
        
        // Random with chunk-specific seed for consistent generation
        Random chunkRandom = new Random(seed + chunkX * 341873128712L + chunkZ * 132897987541L);
        
        // Generate trees
        for (int x = 2; x < Chunk.WIDTH - 2; x++) {
            for (int z = 2; z < Chunk.DEPTH - 2; z++) {
                int worldX = chunkX * Chunk.WIDTH + x;
                int worldZ = chunkZ * Chunk.DEPTH + z;
                
                // Determine biome
                int biome = getBiomeAt(worldX, worldZ);
                
                // Only generate trees in forest biome with higher probability
                // and plains with lower probability
                double treeProbability = 0;
                if (biome == BIOME_FOREST) {
                    treeProbability = TREE_PROBABILITY * 4;
                } else if (biome == BIOME_PLAINS) {
                    treeProbability = TREE_PROBABILITY;
                }
                
                // Find top solid block
                int y;
                for (y = Chunk.HEIGHT - 1; y > 0; y--) {
                    Block block = chunk.getBlock(x, y, z);
                    if (block.isSolid()) {
                        break;
                    }
                }
                
                // If top block is grass and random check passes, generate a tree
                if (chunk.getBlockId(x, y, z) == BlockRegistry.GRASS_ID && 
                        chunkRandom.nextDouble() < treeProbability) {
                    generateTree(chunk, x, y + 1, z, chunkRandom);
                }
            }
        }
        
        chunk.setPopulated(true);
    }
    
    /**
     * Generates a tree at the specified position
     * 
     * @param chunk The chunk to place the tree in
     * @param x Local X position in chunk
     * @param y Y position (should be above ground)
     * @param z Local Z position in chunk
     * @param random Random instance for tree generation
     */
    private void generateTree(Chunk chunk, int x, int y, int z, Random random) {
        // Determine tree height
        int treeHeight = TREE_HEIGHT_MIN + random.nextInt(TREE_HEIGHT_MAX - TREE_HEIGHT_MIN + 1);
        
        // Generate trunk
        for (int trunkY = y; trunkY < y + treeHeight; trunkY++) {
            if (trunkY < 0 || trunkY >= Chunk.HEIGHT) continue;
            chunk.setBlockId(x, trunkY, z, BlockRegistry.WOOD_ID);
        }
        
        // Generate leaves
        int leafRadius = 2;
        int leafBottom = y + treeHeight - 3;
        int leafTop = y + treeHeight;
        
        for (int leafY = leafBottom; leafY <= leafTop; leafY++) {
            if (leafY < 0 || leafY >= Chunk.HEIGHT) continue;
            
            // Smaller radius at top
            int radius = (leafY == leafTop) ? 1 : leafRadius;
            
            for (int leafX = x - radius; leafX <= x + radius; leafX++) {
                for (int leafZ = z - radius; leafZ <= z + radius; leafZ++) {
                    // Skip corners for rounded appearance
                    if ((leafX == x - radius || leafX == x + radius) && 
                        (leafZ == z - radius || leafZ == z + radius)) {
                        continue;
                    }
                    
                    // Skip trunk position
                    if (leafX == x && leafZ == z) {
                        continue;
                    }
                    
                    // If in chunk bounds and not solid block, place leaf
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
     * Applies biome-specific surface blocks
     * 
     * @param chunk The chunk to modify
     * @param x Local X position in chunk
     * @param z Local Z position in chunk
     * @param height Terrain height at this position
     * @param biome Biome type
     */
    private void applyBiomeSurface(Chunk chunk, int x, int z, int height, int biome) {
        // Apply appropriate surface blocks based on biome and height
        if (height < SEA_LEVEL) {
            // Water areas
            for (int y = height + 1; y <= SEA_LEVEL; y++) {
                chunk.setBlockId(x, y, z, BlockRegistry.WATER_ID);
            }
            
            // Underwater surfaces
            if (height >= SEA_LEVEL - BEACH_HEIGHT) {
                // Sand for shallow areas
                chunk.setBlockId(x, height, z, BlockRegistry.SAND_ID);
                
                // Add a layer of sand underneath
                for (int y = height - 1; y > height - 3 && y > 0; y--) {
                    if (chunk.getBlockId(x, y, z) == BlockRegistry.STONE_ID) {
                        chunk.setBlockId(x, y, z, BlockRegistry.SAND_ID);
                    }
                }
            } else {
                // Dirt for deeper areas
                chunk.setBlockId(x, height, z, BlockRegistry.DIRT_ID);
            }
        } else {
            // Above water
            switch (biome) {
                case BIOME_DESERT:
                    // Desert: sand surface with sand underneath
                    chunk.setBlockId(x, height, z, BlockRegistry.SAND_ID);
                    for (int y = height - 1; y > height - 4 && y > 0; y--) {
                        if (chunk.getBlockId(x, y, z) == BlockRegistry.STONE_ID) {
                            chunk.setBlockId(x, y, z, BlockRegistry.SAND_ID);
                        }
                    }
                    break;
                    
                case BIOME_MOUNTAINS:
                    // Mountains: higher areas are stone, lower are grass/dirt
                    if (height > SEA_LEVEL + 20) {
                        // Stone tops for high mountains
                        if (height > SEA_LEVEL + 35) {
                            // No top layer changes, keep as stone
                        } else {
                            // Grass transitions at lower elevations
                            chunk.setBlockId(x, height, z, BlockRegistry.GRASS_ID);
                            chunk.setBlockId(x, height - 1, z, BlockRegistry.DIRT_ID);
                        }
                    } else {
                        // Standard surface
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
                    // Standard grass and dirt
                    if (height <= SEA_LEVEL + BEACH_HEIGHT) {
                        // Beach
                        chunk.setBlockId(x, height, z, BlockRegistry.SAND_ID);
                        for (int y = height - 1; y > height - 3 && y > 0; y--) {
                            if (chunk.getBlockId(x, y, z) == BlockRegistry.STONE_ID) {
                                chunk.setBlockId(x, y, z, BlockRegistry.SAND_ID);
                            }
                        }
                    } else {
                        // Grass with dirt underneath
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
     * Determines the biome type at the specified coordinates
     * 
     * @param worldX X coordinate in world space
     * @param worldZ Z coordinate in world space
     * @return Biome type (0-3)
     */
    public int getBiomeAt(int worldX, int worldZ) {
        // Use Perlin noise for smooth biome transitions
        double biomeValue = biomeNoise.octaveNoise(
                worldX / BIOME_SCALE, 0, worldZ / BIOME_SCALE, BIOME_OCTAVES, 0.5);
        
        // Map noise value to biome type
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
     * Gets the terrain height at the specified coordinates
     * 
     * @param worldX X coordinate in world space
     * @param worldZ Z coordinate in world space
     * @param biome Biome type to use for height calculation
     * @return Y coordinate of surface
     */
    public int getTerrainHeight(int worldX, int worldZ, int biome) {
        // Get biome settings
        double maxHeight = BIOME_SETTINGS[biome][0];
        double scale = BIOME_SETTINGS[biome][1];
        int octaves = (int) BIOME_SETTINGS[biome][2];
        
        // Base height is sea level
        double height = heightNoise.getHeightAt(worldX, worldZ, octaves, scale, maxHeight);
        
        // Add to sea level
        return SEA_LEVEL + (int) height;
    }
    
    /**
     * Determines if a cave should be present at the specified coordinates
     * 
     * @param worldX X coordinate
     * @param worldY Y coordinate
     * @param worldZ Z coordinate
     * @return True if there should be a cave (air block)
     */
    private boolean isCave(int worldX, int worldY, int worldZ) {
        // Don't generate caves above sea level or near the bottom
        if (worldY > SEA_LEVEL - 5 || worldY < 10) {
            return false;
        }
        
        // 3D Perlin noise for cave system
        double caveNoiseSample = caveNoise.octaveNoise(
                worldX / CAVE_SCALE, worldY / CAVE_SCALE, worldZ / CAVE_SCALE, 
                CAVE_OCTAVES, 0.5);
        
        // Use a threshold to determine if this is a cave
        // Higher threshold near the surface for fewer surface cave entrances
        double threshold = CAVE_THRESHOLD;
        if (worldY > SEA_LEVEL - 15) {
            threshold += (worldY - (SEA_LEVEL - 15)) * 0.05;
        }
        
        return caveNoiseSample > threshold;
    }
}