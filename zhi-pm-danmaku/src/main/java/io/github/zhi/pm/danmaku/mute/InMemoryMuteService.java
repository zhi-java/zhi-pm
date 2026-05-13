package io.github.zhi.pm.danmaku.mute;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryMuteService {
    private final ConcurrentMap<String, Set<String>> mutedUsersByRoom = new ConcurrentHashMap<>();

    public boolean isMuted(String roomId, String userId) {
        Set<String> muted = mutedUsersByRoom.get(roomId);
        return muted != null && muted.contains(userId);
    }

    public void mute(String roomId, String userId) {
        mutedUsersByRoom.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(userId);
    }

    public void unmute(String roomId, String userId) {
        Set<String> muted = mutedUsersByRoom.get(roomId);
        if (muted != null) {
            muted.remove(userId);
        }
    }

    public Set<String> getMutedUsers(String roomId) {
        Set<String> muted = mutedUsersByRoom.get(roomId);
        return muted == null ? Set.of() : Set.copyOf(muted);
    }
}
