package main.java.com.minefocado.game.tests;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import main.java.com.minefocado.game.render.ShaderLoader;
import main.java.com.minefocado.game.render.ShaderProgram;
import main.java.com.minefocado.game.render.Texture;
import main.java.com.minefocado.game.world.blocks.BlockRegistry;
import main.java.com.minefocado.game.world.chunk.Chunk;
import main.java.com.minefocado.game.world.chunk.ChunkMesh;
import main.java.com.minefocado.game.world.chunk.ChunkMeshBuilder;
import main.java.com.minefocado.game.world.World;

/**
 * Clase de prueba para verificar el correcto funcionamiento del sistema de renderizado
 */
public class RenderSystemTest {
    
    // Propiedades de la ventana
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final String TITLE = "Minefocado - Test de Renderizado";
    
    // Variables del sistema de renderizado
    private long window;
    private ShaderProgram shader;
    private Texture texture;
    private ChunkMesh testChunkMesh;
    
    // Variables para la cámara
    private Matrix4f projectionMatrix;
    private Matrix4f viewMatrix;
    
    /**
     * Método principal para ejecutar la prueba
     */
    public static void main(String[] args) {
        new RenderSystemTest().run();
    }
    
    /**
     * Ejecuta la prueba de renderizado
     */
    public void run() {
        System.out.println("Iniciando prueba del sistema de renderizado...");
        
        try {
            init();
            loop();
            cleanup();
        } catch (Exception e) {
            System.err.println("Error en la prueba de renderizado: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Inicializa GLFW, OpenGL y los recursos de renderizado
     */
    private void init() throws Exception {
        // Configurar callback de error de GLFW
        GLFWErrorCallback.createPrint(System.err).set();
        
        // Inicializar GLFW
        if (!glfwInit()) {
            throw new IllegalStateException("No se pudo inicializar GLFW");
        }
        
        // Configurar propiedades de la ventana
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        
        // Crear la ventana
        window = glfwCreateWindow(WIDTH, HEIGHT, TITLE, NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Error al crear la ventana GLFW");
        }
        
        // Centrar la ventana en la pantalla
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            
            glfwGetWindowSize(window, pWidth, pHeight);
            
            GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            
            glfwSetWindowPos(
                window,
                (vidMode.width() - pWidth.get(0)) / 2,
                (vidMode.height() - pHeight.get(0)) / 2
            );
        }
        
        // Hacer el contexto OpenGL actual para este hilo
        glfwMakeContextCurrent(window);
        
        // Habilitar v-sync
        glfwSwapInterval(1);
        
        // Mostrar la ventana
        glfwShowWindow(window);
        
        // Inicializar OpenGL
        GL.createCapabilities();
        
        // Configurar OpenGL
        glClearColor(0.529f, 0.808f, 0.922f, 0.0f); // Color azul cielo
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        
        // Inicializar el registro de bloques
        BlockRegistry.getInstance();
        
        // Inicializar los shaders
        String vertexShaderCode = ShaderLoader.getDefaultVertexShader();
        String fragmentShaderCode = ShaderLoader.getDefaultFragmentShader();
        shader = new ShaderProgram(vertexShaderCode, fragmentShaderCode);
        
        // Crear uniforms con manejo de errores
        try {
            shader.createUniform("projectionMatrix");
            shader.createUniform("viewMatrix");
            shader.createUniform("modelMatrix");
            shader.createUniform("textureSampler");
            shader.createUniform("lightPosition");
            shader.createUniform("viewPosition");
            shader.createUniform("ambientStrength");
        } catch (Exception e) {
            System.out.println("Advertencia al crear uniforms: " + e.getMessage());
            System.out.println("Esto podría afectar la visualización pero la prueba continuará.");
        }
        
        // Cargar textura
        texture = Texture.loadTexture("textures/blocks.png");
        
        // Inicializar matrices de cámara
        projectionMatrix = new Matrix4f().perspective(
            (float) Math.toRadians(70.0f), // FOV
            (float) WIDTH / HEIGHT,        // Aspect ratio
            0.1f,                         // Near plane
            1000.0f                       // Far plane
        );
        
        viewMatrix = new Matrix4f().lookAt(
            new Vector3f(10, 10, 10),    // Posición de la cámara
            new Vector3f(0, 0, 0),       // Punto al que mira
            new Vector3f(0, 1, 0)        // Vector "arriba"
        );
        
        // Crear un chunk de prueba con un patrón simple
        World testWorld = new World(12345);
        Chunk testChunk = new Chunk(testWorld, 0, 0);
        
        // Crear un patrón de bloques para visualizar
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                // Crear suelo plano con diferentes bloques para probar el renderizado
                int blockType = (x + z) % 5;
                byte blockId;
                switch (blockType) {
                    case 0: blockId = BlockRegistry.STONE_ID; break;
                    case 1: blockId = BlockRegistry.DIRT_ID; break;
                    case 2: blockId = BlockRegistry.GRASS_ID; break;
                    case 3: blockId = BlockRegistry.SAND_ID; break;
                    case 4: blockId = BlockRegistry.WOOD_ID; break;
                    default: blockId = BlockRegistry.STONE_ID;
                }
                
                testChunk.setBlockId(x, 0, z, blockId);
                
                // Agregar algunas columnas para probar la profundidad y las caras
                if ((x + z) % 7 == 0) {
                    int height = 1 + ((x * z) % 5);
                    for (int y = 1; y <= height; y++) {
                        testChunk.setBlockId(x, y, z, BlockRegistry.STONE_ID);
                    }
                    // Bloque diferente en la parte superior
                    testChunk.setBlockId(x, height, z, BlockRegistry.LEAVES_ID);
                }
            }
        }
        
        // Generar mesh para el chunk de prueba
        ChunkMeshBuilder meshBuilder = new ChunkMeshBuilder();
        testChunkMesh = meshBuilder.buildMesh(testChunk);
        
        System.out.println("Sistema de renderizado inicializado correctamente");
    }
    
    /**
     * Bucle principal de renderizado
     */
    private void loop() {
        // Para animación de rotación
        float angle = 0;
        long lastTime = System.currentTimeMillis();
        
        // Bucle de renderizado hasta que se cierre la ventana
        while (!glfwWindowShouldClose(window)) {
            // Calcular delta time para animación
            long currentTime = System.currentTimeMillis();
            float deltaTime = (currentTime - lastTime) / 1000.0f;
            lastTime = currentTime;
            
            // Actualizar ángulo de rotación
            angle += deltaTime * 0.5f;
            
            // Actualizar matriz de vista para rotación alrededor del origen
            float radius = 30.0f;
            float camX = (float) (Math.sin(angle) * radius);
            float camZ = (float) (Math.cos(angle) * radius);
            viewMatrix = new Matrix4f().lookAt(
                new Vector3f(camX, 15, camZ),
                new Vector3f(0, 0, 0),
                new Vector3f(0, 1, 0)
            );
            
            // Renderizar frame
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            
            // Usar shader y textura
            shader.bind();
            texture.bind();
            
            // Establecer uniforms comunes
            try {
                shader.setUniform("projectionMatrix", projectionMatrix);
                shader.setUniform("viewMatrix", viewMatrix);
                shader.setUniform("textureSampler", 0);
                shader.setUniform("lightPosition", new Vector3f(100, 100, 100));
                shader.setUniform("viewPosition", new Vector3f(camX, 15, camZ));
                shader.setUniform("ambientStrength", 0.6f);
            } catch (Exception e) {
                // Ignorar errores de uniforms
            }
            
            // Renderizar el mesh de prueba
            if (testChunkMesh != null) {
                testChunkMesh.render(shader);
            }
            
            // Desvincular
            texture.unbind();
            shader.unbind();
            
            // Intercambiar buffers y sondear eventos
            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }
    
    /**
     * Limpia los recursos utilizados
     */
    private void cleanup() {
        if (testChunkMesh != null) {
            testChunkMesh.cleanup();
        }
        
        if (shader != null) {
            shader.cleanup();
        }
        
        if (texture != null) {
            texture.cleanup();
        }
        
        // Liberar ventana y terminar GLFW
        glfwSetWindowCloseCallback(window, null);
        glfwDestroyWindow(window);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }
}