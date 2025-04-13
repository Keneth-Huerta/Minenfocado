package main.java.com.minefocado.game.player;

import org.joml.Vector3f;

import main.java.com.minefocado.game.world.World;
import main.java.com.minefocado.game.world.blocks.Block;
import main.java.com.minefocado.game.world.blocks.BlockRegistry;

/**
 * Represents the player in the game world.
 * Handles player movement, physics, collision, and interactions with the world.
 */
public class Player {
    // Player position in world space
    private final Vector3f position;
    
    // Player velocity
    private final Vector3f velocity;
    
    // Player dimensions (hitbox)
    private static final float WIDTH = 0.6f;
    private static final float HEIGHT = 1.8f;
    
    // Player camera (first-person view)
    private final Camera camera;
    
    // Physics constants
    private static final float GRAVITY = 20.0f;
    private static final float JUMP_FORCE = 8.0f;
    private static final float MOVEMENT_SPEED = 4.5f;
    private static final float FLYING_SPEED = 10.0f;
    
    // Reference to the game world
    private final World world;
    
    // Player state
    private boolean onGround;
    private boolean isFlying;
    private int selectedHotbarSlot;
    private final int[] hotbar;
    
    // Ray casting constants for block interaction
    private static final float REACH_DISTANCE = 5.0f;
    private static final float RAY_STEP = 0.05f;
    
    /**
     * Creates a new player at the specified position in the world
     * 
     * @param x Starting X position
     * @param y Starting Y position
     * @param z Starting Z position
     * @param world The game world
     */
    public Player(float x, float y, float z, World world) {
        this.position = new Vector3f(x, y, z);
        this.velocity = new Vector3f(0, 0, 0);
        this.camera = new Camera(x, y + HEIGHT * 0.85f, z); // Eye level
        this.world = world;
        this.onGround = false;
        this.isFlying = false;
        this.selectedHotbarSlot = 0;
        
        // Initialize hotbar with block types (IDs)
        this.hotbar = new int[9];
        hotbar[0] = BlockRegistry.STONE_ID;
        hotbar[1] = BlockRegistry.DIRT_ID;
        hotbar[2] = BlockRegistry.GRASS_ID;
        hotbar[3] = BlockRegistry.SAND_ID;
        hotbar[4] = BlockRegistry.WOOD_ID;
        hotbar[5] = BlockRegistry.LEAVES_ID;
        hotbar[6] = 0; // Empty slot
        hotbar[7] = 0; // Empty slot
        hotbar[8] = 0; // Empty slot
    }
    
    /**
     * Updates player physics, position, and camera
     * 
     * @param deltaTime Time elapsed since the last frame
     */
    public void update(float deltaTime) {
        // Apply gravity unless flying
        if (!isFlying) {
            velocity.y -= GRAVITY * deltaTime;
        } else {
            // Reset vertical velocity when flying
            velocity.y *= 0.9f;
        }
        
        // Limit terminal velocity
        if (velocity.y < -30) {
            velocity.y = -30;
        }
        
        // Apply velocity to position with collision detection
        moveWithCollision(deltaTime);
        
        // Update camera position to follow the player's head
        camera.setPosition(
            position.x,
            position.y + HEIGHT * 0.85f, // Eye level
            position.z
        );
        
        // If player falls below world, teleport to spawn
        if (position.y < -10) {
            position.set(0, 100, 0);
            velocity.set(0, 0, 0);
            camera.setPosition(0, 100 + HEIGHT * 0.85f, 0);
        }
    }
    
    /**
     * Handles player movement input
     * 
     * @param forward W key pressed
     * @param backward S key pressed
     * @param left A key pressed
     * @param right D key pressed
     * @param jump Space key pressed
     * @param crouch Shift key pressed
     * @param deltaTime Time since last frame
     */
    public void handleMovement(boolean forward, boolean backward, boolean left,
                               boolean right, boolean jump, boolean crouch,
                               float deltaTime) {
        // Calculate movement direction based on camera orientation
        float yaw = (float) Math.toRadians(camera.getYaw());
        
        // Calculate forward and right vectors
        float fx = (float) Math.sin(yaw);
        float fz = (float) -Math.cos(yaw);
        float rx = fz;
        float rz = -fx;
        
        // Reset horizontal velocity
        velocity.x = 0;
        velocity.z = 0;
        
        // Calculate current speed (different for flying mode)
        float speed = isFlying ? FLYING_SPEED : MOVEMENT_SPEED;
        
        // Apply movement input
        if (forward) {
            velocity.x += fx * speed;
            velocity.z += fz * speed;
        }
        if (backward) {
            velocity.x -= fx * speed;
            velocity.z -= fz * speed;
        }
        if (left) {
            velocity.x -= rx * speed;
            velocity.z -= rz * speed;
        }
        if (right) {
            velocity.x += rx * speed;
            velocity.z += rz * speed;
        }
        
        // Handle jumping (only when on ground or flying)
        if (jump) {
            if (onGround && !isFlying) {
                velocity.y = JUMP_FORCE;
                onGround = false;
            } else if (isFlying) {
                velocity.y = FLYING_SPEED;
            }
        }
        
        // Handle crouching/descending
        if (crouch && isFlying) {
            velocity.y = -FLYING_SPEED;
        }
    }
    
    /**
     * Moves the player with collision detection
     * 
     * @param deltaTime Time since last frame
     */
    private void moveWithCollision(float deltaTime) {
        // Save original position for restoring in case of collision
        float originalX = position.x;
        float originalY = position.y;
        float originalZ = position.z;
        
        // Move in X direction
        position.x += velocity.x * deltaTime;
        if (checkCollision()) {
            position.x = originalX;
            velocity.x = 0;
        }
        
        // Move in Y direction
        position.y += velocity.y * deltaTime;
        if (checkCollision()) {
            position.y = originalY;
            
            // If moving downward and collision, we've hit the ground
            if (velocity.y < 0) {
                onGround = true;
            }
            
            velocity.y = 0;
        } else {
            // We're in air if we can move vertically
            onGround = false;
        }
        
        // Move in Z direction
        position.z += velocity.z * deltaTime;
        if (checkCollision()) {
            position.z = originalZ;
            velocity.z = 0;
        }
    }
    
    /**
     * Checks if the player is colliding with any solid blocks
     * 
     * @return true if collision detected, false otherwise
     */
    private boolean checkCollision() {
        // Get block positions that overlap with player hitbox
        int minX = (int) Math.floor(position.x - WIDTH / 2);
        int maxX = (int) Math.floor(position.x + WIDTH / 2);
        int minY = (int) Math.floor(position.y);
        int maxY = (int) Math.floor(position.y + HEIGHT);
        int minZ = (int) Math.floor(position.z - WIDTH / 2);
        int maxZ = (int) Math.floor(position.z + WIDTH / 2);
        
        // Check each potential block for collision
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block != null && block.isSolid()) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Breaks the block the player is looking at
     */
    public void breakBlock() {
        // Cast a ray to find the block
        BlockRayResult result = castRay(true);
        if (result != null && result.blockType != BlockRegistry.AIR_ID) {
            // Replace with air
            world.setBlockAt(result.x, result.y, result.z, BlockRegistry.AIR_ID);
        }
    }
    
    /**
     * Places a block adjacent to where the player is looking
     */
    public void placeBlock() {
        // Cast a ray to find the face to place against
        BlockRayResult result = castRay(true);
        if (result != null && result.faceX != 0 || result.faceY != 0 || result.faceZ != 0) {
            // Calculate position to place the new block
            int placeX = result.x + result.faceX;
            int placeY = result.y + result.faceY;
            int placeZ = result.z + result.faceZ;
            
            // Get selected block from hotbar
            int blockId = hotbar[selectedHotbarSlot];
            
            // Don't place if selected slot is empty or the spot is occupied
            if (blockId != 0 && world.getBlockAt(placeX, placeY, placeZ).getId() == BlockRegistry.AIR_ID) {
                // Check for collision with player first
                float px = position.x;
                float py = position.y;
                float pz = position.z;
                
                // Check if placing would cause player to be stuck
                position.x = (placeX + 0.5f);
                position.y = (placeY + 0.5f);
                position.z = (placeZ + 0.5f);
                
                boolean wouldCollide = checkCollision();
                
                // Restore position
                position.x = px;
                position.y = py;
                position.z = pz;
                
                // Only place if it won't cause player to get stuck
                if (!wouldCollide) {
                    world.setBlockAt(placeX, placeY, placeZ, blockId);
                }
            }
        }
    }
    
    /**
     * Casts a ray from the player's viewpoint and returns the first block hit
     * 
     * @param includeOutsideInfo Whether to include face normal information
     * @return Information about the block hit, or null if none hit
     */
    private BlockRayResult castRay(boolean includeOutsideInfo) {
        // Get ray origin (camera position) and direction (camera front vector)
        Vector3f origin = camera.getPosition();
        Vector3f direction = new Vector3f();
        
        // Calculate direction vector from camera angles
        float yaw = (float) Math.toRadians(camera.getYaw());
        float pitch = (float) Math.toRadians(camera.getPitch());
        
        direction.x = (float) (Math.cos(pitch) * Math.sin(yaw));
        direction.y = (float) -Math.sin(pitch);
        direction.z = (float) (Math.cos(pitch) * -Math.cos(yaw));
        direction.normalize();
        
        // Ray step size and current distance
        float distance = 0;
        
        // Previous position (for face normal calculation)
        int lastX = (int) Math.floor(origin.x);
        int lastY = (int) Math.floor(origin.y);
        int lastZ = (int) Math.floor(origin.z);
        
        // Ray marching
        while (distance < REACH_DISTANCE) {
            // Calculate current position
            float x = origin.x + direction.x * distance;
            float y = origin.y + direction.y * distance;
            float z = origin.z + direction.z * distance;
            
            // Convert to block coordinates
            int blockX = (int) Math.floor(x);
            int blockY = (int) Math.floor(y);
            int blockZ = (int) Math.floor(z);
            
            // Check if block is solid
            Block block = world.getBlockAt(blockX, blockY, blockZ);
            if (block != null && block.isSolid()) {
                // Calculate which face was hit (for placing blocks)
                int faceX = 0, faceY = 0, faceZ = 0;
                
                if (includeOutsideInfo) {
                    if (blockX > lastX) faceX = -1;
                    else if (blockX < lastX) faceX = 1;
                    else if (blockY > lastY) faceY = -1;
                    else if (blockY < lastY) faceY = 1;
                    else if (blockZ > lastZ) faceZ = -1;
                    else if (blockZ < lastZ) faceZ = 1;
                }
                
                // Return information about the block hit
                BlockRayResult result = new BlockRayResult();
                result.x = blockX;
                result.y = blockY;
                result.z = blockZ;
                result.blockType = block.getId();
                result.faceX = faceX;
                result.faceY = faceY;
                result.faceZ = faceZ;
                return result;
            }
            
            // Remember current position for face calculation
            lastX = blockX;
            lastY = blockY;
            lastZ = blockZ;
            
            // Step along the ray
            distance += RAY_STEP;
        }
        
        return null;
    }
    
    /**
     * Changes the selected hotbar slot
     * 
     * @param delta Change amount (positive or negative)
     */
    public void changeHotbarSelection(int delta) {
        selectedHotbarSlot = (selectedHotbarSlot + delta + hotbar.length) % hotbar.length;
    }
    
    /**
     * Sets the selected hotbar slot directly
     * 
     * @param slot The slot to select (0-8)
     */
    public void setHotbarSelection(int slot) {
        if (slot >= 0 && slot < hotbar.length) {
            selectedHotbarSlot = slot;
        }
    }
    
    /**
     * Gets the currently selected block
     * 
     * @return The block ID in the selected hotbar slot
     */
    public int getSelectedBlockId() {
        return hotbar[selectedHotbarSlot];
    }
    
    /**
     * Toggles the flying mode
     */
    public void toggleFlying() {
        isFlying = !isFlying;
        
        // Reset vertical velocity when toggling
        velocity.y = 0;
    }
    
    /**
     * Gets the player's camera
     */
    public Camera getCamera() {
        return camera;
    }
    
    /**
     * Gets the player's position
     */
    public Vector3f getPosition() {
        return position;
    }
    
    /**
     * Gets whether the player is on the ground
     */
    public boolean isOnGround() {
        return onGround;
    }
    
    /**
     * Gets whether the player is in flying mode
     */
    public boolean isFlying() {
        return isFlying;
    }
    
    /**
     * Gets the currently selected hotbar slot
     */
    public int getSelectedHotbarSlot() {
        return selectedHotbarSlot;
    }
    
    /**
     * Gets the contents of the hotbar
     */
    public int[] getHotbar() {
        return hotbar;
    }
    
    /**
     * Helper class to store ray cast results
     */
    private static class BlockRayResult {
        public int x, y, z;
        public int blockType;
        public int faceX, faceY, faceZ;
    }
}