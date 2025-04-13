package main.java.com.minefocado.game;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.GL_BACK;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glCullFace;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.nio.IntBuffer;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.Version;
import org.lwjgl.system.MemoryStack;

import main.java.com.minefocado.game.player.Player;
import main.java.com.minefocado.game.world.World;
import main.java.com.minefocado.game.world.blocks.BlockRegistry;

/**
 * Main game class for the Minefocado game
 * A Minecraft-like voxel game with terrain generation and player interaction.
 */
public class MinefocadoGame {
	
    public static void main(String[] args) {
        new MinefocadoGame().run();
    }

    // Window properties
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final String TITLE = "Minefocado - Minecraft Clone";

    // The window handle
    private long window;
    
    // Game state
    private World world;
    private Player player;
    
    // Input handling
    private boolean[] keyPressed = new boolean[GLFW_KEY_LAST + 1];
    private double lastMouseX, lastMouseY;
    private boolean mouseGrabbed = true;
    
    // Game timing
    private float deltaTime;
    private long lastFrameTime;

w    public void run() {
        System.out.println("Starting Minefocado Game using LWJGL " + Version.getVersion());

        init();
        loop();

        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();
        
        // Clean up game resources
        if (world != null) {
            world.cleanup();
        }
    }

    private void init() {
        // Setup an error callback
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configure GLFW window properties
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        
        // Create the window
        window = glfwCreateWindow(WIDTH, HEIGHT, TITLE, NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        // Setup key callback
        glfwSetKeyCallback(window, this::handleKeyInput);
        
        // Setup mouse callbacks
        glfwSetCursorPosCallback(window, this::handleMouseMove);
        glfwSetMouseButtonCallback(window, this::handleMouseButton);
        glfwSetScrollCallback(window, this::handleScroll);
        
        // Setup window resize callback
        glfwSetFramebufferSizeCallback(window, this::handleWindowResize);

        // Get the thread stack and push a new frame
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight);

            // Get the resolution of the primary monitor
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            // Center the window
            glfwSetWindowPos(
                    window,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            );
        } // the stack frame is popped automatically

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);
        
        // Enable v-sync
        glfwSwapInterval(1);
        
        // Capture the cursor for first-person controls
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);

        // Make the window visible
        glfwShowWindow(window);
        
        // Initialize OpenGL
        GL.createCapabilities();
        
        // Configure global OpenGL state
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        
        // Initialize game components
        initGame();
    }
    
    private void initGame() {
        // Initialize block registry first (singleton)
        BlockRegistry blockRegistry = BlockRegistry.getInstance();
        // Ensure block registry is initialized before creating the world
        System.out.println("Block registry initialized with " + blockRegistry.getBlockCount() + " blocks");
        
        try {
            // Create world with random seed
            long seed = System.currentTimeMillis();
            world = new World(seed);
            System.out.println("World initialized with seed: " + seed);
            
            // Create player at position (0, 100, 0) - well above ground level to ensure no spawning inside terrain
            player = new Player(0, 100, 0, world);
            
            // Initialize camera projection matrix
            player.getCamera().updateProjectionMatrix(WIDTH, HEIGHT);
            
            // Initialize game timing
            lastFrameTime = System.nanoTime();
            
        } catch (Exception e) {
            System.err.println("Error initializing game: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loop() {
        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while (!glfwWindowShouldClose(window)) {
            // Calculate delta time
            long currentTime = System.nanoTime();
            deltaTime = (currentTime - lastFrameTime) / 1_000_000_000.0f;
            lastFrameTime = currentTime;
            
            // Cap deltaTime to prevent physics glitches after pause/breakpoint
            if (deltaTime > 0.1f) deltaTime = 0.1f;
            
            // Process input
            processInput();
            
            // Update game state
            update();
            
            // Render frame
            render();
            
            // Poll for window events
            glfwPollEvents();
        }
    }
    
    private void update() {
        // Update player (physics, collisions, etc.)
        player.update(deltaTime);
        
        // Update world (chunk loading/unloading)
        Vector3f playerPos = player.getPosition();
        world.update(playerPos.x, playerPos.z);
        world.updateChunkMeshes();
    }
    
    private void render() {
        // Clear the framebuffer
        glClearColor(0.529f, 0.808f, 0.922f, 0.0f); // Sky blue color
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        
        // Get camera matrices for rendering
        Matrix4f projectionMatrix = player.getCamera().getProjectionMatrix();
        Matrix4f viewMatrix = player.getCamera().getViewMatrix();
        Vector3f playerPos = player.getPosition();
        
        // Render world (chunks)
        world.render(projectionMatrix, viewMatrix, playerPos);
        
        // Render UI and HUD (will be implemented later)
        
        // Swap the color buffers
        glfwSwapBuffers(window);
    }
    
    private void processInput() {
        // Process movement input
        boolean forward = keyPressed[GLFW_KEY_W];
        boolean backward = keyPressed[GLFW_KEY_S];
        boolean left = keyPressed[GLFW_KEY_A];
        boolean right = keyPressed[GLFW_KEY_D];
        boolean jump = keyPressed[GLFW_KEY_SPACE];
        boolean crouch = keyPressed[GLFW_KEY_LEFT_SHIFT];
        
        player.handleMovement(forward, backward, left, right, jump, crouch, deltaTime);
    }
    
    private void handleKeyInput(long window, int key, int scancode, int action, int mods) {
        // Close the game when Escape is pressed
        if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
            glfwSetWindowShouldClose(window, true);
            return;
        }
        
        // Track key press/release state
        if (key >= 0 && key <= GLFW_KEY_LAST) {
            if (action == GLFW_PRESS) {
                keyPressed[key] = true;
                
                // Handle hotbar selection with number keys
                if (key >= GLFW_KEY_1 && key <= GLFW_KEY_9) {
                    int slot = key - GLFW_KEY_1;
                    player.setHotbarSelection(slot);
                }
                
                // Toggle flying mode with F key
                if (key == GLFW_KEY_F) {
                    player.toggleFlying();
                }
            } else if (action == GLFW_RELEASE) {
                keyPressed[key] = false;
            }
        }
    }
    
    private void handleMouseMove(long window, double xpos, double ypos) {
        // Only rotate camera if mouse is grabbed
        if (mouseGrabbed) {
            // Calculate mouse delta
            double deltaX = xpos - lastMouseX;
            double deltaY = ypos - lastMouseY;
            
            // First frame has no deltas
            if (lastMouseX != 0 && lastMouseY != 0) {
                player.getCamera().rotate((float) deltaX, (float) deltaY);
            }
            
            // Update last known position
            lastMouseX = xpos;
            lastMouseY = ypos;
        }
    }
    
    private void handleMouseButton(long window, int button, int action, int mods) {
        if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
            // Break block with left mouse button
            player.breakBlock();
        } else if (button == GLFW_MOUSE_BUTTON_RIGHT && action == GLFW_PRESS) {
            // Place block with right mouse button
            player.placeBlock();
        }
    }
    
    private void handleScroll(long window, double xoffset, double yoffset) {
        // Scroll wheel changes hotbar selection
        player.changeHotbarSelection((int) -yoffset);
    }
    
    private void handleWindowResize(long window, int width, int height) {
        // Update OpenGL viewport
        glViewport(0, 0, width, height);
        
        // Update camera projection matrix
        player.getCamera().updateProjectionMatrix(width, height);
    }
}