package dev.welles.forestgrowth;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

//? if =26.2 {
/*import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
*///?}

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lets natural leaves drop seeds that sprout into saplings of the same kind nearby.
 * <p>
 * Called for every random tick of a leaves block. Only leaves that were not placed by a player
 * ({@code persistent=false}) and are still connected to a log take part.
 */
public final class SeedDispersal {
    /**
     * Seed chance per leaves random tick at {@code spreadSpeed=1.0}. A typical tree has about 60 leaves, which get
     * about 1000 random ticks per in-game day at the default {@code randomTickSpeed} of 3: one seed every ~4 days.
     */
    static final double BASE_SEED_CHANCE = 1.0 / 4000;

    /** How far a seed can fall below the leaves it came from. */
    private static final int MAX_FALL = 24;
    /** Saplings that only grow into a tree when planted as a 2x2 square. */
    private static final Set<String> SQUARE_SAPLINGS = Set.of("minecraft:dark_oak_sapling", "minecraft:pale_oak_sapling");

    //? if =26.2 {
    /*// BlockTags.SAPLINGS is missing in 26.2, the tag itself still exists
    private static final TagKey<Block> SAPLING_TAG = TagKey.create(Registries.BLOCK, Identifier.withDefaultNamespace("saplings"));
    *///?} else
    private static final TagKey<Block> SAPLING_TAG = BlockTags.SAPLINGS;

    private static final Map<Block, Optional<Block>> SAPLING_CACHE = new ConcurrentHashMap<>();
    private static volatile Map<String, Block> blocksById;

    private SeedDispersal() {}

    public static void onRandomTick(BlockState leaves, ServerLevel level, BlockPos pos, RandomSource random) {
        GrowthConfig config = GrowthConfig.get();
        if (random.nextDouble() >= config.seedChance()) return;
        if (leaves.getValue(LeavesBlock.PERSISTENT) || leaves.getValue(LeavesBlock.DISTANCE) >= LeavesBlock.DECAY_DISTANCE) return;

        Block sapling = saplingFor(leaves.getBlock(), config);
        if (sapling == null) return;

        int radius = config.spreadRadius;
        BlockPos landing = findLanding(level, pos.offset(
            random.nextIntBetweenInclusive(-radius, radius), 2, random.nextIntBetweenInclusive(-radius, radius)));
        if (landing == null) return;

        String saplingId = BuiltInRegistries.BLOCK.getKey(sapling).toString();
        List<BlockPos> spots = SQUARE_SAPLINGS.contains(saplingId)
            ? findSquare(level, landing, sapling.defaultBlockState(), config, random)
            : findSingle(level, landing, sapling.defaultBlockState(), config);
        if (spots == null) return;

        for (BlockPos spot : spots) level.setBlock(spot, sapling.defaultBlockState(), Block.UPDATE_ALL);
        if (config.logPlanting) {
            ForestGrowth.LOGGER.info("Planted {} at {} (seed from {})", saplingId, spots.getFirst().toShortString(),
                pos.toShortString());
        }
    }

    /** Forgets which sapling belongs to which leaves, e.g. after the config changed. */
    public static void clearCache() {
        SAPLING_CACHE.clear();
    }

    /** Lets a seed fall down from {@code start}; returns where it comes to rest, or {@code null} if it got stuck. */
    private static BlockPos findLanding(ServerLevel level, BlockPos start) {
        if (!level.isLoaded(start) || !isOpen(level.getBlockState(start))) return null;

        BlockPos.MutableBlockPos cursor = start.mutable();
        for (int fallen = 0; fallen < MAX_FALL; fallen++) {
            BlockState state = level.getBlockState(cursor.move(Direction.DOWN));
            if (isOpen(state)) continue;
            // Caught in a canopy or fell into water
            if (state.is(BlockTags.LEAVES) || !state.getFluidState().isEmpty()) return null;
            return cursor.above().immutable();
        }
        return null;
    }

    private static List<BlockPos> findSingle(ServerLevel level, BlockPos spot, BlockState sapling, GrowthConfig config) {
        if (!canSprout(level, spot, sapling, config)) return null;
        if (!hasRoom(level, spot, spot, config.minSpacing)) return null;
        return List.of(spot);
    }

    /** Finds a 2x2 square containing {@code landing} where all four saplings can sprout. */
    private static List<BlockPos> findSquare(ServerLevel level, BlockPos landing, BlockState sapling, GrowthConfig config,
                                             RandomSource random) {
        int first = random.nextInt(4);
        for (int i = 0; i < 4; i++) {
            int corner = (first + i) % 4;
            BlockPos min = landing.offset((corner & 1) == 0 ? 0 : -1, 0, (corner & 2) == 0 ? 0 : -1);
            List<BlockPos> square = List.of(min, min.east(), min.south(), min.south().east());
            if (square.stream().allMatch(spot -> canSprout(level, spot, sapling, config))
                && hasRoom(level, min, min.south().east(), config.minSpacing)) {
                return square;
            }
        }
        return null;
    }

    private static boolean canSprout(ServerLevel level, BlockPos spot, BlockState sapling, GrowthConfig config) {
        return level.isLoaded(spot)
            && level.getWorldBorder().isWithinBounds(spot)
            && isOpen(level.getBlockState(spot))
            // Farmland is for crops, not for trees
            && !level.getBlockState(spot.below()).is(Blocks.FARMLAND)
            && sapling.canSurvive(level, spot)
            && level.getRawBrightness(spot, 0) >= config.minLight;
    }

    /** Whether there are no logs or saplings within {@code spacing} blocks around the area from {@code min} to {@code max}. */
    private static boolean hasRoom(ServerLevel level, BlockPos min, BlockPos max, int spacing) {
        for (BlockPos pos : BlockPos.betweenClosed(min.offset(-spacing, -1, -spacing), max.offset(spacing, 3, spacing))) {
            if (!level.isLoaded(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.LOGS) || state.is(SAPLING_TAG)) return false;
        }
        return true;
    }

    /** Air or something a seed can push aside, like grass or a thin snow layer, but not water. */
    private static boolean isOpen(BlockState state) {
        return (state.isAir() || state.canBeReplaced()) && state.getFluidState().isEmpty();
    }

    private static Block saplingFor(Block leaves, GrowthConfig config) {
        return SAPLING_CACHE.computeIfAbsent(leaves, block -> Optional.ofNullable(resolveSapling(block, config))).orElse(null);
    }

    /**
     * Finds the sapling for a leaves block by its id: configured pairs first, then {@code <name>_sapling},
     * {@code <name>_propagule} (mangrove) and {@code <name>} if that is a sapling (azalea).
     */
    private static Block resolveSapling(Block leaves, GrowthConfig config) {
        String leavesId = BuiltInRegistries.BLOCK.getKey(leaves).toString();
        if (config.isExcluded(leavesId)) return null;

        Map<String, Block> blocks = blocksById();
        List<String> candidates = new ArrayList<>();
        String paired = config.pairedSapling(leavesId);
        if (paired != null) candidates.add(paired);
        if (leavesId.endsWith("_leaves")) {
            String name = leavesId.substring(0, leavesId.length() - "_leaves".length());
            candidates.add(name + "_sapling");
            candidates.add(name + "_propagule");
            Block bare = blocks.get(name);
            if (bare != null && bare.defaultBlockState().is(SAPLING_TAG)) candidates.add(name);
        }

        for (String candidate : candidates) {
            Block sapling = blocks.get(candidate);
            if (sapling == null) continue;
            if (config.isExcluded(candidate)) return null;
            if (config.logPlanting) ForestGrowth.LOGGER.info("{} spreads as {}", leavesId, candidate);
            return sapling;
        }
        if (paired != null) ForestGrowth.LOGGER.warn("Unknown sapling in additionalPairs: {}", paired);
        return null;
    }

    private static Map<String, Block> blocksById() {
        Map<String, Block> blocks = blocksById;
        if (blocks == null) {
            blocks = new HashMap<>();
            for (Block block : BuiltInRegistries.BLOCK) blocks.put(BuiltInRegistries.BLOCK.getKey(block).toString(), block);
            blocksById = blocks;
        }
        return blocks;
    }
}
