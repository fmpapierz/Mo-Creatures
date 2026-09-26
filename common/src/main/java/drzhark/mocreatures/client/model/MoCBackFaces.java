package drzhark.mocreatures.client.model;

import java.util.Set;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.core.Direction;

/**
 * Back faces that Minecraft 1.12.2 drew and 26.2's culled render type does not.
 *
 * <p>1.12.2 rendered every living entity with face culling OFF ({@code RenderLivingBase.doRender} calls
 * {@code GlStateManager.disableCull()} at entry, obf {@code caa.a(vp,DDDFF)}), and DrZhark relied on it: many
 * boxes are painted on only some faces and left transparent on the others, so the painted faces are seen from
 * BEHIND through the transparent ones. Stag antlers and rodent whiskers are painted on one face and must read
 * from both sides; bird and duck legs are a stick on the box's back face plus toes on its bottom face, seen from
 * the front and from above through the empty faces; boar ears, the big cat collar medallion, the manticore wing
 * membranes and werewolf mouths work the same way. Most of this port renders with
 * {@code RenderTypes::entityCutoutCull} (26.2 naming: {@code entityCutout} is the no-cull type), which culls
 * those back sides, so the detail vanished from every angle that looked at it through the box.
 *
 * <p>Fix, the same one used for the wyvern's eye sockets: for each painted face of such a box, add a single
 * face on the same plane, covering the same rectangle, turned to face INTO the box, and mapping every point
 * to the same texel as the painted face. Seen from behind the painted face, the twin shows exactly the texel
 * 1.12 showed there; seen from in front, the twin is back-facing and culled, so the pair never draws from the
 * same side (no z-fighting). The original box is left untouched; its twins live in a child part added with
 * {@link #add}, posed at zero, so they share the part's pivot, rotation, visibility and animation exactly.
 *
 * <p>How each twin is built from the painted face's box ({@code texOffs u,v}, size {@code w,h,d}, mirror
 * {@code m}), so that the vanilla {@code ModelPart.Cube} UV layout gives the same point-to-texel map:
 * <ul>
 *   <li>WEST/EAST face: the same face with mirror toggled. {@code mirror} turns an X face round without
 *       changing how it maps z/y to u/v. Zero-width box on the face's plane, texOffs {@code (u, v)} for WEST,
 *       {@code (u + w, v)} for EAST.</li>
 *   <li>NORTH face: a SOUTH face with mirror toggled, texOffs {@code (u + d - w, v + d)}. SOUTH face: a NORTH
 *       face with mirror toggled, texOffs {@code (u + 2d + w, v + d)}. Zero-depth box on the face's plane.</li>
 *   <li>DOWN face: an UP face, mirror unchanged, texOffs {@code (u - w, v)}. UP face: a DOWN face, mirror
 *       unchanged, texOffs {@code (u + w, v)}. Zero-height box on the face's plane. (DOWN and UP already run
 *       u and v the same way.)</li>
 * </ul>
 * Each twin in this port was generated from its box's definition and checked against the baked polygons:
 * identical corner positions with identical UVs, opposite normal, consistent winding.
 *
 * <p>Lighting note: a twin is lit with its own (inward) normal, whereas 1.12's fixed-function lighting lit a
 * back face with the front face's normal; the texels are identical, the shading of an inner face can differ.
 */
public final class MoCBackFaces {

    public static final Set<Direction> DOWN = Set.of(Direction.DOWN);
    public static final Set<Direction> UP = Set.of(Direction.UP);
    public static final Set<Direction> WEST = Set.of(Direction.WEST);
    public static final Set<Direction> EAST = Set.of(Direction.EAST);
    public static final Set<Direction> NORTH = Set.of(Direction.NORTH);
    public static final Set<Direction> SOUTH = Set.of(Direction.SOUTH);

    /** Suffix of the child part that holds a part's back-face twins. */
    public static final String SUFFIX = "_back_faces";

    private MoCBackFaces() {
    }

    /**
     * Adds {@code twins} as a zero-pose child of the part at {@code path} (child names joined by '/', starting
     * below {@code root}), named after that part plus {@link #SUFFIX}.
     */
    public static PartDefinition add(PartDefinition root, String path, CubeListBuilder twins) {
        PartDefinition part = root;
        for (String name : path.split("/")) {
            PartDefinition child = part.getChild(name);
            if (child == null) {
                throw new IllegalArgumentException("No part '" + name + "' on the path " + path);
            }
            part = child;
        }
        String last = path.substring(path.lastIndexOf('/') + 1);
        return part.addOrReplaceChild(last + SUFFIX, twins, PartPose.ZERO);
    }
}
