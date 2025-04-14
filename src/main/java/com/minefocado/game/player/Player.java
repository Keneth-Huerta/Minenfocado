package main.java.com.minefocado.game.player;

import org.joml.Vector3f;

import main.java.com.minefocado.game.world.World;
import main.java.com.minefocado.game.world.blocks.Block;
import main.java.com.minefocado.game.world.blocks.BlockRegistry;

/**
 * Representa al jugador en el mundo del juego.
 * Maneja el movimiento del jugador, física, colisión, e interacciones con el mundo.
 */
public class Player {
    // Posición del jugador en el espacio del mundo
    private final Vector3f position;
    
    // Velocidad del jugador
    private final Vector3f velocity;
    
    // Dimensiones del jugador (caja de colisión)
    private static final float WIDTH = 0.6f;
    private static final float HEIGHT = 1.8f;
    
    // Cámara del jugador (vista en primera persona)
    private final Camera camera;
    
    // Constantes físicas
    private static final float GRAVITY = 20.0f;
    private static final float JUMP_FORCE = 8.0f;
    private static final float MOVEMENT_SPEED = 4.5f;
    private static final float FLYING_SPEED = 10.0f;
    
    // Referencia al mundo del juego
    private final World world;
    
    // Estado del jugador
    private boolean onGround;
    private boolean isFlying;
    private int selectedHotbarSlot;
    private final int[] hotbar;
    
    // Constantes de lanzamiento de rayos para interacción con bloques
    private static final float REACH_DISTANCE = 5.0f;
    private static final float RAY_STEP = 0.05f;
    
    /**
     * Crea un nuevo jugador en la posición especificada en el mundo
     * 
     * @param x Posición X inicial
     * @param y Posición Y inicial
     * @param z Posición Z inicial
     * @param world El mundo del juego
     */
    public Player(float x, float y, float z, World world) {
        this.position = new Vector3f(x, y, z);
        this.velocity = new Vector3f(0, 0, 0);
        this.camera = new Camera(x, y + HEIGHT * 0.85f, z); // Nivel de los ojos
        this.world = world;
        this.onGround = false;
        this.isFlying = false;
        this.selectedHotbarSlot = 0;
        
        // Inicializar hotbar con tipos de bloques (IDs)
        this.hotbar = new int[9];
        hotbar[0] = BlockRegistry.STONE_ID;
        hotbar[1] = BlockRegistry.DIRT_ID;
        hotbar[2] = BlockRegistry.GRASS_ID;
        hotbar[3] = BlockRegistry.SAND_ID;
        hotbar[4] = BlockRegistry.WOOD_ID;
        hotbar[5] = BlockRegistry.LEAVES_ID;
        hotbar[6] = 0; // Ranura vacía
        hotbar[7] = 0; // Ranura vacía
        hotbar[8] = 0; // Ranura vacía
    }
    
    /**
     * Actualiza la física del jugador, posición y cámara
     * 
     * @param deltaTime Tiempo transcurrido desde el último frame
     */
    public void update(float deltaTime) {
        // Aplicar gravedad a menos que esté volando
        if (!isFlying) {
            velocity.y -= GRAVITY * deltaTime;
        } else {
            // Restablecer velocidad vertical al volar
            velocity.y *= 0.9f;
        }
        
        // Limitar velocidad terminal
        if (velocity.y < -30) {
            velocity.y = -30;
        }
        
        // Aplicar velocidad a la posición con detección de colisiones
        moveWithCollision(deltaTime);
        
        // Actualizar posición de la cámara para seguir la cabeza del jugador
        camera.setPosition(
            position.x,
            position.y + HEIGHT * 0.85f, // Nivel de los ojos
            position.z
        );
        
        // Si el jugador cae por debajo del mundo, teletransportar al spawn
        if (position.y < -10) {
            position.set(0, 100, 0);
            velocity.set(0, 0, 0);
            camera.setPosition(0, 100 + HEIGHT * 0.85f, 0);
        }
    }
    
    /**
     * Maneja la entrada de movimiento del jugador
     * 
     * @param forward Tecla W presionada
     * @param backward Tecla S presionada
     * @param left Tecla A presionada
     * @param right Tecla D presionada
     * @param jump Tecla Espacio presionada
     * @param crouch Tecla Shift presionada
     * @param deltaTime Tiempo desde el último frame
     */
    public void handleMovement(boolean forward, boolean backward, boolean left,
                               boolean right, boolean jump, boolean crouch,
                               float deltaTime) {
        // Calcular dirección de movimiento basada en orientación de cámara
        float yaw = (float) Math.toRadians(camera.getYaw());
        
        // Calcular vectores hacia adelante y derecha
        float fx = (float) Math.sin(yaw);
        float fz = (float) -Math.cos(yaw);
        float rx = fz;
        float rz = -fx;
        
        // Restablecer velocidad horizontal
        velocity.x = 0;
        velocity.z = 0;
        
        // Calcular velocidad actual (diferente para modo vuelo)
        float speed = isFlying ? FLYING_SPEED : MOVEMENT_SPEED;
        
        // Aplicar entrada de movimiento
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
        
        // Manejar salto (solo cuando está en el suelo o volando)
        if (jump) {
            if (onGround && !isFlying) {
                velocity.y = JUMP_FORCE;
                onGround = false;
            } else if (isFlying) {
                velocity.y = FLYING_SPEED;
            }
        }
        
        // Manejar agacharse/descender
        if (crouch && isFlying) {
            velocity.y = -FLYING_SPEED;
        }
    }
    
    /**
     * Mueve al jugador con detección de colisiones
     * 
     * @param deltaTime Tiempo desde el último frame
     */
    private void moveWithCollision(float deltaTime) {
        // Guardar posición original para restaurar en caso de colisión
        float originalX = position.x;
        float originalY = position.y;
        float originalZ = position.z;
        
        // Mover en dirección X
        position.x += velocity.x * deltaTime;
        if (checkCollision()) {
            position.x = originalX;
            velocity.x = 0;
        }
        
        // Mover en dirección Y
        position.y += velocity.y * deltaTime;
        if (checkCollision()) {
            position.y = originalY;
            
            // Si se mueve hacia abajo y hay colisión, hemos golpeado el suelo
            if (velocity.y < 0) {
                onGround = true;
            }
            
            velocity.y = 0;
        } else {
            // Estamos en el aire si podemos movernos verticalmente
            onGround = false;
        }
        
        // Mover en dirección Z
        position.z += velocity.z * deltaTime;
        if (checkCollision()) {
            position.z = originalZ;
            velocity.z = 0;
        }
    }
    
    /**
     * Comprueba si el jugador está colisionando con algún bloque sólido
     * 
     * @return true si se detecta colisión, false en caso contrario
     */
    private boolean checkCollision() {
        // Obtener posiciones de bloques que se superponen con la caja de colisión del jugador
        int minX = (int) Math.floor(position.x - WIDTH / 2);
        int maxX = (int) Math.floor(position.x + WIDTH / 2);
        int minY = (int) Math.floor(position.y);
        int maxY = (int) Math.floor(position.y + HEIGHT);
        int minZ = (int) Math.floor(position.z - WIDTH / 2);
        int maxZ = (int) Math.floor(position.z + WIDTH / 2);
        
        // Comprobar cada bloque potencial para colisión
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
     * Rompe el bloque que el jugador está mirando
     */
    public void breakBlock() {
        // Lanzar un rayo para encontrar el bloque
        BlockRayResult result = castRay(true);
        if (result != null && result.blockType != BlockRegistry.AIR_ID) {
            // Reemplazar con aire
            world.setBlockAt(result.x, result.y, result.z, BlockRegistry.AIR_ID);
        }
    }
    
    /**
     * Coloca un bloque adyacente a donde el jugador está mirando
     */
    public void placeBlock() {
        // Lanzar un rayo para encontrar la cara contra la que colocar
        BlockRayResult result = castRay(true);
        if (result != null && result.faceX != 0 || result.faceY != 0 || result.faceZ != 0) {
            // Calcular posición para colocar el nuevo bloque
            int placeX = result.x + result.faceX;
            int placeY = result.y + result.faceY;
            int placeZ = result.z + result.faceZ;
            
            // Obtener bloque seleccionado del hotbar
            int blockId = hotbar[selectedHotbarSlot];
            
            // No colocar si la ranura seleccionada está vacía o el lugar está ocupado
            if (blockId != 0 && world.getBlockAt(placeX, placeY, placeZ).getId() == BlockRegistry.AIR_ID) {
                // Comprobar colisión con jugador primero
                float px = position.x;
                float py = position.y;
                float pz = position.z;
                
                // Comprobar si colocar causaría que el jugador quede atrapado
                position.x = (placeX + 0.5f);
                position.y = (placeY + 0.5f);
                position.z = (placeZ + 0.5f);
                
                boolean wouldCollide = checkCollision();
                
                // Restaurar posición
                position.x = px;
                position.y = py;
                position.z = pz;
                
                // Solo colocar si no causará que el jugador quede atrapado
                if (!wouldCollide) {
                    world.setBlockAt(placeX, placeY, placeZ, blockId);
                }
            }
        }
    }
    
    /**
     * Lanza un rayo desde el punto de vista del jugador y devuelve el primer bloque golpeado
     * 
     * @param includeOutsideInfo Si se debe incluir información de la cara normal
     * @return Información sobre el bloque golpeado, o null si no se golpeó ninguno
     */
    private BlockRayResult castRay(boolean includeOutsideInfo) {
        // Obtener origen del rayo (posición de la cámara) y dirección (vector frontal de la cámara)
        Vector3f origin = camera.getPosition();
        Vector3f direction = new Vector3f();
        
        // Calcular vector de dirección a partir de los ángulos de la cámara
        float yaw = (float) Math.toRadians(camera.getYaw());
        float pitch = (float) Math.toRadians(camera.getPitch());
        
        direction.x = (float) (Math.cos(pitch) * Math.sin(yaw));
        direction.y = (float) -Math.sin(pitch);
        direction.z = (float) (Math.cos(pitch) * -Math.cos(yaw));
        direction.normalize();
        
        // Tamaño del paso del rayo y distancia actual
        float distance = 0;
        
        // Posición anterior (para cálculo de cara normal)
        int lastX = (int) Math.floor(origin.x);
        int lastY = (int) Math.floor(origin.y);
        int lastZ = (int) Math.floor(origin.z);
        
        // Marcha del rayo
        while (distance < REACH_DISTANCE) {
            // Calcular posición actual
            float x = origin.x + direction.x * distance;
            float y = origin.y + direction.y * distance;
            float z = origin.z + direction.z * distance;
            
            // Convertir a coordenadas de bloque
            int blockX = (int) Math.floor(x);
            int blockY = (int) Math.floor(y);
            int blockZ = (int) Math.floor(z);
            
            // Comprobar si el bloque es sólido
            Block block = world.getBlockAt(blockX, blockY, blockZ);
            if (block != null && block.isSolid()) {
                // Calcular qué cara fue golpeada (para colocación de bloques)
                int faceX = 0, faceY = 0, faceZ = 0;
                
                if (includeOutsideInfo) {
                    if (blockX > lastX) faceX = -1;
                    else if (blockX < lastX) faceX = 1;
                    else if (blockY > lastY) faceY = -1;
                    else if (blockY < lastY) faceY = 1;
                    else if (blockZ > lastZ) faceZ = -1;
                    else if (blockZ < lastZ) faceZ = 1;
                }
                
                // Devolver información sobre el bloque golpeado
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
            
            // Recordar posición actual para cálculo de cara
            lastX = blockX;
            lastY = blockY;
            lastZ = blockZ;
            
            // Avanzar a lo largo del rayo
            distance += RAY_STEP;
        }
        
        return null;
    }
    
    /**
     * Cambia la ranura de hotbar seleccionada
     * 
     * @param delta Cantidad de cambio (positivo o negativo)
     */
    public void changeHotbarSelection(int delta) {
        selectedHotbarSlot = (selectedHotbarSlot + delta + hotbar.length) % hotbar.length;
    }
    
    /**
     * Establece la ranura de hotbar seleccionada directamente
     * 
     * @param slot La ranura a seleccionar (0-8)
     */
    public void setHotbarSelection(int slot) {
        if (slot >= 0 && slot < hotbar.length) {
            selectedHotbarSlot = slot;
        }
    }
    
    /**
     * Obtiene el bloque seleccionado actualmente
     * 
     * @return El ID del bloque en la ranura de hotbar seleccionada
     */
    public int getSelectedBlockId() {
        return hotbar[selectedHotbarSlot];
    }
    
    /**
     * Alterna el modo vuelo
     */
    public void toggleFlying() {
        isFlying = !isFlying;
        
        // Restablecer velocidad vertical al alternar
        velocity.y = 0;
    }
    
    /**
     * Obtiene la cámara del jugador
     */
    public Camera getCamera() {
        return camera;
    }
    
    /**
     * Obtiene la posición del jugador
     */
    public Vector3f getPosition() {
        return position;
    }
    
    /**
     * Obtiene si el jugador está en el suelo
     */
    public boolean isOnGround() {
        return onGround;
    }
    
    /**
     * Obtiene si el jugador está en modo vuelo
     */
    public boolean isFlying() {
        return isFlying;
    }
    
    /**
     * Obtiene la ranura de hotbar actualmente seleccionada
     */
    public int getSelectedHotbarSlot() {
        return selectedHotbarSlot;
    }
    
    /**
     * Obtiene el contenido del hotbar
     */
    public int[] getHotbar() {
        return hotbar;
    }
    
    /**
     * Clase auxiliar para almacenar resultados del lanzamiento de rayos
     */
    private static class BlockRayResult {
        public int x, y, z;
        public int blockType;
        public int faceX, faceY, faceZ;
    }
}