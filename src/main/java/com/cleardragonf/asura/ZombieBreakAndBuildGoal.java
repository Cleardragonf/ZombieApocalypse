package com.cleardragonf.asura;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class ZombieBreakAndBuildGoal extends Goal {
    private final Zombie zombie;
    private final double speed;
    private BlockPos targetPos;
    private final MeleeAttackGoal meleeAttackGoal;

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
            var nearestPlayer = world.getNearestPlayer(this.zombie, 30.0D); // Changed to 30 blocks
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
    }

    public void updatePathfinding() {
        // Example method to force pathfinding update or use a custom approach
        this.zombie.getNavigation().stop();
        Vec3 newTarget = Vec3.atCenterOf(this.targetPos);
        this.zombie.getNavigation().moveTo(newTarget.x, newTarget.y, newTarget.z, this.speed);
        System.out.println("Updated pathfinding to new target: " + newTarget);
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

        // Debug logs
        System.out.println("Current target position: " + this.targetPos);
        System.out.println("Current zombie position: " + zombiePos);
        System.out.println("Distance squared to target: " + distanceSquared);

        if (distanceSquared <= 1.5D) {
            this.meleeAttackGoal.start();
        } else {
            this.meleeAttackGoal.stop();

            if (distanceSquared <= 2.0D) {
                // This is within melee attack range
                this.meleeAttackGoal.tick();
            } else if (zombiePos.getY() < this.targetPos.getY()) {
                // buildUp();
            } else {
                buildTowards();
            }

            // Check if the zombie's navigation needs to be updated
            if (this.zombie.getNavigation().isDone()) {
                System.out.println("Zombie navigation is done. Recalculating path.");
                this.zombie.getNavigation().moveTo(targetVec.x, targetVec.y, targetVec.z, this.speed);
            } else {
                System.out.println("Zombie navigation is not done. Current position: " + this.zombie.blockPosition());
            }
        }
    }

    private void buildTowards() {
        ServerLevel world = (ServerLevel) this.zombie.getCommandSenderWorld();
        BlockPos zombiePos = this.zombie.blockPosition();
        Direction direction = this.zombie.getDirection();

        // Define block positions in front and around the zombie, but one block below
        BlockPos blockPosInFront = zombiePos.relative(direction).below(); // One block below
        BlockPos blockPosBelowInFront = blockPosInFront.below();
        BlockPos blockPosSide1 = zombiePos.relative(direction.getClockWise()).below();
        BlockPos blockPosSide2 = zombiePos.relative(direction.getCounterClockWise()).below();

        // Place the block in front if it's air
        if (world.getBlockState(blockPosInFront).isAir()) {
//            if (world.getBlockState(blockPosBelowInFront).isAir()) {
//                world.setBlock(blockPosBelowInFront, Blocks.DIRT.defaultBlockState(), 3);
//                System.out.println("Placed block below in front at: " + blockPosBelowInFront);
//            }
            world.setBlock(blockPosInFront, Blocks.DIRT.defaultBlockState(), 3);
            System.out.println("Placed block in front at: " + blockPosInFront);

            // Force pathfinding update
            this.zombie.getNavigation().stop();
            this.zombie.getNavigation().moveTo(blockPosInFront.getX(), blockPosInFront.getY(), blockPosInFront.getZ(), this.speed);
        } else {
            // If the block in front is not accessible, try to build at the sides
            if (world.getBlockState(blockPosSide1).isAir()) {
                world.setBlock(blockPosSide1, Blocks.DIRT.defaultBlockState(), 3);
                System.out.println("Placed block at side 1: " + blockPosSide1);
            } else if (world.getBlockState(blockPosSide2).isAir()) {
                world.setBlock(blockPosSide2, Blocks.DIRT.defaultBlockState(), 3);
                System.out.println("Placed block at side 2: " + blockPosSide2);
            }
        }
    }
}
