package main.java.com.minefocado.game.render;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.opengl.GL20.*;

/**
 * Represents an OpenGL shader program with vertex and fragment shaders.
 * Handles shader compilation, linking, and uniform management.
 */
public class ShaderProgram {
    // Program and shader IDs
    private final int programId;
    private int vertexShaderId;
    private int fragmentShaderId;
    
    // Uniform cache for performance
    private final Map<String, Integer> uniforms;
    
    // Temporary buffer for passing matrices
    private final FloatBuffer matrixBuffer;
    
    /**
     * Creates a new shader program
     * 
     * @param vertexShaderCode Vertex shader code as string
     * @param fragmentShaderCode Fragment shader code as string
     * @throws Exception If shader compilation or linking fails
     */
    public ShaderProgram(String vertexShaderCode, String fragmentShaderCode) throws Exception {
        uniforms = new HashMap<>();
        matrixBuffer = MemoryUtil.memAllocFloat(16);
        
        // Create shader program
        programId = glCreateProgram();
        if (programId == 0) {
            throw new Exception("Could not create shader program");
        }
        
        // Create and compile shaders
        vertexShaderId = createShader(vertexShaderCode, GL_VERTEX_SHADER);
        fragmentShaderId = createShader(fragmentShaderCode, GL_FRAGMENT_SHADER);
        
        // Link program
        link();
    }
    
    /**
     * Creates a shader of the specified type
     * 
     * @param shaderCode Shader source code
     * @param shaderType Shader type (GL_VERTEX_SHADER or GL_FRAGMENT_SHADER)
     * @return Shader ID
     * @throws Exception If shader compilation fails
     */
    private int createShader(String shaderCode, int shaderType) throws Exception {
        int shaderId = glCreateShader(shaderType);
        if (shaderId == 0) {
            throw new Exception("Error creating shader. Type: " + shaderType);
        }
        
        // Compile the shader
        glShaderSource(shaderId, shaderCode);
        glCompileShader(shaderId);
        
        // Check for compilation errors
        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == 0) {
            throw new Exception("Error compiling shader: " + glGetShaderInfoLog(shaderId, 1024));
        }
        
        // Attach the shader to the program
        glAttachShader(programId, shaderId);
        
        return shaderId;
    }
    
    /**
     * Links the shader program
     * 
     * @throws Exception If program linking fails
     */
    private void link() throws Exception {
        // Link program
        glLinkProgram(programId);
        
        // Check for linking errors
        if (glGetProgrami(programId, GL_LINK_STATUS) == 0) {
            throw new Exception("Error linking shader program: " + 
                    glGetProgramInfoLog(programId, 1024));
        }
        
        // Detach shaders after linking
        if (vertexShaderId != 0) {
            glDetachShader(programId, vertexShaderId);
        }
        if (fragmentShaderId != 0) {
            glDetachShader(programId, fragmentShaderId);
        }
        
        // Validate program
        glValidateProgram(programId);
        if (glGetProgrami(programId, GL_VALIDATE_STATUS) == 0) {
            System.err.println("Warning validating shader program: " + 
                    glGetProgramInfoLog(programId, 1024));
        }
    }
    
    /**
     * Binds the shader program for use
     */
    public void bind() {
        glUseProgram(programId);
    }
    
    /**
     * Unbinds the shader program
     */
    public void unbind() {
        glUseProgram(0);
    }
    
    /**
     * Cleans up resources used by the shader program
     */
    public void cleanup() {
        unbind();
        
        // Delete shaders
        if (programId != 0) {
            glDeleteProgram(programId);
        }
        
        // Free matrix buffer
        MemoryUtil.memFree(matrixBuffer);
    }
    
    /**
     * Creates a uniform variable in the shader
     * 
     * @param uniformName Name of the uniform
     * @throws Exception If the uniform doesn't exist
     */
    public void createUniform(String uniformName) throws Exception {
        int uniformLocation = glGetUniformLocation(programId, uniformName);
        if (uniformLocation < 0) {
            throw new Exception("Could not find uniform: " + uniformName);
        }
        uniforms.put(uniformName, uniformLocation);
    }
    
    /**
     * Sets an integer uniform
     * 
     * @param uniformName Name of the uniform
     * @param value Integer value
     */
    public void setUniform(String uniformName, int value) {
        // Verificar si el uniform existe antes de establecer el valor
        Integer location = uniforms.get(uniformName);
        if (location != null) {
            glUniform1i(location, value);
        }
    }
    
    /**
     * Sets a float uniform
     * 
     * @param uniformName Name of the uniform
     * @param value Float value
     */
    public void setUniform(String uniformName, float value) {
        // Verificar si el uniform existe antes de establecer el valor
        Integer location = uniforms.get(uniformName);
        if (location != null) {
            glUniform1f(location, value);
        }
    }
    
    /**
     * Sets a Vector3f uniform
     * 
     * @param uniformName Name of the uniform
     * @param value Vector3f value
     */
    public void setUniform(String uniformName, Vector3f value) {
        // Verificar si el uniform existe antes de establecer el valor
        Integer location = uniforms.get(uniformName);
        if (location != null) {
            glUniform3f(location, value.x, value.y, value.z);
        }
    }
    
    /**
     * Sets a Matrix4f uniform
     * 
     * @param uniformName Name of the uniform
     * @param value Matrix4f value
     */
    public void setUniform(String uniformName, Matrix4f value) {
        // Verificar si el uniform existe antes de establecer el valor
        Integer location = uniforms.get(uniformName);
        if (location != null) {
            // Store the matrix data in the buffer
            value.get(matrixBuffer);
            glUniformMatrix4fv(location, false, matrixBuffer);
        }
    }
}