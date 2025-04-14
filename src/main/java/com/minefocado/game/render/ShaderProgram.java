package main.java.com.minefocado.game.render;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.opengl.GL20.*;

/**
 * Representa un programa shader OpenGL con shaders de vértices y fragmentos.
 * Maneja la compilación de shaders, enlazado y gestión de uniformes.
 */
public class ShaderProgram {
    // IDs de programa y shader
    private final int programId;
    private int vertexShaderId;
    private int fragmentShaderId;
    
    // Caché de uniformes para rendimiento
    private final Map<String, Integer> uniforms;
    
    // Buffer temporal para pasar matrices
    private final FloatBuffer matrixBuffer;
    
    /**
     * Crea un nuevo programa shader
     * 
     * @param vertexShaderCode Código del shader de vértices como string
     * @param fragmentShaderCode Código del shader de fragmentos como string
     * @throws Exception Si la compilación o enlazado de shader falla
     */
    public ShaderProgram(String vertexShaderCode, String fragmentShaderCode) throws Exception {
        uniforms = new HashMap<>();
        matrixBuffer = MemoryUtil.memAllocFloat(16);
        
        // Crear programa shader
        programId = glCreateProgram();
        if (programId == 0) {
            throw new Exception("No se pudo crear el programa shader");
        }
        
        // Crear y compilar shaders
        vertexShaderId = createShader(vertexShaderCode, GL_VERTEX_SHADER);
        fragmentShaderId = createShader(fragmentShaderCode, GL_FRAGMENT_SHADER);
        
        // Enlazar programa
        link();
    }
    
    /**
     * Crea un shader del tipo especificado
     * 
     * @param shaderCode Código fuente del shader
     * @param shaderType Tipo de shader (GL_VERTEX_SHADER o GL_FRAGMENT_SHADER)
     * @return ID del shader
     * @throws Exception Si la compilación del shader falla
     */
    private int createShader(String shaderCode, int shaderType) throws Exception {
        int shaderId = glCreateShader(shaderType);
        if (shaderId == 0) {
            throw new Exception("Error creando shader. Tipo: " + shaderType);
        }
        
        // Compilar el shader
        glShaderSource(shaderId, shaderCode);
        glCompileShader(shaderId);
        
        // Verificar errores de compilación
        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == 0) {
            throw new Exception("Error compilando shader: " + glGetShaderInfoLog(shaderId, 1024));
        }
        
        // Adjuntar el shader al programa
        glAttachShader(programId, shaderId);
        
        return shaderId;
    }
    
    /**
     * Enlaza el programa shader
     * 
     * @throws Exception Si el enlazado del programa falla
     */
    private void link() throws Exception {
        // Enlazar programa
        glLinkProgram(programId);
        
        // Verificar errores de enlazado
        if (glGetProgrami(programId, GL_LINK_STATUS) == 0) {
            throw new Exception("Error enlazando programa shader: " + 
                    glGetProgramInfoLog(programId, 1024));
        }
        
        // Separar shaders después del enlazado
        if (vertexShaderId != 0) {
            glDetachShader(programId, vertexShaderId);
        }
        if (fragmentShaderId != 0) {
            glDetachShader(programId, fragmentShaderId);
        }
        
        // Validar programa
        glValidateProgram(programId);
        if (glGetProgrami(programId, GL_VALIDATE_STATUS) == 0) {
            System.err.println("Advertencia validando programa shader: " + 
                    glGetProgramInfoLog(programId, 1024));
        }
    }
    
    /**
     * Vincula el programa shader para uso
     */
    public void bind() {
        glUseProgram(programId);
    }
    
    /**
     * Desvincula el programa shader
     */
    public void unbind() {
        glUseProgram(0);
    }
    
    /**
     * Limpia los recursos utilizados por el programa shader
     */
    public void cleanup() {
        unbind();
        
        // Eliminar shaders
        if (programId != 0) {
            glDeleteProgram(programId);
        }
        
        // Liberar buffer de matriz
        MemoryUtil.memFree(matrixBuffer);
    }
    
    /**
     * Crea una variable uniforme en el shader
     * 
     * @param uniformName Nombre del uniforme
     * @throws Exception Si el uniforme no existe
     */
    public void createUniform(String uniformName) throws Exception {
        int uniformLocation = glGetUniformLocation(programId, uniformName);
        if (uniformLocation < 0) {
            throw new Exception("No se pudo encontrar el uniforme: " + uniformName);
        }
        uniforms.put(uniformName, uniformLocation);
    }
    
    /**
     * Establece un uniforme entero
     * 
     * @param uniformName Nombre del uniforme
     * @param value Valor entero
     */
    public void setUniform(String uniformName, int value) {
        // Verificar si el uniforme existe antes de establecer el valor
        Integer location = uniforms.get(uniformName);
        if (location != null) {
            glUniform1i(location, value);
        }
    }
    
    /**
     * Establece un uniforme float
     * 
     * @param uniformName Nombre del uniforme
     * @param value Valor float
     */
    public void setUniform(String uniformName, float value) {
        // Verificar si el uniforme existe antes de establecer el valor
        Integer location = uniforms.get(uniformName);
        if (location != null) {
            glUniform1f(location, value);
        }
    }
    
    /**
     * Establece un uniforme Vector3f
     * 
     * @param uniformName Nombre del uniforme
     * @param value Valor Vector3f
     */
    public void setUniform(String uniformName, Vector3f value) {
        // Verificar si el uniforme existe antes de establecer el valor
        Integer location = uniforms.get(uniformName);
        if (location != null) {
            glUniform3f(location, value.x, value.y, value.z);
        }
    }
    
    /**
     * Establece un uniforme Matrix4f
     * 
     * @param uniformName Nombre del uniforme
     * @param value Valor Matrix4f
     */
    public void setUniform(String uniformName, Matrix4f value) {
        // Verificar si el uniforme existe antes de establecer el valor
        Integer location = uniforms.get(uniformName);
        if (location != null) {
            // Almacenar los datos de la matriz en el buffer
            value.get(matrixBuffer);
            glUniformMatrix4fv(location, false, matrixBuffer);
        }
    }
}