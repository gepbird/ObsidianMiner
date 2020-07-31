package com.gutyina70.obsidianminer;

import java.util.List;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.client.event.InputEvent.KeyInputEvent;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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
    	
    	/*
    	 * Feed the player from the offhand when it's hungry
    	 */
    	    	
    	ItemStack offhandItem = player.getHeldItemOffhand();
    	if(	IsFood(offhandItem) &&
			player.getFoodStats().getFoodLevel() < 8)
    	{
    		mc.playerController.processRightClick(player, mc.world, Hand.OFF_HAND);
    		return;
    	}
    	
    	/*
    	 * Stop when player is too hungry
    	 */
    	
    	if(player.getFoodStats().getFoodLevel() < 7)
    	{
    		TryWarningThePlayer("You are hungry, put some food in your offhand");
    		return;	
    	}
    	
    	/*
    	 * Stop when the player is below Y=60 for safety
    	 */
    	
    	Vector3d playerPos = player.getPositionVec();
    	if(playerPos.y < 60)
    	{
    		TryWarningThePlayer("You are below the safety area (Y=60)");
    		return;
    	}
    	
    	/*
    	 * Stop when inventory is full
    	 */
    	
    	if(player.inventory.getFirstEmptyStack() == -1)
    	{
    		TryWarningThePlayer("Inventory full");
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
        		TryWarningThePlayer("No near obsidian found");
            	return;
        	}
        	
        	miningBlock = blockPos;
    	}
    	
    	/*
    	 * Stop when the player doesn't have the right tool
    	 */
    	
		BlockState blockToBreak = mc.world.getBlockState(miningBlock);
    	ItemStack handItem = player.getHeldItemMainhand();
    	if(!handItem.canHarvestBlock(blockToBreak))
    	{
    		TryWarningThePlayer("You can't mine obsidian with this item");
    		return;
    	}
    	
    	/*
    	 * Stop when the player's tool is about to break
    	 */
    	
    	int handItemDamageLeft = handItem.getMaxDamage() - handItem.getDamage();
    	if(handItemDamageLeft <= 1)
    	{
    		TryWarningThePlayer("Your tool is about to break");
    		return;
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

	// Warn the player with sound and text
    void TryWarningThePlayer(String message)
    {
    	// Display action bar message
    	mc.ingameGUI.setOverlayMessage(new StringTextComponent("§c" + message), false);
    	
    	// Only run this every 5 seconds
    	if(ticks % 100 == 0)
    	{
    		// Play XP sound
    		ResourceLocation location = new ResourceLocation("minecraft", "entity.experience_orb.pickup");
        	SoundEvent event = new SoundEvent(location);
        	player.playSound(event, 1, 1);
    	}
    }
    
    boolean IsObsidian(BlockPos pos)
    {
    	return mc.world.getBlockState(pos).getBlock() == Blocks.OBSIDIAN;
    }
    
    boolean IsFood(ItemStack item)
    {
    	ItemGroup group = item.getItem().getGroup();
    	if(group == null)
    	{
    		return false;
    	}
    	
    	return group.getTabLabel().equals("food");
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