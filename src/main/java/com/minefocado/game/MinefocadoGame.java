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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.Version;
import org.lwjgl.system.MemoryStack;

import main.java.com.minefocado.game.player.Player;
import main.java.com.minefocado.game.world.World;
import main.java.com.minefocado.game.world.blocks.BlockRegistry;
import main.java.com.minefocado.game.world.chunk.ChunkMesh;
import main.java.com.minefocado.game.render.CrosshairRenderer;

/**
 * Clase principal del juego Minefocado
 * Un juego de vóxeles tipo Minecraft con generación de terreno e interacción de jugador.
 */
public class MinefocadoGame {
    
    // Cola estática para operaciones de limpieza de mallas desde hilos en segundo plano
    private static final List<ChunkMesh> meshCleanupQueue = Collections.synchronizedList(new ArrayList<>());
    
    /**
     * Añade una malla a la cola de limpieza para ser eliminada de forma segura en el hilo principal
     * @param mesh La malla que se va a limpiar
     */
    public static void queueMeshForCleanup(ChunkMesh mesh) {
        if (mesh != null) {
            meshCleanupQueue.add(mesh);
        }
    }
	
    public static void main(String[] args) {
        new MinefocadoGame().run();
    }

    // Propiedades de la ventana
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final String TITLE = "Minefocado - Clon de Minecraft";

    // Manejador de la ventana
    private long window;
    
    // Estado del juego
    private World world;
    private Player player;
    
    // Manejo de entrada
    private boolean[] keyPressed = new boolean[GLFW_KEY_LAST + 1];
    private double lastMouseX, lastMouseY;
    private boolean mouseGrabbed = true;
    
    // Control de doble pulsación de espacio
    private long lastSpacePress = 0;
    private static final long DOUBLE_PRESS_TIME = 300; // tiempo máximo entre pulsaciones (milisegundos)
    
    // Temporización del juego
    private float deltaTime;
    private long lastFrameTime;

    // Punto de mira (crosshair) para indicar dónde se está apuntando
    private CrosshairRenderer crosshairRenderer;

    public void run() {
        System.out.println("Starting Minefocado Game using LWJGL " + Version.getVersion());

        init();
        loop();

        // Liberar los callbacks de la ventana y destruir la ventana
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        // Terminar GLFW y liberar el callback de error
        glfwTerminate();
        glfwSetErrorCallback(null).free();
        
        // Liberar recursos del juego
        if (world != null) {
            world.cleanup();
        }
        
        // Limpiar recursos del punto de mira
        if (crosshairRenderer != null) {
            crosshairRenderer.cleanup();
        }
    }

    private void init() {
        // Configurar callback de error
        GLFWErrorCallback.createPrint(System.err).set();

        // Inicializar GLFW
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configurar propiedades de la ventana GLFW
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        
        // Crear la ventana
        window = glfwCreateWindow(WIDTH, HEIGHT, TITLE, NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        // Configurar callback de teclado
        glfwSetKeyCallback(window, this::handleKeyInput);
        
        // Configurar callbacks de ratón
        glfwSetCursorPosCallback(window, this::handleMouseMove);
        glfwSetMouseButtonCallback(window, this::handleMouseButton);
        glfwSetScrollCallback(window, this::handleScroll);
        
        // Configurar callback de redimensionamiento de ventana
        glfwSetFramebufferSizeCallback(window, this::handleWindowResize);

        // Obtener la pila de hilos y empujar un nuevo marco
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);

            // Obtener el tamaño de la ventana pasado a glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight);

            // Obtener la resolución del monitor principal
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            // Centrar la ventana
            glfwSetWindowPos(
                    window,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            );
        } // el marco de la pila se elimina automáticamente

        // Hacer que el contexto OpenGL sea el actual
        glfwMakeContextCurrent(window);
        
        // Habilitar v-sync
        glfwSwapInterval(1);
        
        // Capturar el cursor para controles en primera persona
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);

        // Hacer la ventana visible
        glfwShowWindow(window);
        
        // Inicializar OpenGL
        GL.createCapabilities();
        
        // Configurar estado global de OpenGL
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        
        // Inicializar componentes del juego
        initGame();
    }
    
    private void initGame() {
        // Mostrar una pantalla de carga
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f); // Fondo negro para la pantalla de carga
        
        // Inicializar primero el registro de bloques (singleton)
        BlockRegistry blockRegistry = BlockRegistry.getInstance();
        // Asegurar que el registro de bloques esté inicializado antes de crear el mundo
        System.out.println("Block registry initialized with " + blockRegistry.getBlockCount() + " blocks");
        
        try {
            // Renderizar texto de carga en la pantalla
            renderLoadingScreen("Generando mundo...");
            glfwSwapBuffers(window);
            
            // Crear mundo con semilla aleatoria
            long seed = System.currentTimeMillis();
            world = new World(seed);
            System.out.println("World initialized with seed: " + seed);
            
            // Asegurar que los chunks alrededor del spawn estén cargados antes de continuar
            // Precargar el chunk donde aparecerá el jugador y los chunks alrededor
            world.preloadSpawnArea(0, 0);
            
            // Actualizar pantalla de carga
            renderLoadingScreen("Generando personaje...");
            glfwSwapBuffers(window);
            
            // Crear jugador en la posición (0, 100, 0) - muy por encima del nivel del suelo para asegurar que no aparezca dentro del terreno
            player = new Player(0, 100, 0, world);
            
            // Activar modo de vuelo por defecto
            player.toggleFlying();
            
            // Inicializar matriz de proyección de la cámara
            player.getCamera().updateProjectionMatrix(WIDTH, HEIGHT);
            
            // Inicializar punto de mira (crosshair)
            crosshairRenderer = new CrosshairRenderer();
            
            // Inicializar temporización del juego
            lastFrameTime = System.nanoTime();
            
        } catch (Exception e) {
            System.err.println("Error initializing game: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Renderiza un texto de carga sencillo en la pantalla 
     * @param text El texto a mostrar
     */
    private void renderLoadingScreen(String text) {
        // Esta es una implementación sencilla - en un juego real usarías una biblioteca de UI
        // Para fines de demo, solo mostraremos texto en la consola
        System.out.println(text);
        
        // Podríamos agregar un render básico con OpenGL para mostrar texto,
        // pero para mantenerlo simple, dejamos la pantalla negra con la información en consola
        
        // Procesar eventos mientras carga para evitar que la aplicación parezca bloqueada
        glfwPollEvents();
    }

    private void loop() {
        // Ejecutar el bucle de renderizado hasta que el usuario intente cerrar
        // la ventana o haya presionado la tecla ESCAPE.
        while (!glfwWindowShouldClose(window)) {
            // Calcular delta time
            long currentTime = System.nanoTime();
            deltaTime = (currentTime - lastFrameTime) / 1_000_000_000.0f;
            lastFrameTime = currentTime;
            
            // Limitar deltaTime para prevenir problemas de física después de pausas/puntos de interrupción
            if (deltaTime > 0.1f) deltaTime = 0.1f;
            
            // Procesar entrada
            processInput();
            
            // Actualizar estado del juego
            update();
            
            // Procesar cualquier limpieza de malla pendiente (debe hacerse en el hilo principal)
            processMeshCleanupQueue();
            
            // Renderizar frame
            render();
            
            // Sondear eventos de ventana
            glfwPollEvents();
        }
    }
    
    /**
     * Procesa cualquier limpieza de malla pendiente en el hilo principal con el contexto OpenGL
     */
    private void processMeshCleanupQueue() {
        synchronized (meshCleanupQueue) {
            for (ChunkMesh mesh : meshCleanupQueue) {
                try {
                    mesh.cleanupOnMainThread();
                } catch (Exception e) {
                    System.err.println("Error cleaning up mesh: " + e.getMessage());
                }
            }
            meshCleanupQueue.clear();
        }
    }
    
    private void update() {
        // Actualizar jugador (física, colisiones, etc.)
        player.update(deltaTime);
        
        // Actualizar mundo (carga/descarga de chunks)
        Vector3f playerPos = player.getPosition();
        world.update(playerPos.x, playerPos.z);
        world.updateChunkMeshes();
    }
    
    private void render() {
        // Limpiar el framebuffer
        glClearColor(0.529f, 0.808f, 0.922f, 0.0f); // Color azul cielo
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        
        // Obtener matrices de cámara para renderizado
        Matrix4f projectionMatrix = player.getCamera().getProjectionMatrix();
        Matrix4f viewMatrix = player.getCamera().getViewMatrix();
        Vector3f playerPos = player.getPosition();
        
        // Renderizar mundo (chunks)
        world.render(projectionMatrix, viewMatrix, playerPos);
        
        // Renderizar el punto de mira (crosshair) en el centro de la pantalla
        if (crosshairRenderer != null) {
            crosshairRenderer.render();
        }
        
        // Renderizar UI y HUD (se implementará más tarde)
        
        // Intercambiar los buffers de color
        glfwSwapBuffers(window);
    }
    
    private void processInput() {
        // Procesar entrada de movimiento
        boolean forward = keyPressed[GLFW_KEY_W];
        boolean backward = keyPressed[GLFW_KEY_S];
        // Intercambiar las teclas A y D para corregir el movimiento lateral
        boolean left = keyPressed[GLFW_KEY_D];  // Antes era A
        boolean right = keyPressed[GLFW_KEY_A]; // Antes era D
        boolean jump = keyPressed[GLFW_KEY_SPACE];
        boolean crouch = keyPressed[GLFW_KEY_LEFT_SHIFT];
        
        player.handleMovement(forward, backward, left, right, jump, crouch, deltaTime);
    }
    
    private void handleKeyInput(long window, int key, int scancode, int action, int mods) {
        // Cerrar el juego cuando se presiona Escape
        if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
            glfwSetWindowShouldClose(window, true);
            return;
        }
        
        // Rastrear estado de presión/liberación de teclas
        if (key >= 0 && key <= GLFW_KEY_LAST) {
            if (action == GLFW_PRESS) {
                keyPressed[key] = true;
                
                // Manejar selección de hotbar con teclas numéricas
                if (key >= GLFW_KEY_1 && key <= GLFW_KEY_9) {
                    int slot = key - GLFW_KEY_1;
                    player.setHotbarSelection(slot);
                }
                
                // Detectar doble pulsación de espacio para vuelo
                if (key == GLFW_KEY_SPACE) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastSpacePress < DOUBLE_PRESS_TIME) {
                        // Doble pulsación detectada, alternar modo vuelo
                        player.toggleFlying();
                        
                        // Mostrar mensaje según el estado de vuelo
                        if (player.isFlying()) {
                            System.out.println("Modo de vuelo activado - Pulsa espacio dos veces para desactivar");
                        } else {
                            System.out.println("Modo de vuelo desactivado - Pulsa espacio dos veces para activar");
                        }
                    }
                    lastSpacePress = currentTime;
                }
            } else if (action == GLFW_RELEASE) {
                keyPressed[key] = false;
            }
        }
    }
    
    private void handleMouseMove(long window, double xpos, double ypos) {
        // Solo rotar la cámara si el ratón está capturado
        if (mouseGrabbed) {
            // Calcular delta del ratón
            double deltaX = xpos - lastMouseX;
            double deltaY = ypos - lastMouseY;
            
            // El primer frame no tiene deltas
            if (lastMouseX != 0 && lastMouseY != 0) {
                player.getCamera().rotate((float) deltaX, (float) deltaY);
            }
            
            // Actualizar última posición conocida
            lastMouseX = xpos;
            lastMouseY = ypos;
        }
    }
    
    private void handleMouseButton(long window, int button, int action, int mods) {
        if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
            // Romper bloque con botón izquierdo del ratón
            player.breakBlock();
        } else if (button == GLFW_MOUSE_BUTTON_RIGHT && action == GLFW_PRESS) {
            // Colocar bloque con botón derecho del ratón
            player.placeBlock();
        }
    }
    
    private void handleScroll(long window, double xoffset, double yoffset) {
        // La rueda de desplazamiento cambia la selección de la barra de acceso rápido
        player.changeHotbarSelection((int) -yoffset);
    }
    
    private void handleWindowResize(long window, int width, int height) {
        // Actualizar viewport de OpenGL
        glViewport(0, 0, width, height);
        
        // Actualizar matriz de proyección de la cámara
        player.getCamera().updateProjectionMatrix(width, height);
    }
}