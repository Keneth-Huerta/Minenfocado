package main.java.com.minefocado.game.world.blocks;

/**
 * Clase base para todos los tipos de bloques en el mundo voxel.
 * Define propiedades básicas como solidez, índices de textura y comportamiento.
 */
public class Block {
    // Constantes de tipo de bloque
    private final byte id;
    private final String name;
    
    // Propiedades físicas
    private final boolean solid;
    private final boolean transparent;
    private final boolean liquid;
    
    // Índices de textura para cada cara
    // Orden: ARRIBA, ABAJO, FRENTE, ATRÁS, IZQUIERDA, DERECHA
    private final int[] textureIndices;
    
    // Constantes para índices de caras
    public static final int FACE_UP = 0;
    public static final int FACE_DOWN = 1;
    public static final int FACE_FRONT = 2;
    public static final int FACE_BACK = 3;
    public static final int FACE_LEFT = 4;
    public static final int FACE_RIGHT = 5;
    
    /**
     * Crea un nuevo bloque con las propiedades especificadas
     * 
     * @param id ID del bloque
     * @param name Nombre del bloque
     * @param solid Si el bloque es sólido (se puede colisionar con él)
     * @param transparent Si el bloque es transparente (se renderizan las caras adyacentes)
     * @param liquid Si el bloque es líquido (tiene física especial)
     * @param textureIndices Índices de textura para cada cara, o un solo índice para todas las caras
     */
    public Block(byte id, String name, boolean solid, boolean transparent, boolean liquid, int... textureIndices) {
        this.id = id;
        this.name = name;
        this.solid = solid;
        this.transparent = transparent;
        this.liquid = liquid;
        
        // Copiar índices de textura, usando el primer índice para todas las caras si solo se proporciona uno
        this.textureIndices = new int[6];
        if (textureIndices.length == 1) {
            for (int i = 0; i < 6; i++) {
                this.textureIndices[i] = textureIndices[0];
            }
        } else if (textureIndices.length == 6) {
            System.arraycopy(textureIndices, 0, this.textureIndices, 0, 6);
        } else {
            throw new IllegalArgumentException("Los índices de textura deben ser 1 o 6 valores");
        }
    }
    
    /**
     * Obtiene el ID del bloque
     */
    public byte getId() {
        return id;
    }
    
    /**
     * Obtiene el nombre del bloque
     */
    public String getName() {
        return name;
    }
    
    /**
     * Comprueba si el bloque es sólido
     */
    public boolean isSolid() {
        return solid;
    }
    
    /**
     * Comprueba si el bloque es transparente
     */
    public boolean isTransparent() {
        return transparent;
    }
    
    /**
     * Comprueba si el bloque es opaco (no transparente)
     */
    public boolean isOpaque() {
        return !transparent;
    }
    
    /**
     * Comprueba si el bloque es líquido
     */
    public boolean isLiquid() {
        return liquid;
    }
    
    /**
     * Obtiene el índice de textura para una cara específica
     * 
     * @param face Índice de cara (0-5 para ARRIBA, ABAJO, FRENTE, ATRÁS, IZQUIERDA, DERECHA)
     * @return Índice de textura
     */
    public int getTextureIndex(int face) {
        return textureIndices[face];
    }
    
    /**
     * Obtiene la coordenada X de textura para la cara superior
     * @return Coordenada X de textura
     */
    public float getTopTextureX() {
        return getTextureIndex(FACE_UP) % 16;  // Asumiendo 16 texturas por fila en el atlas
    }
    
    /**
     * Obtiene la coordenada Y de textura para la cara superior
     * @return Coordenada Y de textura
     */
    public float getTopTextureY() {
        return getTextureIndex(FACE_UP) / 16;  // Asumiendo 16 texturas por fila en el atlas
    }
    
    /**
     * Obtiene la coordenada X de textura para las caras laterales (usando la cara FRONTAL)
     * @return Coordenada X de textura
     */
    public float getSideTextureX() {
        return getTextureIndex(FACE_FRONT) % 16;  // Asumiendo 16 texturas por fila en el atlas
    }
    
    /**
     * Obtiene la coordenada Y de textura para las caras laterales (usando la cara FRONTAL)
     * @return Coordenada Y de textura
     */
    public float getSideTextureY() {
        return getTextureIndex(FACE_FRONT) / 16;  // Asumiendo 16 texturas por fila en el atlas
    }
    
    /**
     * Obtiene la coordenada X de textura para la cara inferior
     * @return Coordenada X de textura
     */
    public float getBottomTextureX() {
        return getTextureIndex(FACE_DOWN) % 16;  // Asumiendo 16 texturas por fila en el atlas
    }
    
    /**
     * Obtiene la coordenada Y de textura para la cara inferior
     * @return Coordenada Y de textura
     */
    public float getBottomTextureY() {
        return getTextureIndex(FACE_DOWN) / 16;  // Asumiendo 16 texturas por fila en el atlas
    }
    
    /**
     * Comprueba si esta cara debe renderizarse cuando está adyacente al bloque especificado
     * 
     * @param adjacentBlock El bloque adyacente a esta cara
     * @return True si la cara debe renderizarse
     */
    public boolean shouldRenderFace(Block adjacentBlock) {
        // Renderizar cara si el bloque adyacente es aire o transparente
        return adjacentBlock == null || adjacentBlock.isTransparent();
    }
    
    /**
     * Devuelve una representación en cadena de este bloque
     */
    @Override
    public String toString() {
        return name;
    }
}