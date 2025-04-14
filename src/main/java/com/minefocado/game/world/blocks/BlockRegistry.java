package main.java.com.minefocado.game.world.blocks;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for all block types in the game.
 * Manages block IDs and provides lookup functionality.
 */
public class BlockRegistry {
    // Singleton instance
    private static BlockRegistry instance;
    
    // Constants for block IDs
    public static final byte AIR_ID = 0;
    public static final byte STONE_ID = 1;
    public static final byte DIRT_ID = 2;
    public static final byte GRASS_ID = 3;
    public static final byte SAND_ID = 4;
    public static final byte WATER_ID = 5;
    public static final byte BEDROCK_ID = 6;
    public static final byte WOOD_ID = 7;
    public static final byte LEAVES_ID = 8;
    public static final byte GLASS_ID = 9;
    public static final byte COAL_ORE_ID = 10;
    public static final byte IRON_ORE_ID = 11;
    public static final byte GOLD_ORE_ID = 12;
    public static final byte DIAMOND_ORE_ID = 13;
    
    // Maximum number of blocks
    public static final int MAX_BLOCKS = 256;
    
    // Block registry storage
    private final Map<Byte, Block> blocks;
    private final Block[] blocksByID;
    
    /**
     * Get the singleton instance of the registry
     */
    public static synchronized BlockRegistry getInstance() {
        if (instance == null) {
            instance = new BlockRegistry();
        }
        return instance;
    }
    
    /**
     * Private constructor to initialize the registry and register default blocks
     */
    private BlockRegistry() {
        blocks = new HashMap<>();
        blocksByID = new Block[MAX_BLOCKS];
        
        // Register default blocks
        registerDefaultBlocks();
    }
    
    /**
     * Registers all default blocks in the game
     */
    private void registerDefaultBlocks() {
        // Define texture indices for blocks
        // In a real implementation these would be replaced with actual texture IDs
        final int AIR_TEX = 0;
        final int STONE_TEX = 1;
        final int DIRT_TEX = 2;
        final int GRASS_SIDE_TEX = 3;
        final int GRASS_TOP_TEX = 4;
        final int SAND_TEX = 5;
        final int WATER_TEX = 6;
        final int BEDROCK_TEX = 7;
        final int WOOD_SIDE_TEX = 8;
        final int WOOD_END_TEX = 9;
        final int LEAVES_TEX = 10;
        final int GLASS_TEX = 11;
        final int COAL_ORE_TEX = 12;
        final int IRON_ORE_TEX = 13;
        final int GOLD_ORE_TEX = 14;
        final int DIAMOND_ORE_TEX = 15;
        
        // Register blocks with their properties
        // Air - completely transparent, not solid
        registerBlock(new Block(AIR_ID, "Air", false, true, false, AIR_TEX));
        
        // Stone - simple solid block
        registerBlock(new Block(STONE_ID, "Stone", true, false, false, STONE_TEX));
        
        // Dirt - simple solid block
        registerBlock(new Block(DIRT_ID, "Dirt", true, false, false, DIRT_TEX));
        
        // Grass - solid with different top texture
        registerBlock(new Block(GRASS_ID, "Grass", true, false, false, 
                GRASS_TOP_TEX, DIRT_TEX, GRASS_SIDE_TEX, GRASS_SIDE_TEX, GRASS_SIDE_TEX, GRASS_SIDE_TEX));
        
        // Sand - solid block
        registerBlock(new Block(SAND_ID, "Sand", true, false, false, SAND_TEX));
        
        // Water - semi-transparent liquid
        registerBlock(new Block(WATER_ID, "Water", false, true, true, WATER_TEX));
        
        // Bedrock - indestructible solid block
        registerBlock(new Block(BEDROCK_ID, "Bedrock", true, false, false, BEDROCK_TEX));
        
        // Wood - solid with different top/bottom
        registerBlock(new Block(WOOD_ID, "Wood", true, false, false, 
                WOOD_END_TEX, WOOD_END_TEX, WOOD_SIDE_TEX, WOOD_SIDE_TEX, WOOD_SIDE_TEX, WOOD_SIDE_TEX));
        
        // Leaves - semi-transparent foliage
        registerBlock(new Block(LEAVES_ID, "Leaves", true, true, false, LEAVES_TEX));
        
        // Glass - transparent solid
        registerBlock(new Block(GLASS_ID, "Glass", true, true, false, GLASS_TEX));
        
        // Ore blocks - solid with ore textures
        registerBlock(new Block(COAL_ORE_ID, "Coal Ore", true, false, false, COAL_ORE_TEX));
        registerBlock(new Block(IRON_ORE_ID, "Iron Ore", true, false, false, IRON_ORE_TEX));
        registerBlock(new Block(GOLD_ORE_ID, "Gold Ore", true, false, false, GOLD_ORE_TEX));
        registerBlock(new Block(DIAMOND_ORE_ID, "Diamond Ore", true, false, false, DIAMOND_ORE_TEX));
    }
    
    /**
     * Register a new block type
     * 
     * @param block The block to register
     */
    public void registerBlock(Block block) {
        byte id = block.getId();
        
        // Check ID range
        if (id < 0 || id >= MAX_BLOCKS) {
            throw new IllegalArgumentException("Block ID out of range: " + id);
        }
        
        // Check for duplicate ID
        if (blocksByID[id] != null) {
            throw new IllegalArgumentException("Block ID already registered: " + id);
        }
        
        // Register the block
        blocks.put(id, block);
        blocksByID[id] = block;
    }
    
    /**
     * Get a block by its ID
     * 
     * @param id Block ID
     * @return Block instance, or air block if not found
     */
    public Block getBlock(byte id) {
        // Handle invalid IDs
        if (id < 0 || id >= MAX_BLOCKS) {
            System.err.println("Warning: Invalid block ID requested: " + id + ", returning AIR");
            return blocksByID[AIR_ID]; // Return air for invalid IDs
        }
        
        // Handle null blocks (unregistered IDs)
        if (blocksByID[id] == null) {
            System.err.println("Warning: Unregistered block ID requested: " + id + ", returning AIR");
            return blocksByID[AIR_ID];
        }
        
        return blocksByID[id];
    }
    
    /**
     * Get a block by its ID
     * 
     * @param id Block ID as int
     * @return Block instance, or null if not found
     */
    public Block getBlock(int id) {
        return getBlock((byte) id);
    }
    
    /**
     * Get a block by its name
     * 
     * @param name Block name
     * @return Block instance, or null if not found
     */
    public Block getBlockByName(String name) {
        for (Block block : blocks.values()) {
            if (block.getName().equalsIgnoreCase(name)) {
                return block;
            }
        }
        return null;
    }
    
    /**
     * Get the total number of registered blocks
     */
    public int getBlockCount() {
        return blocks.size();
    }
}