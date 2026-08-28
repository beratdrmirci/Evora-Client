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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class AnchorMacro extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAim = settings.createGroup("Aim");

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
        .description("Anchor konulamazsa bakış açısını sessizce düzenler.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> aimSpeed = sgAim.add(new DoubleSetting.Builder()
        .name("aim-speed")
        .description("Silent aim hızı (derece/tick).")
        .defaultValue(30)
        .min(1)
        .sliderMax(90)
        .build()
    );

    private enum Stage { IDLE, PLACE_ANCHOR, CHARGE, SHIELD, EXPLODE }
    private Stage stage = Stage.IDLE;
    private BlockPos anchorPos = null;

    public AnchorMacro() {
        super(Categories.Combat, "anchor-macro", "Anchor'u otomatik koyar, şarj eder ve patlatır.");
    }

    @Override
    public void onActivate() {
        stage = Stage.PLACE_ANCHOR;
    }

    @Override
    public void onDeactivate() {
        stage = Stage.IDLE;
        anchorPos = null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null) return;

        switch (stage) {
            case PLACE_ANCHOR -> placeAnchor();
            case CHARGE -> chargeAnchor();
            case SHIELD -> placeShield();
            case EXPLODE -> explodeAnchor();
            default -> {}
        }
    }

    private void placeAnchor() {
        int anchorSlot = findItem(Items.RESPAWN_ANCHOR);
        if (anchorSlot == -1) { toggle(); return; }

        BlockPos target = findBestPlacementPos();
        if (target == null) {
            if (silentAim.get()) {
                float targetPitch = Math.min(mc.player.getXRot() + aimSpeed.get().floatValue(), 89.9f);
                sendSilentRotation(mc.player.getYRot(), targetPitch);
            }
            return;
        }

        mc.player.getInventory().selected = anchorSlot;

        BlockHitResult hit = new BlockHitResult(
            Vec3.atCenterOf(target),
            Direction.UP,
            target,
            false
        );

        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        anchorPos = target;
        stage = Stage.CHARGE;
    }

    private void chargeAnchor() {
        if (anchorPos == null) { toggle(); return; }

        int glowstoneSlot = findItem(Items.GLOWSTONE);
        if (glowstoneSlot == -1) { toggle(); return; }

        mc.player.getInventory().selected = glowstoneSlot;

        BlockHitResult hit = new BlockHitResult(
            Vec3.atCenterOf(anchorPos),
            Direction.UP,
            anchorPos,
            false
        );

        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        stage = glowstoneShield.get() ? Stage.SHIELD : Stage.EXPLODE;
    }

    private void placeShield() {
        if (anchorPos == null) { toggle(); return; }

        int glowstoneSlot = findItem(Items.GLOWSTONE);
        if (glowstoneSlot == -1) {
            stage = Stage.EXPLODE;
            return;
        }

        BlockPos shieldPos = anchorPos.above();

        if (!mc.level.getBlockState(shieldPos).isAir()) {
            stage = Stage.EXPLODE;
            return;
        }

        mc.player.getInventory().selected = glowstoneSlot;

        BlockHitResult hit = new BlockHitResult(
            Vec3.atCenterOf(shieldPos),
            Direction.DOWN,
            shieldPos,
            false
        );

        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        stage = Stage.EXPLODE;
    }

    private void explodeAnchor() {
        if (anchorPos == null) { toggle(); return; }

        if (mc.player.getOffhandItem().getItem() != Items.TOTEM_OF_UNDYING) {
            toggle();
            return;
        }

        BlockHitResult hit = new BlockHitResult(
            Vec3.atCenterOf(anchorPos),
            Direction.UP,
            anchorPos,
            false
        );

        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        toggle();
    }

    private BlockPos findBestPlacementPos() {
        if (placeAtFeet.get() && mc.player.getXRot() > 60) {
            BlockPos feet = mc.player.blockPosition().below();
            if (canPlace(feet)) return feet;
        }

        BlockPos best = null;
        double bestDist = -1;
        BlockPos origin = mc.player.blockPosition();

        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                for (int y = -2; y <= 1; y++) {
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
        if (!mc.level.getBlockState(pos).isAir()) return false;
        BlockPos below = pos.below();
        return !mc.level.getBlockState(below).isAir();
    }

    private int findItem(net.minecraft.world.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).getItem() == item) return i;
        }
        return -1;
    }

    private void sendSilentRotation(float yaw, float pitch) {
        mc.getConnection().send(new ServerboundMovePlayerPacket.Rot(yaw, pitch, mc.player.onGround(), false));
    }
}