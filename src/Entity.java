
/**
 * Every model extends this. Public fields on purpose — keeps the whole
 * project short (no getter/setter boilerplate) since this is a school project,
 * not a public library. id = 0 means "not saved yet".
 */
public abstract class Entity {
    public int id;
}
