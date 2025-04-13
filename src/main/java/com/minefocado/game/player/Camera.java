package main.java.com.minefocado.game.player;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import static org.lwjgl.glfw.GLFW.*;

/**
 * Represents the first-person camera in the game.
 * Handles positioning, rotation, and view/projection matrices.
 */
public class Camera {
    // Camera position in world space
    private final Vector3f position;
    
    // Camera orientation
    private float yaw;   // Horizontal rotation (left/right)
    private float pitch; // Vertical rotation (up/down)
    
    // View matrix (updated when camera changes)
    private final Matrix4f viewMatrix;
    
    // Projection matrix (updated when window resizes)
    private final Matrix4f projectionMatrix;
    
    // Field of view in degrees
    private float fov = 70.0f;
    
    // Near and far planes for the projection
    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 1000.0f;
    
    // For smooth camera movement
    private static final float MOVEMENT_SPEED = 5.0f;
    private static final float MOUSE_SENSITIVITY = 0.1f;
    
    /**
     * Creates a new camera at the specified position
     * 
     * @param startX Initial X position
     * @param startY Initial Y position
     * @param startZ Initial Z position
     */
    public Camera(float startX, float startY, float startZ) {
        this.position = new Vector3f(startX, startY, startZ);
        this.yaw = 0.0f;    // Default: looking along negative Z
        this.pitch = 0.0f;  // Default: looking straight ahead
        this.viewMatrix = new Matrix4f();
        this.projectionMatrix = new Matrix4f();
        
        // Initialize matrices
        updateViewMatrix();
    }
    
    /**
     * Updates the camera position based on player input
     * 
     * @param forward Whether W key is pressed
     * @param backward Whether S key is pressed
     * @param left Whether A key is pressed
     * @param right Whether D key is pressed
     * @param up Whether Space key is pressed
     * @param down Whether Shift key is pressed
     * @param deltaTime Time since last frame in seconds
     */
    public void move(boolean forward, boolean backward, boolean left, 
                    boolean right, boolean up, boolean down, float deltaTime) {
        float speed = MOVEMENT_SPEED * deltaTime;
        
        // Calculate movement direction based on camera orientation
        if (forward) {
            position.x += Math.sin(Math.toRadians(yaw)) * speed;
            position.z -= Math.cos(Math.toRadians(yaw)) * speed;
        }
        if (backward) {
            position.x -= Math.sin(Math.toRadians(yaw)) * speed;
            position.z += Math.cos(Math.toRadians(yaw)) * speed;
        }
        if (left) {
            position.x -= Math.sin(Math.toRadians(yaw - 90)) * speed;
            position.z += Math.cos(Math.toRadians(yaw - 90)) * speed;
        }
        if (right) {
            position.x -= Math.sin(Math.toRadians(yaw + 90)) * speed;
            position.z += Math.cos(Math.toRadians(yaw + 90)) * speed;
        }
        if (up) {
            position.y += speed;
        }
        if (down) {
            position.y -= speed;
        }
        
        // Update view matrix after position change
        updateViewMatrix();
    }
    
    /**
     * Processes mouse movement to rotate the camera view
     * 
     * @param deltaX Change in mouse X position
     * @param deltaY Change in mouse Y position
     */
    public void rotate(float deltaX, float deltaY) {
        // Update yaw (horizontal rotation)
        yaw += deltaX * MOUSE_SENSITIVITY;
        
        // Update pitch (vertical rotation) with limits to avoid gimbal lock
        pitch -= deltaY * MOUSE_SENSITIVITY;
        if (pitch > 89.0f) {
            pitch = 89.0f;
        }
        if (pitch < -89.0f) {
            pitch = -89.0f;
        }
        
        // Update the view matrix after orientation change
        updateViewMatrix();
    }
    
    /**
     * Updates the view matrix based on current position and orientation
     */
    private void updateViewMatrix() {
        // Reset the view matrix
        viewMatrix.identity();
        
        // Apply pitch rotation
        viewMatrix.rotate((float) Math.toRadians(pitch), 1.0f, 0.0f, 0.0f);
        
        // Apply yaw rotation
        viewMatrix.rotate((float) Math.toRadians(yaw), 0.0f, 1.0f, 0.0f);
        
        // Apply translation (inverse of camera position)
        viewMatrix.translate(-position.x, -position.y, -position.z);
    }
    
    /**
     * Updates the projection matrix for the specified window dimensions
     * 
     * @param width Window width
     * @param height Window height
     */
    public void updateProjectionMatrix(int width, int height) {
        float aspectRatio = (float) width / height;
        projectionMatrix.identity();
        projectionMatrix.perspective((float) Math.toRadians(fov), aspectRatio, Z_NEAR, Z_FAR);
    }
    
    /**
     * Gets the camera's current position
     */
    public Vector3f getPosition() {
        return new Vector3f(position);
    }
    
    /**
     * Sets the camera position
     * 
     * @param x New X position
     * @param y New Y position
     * @param z New Z position
     */
    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
        updateViewMatrix();
    }
    
    /**
     * Gets the camera's current yaw rotation
     */
    public float getYaw() {
        return yaw;
    }
    
    /**
     * Gets the camera's current pitch rotation
     */
    public float getPitch() {
        return pitch;
    }
    
    /**
     * Sets the camera orientation
     * 
     * @param yaw New yaw angle (horizontal)
     * @param pitch New pitch angle (vertical)
     */
    public void setRotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
        updateViewMatrix();
    }
    
    /**
     * Gets the view matrix
     */
    public Matrix4f getViewMatrix() {
        return viewMatrix;
    }
    
    /**
     * Gets the projection matrix
     */
    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }
    
    /**
     * Gets the current field of view in degrees
     */
    public float getFov() {
        return fov;
    }
    
    /**
     * Sets a new field of view in degrees
     * 
     * @param fov The new field of view (between 1 and 120)
     */
    public void setFov(float fov) {
        if (fov >= 1.0f && fov <= 120.0f) {
            this.fov = fov;
        }
    }
}