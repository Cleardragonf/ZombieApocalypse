package com.cleardragonf.asura;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;


//TODO:  Create A Dig Down Method.
//TODO:  Update BuildUP() to include a Break Blocks...if Above and continue.

import java.util.EnumSet;

public class ZombieBreakAndBuildGoal extends Goal {
    private final Zombie zombie;
    private final double speed;
    private BlockPos targetPos;
    private final MeleeAttackGoal meleeAttackGoal;

    private boolean isBuildingUp = false;
    private boolean isDiggingDown = false;
    private boolean isJumping = false;
    private int jumpTicks = 0;

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
                // This is within melee attack range
                this.meleeAttackGoal.tick();
            } else if (zombiePos.getY() < this.targetPos.getY()) {
                if (!isBuildingUp) {
                    isBuildingUp = true;
                }
                buildUp();
            } else if(zombiePos.getY() > this.targetPos.getY()){
                if(!isDiggingDown){
                    isDiggingDown = true;
                }
                digDown();
            }else {
                if (isBuildingUp) {
                    isBuildingUp = false;
                }
                buildTowards();
            }

            // Check if the zombie's navigation needs to be updated
            if (!isBuildingUp && this.zombie.getNavigation().isDone()) {
                this.zombie.getNavigation().moveTo(targetVec.x, targetVec.y, targetVec.z, this.speed);
            } else {
                System.out.println("Zombie navigation is not done or building up. Current position: " + this.zombie.blockPosition());
            }
        }
    }

    private void buildTowards() {
        ServerLevel world = (ServerLevel) this.zombie.getCommandSenderWorld();
        BlockPos zombiePos = this.zombie.blockPosition();
        var direction = this.zombie.getDirection();

        // Define block positions in front and around the zombie, but one block below
        var blockPosInFront = zombiePos.relative(direction).below(); // One block below
        var blockPosSide1 = zombiePos.relative(direction.getClockWise()).below();
        var blockPosSide2 = zombiePos.relative(direction.getCounterClockWise()).below();

        // Place the block in front if it's air
        if (world.getBlockState(blockPosInFront).isAir()) {
            world.setBlock(blockPosInFront, Blocks.DIRT.defaultBlockState(), 3);

            // Force pathfinding update
            this.zombie.getNavigation().stop();
            this.zombie.getNavigation().moveTo(blockPosInFront.getX(), blockPosInFront.getY(), blockPosInFront.getZ(), this.speed);
        } else {
            // If the block in front is not accessible, try to build at the sides
            if (world.getBlockState(blockPosSide1).isAir()) {
                world.setBlock(blockPosSide1, Blocks.DIRT.defaultBlockState(), 3);
            } else if (world.getBlockState(blockPosSide2).isAir()) {
                world.setBlock(blockPosSide2, Blocks.DIRT.defaultBlockState(), 3);
            }
        }
    }

    private void buildUp() {
        ServerLevel world = (ServerLevel) this.zombie.getCommandSenderWorld();
        BlockPos zombiePos = this.zombie.blockPosition();

        // Check if the space above the zombie is air
        BlockPos blockAbove = zombiePos.above(1);
        BlockPos blockTwoAbove = zombiePos.above(2);
        BlockState blockAboveState = world.getBlockState(blockAbove);
        BlockState blockTwoAboveState = world.getBlockState(blockTwoAbove);

        if (!blockAboveState.isAir()) {
            // If the block directly above is not air, break it
            Dig(blockAbove);
        }

        if (!blockTwoAboveState.isAir()) {
            // If the block two blocks above is not air, break it
            Dig(blockTwoAbove);
        }

        // Check if the zombie is on the ground
        boolean onGround = this.zombie.onGround();

        if (onGround) {
            // If the zombie is on the ground, make it jump
            if (!isJumping) {
                this.zombie.getJumpControl().jump();
                isJumping = true;
                jumpTicks = 0;
            } else {
                jumpTicks++;
                // After a short delay (to ensure the zombie is in the air), place the block
                if (jumpTicks > 10) {
                    BlockPos blockBelow = zombiePos;
                    if (world.getBlockState(blockBelow).isAir()) {
                        world.setBlock(blockBelow, Blocks.DIRT.defaultBlockState(), 3);
                        isJumping = false;
                    }
                }
            }
        }

        // Pause pathfinding until the zombie reaches the target's Y level
        if (zombiePos.getY() >= this.targetPos.getY()) {
            this.zombie.getNavigation().moveTo(this.targetPos.getX(), this.targetPos.getY(), this.targetPos.getZ(), this.speed);
        } else {
            this.zombie.getNavigation().stop();
        }
    }

    private void digDown(){
        ServerLevel world = (ServerLevel) this.zombie.getCommandSenderWorld();
        BlockPos zombiePos = this.zombie.blockPosition();

        // Check if the space above the zombie is air
        BlockPos blockBelow = zombiePos.below();


        // Check if the zombie is on the ground
        boolean onGround = this.zombie.onGround();

        if (onGround) {
            if (!world.getBlockState(blockBelow).isAir()) {
                Dig(blockBelow);
            }
        }

        // Pause pathfinding until the zombie reaches the target's Y level
        if (zombiePos.getY() <= this.targetPos.getY()) {
            this.zombie.getNavigation().moveTo(this.targetPos.getX(), this.targetPos.getY(), this.targetPos.getZ(), this.speed);
        } else {
            this.zombie.getNavigation().stop();
        }
    }

    private void Dig(BlockPos blockPos) {
        ServerLevel world = (ServerLevel) this.zombie.getCommandSenderWorld();

        if (!world.getBlockState(blockPos).isAir()) {
            world.destroyBlock(blockPos, true);
            System.out.println("Broke block at: " + blockPos);
        }
    }
}