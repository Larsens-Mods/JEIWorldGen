package de.larsensmods.jeiworldgen.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LootData {

    public final Map<Item, Set<BlockLootData>> lootData = new HashMap<>();

    public void addLootData(Item item, Set<BlockLootData> lootData){
        this.lootData.put(item, lootData);
    }

    public boolean knownBlock(Item item){
        return this.lootData.containsKey(item);
    }

    public Set<BlockLootData> dataForEntry(Item item){
        return this.lootData.getOrDefault(item, Set.of());
    }

    public void writeTo(FriendlyByteBuf byteBuf){
        byteBuf.writeInt(lootData.size());
        for(Map.Entry<Item, Set<BlockLootData>> entry : lootData.entrySet()){
            byteBuf.writeInt(Item.getId(entry.getKey()));
            byteBuf.writeInt(entry.getValue().size());
            for(BlockLootData data : entry.getValue()){
                data.writeTo(byteBuf);
            }
        }
    }

    public static LootData readFrom(FriendlyByteBuf byteBuf){
        LootData data = new LootData();

        int size = byteBuf.readInt();
        for(int i = 0; i < size; i++){
            Item key = Item.byId(byteBuf.readInt());
            int setSize = byteBuf.readInt();
            Set<BlockLootData> value = new HashSet<>();
            for(int j = 0; j < setSize; j++) {
                value.add(BlockLootData.readFrom(byteBuf));
            }
            data.lootData.put(key, value);
        }

        return data;
    }

    public static abstract class BlockLootData {

        abstract void writeTo(FriendlyByteBuf byteBuf);

        static BlockLootData readFrom(FriendlyByteBuf byteBuf){
            int type = byteBuf.readInt();
            return switch (type) {
                case 0 -> ItemDropData.readFrom(byteBuf);
                case 1 -> AlternativesLootData.readFrom(byteBuf);
                default -> throw new IllegalStateException("Unexpected type value: " + type);
            };
        }

    }

    public static class ItemDropData extends BlockLootData {

        public boolean affectedByFortune = false, silkTouchOnly = false;
        public int minCount = 1, maxCount = 1;
        public final ItemStack dropItem;

        public ItemDropData(ItemStack dropItem){
            this.dropItem = dropItem;
        }

        public ItemDropData(Item dropItem){
            this(dropItem.getDefaultInstance());
        }

        @Override
        void writeTo(FriendlyByteBuf byteBuf) {
            byteBuf.writeInt(0);
            byteBuf.writeJsonWithCodec(ItemStack.CODEC, dropItem);
            byteBuf.writeBoolean(affectedByFortune);
            byteBuf.writeBoolean(silkTouchOnly);
            byteBuf.writeInt(minCount);
            byteBuf.writeInt(maxCount);
        }

        static ItemDropData readFrom(FriendlyByteBuf byteBuf){
            ItemDropData data = new ItemDropData(byteBuf.readLenientJsonWithCodec(ItemStack.CODEC));
            data.affectedByFortune = byteBuf.readBoolean();
            data.silkTouchOnly = byteBuf.readBoolean();
            data.minCount = byteBuf.readInt();
            data.maxCount = byteBuf.readInt();
            return data;
        }
    }

    public static class AlternativesLootData extends BlockLootData {

        public final Set<BlockLootData> alternatives;

        public AlternativesLootData(Set<BlockLootData> alternatives){
            this.alternatives = alternatives;
        }

        @Override
        void writeTo(FriendlyByteBuf byteBuf) {
            byteBuf.writeInt(1);
            byteBuf.writeInt(alternatives.size());
            for(BlockLootData data : alternatives){
                data.writeTo(byteBuf);
            }
        }

        static AlternativesLootData readFrom(FriendlyByteBuf byteBuf){
            Set<BlockLootData> data = new HashSet<>();

            int size = byteBuf.readInt();
            for(int i = 0; i < size; i++){
                data.add(BlockLootData.readFrom(byteBuf));
            }
            return new AlternativesLootData(data);
        }
    }

}
