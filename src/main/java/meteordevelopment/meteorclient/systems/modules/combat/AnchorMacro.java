/*
 * This file is part of the Evora Client distribution.
 * Copyright (c) Evora Development.
 */

package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractResultType;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class AnchorMacro extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAim = settings.createGroup("Aim");

    // Settings
    private final Setting<KeyAction> key = sgGeneral.add(new KeybindSetting.Builder()
        .name("macro-key")
        .description("Anchor macro'yu tetikleyen tuş.")
        .build()
    );

    private final Setting<Boolean> glowstoneShield = sgGeneral.add(new BoolSetting.Builder()
        .name("glowstone-shield")
        .description("Oyuncu ile anchor arasına glowstone koyarak hasarı bloke eder.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> placeAtFeet = sgGeneral.add(new BoolSetting.Builder()
        .name("place-at-feet")
        .description("Aşağı bakarken anchor ayakların altına yerleştirilir.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> silentAim = sgAim.add(new BoolSetting.Builder()
        .name("silent-aim")
        .description("Anchor konulamazsa bakış açısını sessizce düzenler, ekranda görünmez.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> aimSpeed = sgAim.add(new DoubleSetting.Builder()
        .name("aim-speed")
        .description("Silent aim hızı.")
        .defaultValue(30)
        .min(1)
        .sliderMax(90)
        .build()
    );

    // State
    private enum Stage { IDLE, PLACE_ANCHOR, CHARGE, SHIELD, EXPLODE }
    private Stage stage = Stage.IDLE;
    private BlockPos anchorPos = null;
    private boolean keyWasPressed = false;

    public AnchorMacro() {
        super(Categories.Combat, "anchor-macro", "Anchor'u otomatik koyar, şarj eder ve patlatır.");
    }

    @Override
    public void onDeactivate() {
        stage = Stage.IDLE;
        anchorPos = null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null) return;

        // Tuş kontrolü
        boolean keyPressed = key.get().isPressed();
        if (keyPressed && !keyWasPressed) {
            stage = Stage.PLACE_ANCHOR;
        }
        keyWasPressed = keyPressed;

        if (stage == Stage.IDLE) return;

        switch (stage) {
            case PLACE_ANCHOR -> placeAnchor();
            case CHARGE -> chargeAnchor();
            case SHIELD -> placeShield();
            case EXPLODE -> explodeAnchor();
            default -> {}
        }
    }

    private void placeAnchor() {
        // Envanterde anchor var mı
        int anchorSlot = findItem(Items.RESPAWN_ANCHOR);
        if (anchorSlot == -1) {
            stage = Stage.IDLE;
            return;
        }

        // Hedef pozisyonu bul
        BlockPos target = findBestPlacementPos();
        if (target == null) {
            if (silentAim.get()) {
                // Bakışı aşağı çek
                float targetPitch = Math.min(mc.player.getXRot() + (float) aimSpeed.get(), 89.9f);
                if (silentAim.get()) {
                    sendSilentRotation(mc.player.getYRot(), targetPitch);
                } else {
                    mc.player.setXRot(targetPitch);
                }
            }
            return;
        }

        // Anchor'u tut
        mc.player.getInventory().selected = anchorSlot - 36;

        // Yerleştir
        BlockHitResult hit = new BlockHitResult(
            Vec3.atCenterOf(target),
            Direction.UP,
            target,
            false
        );

        mc.gameMode.useItemOn(mc.player, mc.player.getMainHandItem(), hit);
        anchorPos = target;
        stage = Stage.CHARGE;
    }

    private void chargeAnchor() {
        if (anchorPos == null) { stage = Stage.IDLE; return; }

        int glowstoneSlot = findItem(Items.GLOWSTONE);
        if (glowstoneSlot == -1) { stage = Stage.IDLE; return; }

        mc.player.getInventory().selected = glowstoneSlot - 36;

        BlockHitResult hit = new BlockHitResult(
            Vec3.atCenterOf(anchorPos),
            Direction.UP,
            anchorPos,
            false
        );

        mc.gameMode.useItemOn(mc.player, mc.player.getMainHandItem(), hit);

        if (glowstoneShield.get()) {
            stage = Stage.SHIELD;
        } else {
            stage = Stage.EXPLODE;
        }
    }

    private void placeShield() {
        if (anchorPos == null) { stage = Stage.IDLE; return; }

        int glowstoneSlot = findItem(Items.GLOWSTONE);
        if (glowstoneSlot == -1) {
            stage = Stage.EXPLODE;
            return;
        }

        // Oyuncu ile anchor arasındaki pozisyon
        BlockPos shieldPos = anchorPos.above();

        mc.player.getInventory().selected = glowstoneSlot - 36;

        BlockHitResult hit = new BlockHitResult(
            Vec3.atCenterOf(shieldPos),
            Direction.UP,
            shieldPos,
            false
        );

        mc.gameMode.useItemOn(mc.player, mc.player.getMainHandItem(), hit);
        stage = Stage.EXPLODE;
    }

    private void explodeAnchor() {
        if (anchorPos == null) { stage = Stage.IDLE; return; }

        // Totem slotu — offhand
        if (mc.player.getOffhandItem().getItem() != Items.TOTEM_OF_UNDYING) {
            stage = Stage.IDLE;
            return;
        }

        BlockHitResult hit = new BlockHitResult(
            Vec3.atCenterOf(anchorPos),
            Direction.UP,
            anchorPos,
            false
        );

        mc.gameMode.useItemOn(mc.player, mc.player.getMainHandItem(), hit);
        stage = Stage.IDLE;
        anchorPos = null;
    }

    private BlockPos findBestPlacementPos() {
        // Aşağı bakıyorsa ayak altı
        if (placeAtFeet.get() && mc.player.getXRot() > 60) {
            BlockPos feet = mc.player.blockPosition().below();
            if (canPlace(feet)) return feet;
        }

        // En uzak yerleştirilebilir pozisyon — 4 blok yarıçap
        BlockPos best = null;
        double bestDist = -1;

        BlockPos origin = mc.player.blockPosition();
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                for (int y = -2; y <= 0; y++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (!canPlace(pos)) continue;
                    double dist = pos.distSqr(origin);
                    if (dist > bestDist) {
                        bestDist = dist;
                        best = pos;
                    }
                }
            }
        }

        return best;
    }

    private boolean canPlace(BlockPos pos) {
        return mc.level.getBlockState(pos).isAir() &&
               !mc.level.getBlockState(pos.below()).isAir();
    }

    private int findItem(net.minecraft.world.item.Item item) {
        for (int i = 36; i < 45; i++) {
            if (mc.player.getInventory().getItem(i - 36).getItem() == item) return i;
        }
        return -1;
    }

    private void sendSilentRotation(float yaw, float pitch) {
        mc.getConnection().send(new ServerboundMovePlayerPacket.Rot(yaw, pitch, mc.player.onGround(), false));
    }
}