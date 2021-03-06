package jackyy.avaritiatweaks.tweaks;

import jackyy.avaritiatweaks.client.Keys;
import jackyy.avaritiatweaks.config.ModConfig;
import jackyy.avaritiatweaks.packet.PacketHandler;
import jackyy.avaritiatweaks.packet.PacketToggleNoClip;
import morph.avaritia.handler.AvaritiaEventHandler;
import morph.avaritia.init.ModItems;
import morph.avaritia.item.ItemArmorInfinity;
import morph.avaritia.item.tools.ItemSwordInfinity;
import morph.avaritia.util.TextUtils;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModEventsHandler {

    private static final Map<EntityPlayer, Boolean> MAP = new HashMap<>();
    private static boolean noClip = false;

    @SubscribeEvent
    public void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        EntityPlayer player = event.getEntityPlayer();
        ItemStack stack = event.getItemStack();
        EnumFacing facing = event.getFace();
        Vec3d vec = event.getHitVec();
        if (!ModConfig.tweaks.makeWorldBreakerGreatAgain || facing == null || world.isRemote
                || stack == ItemStack.EMPTY || player.getHeldItemMainhand() == ItemStack.EMPTY || player.capabilities.isCreativeMode) {
            return;
        }
        if (state.getBlockHardness(world, pos) <= -1 && stack.getItem() == ModItems.infinity_pickaxe) {
            if (stack.getTagCompound() != null && stack.getTagCompound().getBoolean("hammer")) {
                ModItems.infinity_pickaxe.onBlockStartBreak(player.getHeldItemMainhand(), pos, player);
            } else {
                ItemStack drop = block.getPickBlock(state, new RayTraceResult(vec, facing), world, pos, player);
                event.getWorld().destroyBlock(pos, false);
                world.spawnEntity(new EntityItem(world, pos.getX(), pos.getY(), pos.getZ(), drop));
            }
        }
    }

    @SubscribeEvent
    public void armorTick(LivingEvent.LivingUpdateEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer)event.getEntityLiving();
            if (isArmorValid(player, EntityEquipmentSlot.HEAD)) {
                checkAndAddEffect(player, ModConfig.infinityArmor.infinityHelmetPotionEffects);
            }
            if (isArmorValid(player, EntityEquipmentSlot.CHEST)) {
                checkAndAddEffect(player, ModConfig.infinityArmor.infinityChestplatePotionEffects);
            }
            if (isArmorValid(player, EntityEquipmentSlot.LEGS)) {
                checkAndAddEffect(player, ModConfig.infinityArmor.infinityLeggingsPotionEffects);
            }
            if (isArmorValid(player, EntityEquipmentSlot.FEET)) {
                checkAndAddEffect(player, ModConfig.infinityArmor.infinityBootsPotionEffects);
            }
            if (isArmorValid(player, EntityEquipmentSlot.CHEST) && AvaritiaEventHandler.isInfinite(player) && noClip) {
                player.capabilities.isFlying = true;
                player.noClip = true;
            }
            if (!AvaritiaEventHandler.isInfinite(player) && !noClip) {
                player.noClip = false;
            }
            if (noClip && !player.capabilities.isFlying) {
                player.capabilities.isFlying = true;
            }
        }
    }

    private static boolean isArmorValid(EntityPlayer player, EntityEquipmentSlot slot) {
        ItemStack armor = player.getItemStackFromSlot(slot);
        return armor != ItemStack.EMPTY && armor.getItem() instanceof ItemArmorInfinity
                && armor.getTagCompound() != null && armor.getTagCompound().getInteger("enhanced") == 1;
    }

    private static void checkAndAddEffect(EntityPlayer player, String[] potions) {
        for (String potion : potions) {
            Potion effect = Potion.getPotionFromResourceLocation(potion);
            if (effect != null) {
                player.addPotionEffect(new PotionEffect(effect, 300, 0, false, false));
            }
        }
    }

    public static void toggleNoClip(EntityPlayer player) {
        if (isArmorValid(player, EntityEquipmentSlot.CHEST)) {
            if (MAP.containsKey(player) && MAP.get(player)) {
                MAP.remove(player);
                noClip = false;
                player.sendStatusMessage(new TextComponentTranslation("msg.avaritiatweaks.noclip.disabled"), true);
            } else {
                MAP.put(player, true);
                noClip = true;
                player.sendStatusMessage(new TextComponentTranslation("msg.avaritiatweaks.noclip.enabled"), true);
            }
        }
    }

    @SubscribeEvent @SideOnly(Side.CLIENT)
    public void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        Item item = stack.getItem();
        List<String> tooltip = event.getToolTip();
        if (item instanceof ItemSwordInfinity && ModConfig.tweaks.fixInfinitySwordTooltip) {
            for (int x = 0; x < tooltip.size(); x++) {
                if (tooltip.get(x).contains(I18n.format("attribute.name.generic.attackDamage"))) {
                    tooltip.set(x, " " + TextUtils.makeFabulous(I18n.format("tip.infinity")) + " "
                            + TextFormatting.GRAY + I18n.format("attribute.name.generic.attackDamage"));
                    return;
                }
            }
        }
        if (item == ModItems.infinity_chestplate && ModConfig.infinityArmor.infinityArmorNoClip
                && stack.getTagCompound() != null && stack.getTagCompound().getInteger("enhanced") == 1) {
            for (int x = 0; x < tooltip.size(); x++) {
                if (tooltip.get(x).contains(I18n.format("attribute.name.generic.armorToughness"))) {
                    tooltip.add(x - 1, MAP.containsKey(event.getEntityPlayer()) && MAP.get(event.getEntityPlayer())
                            ? I18n.format("tooltips.avaritiatweaks.noclip.enabled")
                            : I18n.format("tooltips.avaritiatweaks.noclip.disabled"));
                    tooltip.add(x, " ");
                    return;
                }
            }
        }
    }

    @SubscribeEvent @SideOnly(Side.CLIENT)
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (ModConfig.infinityArmor.infinityArmorNoClip) {
            if (Keys.NOCLIP.isPressed()) {
                PacketHandler.INSTANCE.sendToServer(new PacketToggleNoClip());
            }
        }
    }

}
