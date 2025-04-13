package main.java.com.minefocado.game.world.blocks;

/**
 * Base class for all block types in the voxel world.
 * Defines basic properties like solidity, texture indices, and behavior.
 */
public class Block {
    // Block type constants
    private final byte id;
    private final String name;
    
    // Physical properties
    private final boolean solid;
    private final boolean transparent;
    private final boolean liquid;
    
    // Texture indices for each face
    // Order: UP, DOWN, FRONT, BACK, LEFT, RIGHT
    private final int[] textureIndices;
    
    // Constants for face indices
    public static final int FACE_UP = 0;
    public static final int FACE_DOWN = 1;
    public static final int FACE_FRONT = 2;
    public static final int FACE_BACK = 3;
    public static final int FACE_LEFT = 4;
    public static final int FACE_RIGHT = 5;
    
    /**
     * Creates a new block with the specified properties
     * 
     * @param id Block ID
     * @param name Block name
     * @param solid Whether the block is solid (can be collided with)
     * @param transparent Whether the block is transparent (adjacent faces are rendered)
     * @param liquid Whether the block is a liquid (has special physics)
     * @param textureIndices Texture indices for each face, or a single index for all faces
     */
    public Block(byte id, String name, boolean solid, boolean transparent, boolean liquid, int... textureIndices) {
        this.id = id;
        this.name = name;
        this.solid = solid;
        this.transparent = transparent;
        this.liquid = liquid;
        
        // Copy texture indices, using first index for all faces if only one is provided
        this.textureIndices = new int[6];
        if (textureIndices.length == 1) {
            for (int i = 0; i < 6; i++) {
                this.textureIndices[i] = textureIndices[0];
            }
        } else if (textureIndices.length == 6) {
            System.arraycopy(textureIndices, 0, this.textureIndices, 0, 6);
        } else {
            throw new IllegalArgumentException("Texture indices must be either 1 or 6 values");
        }
    }
    
    /**
     * Gets the block ID
     */
    public byte getId() {
        return id;
    }
    
    /**
     * Gets the block name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Checks if the block is solid
     */
    public boolean isSolid() {
        return solid;
    }
    
    /**
     * Checks if the block is transparent
     */
    public boolean isTransparent() {
        return transparent;
    }
    
    /**
     * Checks if the block is opaque (not transparent)
     */
    public boolean isOpaque() {
        return !transparent;
    }
    
    /**
     * Checks if the block is a liquid
     */
    public boolean isLiquid() {
        return liquid;
    }
    
    /**
     * Gets the texture index for a specific face
     * 
     * @param face Face index (0-5 for UP, DOWN, FRONT, BACK, LEFT, RIGHT)
     * @return Texture index
     */
    public int getTextureIndex(int face) {
        return textureIndices[face];
    }
    
    /**
     * Gets the X texture coordinate for the top face
     * @return X texture coordinate
     */
    public float getTopTextureX() {
        return getTextureIndex(FACE_UP) % 16;  // Assuming 16 textures per row in atlas
    }
    
    /**
     * Gets the Y texture coordinate for the top face
     * @return Y texture coordinate
     */
    public float getTopTextureY() {
        return getTextureIndex(FACE_UP) / 16;  // Assuming 16 textures per row in atlas
    }
    
    /**
     * Gets the X texture coordinate for side faces (using FRONT face)
     * @return X texture coordinate
     */
    public float getSideTextureX() {
        return getTextureIndex(FACE_FRONT) % 16;  // Assuming 16 textures per row in atlas
    }
    
    /**
     * Gets the Y texture coordinate for side faces (using FRONT face)
     * @return Y texture coordinate
     */
    public float getSideTextureY() {
        return getTextureIndex(FACE_FRONT) / 16;  // Assuming 16 textures per row in atlas
    }
    
    /**
     * Gets the X texture coordinate for the bottom face
     * @return X texture coordinate
     */
    public float getBottomTextureX() {
        return getTextureIndex(FACE_DOWN) % 16;  // Assuming 16 textures per row in atlas
    }
    
    /**
     * Gets the Y texture coordinate for the bottom face
     * @return Y texture coordinate
     */
    public float getBottomTextureY() {
        return getTextureIndex(FACE_DOWN) / 16;  // Assuming 16 textures per row in atlas
    }
    
    /**
     * Checks if this face should be rendered when adjacent to the specified block
     * 
     * @param adjacentBlock The block adjacent to this face
     * @return True if the face should be rendered
     */
    public boolean shouldRenderFace(Block adjacentBlock) {
        // Render face if adjacent block is air or transparent
        return adjacentBlock == null || adjacentBlock.isTransparent();
    }
    
    /**
     * Returns a string representation of this block
     */
    @Override
    public String toString() {
        return name;
    }
}