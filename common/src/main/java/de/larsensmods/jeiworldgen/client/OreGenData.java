package de.larsensmods.jeiworldgen.client;

import de.larsensmods.jeiworldgen.util.CompareUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.RarityFilter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class OreGenData {

    public final Map<ResourceLocation, BiomeData> biomeData = new HashMap<>();

    public void addBiomeData(ResourceLocation biome, BiomeData data){
        this.biomeData.put(biome, data);
    }

    public void writeTo(FriendlyByteBuf byteBuf){
        byteBuf.writeInt(biomeData.size());
        for(Map.Entry<ResourceLocation, BiomeData> biomeEntry : biomeData.entrySet()){
            byteBuf.writeResourceLocation(biomeEntry.getKey());
            BiomeData data = biomeEntry.getValue();
            data.writeTo(byteBuf);
        }
    }

    public static OreGenData readFrom(FriendlyByteBuf byteBuf){
        OreGenData data = new OreGenData();

        int size = byteBuf.readInt();
        for(int i = 0; i < size; i++){
            ResourceLocation biome = byteBuf.readResourceLocation();
            BiomeData biomeData = BiomeData.readFrom(byteBuf);
            data.addBiomeData(biome, biomeData);
        }

        return data;
    }

    public static class BiomeData {

        public final Set<OreData> ores = new HashSet<>();

        public void addOreData(OreData data){
            this.ores.add(data);
        }

        public void writeTo(FriendlyByteBuf byteBuf){
            byteBuf.writeInt(ores.size());
            for(OreData data : ores){
                data.writeTo(byteBuf);
            }
        }

        public static BiomeData readFrom(FriendlyByteBuf byteBuf){
            BiomeData data = new BiomeData();

            int size = byteBuf.readInt();
            for (int i = 0; i < size; i++){
                data.addOreData(OreData.readFrom(byteBuf));
            }

            return data;
        }

    }

    public static class OreData {

        private final Set<ItemStack> targets;
        private final int size;
        private final CountPlacement countPlacement;
        private final RarityFilter rarityFilter;
        private final HeightRangePlacement heightRangePlacement;

        public OreData(Set<ItemStack> targets, int size, CountPlacement countPlacement, HeightRangePlacement heightRangePlacement){
            this.targets = targets;
            this.size = size;
            this.countPlacement = countPlacement;
            this.rarityFilter = null;
            this.heightRangePlacement = heightRangePlacement;
        }

        public OreData(Set<ItemStack> targets, int size, RarityFilter rarityFilter, HeightRangePlacement heightRangePlacement){
            this.targets = targets;
            this.size = size;
            this.countPlacement = null;
            this.rarityFilter = rarityFilter;
            this.heightRangePlacement = heightRangePlacement;
        }

        public Set<ItemStack> getTargets() {
            return targets;
        }

        public int getSize() {
            return size;
        }

        public CountPlacement getCountPlacement() {
            return countPlacement;
        }

        public RarityFilter getRarityFilter(){
            return rarityFilter;
        }

        public HeightRangePlacement getHeightRangePlacement() {
            return heightRangePlacement;
        }

        @Override
        public boolean equals(Object obj) {
            if(obj instanceof OreData other){
                return size == other.size
                        && CompareUtils.areItemStackSetsEqual(targets, other.targets)
                        && CompareUtils.countPlacementEquals(countPlacement, other.countPlacement)
                        && CompareUtils.rarityFilterEquals(rarityFilter, other.rarityFilter)
                        && CompareUtils.heightPlacementEquals(heightRangePlacement, other.heightRangePlacement);
            }
            return false;
        }

        public void writeTo(FriendlyByteBuf byteBuf){
            byteBuf.writeInt(targets.size());
            for(ItemStack target : targets){
                byteBuf.writeItem(target);
            }
            byteBuf.writeInt(size);
            if(countPlacement != null) {
                byteBuf.writeChar('c');
                byteBuf.writeJsonWithCodec(CountPlacement.CODEC, countPlacement);
            }else{
                byteBuf.writeChar('r');
                byteBuf.writeJsonWithCodec(RarityFilter.CODEC, rarityFilter);
            }
            byteBuf.writeJsonWithCodec(HeightRangePlacement.CODEC, heightRangePlacement);
        }

        public static OreData readFrom(FriendlyByteBuf byteBuf){
            int targetSize = byteBuf.readInt();
            Set<ItemStack> targets = new HashSet<>();
            for (int i = 0; i < targetSize; i++){
                targets.add(byteBuf.readItem());
            }
            int size = byteBuf.readInt();
            CountPlacement countPlacement = null;
            RarityFilter rarityFilter = null;
            char placementType = byteBuf.readChar();
            if(placementType == 'c'){
                countPlacement = byteBuf.readJsonWithCodec(CountPlacement.CODEC);
            }else if(placementType == 'r'){
                rarityFilter = byteBuf.readJsonWithCodec(RarityFilter.CODEC);
            }else{
                throw new IllegalStateException("Unknown placement type: " + placementType);
            }
            HeightRangePlacement heightRangePlacement = byteBuf.readJsonWithCodec(HeightRangePlacement.CODEC);
            return countPlacement != null
                    ? new OreData(targets, size, countPlacement, heightRangePlacement)
                    : new OreData(targets, size, rarityFilter, heightRangePlacement);
        }
    }

}
