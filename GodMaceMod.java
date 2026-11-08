package de.godmace;

import java.util.List;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;

public class GodMaceMod implements ModInitializer {
    public static final String MOD_ID = "godmace";
    public static Item GOD_MACE;

    @Override
    public void onInitialize() {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(MOD_ID, "god_mace"));

        // 14 Schaden insgesamt (1 Basis + 13), langsame Schlaggeschwindigkeit wie eine Mace
        ItemAttributeModifiers attributes = ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 13.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -3.2, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();

        ItemLore lore = new ItemLore(List.of(
                Component.translatable("item.godmace.god_mace.lore1").withStyle(ChatFormatting.GOLD),
                Component.translatable("item.godmace.god_mace.lore2").withStyle(ChatFormatting.AQUA),
                Component.translatable("item.godmace.god_mace.lore3").withStyle(ChatFormatting.AQUA),
                Component.translatable("item.godmace.god_mace.lore4").withStyle(ChatFormatting.AQUA)));

        Item.Properties properties = new Item.Properties()
                .setId(key)
                .stacksTo(1)
                .rarity(Rarity.EPIC)
                .fireResistant()
                .attributes(attributes)
                .component(DataComponents.LORE, lore)
                .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);

        GOD_MACE = Registry.register(BuiltInRegistries.ITEM, key, new GodMaceItem(properties));

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.COMBAT).register(entries -> entries.accept(GOD_MACE));
    }
}
