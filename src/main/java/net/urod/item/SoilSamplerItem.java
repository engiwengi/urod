package net.urod.item;

import com.google.common.collect.Maps;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.urod.block.UltraRichOreBlock;
import net.urod.config.URODConfigManager;

import java.util.Map;

public class SoilSamplerItem extends Item {
    private long lastUse;
    private long lastWarning;

    public SoilSamplerItem(Settings settings) {
        super(settings);
        lastUse = 0;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getPlayer() == null) {
            return ActionResult.PASS;
        }
        World world = context.getWorld();
        if (!world.isClient) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastUse < 10000) {
                if (currentTime - lastWarning > 2000) {
                    lastWarning = currentTime;
                    context.getPlayer().addChatMessage(new LiteralText("Sampler still recharging..."), false);
                }
                return ActionResult.PASS;
            }
            lastUse = currentTime;
            Map<Block, Integer> nearbyOres;
            nearbyOres = findNearbyOres(context.getBlockPos(), (ServerWorld) world);
            if (nearbyOres.isEmpty()) {
                context.getPlayer().addChatMessage(new LiteralText("No trace of ores"), true);
            } else {
                for (Map.Entry<Block, Integer> entry : nearbyOres.entrySet()) {
                    Text message;
                    if (URODConfigManager.getConfig().isEasySampler()) {
                        message = new LiteralText(String.format("%s found %s blocks away", entry.getKey().getName(), entry.getValue()));
                    } else {
                        message = new LiteralText(String.format("%s of %s found", remapDistanceToString(entry.getValue()), entry.getKey().getName()));
                    }
                    context.getPlayer().addChatMessage(message, true);
                }
            }
        }
        return ActionResult.SUCCESS;
    }

    private String remapDistanceToString(int i) {
        if (i < 32) {
            return "Large traces";
        } else if (i < 64) {
            return "Traces";
        } else if (i < 128) {
            return "Some traces";
        } else if (i < 256) {
            return "Slight traces";
        } else {
            return "Very slight traces";
        }
    }

    private Map<Block, Integer> findNearbyOres(BlockPos pos, ServerWorld world) {
        Map<Block, Integer> map = Maps.newHashMap();
        Box box = new Box(pos);
        box = box.expand(150, 128, 150);

        for (int i = (int) box.x1; i <= (int) box.x2; ++i) {
            for (int j = (int) box.y1; j <= (int) box.y2; ++j) {
                for (int k = (int) box.z1; k <= (int) box.z2; ++k) {
                    BlockPos blockPos = new BlockPos(i, j, k);
                    BlockState blockState = world.getBlockState(blockPos);
                    if (blockState.getBlock() instanceof UltraRichOreBlock) {
                        Integer distance = map.getOrDefault(blockState.getBlock(), Integer.MAX_VALUE);
                        Integer blockDistance = pos.getManhattanDistance(blockPos);
                        if (blockDistance < distance) {
                            map.put(blockState.getBlock(), blockDistance);
                        }
                    }
                }
            }
        }
        return map;
    }
}