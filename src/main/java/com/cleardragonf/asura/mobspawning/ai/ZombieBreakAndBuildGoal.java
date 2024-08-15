package com.cleardragonf.asura.mobspawning.ai;

import com.cleardragonf.asura.HOB;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

//TODO:  Look at changing What type of block the zombie's use to place down.
//TODO:  Look at making a bunch of these things configurable...maybe even per night???
    /*TODO:  Possible Featuers:
                •  Healer Zombies - Heals entities within a specific Range of them
                •  Fire Zombies - Sets Everything around it on fire...till they die
                .  Zombie King - spawns new Zombies around it.
                •  Zombie Knights - spawn in Armour and have their own AI Attack Setups...
                •  Zombie heals by doing damage to you.
                •  Zombie can Steal Weapon
                •  Make distance a zombie can sense you configurable
     */



import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class ZombieBreakAndBuildGoal extends Goal {
    private final Zombie zombie;
    private final double speed;
    private BlockPos targetPos;
    private final MeleeAttackGoal meleeAttackGoal;

    private boolean isBuildingUp = false;
    private boolean isDiggingDown = false;
    private boolean isJumping = false;
    private int jumpTicks = 0;
    private final Map<BlockPos, Integer> blockBreakProgress = new HashMap<>();

    public ZombieBreakAndBuildGoal(Zombie zombie, double speed) {
        this.zombie = zombie;
        this.speed = speed;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        this.meleeAttackGoal = new MeleeAttackGoal(zombie, speed, true);
    }

    @Override
    public boolean canUse() {
        Vec3 playerPos = null;
        if (this.zombie.getCommandSenderWorld() instanceof ServerLevel world) {
            var nearestPlayer = world.getNearestPlayer(this.zombie, 30.0D);
            if (nearestPlayer != null) {
                playerPos = nearestPlayer.position();
            }
        }

        if (playerPos == null) {
            return false;
        }

        this.targetPos = new BlockPos((int) playerPos.x, (int) playerPos.y, (int) playerPos.z);
        return true;
    }

    @Override
    public void start() {
        // Initialize or reset anything if needed
    }

    @Override
    public void stop() {
        this.targetPos = null;
        this.isBuildingUp = false;
        this.isDiggingDown = false;
        this.isJumping = false;
        this.jumpTicks = 0;
        this.blockBreakProgress.clear();
    }

    @Override
    public void tick() {
        if (this.targetPos == null) {
            return;
        }

        Vec3 targetVec = Vec3.atCenterOf(this.targetPos);
        this.zombie.getLookControl().setLookAt(targetVec);

        BlockPos zombiePos = this.zombie.blockPosition();
        double distanceSquared = zombiePos.distSqr(this.targetPos);

        if (distanceSquared <= 1.5D) {
            this.meleeAttackGoal.start();
        } else {
            this.meleeAttackGoal.stop();

            if (distanceSquared <= 2.0D) {
                this.meleeAttackGoal.tick();
            } else if (zombiePos.getY() < this.targetPos.getY()) {
                if (!isBuildingUp) {
                    isBuildingUp = true;
                }
                buildUp();
            } else if (zombiePos.getY() > this.targetPos.getY()) {
                if (!isDiggingDown) {
                    isDiggingDown = true;
                }
                digDown();
            } else {
                if (isBuildingUp) {
                    isBuildingUp = false;
                }
                buildTowards();
            }

            if (!isBuildingUp && this.zombie.getNavigation().isDone()) {
                this.zombie.getNavigation().moveTo(targetVec.x, targetVec.y, targetVec.z, this.speed);
            } else {
            }
        }
    }

    private void buildTowards() {
        ServerLevel world = (ServerLevel) this.zombie.getCommandSenderWorld();
        BlockPos zombiePos = this.zombie.blockPosition();
        Direction direction = this.zombie.getDirection();

        BlockPos blockPosInFrontFoot = zombiePos.relative(direction);
        BlockPos blockPosInFrontEye = zombiePos.relative(direction).above((int) this.zombie.getEyeHeight());
        BlockPos blockPosInFrontBelow = blockPosInFrontFoot.below();
        BlockPos blockPosSide1 = zombiePos.relative(direction.getClockWise()).below();
        BlockPos blockPosSide2 = zombiePos.relative(direction.getCounterClockWise()).below();

        BlockPos blockPosFrontRightFoot = blockPosInFrontFoot.relative(direction.getClockWise());
        BlockPos blockPosFrontLeftFoot = blockPosInFrontFoot.relative(direction.getCounterClockWise());
        BlockPos blockPosFrontRightEye = blockPosInFrontEye.relative(direction.getClockWise());
        BlockPos blockPosFrontLeftEye = blockPosInFrontEye.relative(direction.getCounterClockWise());

        boolean shouldMove = true;

        if (!world.getBlockState(blockPosInFrontEye).isAir()) {
            Dig(blockPosInFrontEye);
            shouldMove = false;
        }
        if (!world.getBlockState(blockPosFrontRightEye).isAir()) {
            Dig(blockPosFrontRightEye);
            shouldMove = false;
        }
        if (!world.getBlockState(blockPosFrontLeftEye).isAir()) {
            Dig(blockPosFrontLeftEye);
            shouldMove = false;
        }

        if (!world.getBlockState(blockPosInFrontFoot).isAir()) {
            Dig(blockPosInFrontFoot);
            shouldMove = false;
        }
        if (!world.getBlockState(blockPosFrontRightFoot).isAir()) {
            Dig(blockPosFrontRightFoot);
            shouldMove = false;
        }
        if (!world.getBlockState(blockPosFrontLeftFoot).isAir()) {
            Dig(blockPosFrontLeftFoot);
            shouldMove = false;
        } else {
            if (world.getBlockState(blockPosInFrontBelow).isAir()) {
                world.setBlock(blockPosInFrontBelow, Blocks.DIRT.defaultBlockState(), 3);
                HOB.addBlockToHOBPlaced(blockPosInFrontBelow);
                this.zombie.getNavigation().stop();
                this.zombie.getNavigation().moveTo(blockPosInFrontBelow.getX(), blockPosInFrontBelow.getY(), blockPosInFrontBelow.getZ(), this.speed);
            } else {
                if (world.getBlockState(blockPosSide1).isAir()) {
                    world.setBlock(blockPosSide1, Blocks.DIRT.defaultBlockState(), 3);
                    HOB.addBlockToHOBPlaced(blockPosSide1);
                } else if (world.getBlockState(blockPosSide2).isAir()) {
                    world.setBlock(blockPosSide2, Blocks.DIRT.defaultBlockState(), 3);
                    HOB.addBlockToHOBPlaced(blockPosSide2);
                }
            }
        }

        if (shouldMove) {
            Vec3 targetVec = Vec3.atCenterOf(this.targetPos);
            this.zombie.getNavigation().moveTo(targetVec.x, targetVec.y, targetVec.z, this.speed);
        }
    }


    private void buildUp() {
        ServerLevel world = (ServerLevel) this.zombie.getCommandSenderWorld();
        BlockPos zombiePos = this.zombie.blockPosition();

        BlockPos blockAbove = zombiePos.above(1);
        BlockPos blockTwoAbove = zombiePos.above(2);
        BlockState blockAboveState = world.getBlockState(blockAbove);
        BlockState blockTwoAboveState = world.getBlockState(blockTwoAbove);

        if (!blockAboveState.isAir()) {
            Dig(blockAbove);
        } else if (!blockTwoAboveState.isAir()) {
            Dig(blockTwoAbove);
        } else if (blockAboveState.isAir() && blockTwoAboveState.isAir() && !isJumping) {
            isJumping = true;
            this.zombie.getJumpControl().jump();
        } else if (isJumping) {
            jumpTicks++;
            if (jumpTicks > 10) {
                BlockPos blockBelow = zombiePos;
                if (world.getBlockState(blockBelow).isAir()) {
                    world.setBlock(blockBelow, Blocks.DIRT.defaultBlockState(), 3);
                    HOB.addBlockToHOBPlaced(blockBelow);
                    isJumping = false;
                    jumpTicks = 0;
                }
            }
        }

        if (zombiePos.getY() >= this.targetPos.getY()) {
            this.zombie.getNavigation().moveTo(this.targetPos.getX(), this.targetPos.getY(), this.targetPos.getZ(), this.speed);
        } else {
            this.zombie.getNavigation().stop();
        }
    }

    private void digDown() {
        ServerLevel world = (ServerLevel) this.zombie.getCommandSenderWorld();
        BlockPos zombiePos = this.zombie.blockPosition();

        BlockPos blockBelow = zombiePos.below();

        if (this.zombie.onGround()) {
            if (!world.getBlockState(blockBelow).isAir()) {
                Dig(blockBelow);
            } else {
                this.zombie.getJumpControl().jump();
                this.zombie.moveTo(this.zombie.getX(), this.zombie.getY() - 1, this.zombie.getZ());
            }
        }

        if (zombiePos.getY() <= this.targetPos.getY()) {
            this.zombie.getNavigation().moveTo(this.targetPos.getX(), this.targetPos.getY(), this.targetPos.getZ(), this.speed);
        } else {
            this.zombie.getNavigation().stop();
        }
    }

    private void Dig(BlockPos blockPos) {
        ServerLevel world = (ServerLevel) this.zombie.getCommandSenderWorld();

        if (!world.getBlockState(blockPos).isAir()) {
            BlockState blockState = world.getBlockState(blockPos);
            float blockHardness = blockState.getDestroySpeed(world, blockPos);
            int progress = blockBreakProgress.getOrDefault(blockPos, 0);

            // Play digging sound
            world.playSound(null, blockPos, SoundEvents.BONE_BLOCK_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);

            progress += 1;

            if (progress >= (blockHardness * 10)) {
                world.destroyBlock(blockPos, true);
                blockBreakProgress.remove(blockPos);
            } else {
                blockBreakProgress.put(blockPos, progress);
            }
        }
    }

}
