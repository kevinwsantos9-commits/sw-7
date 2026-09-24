package com.shadowservants;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ServantData {
    private static final String ROOT = "ShadowServants";
    private static final String DEFEATS = "Defeats";
    private static final String UNLOCKED = "Unlocked";
    private static final String ACTIVE = "Active";

    private ServantData() {}

    private static CompoundTag root(ServerPlayer player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT)) persistent.put(ROOT, new CompoundTag());
        return persistent.getCompound(ROOT);
    }

    // CompoundTag não tem getStringList(); listas de String precisam ser lidas via
    // getList(key, Tag.TAG_STRING) e depois convertidas item a item.
    private static List<String> readStrings(CompoundTag r, String key) {
        List<String> out = new ArrayList<>();
        ListTag list = r.getList(key, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) out.add(list.getString(i));
        return out;
    }

    public static int defeats(ServerPlayer p) { return root(p).getInt(DEFEATS); }
    public static int unlocked(ServerPlayer p) { return root(p).getInt(UNLOCKED); }

    public static int registerDefeat(ServerPlayer p) {
        CompoundTag r = root(p);
        int defeats = r.getInt(DEFEATS) + 1;
        int unlocked = r.getInt(UNLOCKED);
        while (defeats >= 10) {
            defeats -= 10;
            unlocked++;
        }
        r.putInt(DEFEATS, defeats);
        r.putInt(UNLOCKED, unlocked);
        p.getPersistentData().put(ROOT, r);
        return unlocked;
    }

    public static List<UUID> active(ServerPlayer p) {
        CompoundTag r = root(p);
        List<UUID> list = new ArrayList<>();
        if (!r.contains(ACTIVE)) return list;
        for (String s : readStrings(r, ACTIVE)) {
            try { list.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
        }
        return list;
    }

    public static boolean canSummon(ServerPlayer p) {
        return active(p).size() < unlocked(p);
    }

    public static void addActive(ServerPlayer p, UUID uuid) {
        CompoundTag r = root(p);
        List<String> ids = readStrings(r, ACTIVE);
        String s = uuid.toString();
        if (!ids.contains(s)) ids.add(s);
        ListTag list = new ListTag();
        for (String id : ids) list.add(StringTag.valueOf(id));
        r.put(ACTIVE, list);
        p.getPersistentData().put(ROOT, r);
    }

    public static void removeActive(ServerPlayer p, UUID uuid) {
        CompoundTag r = root(p);
        ListTag list = new ListTag();
        for (String id : readStrings(r, ACTIVE)) {
            if (!id.equals(uuid.toString())) list.add(StringTag.valueOf(id));
        }
        r.put(ACTIVE, list);
        p.getPersistentData().put(ROOT, r);
    }

    public static void consumeOnDeath(ServerPlayer p, UUID uuid) {
        removeActive(p, uuid);
        CompoundTag r = root(p);
        int n = Math.max(0, r.getInt(UNLOCKED) - 1);
        r.putInt(UNLOCKED, n);
        p.getPersistentData().put(ROOT, r);
    }
}
