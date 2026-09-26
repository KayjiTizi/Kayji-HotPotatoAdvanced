package com.kayjifamily.hotpotatoadvanced.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public final class ItemSerializer {

    private ItemSerializer() {
    }

    public static String serialize(ItemStack stack) {
        if (stack == null) {
            return null;
        }
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
            dataOutput.writeObject(stack);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (IOException e) {
            return null;
        }
    }

    public static ItemStack deserialize(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
            Object obj = dataInput.readObject();
            if (obj instanceof ItemStack) {
                return (ItemStack) obj;
            }
        } catch (IOException | ClassNotFoundException ignored) {
        }
        return null;
    }

    public static List<String> serializeList(List<ItemStack> stacks) {
        List<String> result = new ArrayList<>();
        if (stacks == null) {
            return result;
        }
        for (ItemStack stack : stacks) {
            String serialized = serialize(stack);
            if (serialized != null) {
                result.add(serialized);
            }
        }
        return result;
    }

    public static List<ItemStack> deserializeList(List<String> data) {
        List<ItemStack> result = new ArrayList<>();
        if (data == null) {
            return result;
        }
        for (String entry : data) {
            ItemStack stack = deserialize(entry);
            if (stack != null && stack.getType().isItem()) {
                result.add(stack);
            }
        }
        return result;
    }
}
