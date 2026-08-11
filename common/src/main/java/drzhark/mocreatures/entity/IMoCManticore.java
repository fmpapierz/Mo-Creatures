package drzhark.mocreatures.entity;

/**
 * The animation contract shared by the two manticore forms — the wild
 * {@link drzhark.mocreatures.entity.monster.MoCEntityManticore} and the tameable
 * {@link drzhark.mocreatures.entity.passive.MoCEntityManticorePet}.
 *
 * <p>Legacy kept both forms on one client model ({@code MoCModelManticore extends MoCModelNewBigCat})
 * and fed it the entity's raw animation counters: {@code wingFlapCounter != 0} drove the wing beat,
 * {@code swingingTail()} threw the scorpion sting forward, {@code isOnAir()} unfolded the wings and
 * tucked the legs, and {@code mouthCounter} dropped the jaw. In 26.2 the model may only read a render
 * state, and the two forms sit on different base classes ({@code MoCMob} vs {@code MoCAnimal}) with no
 * common superclass — so this interface is what lets the renderer extract those four flags with a
 * single {@code instanceof} instead of one per form.
 */
public interface IMoCManticore {

    /** Wing beat in progress (legacy {@code wingFlapCounter != 0}) — the wings sweep at full amplitude. */
    boolean isWingFlapping();

    /**
     * The scorpion tail is mid-strike (legacy {@code swingingTail()}: poisoning and within the first 15
     * ticks of the 50-tick sting) — the barb whips forward over the manticore's head.
     */
    boolean isStingStriking();

    /** The maw is open for a roar or a bite (legacy {@code mouthCounter != 0}). */
    boolean getJawOpen();
}
