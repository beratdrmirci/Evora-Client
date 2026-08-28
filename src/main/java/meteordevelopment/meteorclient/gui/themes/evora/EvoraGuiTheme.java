/*
 * This file is part of the Evora Client distribution.
 * Copyright (c) Evora Development.
 */

package meteordevelopment.meteorclient.gui.themes.evora;

import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public class EvoraGuiTheme extends MeteorGuiTheme {
    private final SettingGroup sgColors = settings.getGroup("Colors");

    public final Setting<SettingColor> accentColor = sgColors.add(new ColorSetting.Builder()
        .name("accent-color")
        .description("Evora accent color.")
        .defaultValue(new SettingColor(180, 180, 190, 255))
        .build()
    );

    public EvoraGuiTheme() {
        super("Evora");

        accent.set(new SettingColor(180, 180, 190, 255));
        background.set(new SettingColor(20, 20, 25, 240));
        background2.set(new SettingColor(30, 30, 35, 240));
        background3.set(new SettingColor(40, 40, 45, 240));
        outline.set(new SettingColor(60, 60, 65, 255));
        text.set(new SettingColor(220, 220, 225, 255));
        textSecondary.set(new SettingColor(150, 150, 155, 255));
        textHighlight.set(new SettingColor(180, 180, 190, 100));
        titleText.set(new SettingColor(220, 220, 225, 255));
        loggedInText.set(new SettingColor(180, 180, 190, 255));
        placeholder.set(new SettingColor(100, 100, 105, 255));
        scrollbar.set(new SettingColor(180, 180, 190, 200));
        separator.set(new SettingColor(60, 60, 65, 255));
        moduleBackground.set(new SettingColor(20, 20, 25, 240));
        danger.set(new SettingColor(200, 60, 60, 255));
        warning.set(new SettingColor(200, 160, 60, 255));
    }

    public static void init() {
        GuiThemes.add(new EvoraGuiTheme());
    }
}