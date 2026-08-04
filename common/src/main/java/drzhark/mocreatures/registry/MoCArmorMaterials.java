package drzhark.mocreatures.registry;

import drzhark.mocreatures.MoCreatures;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.EnumMap;
import java.util.Map;

/**
 * The seven Mo'Creatures humanoid armor materials (reptile/croc, fur, hide and the four scorpion
 * chitin variants).
 *
 * <p>These exist because {@link ArmorMaterial} carries the {@link EquipmentAsset} key that decides
 * the <em>worn</em> texture: {@code Item.Properties.humanoidArmor(material, type)} copies
 * {@code material.assetId()} straight into the item's {@code EQUIPPABLE} data component, and the
 * client's {@code EquipmentAssetManager} then resolves it to
 * {@code assets/<namespace>/equipment/<path>.json}. Every set previously reused
 * {@code ArmorMaterials.IRON}, whose asset id is {@code minecraft:iron} — which is why chitin, croc,
 * fur and hide armor all rendered as iron plate on the player.</p>
 *
 * <p>Protection numbers are deliberately identical to vanilla IRON (durability factor 15,
 * boots 2 / leggings 5 / chestplate 6 / helmet 2, enchantability 9, no toughness or knockback
 * resistance) so this change is cosmetic-plus-repair only and does not re-balance any set: that is
 * exactly what {@code ArmorMaterials.IRON} was already giving them. See the legacy-tier note in
 * {@link MoCItems} above the fur/hide block.</p>
 *
 * <p>The repair ingredient and equip sound, by contrast, are corrected per material — repairing a
 * chitin plate with an iron ingot (the vanilla {@code minecraft:repairs_iron_armor} tag) was a
 * side-effect of the shared material, not a design choice.</p>
 */
public final class MoCArmorMaterials {

    private MoCArmorMaterials() {}

    /** Vanilla IRON's per-slot protection, reused verbatim so no set changes strength. */
    private static final Map<ArmorType, Integer> IRON_TIER_DEFENSE = defense(2, 5, 6, 2, 5);

    /** Croc-hide "reptile" set. Layer art: legacy croc_1 / croc_2. */
    public static final ArmorMaterial REPTILE = ironTier("reptile", SoundEvents.ARMOR_EQUIP_TURTLE);
    /** Fur set. Layer art: legacy fur_1 / fur_2. */
    public static final ArmorMaterial FUR = ironTier("fur", SoundEvents.ARMOR_EQUIP_LEATHER);
    /** Hide set. Layer art: legacy hide_1 / hide_2. */
    public static final ArmorMaterial HIDE = ironTier("hide", SoundEvents.ARMOR_EQUIP_LEATHER);
    /** Cave-scorpion chitin. Layer art: legacy scorpc_1 / scorpc_2. */
    public static final ArmorMaterial SCORPION_CAVE = ironTier("scorpion_cave", SoundEvents.ARMOR_EQUIP_TURTLE);
    /** Dirt-scorpion chitin. Layer art: legacy scorpd_1 / scorpd_2. */
    public static final ArmorMaterial SCORPION_DIRT = ironTier("scorpion_dirt", SoundEvents.ARMOR_EQUIP_TURTLE);
    /** Frost-scorpion chitin. Layer art: legacy scorpf_1 / scorpf_2. */
    public static final ArmorMaterial SCORPION_FROST = ironTier("scorpion_frost", SoundEvents.ARMOR_EQUIP_TURTLE);
    /** Nether-scorpion chitin. Layer art: legacy scorpn_1 / scorpn_2. */
    public static final ArmorMaterial SCORPION_NETHER = ironTier("scorpion_nether", SoundEvents.ARMOR_EQUIP_TURTLE);

    /**
     * Builds an IRON-strength material whose equipment asset, repair tag and item-model textures are
     * all keyed off {@code name}: asset {@code mocreatures:<name>} (which the client reads from
     * {@code assets/mocreatures/equipment/<name>.json}) and repair tag
     * {@code mocreatures:repairs_<name>_armor}.
     */
    private static ArmorMaterial ironTier(String name, net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> equipSound) {
        return new ArmorMaterial(15, IRON_TIER_DEFENSE, 9, equipSound, 0.0F, 0.0F,
                repairTag(name), asset(name));
    }

    /**
     * A mod-namespaced equipment-asset key. {@link EquipmentAssets#createId(String)} must not be used
     * here — it hardcodes {@code Identifier.withDefaultNamespace}, so it would silently return
     * {@code minecraft:<name>} and send the renderer back to vanilla assets.
     */
    private static ResourceKey<EquipmentAsset> asset(String name) {
        return ResourceKey.create(EquipmentAssets.ROOT_ID,
                Identifier.fromNamespaceAndPath(MoCreatures.MOD_ID, name));
    }

    private static TagKey<Item> repairTag(String name) {
        return TagKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(MoCreatures.MOD_ID, "repairs_" + name + "_armor"));
    }

    /** Mirrors vanilla {@code ArmorMaterials.makeDefense}: an EnumMap keyed by slot. */
    private static Map<ArmorType, Integer> defense(int boots, int leggings, int chestplate, int helmet, int body) {
        return new EnumMap<>(Map.of(
                ArmorType.BOOTS, boots,
                ArmorType.LEGGINGS, leggings,
                ArmorType.CHESTPLATE, chestplate,
                ArmorType.HELMET, helmet,
                ArmorType.BODY, body));
    }
}
