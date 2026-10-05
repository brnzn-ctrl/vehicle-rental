package model;

/**
 * Every model extends this. The id is private (encapsulation) and is read/changed through
 * getId()/setId(). id = 0 means "not saved yet".
 */
public abstract class Entity {
    private int id;

    protected Entity() { }
    protected Entity(int id) { this.id = id; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
}
