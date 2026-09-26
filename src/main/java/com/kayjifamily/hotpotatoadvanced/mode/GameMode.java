package com.kayjifamily.hotpotatoadvanced.mode;

import org.bukkit.entity.Player;

public interface GameMode {
    void onStart(Player holder);
    void onTick(int time, Player holder);
    void onPass(Player from, Player to);
    void onEnd(Player lastHolder);
}
