/*
 * This file is part of the Evora Client distribution.
 * Copyright (c) Evora Development.
 */

package meteordevelopment.meteorclient.systems.modules;

import meteordevelopment.meteorclient.addons.AddonManager;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.utils.render.DisplayItemUtils;
import net.minecraft.world.item.Items;

public class Categories {
    public static final Category Combat = new Category("Combat", () -> DisplayItemUtils.toStack(Items.GOLDEN_SWORD));
    public static final Category Mace = new Category("Mace", () -> DisplayItemUtils.toStack(Items.MACE));
    public static final Category Spear = new Category("Spear", () -> DisplayItemUtils.toStack(Items.TRIDENT));
    public static final Category Movement = new Category("Movement", () -> DisplayItemUtils.toStack(Items.DIAMOND_BOOTS));
    public static final Category Misc = new Category("Misc", () -> DisplayItemUtils.toStack(Items.LAVA_BUCKET));
    public static final Category Visual = new Category("Visual", () -> DisplayItemUtils.toStack(Items.GLASS));

    public static boolean REGISTERING;

    public static void init() {
        REGISTERING = true;

        // Evora
        Modules.registerCategory(Combat);
        Modules.registerCategory(Mace);
        Modules.registerCategory(Spear);
        Modules.registerCategory(Movement);
        Modules.registerCategory(Misc);
        Modules.registerCategory(Visual);

        // Addons
        AddonManager.ADDONS.forEach(MeteorAddon::onRegisterCategories);

        REGISTERING = false;
    }
}