package com.gutyina70.obsidianminer;

import java.util.List;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.util.text.TextComponent;
import net.minecraft.world.IEntityReader;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.InputEvent.KeyInputEvent;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
@Mod("obsidianminer")
public class ObsidianMinerMod
{
	final Minecraft mc = Minecraft.getInstance();
	ClientPlayerEntity player;
	Vector3d playerPos;
	BlockPos miningBlock;
	boolean enabled = false;
	int ticks = 0;
	int clientTicks = 0;
	
    public ObsidianMinerMod()
    {
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    @SubscribeEvent
    public void OnKeyInput(KeyInputEvent e)
    {
    	// When key 'F4' is down, toggle the mod
    	if(e.getKey() == 293 && e.getAction() != 1)
    	{
    		enabled = !enabled;
    	}
    }
    
	@SubscribeEvent
	public void OnInputUpdate(InputUpdateEvent e)
	{
		// When the mod is enabled, toggle sneaking
		if(enabled)
		{
			e.getMovementInput().sneaking = !e.getMovementInput().sneaking;
		}
	}
    
    @SubscribeEvent
    public void OnClientTick(TickEvent.ClientTickEvent e)
    {
    	// This event fires 40 times in a second
    	// so let's make it fire only 20 times in a second
    	clientTicks++;
    	if(clientTicks % 2 == 0)
    	{
    		ticks++;
    		return;
    	}
    	
    	player = mc.player;
    	// Only run when the player is in a world and the mod is enabled
    	if(	!enabled ||
			player == null)
    	{
    		return;
    	}
    	
    	Vector3d playerPos = player.getPositionVec();
    	// When the player is below the safety area (Y=60),
    	// don't do anything but remind the player about it
    	if(playerPos.y < 60)
    	{
    		TryPlayingReminderSound();
    		return;
    	}
    	
    	/*
    	 * Choose a block that will be mined and save it to miningBlock
    	 */
    	
    	// Should the miningBlock be recalculated?
		if(	miningBlock == null ||
			!IsObsidian(miningBlock) ||
			miningBlock.distanceSq(new BlockPos(playerPos)) > 3)
    	{
    		// Choose a new mining block
        	BlockPos blockPos = GetNearestObsidian();
        	// When no blocks found, don't do anything but remind the player about it
        	if(blockPos == null)
        	{
        		TryPlayingReminderSound();
            	return;
        	}
        	
        	miningBlock = blockPos;
    	}
    	
    	// Break the miningBlock
    	mc.playerController.onPlayerDamageBlock(miningBlock, Direction.UP);
    	
    	/*
    	 * Try to pick up dropped items
    	 */
    	
    	// Prevent falling off from the tower when the player is in the air 
    	if(!player.collidedVertically)
    	{
    		return;
    	}
    	
    	// Define the area to search for dropped items
    	AxisAlignedBB AABB = new AxisAlignedBB(
    			playerPos.x - 5, playerPos.y, playerPos.z - 5,
    			playerPos.x + 5, playerPos.y + 1, playerPos.z + 5);
    	// Get entities in that area
    	List<Entity> entities = mc.world.getEntitiesInAABBexcluding(player, AABB, null);
    	
    	// Iterate through every entity
    	for(int i = 0; i < entities.size(); i++)
    	{
    		// Only care about dropped items
    		Entity entity = entities.get(i);
    		if(entity instanceof ItemEntity)
    		{
    			ItemEntity item = (ItemEntity)entity;
    			Vector3d itemPos = item.getPositionVec();
    			
    			// Get the block position under the item 
    			BlockPos blockUnderItem = new BlockPos(itemPos).subtract(new Vector3i(0, 1, 0));
    			
    			// If the item is not on an obsidian then check the next entity
    			if(!IsObsidian(blockUnderItem))
    			{
    				continue;
    			}
    			
    			// Get the direction to the item from the player
    			// Start by getting the difference vector from the player and item 
    			Vector3d itemDirection = itemPos.subtract(playerPos);
    			// Ignore the Y value, since the player will only move horizontally
    			itemDirection = new Vector3d(itemDirection.x, 0, itemDirection.z);
    			// Normalize the vector, since we only care about the direction
    			itemDirection = itemDirection.normalize();
    			
    			// Prevent player falling off the tower
    			// by predicting its next step
				Vector3d playerNextStep = playerPos.add(itemDirection);
				BlockPos blockUnderNextStep = new BlockPos(playerNextStep).subtract(new Vector3i(0, 1, 0));
				if(!IsObsidian(blockUnderNextStep))
				{
					continue;
				}
    			
    			// Get a yaw value from the itemDirection vector in degrees
    			double itemYaw = itemDirection.x / -itemDirection.z;
    			itemYaw = Math.atan(itemYaw);
    			itemYaw = Math.toDegrees(itemYaw);
    			
    			// Since player.MoveRelative requires a direction vector
    			// that is relative to the player's fow,
    			// but we have the absolute direction vector,
    			// we need to do more math to get the relative vector
    			
    			// Get the player's yaw in degrees
    			double playerYaw = (double)player.getPitchYaw().y;
    			playerYaw %= 360;
    			
    			// Get the relative yaw
    			double lookYaw = playerYaw - itemYaw;
    			
    			// Get the direction vector from the yaw
				double lookX = Math.cos(lookYaw);
				double lookZ = Math.sin(-lookYaw);
				
				// When the item is too close to the player (player dropped the item in that tick),
				// the result of the calculation will be NaN and the player don't need to move
				// so go check the next item
				if(Double.isNaN(lookX))
				{
					continue;
				}
				
				// Move the player
    			Vector3d playerMoveVec = new Vector3d(lookX, 0, lookZ);
    			player.moveRelative(0.0294f, playerMoveVec);
    			
    			// Don't check other items
    			break;
    		}
    	}
    }

	// Play the exp pickup sound every 5 seconds
    void TryPlayingReminderSound()
    {
    	if(ticks % 100 == 0)
    	{
    		ResourceLocation location = new ResourceLocation("minecraft", "entity.experience_orb.pickup");
        	SoundEvent event = new SoundEvent(location);
        	player.playSound(event, 1, 1);
    	}
    }
    
    boolean IsObsidian(BlockPos pos)
    {
    	return mc.world.getBlockState(pos).getBlock().getRegistryName().toString().equals("minecraft:obsidian");
    }
    
    /* 
     * Gets the nearest obsidian to the player
     * Scanning from top to bottom
     */
    BlockPos GetNearestObsidian()
    {
    	BlockPos blockPos;
    	Vector3d playerPos = mc.player.getPositionVec();
    	int playerX = (int)playerPos.getX();
    	int playerY = (int)playerPos.getY();
    	int playerZ = (int)playerPos.getZ();
    	
    	int x = 0;
    	int y = 0;
    	int z = 0;
    	
    	for(y = 2; y > -2; y--)
    	{
    		for(int radius = 0; radius < 4; radius++)
    		{
    			for(x = -radius; x < radius; x++)
    			{
    				for(z = -radius; z < radius; z++)
        			{
            			blockPos = new BlockPos(playerX + x, playerY + y, playerZ + z);
            			if(IsObsidian(blockPos))
            			{
            				return blockPos;
            			}
        			}
    			}
    		}
    	}
    	
    	return null;
    }
    
}