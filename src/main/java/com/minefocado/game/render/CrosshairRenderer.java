package main.java.com.minefocado.game.render;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Renderiza un punto de mira (crosshair) en el centro de la pantalla
 * para indicar dónde se está apuntando al interactuar con bloques.
 */
public class CrosshairRenderer {
    
    // Identificadores de OpenGL
    private int vaoId;
    private int vboId;
    private ShaderProgram shaderProgram;
    
    // Datos del vértice (un simple punto)
    private final float[] vertices = {
        0.0f, 0.0f, 0.0f  // Un punto en el centro
    };
    
    // Tamaño del punto
    private float pointSize = 10.0f;
    
    /**
     * Crea un nuevo renderizador de punto de mira
     */
    public CrosshairRenderer() {
        init();
    }
    
    /**
     * Inicializa los recursos de OpenGL
     */
    private void init() {
        try {
            // Crear shader para punto de mira
            String vertexShaderCode = 
                "#version 330 core\n" +
                "layout (location = 0) in vec3 position;\n" +
                "uniform float pointSize;\n" +
                "void main() {\n" +
                "    gl_Position = vec4(position, 1.0);\n" +
                "    gl_PointSize = pointSize;\n" +
                "}";
                
            String fragmentShaderCode = 
                "#version 330 core\n" +
                "out vec4 color;\n" +
                "void main() {\n" +
                "    float dist = length(gl_PointCoord - vec2(0.5, 0.5)) * 2.0;\n" +
                "    if (dist > 0.5 && dist < 0.8) {\n" +
                "        color = vec4(1.0, 1.0, 1.0, 1.0);\n" +
                "    } else if (dist <= 0.5) {\n" +
                "        color = vec4(0.0, 0.0, 0.0, 1.0);\n" +
                "    } else {\n" +
                "        discard; // Transparente\n" +
                "    }\n" +
                "}";
            
            shaderProgram = new ShaderProgram(vertexShaderCode, fragmentShaderCode);
            shaderProgram.createUniform("pointSize");
            
            // Crear VAO y VBO para el punto
            vaoId = glGenVertexArrays();
            glBindVertexArray(vaoId);
            
            vboId = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
            
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
            
            // Desenlazar
            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glBindVertexArray(0);
            
        } catch (Exception e) {
            System.err.println("Error al inicializar CrosshairRenderer: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Renderiza el punto de mira en el centro de la pantalla
     */
    public void render() {
        // Guardar el estado de OpenGL
        boolean depthTestEnabled = glIsEnabled(GL_DEPTH_TEST);
        
        // Preparar para renderizado 2D
        if (depthTestEnabled) {
            glDisable(GL_DEPTH_TEST);
        }
        
        try {
            shaderProgram.bind();
            shaderProgram.setUniform("pointSize", pointSize);
            
            glBindVertexArray(vaoId);
            glEnableVertexAttribArray(0);
            
            // Dibujar un punto
            glDrawArrays(GL_POINTS, 0, 1);
            
            // Limpiar
            glDisableVertexAttribArray(0);
            glBindVertexArray(0);
            
        } catch (Exception e) {
            System.err.println("Error al renderizar crosshair: " + e.getMessage());
        } finally {
            shaderProgram.unbind();
            
            // Restaurar estado de OpenGL
            if (depthTestEnabled) {
                glEnable(GL_DEPTH_TEST);
            }
        }
    }
    
    /**
     * Establece el tamaño del punto de mira
     * 
     * @param size El tamaño del punto en píxeles
     */
    public void setPointSize(float size) {
        this.pointSize = size;
    }
    
    /**
     * Libera los recursos de OpenGL
     */
    public void cleanup() {
        shaderProgram.cleanup();
        
        glDeleteBuffers(vboId);
        glDeleteVertexArrays(vaoId);
    }
}