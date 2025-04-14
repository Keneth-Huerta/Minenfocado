package main.java.com.minefocado.game.world.blocks;

import java.util.HashMap;
import java.util.Map;

/**
 * Registro para todos los tipos de bloques en el juego.
 * Gestiona los IDs de bloques y proporciona funcionalidad de búsqueda.
 */
public class BlockRegistry {
    // Instancia singleton
    private static BlockRegistry instance;
    
    // Constantes para IDs de bloques
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
    
    // Número máximo de bloques
    public static final int MAX_BLOCKS = 256;
    
    // Almacenamiento del registro de bloques
    private final Map<Byte, Block> blocks;
    private final Block[] blocksByID;
    
    /**
     * Obtiene la instancia singleton del registro
     */
    public static synchronized BlockRegistry getInstance() {
        if (instance == null) {
            instance = new BlockRegistry();
        }
        return instance;
    }
    
    /**
     * Constructor privado para inicializar el registro y registrar los bloques predeterminados
     */
    private BlockRegistry() {
        blocks = new HashMap<>();
        blocksByID = new Block[MAX_BLOCKS];
        
        // Registrar bloques predeterminados
        registerDefaultBlocks();
    }
    
    /**
     * Registra todos los bloques predeterminados en el juego
     */
    private void registerDefaultBlocks() {
        // Definir índices de textura para bloques
        // En una implementación real, estos serían reemplazados con IDs de textura reales
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
        
        // Registrar bloques con sus propiedades
        // Aire - completamente transparente, no sólido
        registerBlock(new Block(AIR_ID, "Air", false, true, false, AIR_TEX));
        
        // Piedra - bloque sólido simple
        registerBlock(new Block(STONE_ID, "Stone", true, false, false, STONE_TEX));
        
        // Tierra - bloque sólido simple
        registerBlock(new Block(DIRT_ID, "Dirt", true, false, false, DIRT_TEX));
        
        // Hierba - sólido con diferente textura superior
        registerBlock(new Block(GRASS_ID, "Grass", true, false, false, 
                GRASS_TOP_TEX, DIRT_TEX, GRASS_SIDE_TEX, GRASS_SIDE_TEX, GRASS_SIDE_TEX, GRASS_SIDE_TEX));
        
        // Arena - bloque sólido
        registerBlock(new Block(SAND_ID, "Sand", true, false, false, SAND_TEX));
        
        // Agua - líquido semi-transparente
        registerBlock(new Block(WATER_ID, "Water", false, true, true, WATER_TEX));
        
        // Roca madre - bloque sólido indestructible
        registerBlock(new Block(BEDROCK_ID, "Bedrock", true, false, false, BEDROCK_TEX));
        
        // Madera - sólido con parte superior/inferior diferente
        registerBlock(new Block(WOOD_ID, "Wood", true, false, false, 
                WOOD_END_TEX, WOOD_END_TEX, WOOD_SIDE_TEX, WOOD_SIDE_TEX, WOOD_SIDE_TEX, WOOD_SIDE_TEX));
        
        // Hojas - follaje semi-transparente
        registerBlock(new Block(LEAVES_ID, "Leaves", true, true, false, LEAVES_TEX));
        
        // Vidrio - sólido transparente
        registerBlock(new Block(GLASS_ID, "Glass", true, true, false, GLASS_TEX));
        
        // Bloques de mineral - sólidos con texturas de mineral
        registerBlock(new Block(COAL_ORE_ID, "Coal Ore", true, false, false, COAL_ORE_TEX));
        registerBlock(new Block(IRON_ORE_ID, "Iron Ore", true, false, false, IRON_ORE_TEX));
        registerBlock(new Block(GOLD_ORE_ID, "Gold Ore", true, false, false, GOLD_ORE_TEX));
        registerBlock(new Block(DIAMOND_ORE_ID, "Diamond Ore", true, false, false, DIAMOND_ORE_TEX));
    }
    
    /**
     * Registra un nuevo tipo de bloque
     * 
     * @param block El bloque a registrar
     */
    public void registerBlock(Block block) {
        byte id = block.getId();
        
        // Comprobar rango de ID
        if (id < 0 || id >= MAX_BLOCKS) {
            throw new IllegalArgumentException("ID de bloque fuera de rango: " + id);
        }
        
        // Comprobar ID duplicado
        if (blocksByID[id] != null) {
            throw new IllegalArgumentException("ID de bloque ya registrado: " + id);
        }
        
        // Registrar el bloque
        blocks.put(id, block);
        blocksByID[id] = block;
    }
    
    /**
     * Obtiene un bloque por su ID
     * 
     * @param id ID del bloque
     * @return Instancia del bloque, o null si no se encuentra
     */
    public Block getBlock(byte id) {
        if (id < 0 || id >= MAX_BLOCKS) {
            return blocksByID[AIR_ID]; // Devolver aire para IDs inválidos
        }
        return blocksByID[id];
    }
    
    /**
     * Obtiene un bloque por su ID
     * 
     * @param id ID del bloque como entero
     * @return Instancia del bloque, o null si no se encuentra
     */
    public Block getBlock(int id) {
        return getBlock((byte) id);
    }
    
    /**
     * Obtiene un bloque por su nombre
     * 
     * @param name Nombre del bloque
     * @return Instancia del bloque, o null si no se encuentra
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
     * Obtiene el número total de bloques registrados
     */
    public int getBlockCount() {
        return blocks.size();
    }
}