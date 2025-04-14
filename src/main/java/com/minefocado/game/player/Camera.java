package main.java.com.minefocado.game.player;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import static org.lwjgl.glfw.GLFW.*;

/**
 * Representa la cámara en primera persona en el juego.
 * Maneja posicionamiento, rotación, y matrices de vista/proyección.
 */
public class Camera {
    // Posición de la cámara en el espacio del mundo
    private final Vector3f position;
    
    // Orientación de la cámara
    private float yaw;   // Rotación horizontal (izquierda/derecha)
    private float pitch; // Rotación vertical (arriba/abajo)
    
    // Matriz de vista (actualizada cuando cambia la cámara)
    private final Matrix4f viewMatrix;
    
    // Matriz de proyección (actualizada cuando se redimensiona la ventana)
    private final Matrix4f projectionMatrix;
    
    // Campo de visión en grados
    private float fov = 70.0f;
    
    // Planos cercano y lejano para la proyección
    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 1000.0f;
    
    // Para movimiento suave de cámara
    private static final float MOVEMENT_SPEED = 5.0f;
    private static final float MOUSE_SENSITIVITY = 0.1f;
    
    /**
     * Crea una nueva cámara en la posición especificada
     * 
     * @param startX Posición X inicial
     * @param startY Posición Y inicial
     * @param startZ Posición Z inicial
     */
    public Camera(float startX, float startY, float startZ) {
        this.position = new Vector3f(startX, startY, startZ);
        this.yaw = 0.0f;    // Por defecto: mirando a lo largo del eje Z negativo
        this.pitch = 0.0f;  // Por defecto: mirando recto al frente
        this.viewMatrix = new Matrix4f();
        this.projectionMatrix = new Matrix4f();
        
        // Inicializar matrices
        updateViewMatrix();
    }
    
    /**
     * Actualiza la posición de la cámara basada en la entrada del jugador
     * 
     * @param forward Si la tecla W está presionada
     * @param backward Si la tecla S está presionada
     * @param left Si la tecla A está presionada
     * @param right Si la tecla D está presionada
     * @param up Si la tecla Espacio está presionada
     * @param down Si la tecla Shift está presionada
     * @param deltaTime Tiempo desde el último frame en segundos
     */
    public void move(boolean forward, boolean backward, boolean left, 
                    boolean right, boolean up, boolean down, float deltaTime) {
        float speed = MOVEMENT_SPEED * deltaTime;
        
        // Calcular la dirección de movimiento basada en la orientación de la cámara
        if (forward) {
            position.x += Math.sin(Math.toRadians(yaw)) * speed;
            position.z -= Math.cos(Math.toRadians(yaw)) * speed;
        }
        if (backward) {
            position.x -= Math.sin(Math.toRadians(yaw)) * speed;
            position.z += Math.cos(Math.toRadians(yaw)) * speed;
        }
        // Corrección del movimiento lateral (estaba completamente invertido)
        if (left) {
            // Movimiento hacia la izquierda (90 grados en sentido antihorario desde la dirección de vista)
            position.x -= Math.sin(Math.toRadians(yaw + 90)) * speed;
            position.z += Math.cos(Math.toRadians(yaw + 90)) * speed;
        }
        if (right) {
            // Movimiento hacia la derecha (90 grados en sentido horario desde la dirección de vista)
            position.x -= Math.sin(Math.toRadians(yaw - 90)) * speed;
            position.z += Math.cos(Math.toRadians(yaw - 90)) * speed;
        }
        if (up) {
            position.y += speed;
        }
        if (down) {
            position.y -= speed;
        }
        
        // Actualizar matriz de vista después del cambio de posición
        updateViewMatrix();
    }
    
    /**
     * Procesa el movimiento del ratón para rotar la vista de la cámara
     * 
     * @param deltaX Cambio en la posición X del ratón
     * @param deltaY Cambio en la posición Y del ratón
     */
    public void rotate(float deltaX, float deltaY) {
        // Actualizar yaw (rotación horizontal)
        yaw += deltaX * MOUSE_SENSITIVITY;
        
        // Actualizar pitch (rotación vertical) con límites para evitar el bloqueo gimbal
        // CORRECCIÓN: Cambiado de -= a += para corregir el movimiento vertical de cámara invertido
        pitch += deltaY * MOUSE_SENSITIVITY;
        if (pitch > 89.0f) {
            pitch = 89.0f;
        }
        if (pitch < -89.0f) {
            pitch = -89.0f;
        }
        
        // Actualizar la matriz de vista después del cambio de orientación
        updateViewMatrix();
    }
    
    /**
     * Actualiza la matriz de vista basada en la posición y orientación actual
     */
    private void updateViewMatrix() {
        // Resetear la matriz de vista
        viewMatrix.identity();
        
        // Aplicar rotación de pitch
        viewMatrix.rotate((float) Math.toRadians(pitch), 1.0f, 0.0f, 0.0f);
        
        // Aplicar rotación de yaw
        viewMatrix.rotate((float) Math.toRadians(yaw), 0.0f, 1.0f, 0.0f);
        
        // Aplicar traslación (inversa de la posición de la cámara)
        viewMatrix.translate(-position.x, -position.y, -position.z);
    }
    
    /**
     * Actualiza la matriz de proyección para las dimensiones de ventana especificadas
     * 
     * @param width Ancho de la ventana
     * @param height Alto de la ventana
     */
    public void updateProjectionMatrix(int width, int height) {
        float aspectRatio = (float) width / height;
        projectionMatrix.identity();
        projectionMatrix.perspective((float) Math.toRadians(fov), aspectRatio, Z_NEAR, Z_FAR);
    }
    
    /**
     * Obtiene la posición actual de la cámara
     */
    public Vector3f getPosition() {
        return new Vector3f(position);
    }
    
    /**
     * Establece la posición de la cámara
     * 
     * @param x Nueva posición X
     * @param y Nueva posición Y
     * @param z Nueva posición Z
     */
    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
        updateViewMatrix();
    }
    
    /**
     * Obtiene la rotación yaw actual de la cámara
     */
    public float getYaw() {
        return yaw;
    }
    
    /**
     * Obtiene la rotación pitch actual de la cámara
     */
    public float getPitch() {
        return pitch;
    }
    
    /**
     * Establece la orientación de la cámara
     * 
     * @param yaw Nuevo ángulo yaw (horizontal)
     * @param pitch Nuevo ángulo pitch (vertical)
     */
    public void setRotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
        updateViewMatrix();
    }
    
    /**
     * Obtiene la matriz de vista
     */
    public Matrix4f getViewMatrix() {
        return viewMatrix;
    }
    
    /**
     * Obtiene la matriz de proyección
     */
    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }
    
    /**
     * Obtiene el campo de visión actual en grados
     */
    public float getFov() {
        return fov;
    }
    
    /**
     * Establece un nuevo campo de visión en grados
     * 
     * @param fov El nuevo campo de visión (entre 1 y 120)
     */
    public void setFov(float fov) {
        if (fov >= 1.0f && fov <= 120.0f) {
            this.fov = fov;
        }
    }
}